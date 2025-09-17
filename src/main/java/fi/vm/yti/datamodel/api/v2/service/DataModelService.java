package fi.vm.yti.datamodel.api.v2.service;

import fi.vm.yti.common.enums.GraphType;
import fi.vm.yti.common.enums.Status;
import fi.vm.yti.common.exception.MappingError;
import fi.vm.yti.common.exception.ResourceNotFoundException;
import fi.vm.yti.common.properties.SuomiMeta;
import fi.vm.yti.common.service.AuditService;
import fi.vm.yti.common.service.GroupManagementService;
import fi.vm.yti.common.util.MapperUtils;
import fi.vm.yti.datamodel.api.v2.dto.*;
import fi.vm.yti.datamodel.api.v2.mapper.ModelMapper;
import fi.vm.yti.datamodel.api.v2.mapper.ResourceMapper;
import fi.vm.yti.datamodel.api.v2.opensearch.index.IndexResource;
import fi.vm.yti.datamodel.api.v2.repository.CoreRepository;
import fi.vm.yti.datamodel.api.v2.security.DataModelAuthorizationManager;
import fi.vm.yti.datamodel.api.v2.utils.DataModelMapperUtils;
import fi.vm.yti.datamodel.api.v2.utils.DataModelURI;
import fi.vm.yti.datamodel.api.v2.utils.DataModelUtils;
import fi.vm.yti.datamodel.api.v2.utils.SemVer;
import fi.vm.yti.datamodel.api.v2.validator.ValidationConstants;
import fi.vm.yti.security.AuthenticatedUserProvider;
import fi.vm.yti.security.Role;
import org.apache.jena.arq.querybuilder.ConstructBuilder;
import org.apache.jena.arq.querybuilder.SelectBuilder;
import org.apache.jena.arq.querybuilder.UpdateBuilder;
import org.apache.jena.arq.querybuilder.WhereBuilder;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.rdf.model.*;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.vocabulary.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.topbraid.shacl.vocabulary.SH;

import java.io.StringWriter;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static fi.vm.yti.security.AuthorizationException.check;

@Service
public class DataModelService {

    private final AuditService auditService = new AuditService("DATAMODEL");
    private final Logger logger = LoggerFactory.getLogger(DataModelService.class);

    private final CoreRepository coreRepository;
    private final DataModelAuthorizationManager authorizationManager;
    private final GroupManagementService groupManagementService;
    private final ModelMapper mapper;
    private final TerminologyService terminologyService;
    private final VisualizationService visualisationService;
    private final CodeListService codeListService;
    private final IndexService indexService;
    private final AuthenticatedUserProvider userProvider;
    private final DataModelSubscriptionService dataModelSubscriptionService;

    @Autowired
    public DataModelService(CoreRepository coreRepository,
                            DataModelAuthorizationManager authorizationManager,
                            GroupManagementService groupManagementService,
                            ModelMapper modelMapper,
                            TerminologyService terminologyService,
                            VisualizationService visualizationService,
                            CodeListService codeListService,
                            IndexService indexService,
                            AuthenticatedUserProvider userProvider, DataModelSubscriptionService dataModelSubscriptionService) {
        this.coreRepository = coreRepository;
        this.authorizationManager = authorizationManager;
        this.groupManagementService = groupManagementService;
        this.mapper = modelMapper;
        this.terminologyService = terminologyService;
        this.visualisationService = visualizationService;
        this.codeListService = codeListService;
        this.indexService = indexService;
        this.userProvider = userProvider;
        this.dataModelSubscriptionService = dataModelSubscriptionService;
    }

    public DataModelInfoDTO getDraft(String prefix) {
        var uri = DataModelURI.Factory.createModelURI(prefix);
        var model = coreRepository.fetch(uri.getGraphURI());
        check(authorizationManager.hasRightToModel(prefix, model, true));
        return mapper.mapToDataModelDTO(prefix, model, groupManagementService.mapUser());
    }

    public DataModelInfoDTO get(String prefix, String version) {
        var uri = DataModelURI.Factory.createModelURI(prefix, version);
        if (version == null) {
            version = getLatestVersion(uri);
        }
        var model = coreRepository.fetch(DataModelURI.Factory.createModelURI(prefix, version).getGraphURI());
        var hasRightsToModel = authorizationManager.hasRightToModel(prefix, model);

        var userMapper = hasRightsToModel ? groupManagementService.mapUser() : null;
        return mapper.mapToDataModelDTO(prefix, model, userMapper);
    }

    public String getLatestVersion(DataModelURI uri) {
        String version;
        var g = NodeFactory.createURI(uri.getGraphURI());
        var builder = new SelectBuilder().addWhere(
                new WhereBuilder().addGraph(g, g, OWL.priorVersion, "?version"));
        var versionIRI = new ArrayList<String>();
        coreRepository.querySelect(builder.build(),
                result -> versionIRI.add(result.get("version").toString()));

        if (versionIRI.isEmpty()) {
            throw new ResourceNotFoundException(uri.getModelId());
        }
        version = DataModelURI.Factory.fromURI(versionIRI.get(0)).getVersion();
        return version;
    }

    public URI create(DataModelDTO dto, GraphType modelType) throws URISyntaxException {
        check(authorizationManager.hasRightToAnyOrganization(dto.getOrganizations(), Role.DATA_MODEL_EDITOR));
        var graphUri = DataModelURI.Factory.createModelURI(dto.getPrefix()).getGraphURI();

        terminologyService.resolveTerminology(dto.getTerminologies());
        codeListService.resolveCodelistScheme(dto.getCodeLists());
        var jenaModel = mapper.mapToJenaModel(dto, modelType, userProvider.getUser());
        addPrefixes(jenaModel, dto);

        coreRepository.put(graphUri, jenaModel);

        var indexModel = mapper.mapToIndexModel(graphUri, jenaModel);
        indexService.createModelToIndex(indexModel);
        auditService.log(AuditService.ActionType.CREATE, graphUri, userProvider.getUser());
        return new URI(graphUri);
    }

    public void update(String prefix, DataModelDTO dto) {
        var graphUri = DataModelURI.Factory.createModelURI(prefix).getGraphURI();
        var model = coreRepository.fetch(graphUri);

        check(authorizationManager.hasRightToModel(prefix, model));

        terminologyService.resolveTerminology(dto.getTerminologies());
        codeListService.resolveCodelistScheme(dto.getCodeLists());

        var modelResource = model.getResource(graphUri);

        var oldNamespaces = DataModelUtils.getInternalReferenceModels(modelResource).stream()
                .map(DataModelURI.Factory::fromURI)
                .toList();
        var newNamespaces = dto.getInternalNamespaces().stream()
                .map(DataModelURI.Factory::fromURI)
                .toList();

        mapper.mapToUpdateJenaModel(graphUri, dto, model, userProvider.getUser());

        // check if dependency model with references is removed,
        // removing is not allowed if there are any references to the namespace being removed
        handleNamespaceRemove(model, newNamespaces, oldNamespaces);

        addPrefixes(model, dto);

        coreRepository.put(graphUri, model);

        var indexModel = mapper.mapToIndexModel(graphUri, model);
        indexService.updateModelToIndex(indexModel);
        auditService.log(AuditService.ActionType.UPDATE, graphUri, userProvider.getUser());
    }

    public void delete(String prefix, String version) {
        var dataModelURI = DataModelURI.Factory.createModelURI(prefix, version);
        var graphUri = dataModelURI.getGraphURI();

        var model = coreRepository.fetch(graphUri);

        if (!coreRepository.graphExists(graphUri)) {
            throw new ResourceNotFoundException(graphUri);
        }

        check(authorizationManager.hasAdminRightToModel(prefix, model));

        var modelResource = model.getResource(dataModelURI.getModelURI());
        var priorVersion = MapperUtils.propertyToString(modelResource, OWL.priorVersion);
        var status = MapperUtils.getStatus(modelResource);
        var superUser = userProvider.getUser().isSuperuser();
        var referrers = getReferrerModels(graphUri);

        if (!superUser) {
            if (Status.VALID.equals(status)) {
                throw new MappingError("Cannot remove data model with status VALID");
            } else if (List.of(Status.RETIRED, Status.SUPERSEDED).contains(status) && !referrers.isEmpty()) {
                throw new MappingError("Cannot remove data model with references: " + referrers);
            }
        }

        coreRepository.delete(graphUri);

        // ensure the integrity of owl:priorVersion information
        // if removing draft version without any published versions, also notification topic will be removed
        if (version != null) {
            updatePriorVersions(graphUri, priorVersion);
        } else if (priorVersion == null) {
            dataModelSubscriptionService.deleteTopic(prefix);
        }

        indexService.deleteModelFromIndex(graphUri);
        indexService.removeResourceIndexesByDataModel(dataModelURI.getModelURI(), version);
        auditService.log(AuditService.ActionType.DELETE, graphUri, userProvider.getUser());
    }

    public boolean exists(String prefix) {
        if (ValidationConstants.RESERVED_WORDS.contains(prefix)) {
            return true;
        }
        return coreRepository.graphExists(DataModelURI.Factory.createModelURI(prefix).getModelURI());
    }

    public ResponseEntity<String> export(String prefix, String version, String accept, boolean showAsFile, String language) {
        var uri = DataModelURI.Factory.createModelURI(prefix, version);

        Model exportedModel;
        Model model;

        try {
            model = coreRepository.fetch(uri.getGraphURI());
        } catch (ResourceNotFoundException e) {
            // cannot throw ResourceNotFoundException because accept header is not application/json
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            logger.error("Error exporting datamodel", e);
            return ResponseEntity.internalServerError().build();
        }

        var hasRights = authorizationManager.hasRightToModel(prefix, model);

        // if no version is defined, return only metadata of the draft version
        if (version == null && !hasRights) {
            var modelResource = model.getResource(uri.getModelURI());
            exportedModel = modelResource.listProperties().toModel();
        } else {
            exportedModel = model;
        }

        DataModelUtils.addPrefixesToModel(uri.getGraphURI(), exportedModel);

        // remove editorial notes from resources
        if (!hasRights) {
            var hiddenValues = exportedModel.listStatements(
                    new SimpleSelector(null, SKOS.editorialNote, (String) null)).toList();
            exportedModel.remove(hiddenValues);
        }

        var stringWriter = new StringWriter();
        var fileExtension = ".jsonld";
        switch (accept) {
            case "text/turtle":
                fileExtension = ".ttl";
                RDFDataMgr.write(stringWriter, exportedModel, Lang.TURTLE);
                break;
            case "application/rdf+xml":
                fileExtension = ".rdf";
                RDFDataMgr.write(stringWriter, exportedModel, Lang.RDFXML);
                break;
            case "application/vnd+oai+openapi+json":
                fileExtension = ".json";
                OpenAPIBuilder.export(stringWriter, exportedModel, language);
                break;
            case "application/schema+json":
                fileExtension = ".json";
                JSONSchemaBuilder.export(stringWriter, exportedModel, language);
                break;
            case "application/ld+json":
            default:
                RDFDataMgr.write(stringWriter, exportedModel, Lang.JSONLD);
        }

        var contentDisposition = showAsFile
                ? "attachment; filename=" + uri.getModelId() + fileExtension
                : "inline";

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, contentDisposition)
                .body(stringWriter.toString());
    }

    public URI createRelease(String prefix, String version, Status status) throws URISyntaxException {
        var modelVersionURI = DataModelURI.Factory.createModelURI(prefix, version);
        var modelUri = modelVersionURI.getModelURI();
        var graphUri = modelVersionURI.getGraphURI();
        if(!status.equals(Status.SUGGESTED) && !status.equals(Status.VALID)){
            throw new MappingError("Status has to be SUGGESTED or VALID");
        }

        if(!version.matches(SemVer.VALID_REGEX)){
            throw new MappingError("Not valid Semantic version string");
        }

        //This gets the current "DRAFT" version
        var model = coreRepository.fetch(modelVersionURI.getDraftGraphURI());
        check(authorizationManager.hasRightToModel(prefix, model));
        var priorVersionUri = MapperUtils.propertyToString(model.getResource(modelUri), OWL.priorVersion);
        if(priorVersionUri != null){
            var priorVersion = DataModelURI.Factory.fromURI(priorVersionUri).getVersion();
            var result = SemVer.compareSemVers(priorVersion, version);
            if(result == 0){
                throw new MappingError("Same version number");
            }
            if(result > 0) {
                throw new MappingError("Older version given");
            }
        }

        var newDraft = ModelFactory.createDefaultModel().add(model);

        mapper.mapReleaseProperties(model, modelVersionURI, status);
        //Map new newest release to draft model
        mapper.mapPriorVersion(newDraft, modelUri, graphUri);
        coreRepository.put(modelVersionURI.getDraftGraphURI(), newDraft);
        coreRepository.put(modelVersionURI.getGraphURI(), model);

        updateDraftReferences(modelUri, version);

        var newVersion = mapper.mapToIndexModel(modelUri, model);
        indexService.createModelToIndex(newVersion);
        var list = new ArrayList<IndexResource>();
        var resources = model.listSubjectsWithProperty(RDF.type, OWL.Class)
                .andThen(model.listSubjectsWithProperty(RDF.type, OWL.ObjectProperty))
                .andThen(model.listSubjectsWithProperty(RDF.type, OWL.DatatypeProperty))
                .andThen(model.listSubjectsWithProperty(RDF.type, SH.NodeShape))
                .filterDrop(RDFNode::isAnon);
        resources.forEach(resource -> list.add(ResourceMapper.mapToIndexResource(model, resource.getURI())));
        indexService.bulkInsert(IndexService.OPEN_SEARCH_INDEX_RESOURCE, list);
        visualisationService.saveVersionedPositions(prefix, version);
        auditService.log(AuditService.ActionType.CREATE, modelVersionURI.getGraphURI(), userProvider.getUser());

        sendNotification(model, modelVersionURI);
        //Draft model does not need to be indexed since opensearch specific properties on it did not change
        return new URI(modelVersionURI.getGraphURI());
    }

    public List<ModelVersionInfo> getPriorVersions(String prefix, String version){

        var modelUri = DataModelURI.Factory.createModelURI(prefix);
        //Would be nice to do traverse the graphs using SPARQL starting from the correct versionIRI but currently that traversing named graphs is not supported
        var constructBuilder = new ConstructBuilder()
                .addConstruct("?g", OWL2.versionInfo, "?versionInfo")
                .addConstruct("?g", OWL2.versionIRI, "?versionIRI")
                .addConstruct("?g", RDFS.label, "?label")
                .addConstruct("?g", SuomiMeta.publicationStatus, "?publicationStatus");
        var uri = NodeFactory.createURI(modelUri.getModelURI());
        var whereBuilder = new WhereBuilder()
                .addWhere(uri, OWL2.versionInfo, "?versionInfo")
                .addWhere(uri, OWL2.versionIRI, "?versionIRI")
                .addWhere(uri, RDFS.label, "?label")
                .addWhere(uri, SuomiMeta.publicationStatus, "?publicationStatus");
        constructBuilder.addGraph("?g", whereBuilder);
        var model = coreRepository.queryConstruct(constructBuilder.build());

        //filtering cannot be done reasonably in query since semver is not naturally comparable
        var versions = new ArrayList<ModelVersionInfo>();
        model.listSubjects().forEach(subject -> {
            var dto = mapper.mapModelVersionInfo(subject);
            if(version == null || SemVer.compareSemVers(dto.getVersion(), version) < 0){
                versions.add(dto);
            }
        });
        versions.sort((a, b) -> -SemVer.compareSemVers(a.getVersion(), b.getVersion()));
        return versions;
    }

    public void updateVersionedModel(String prefix, String version, VersionedModelDTO dto) {
        var uri = DataModelURI.Factory.createModelURI(prefix, version);

        var model = coreRepository.fetch(uri.getGraphURI());
        check(authorizationManager.hasRightToModel(prefix, model));

        mapper.mapUpdateVersionedModel(model, uri.getModelURI(), dto, userProvider.getUser());

        coreRepository.put(uri.getGraphURI(), model);

        var indexModel = mapper.mapToIndexModel(uri.getModelURI(), model);
        indexService.updateModelToIndex(indexModel);
        auditService.log(AuditService.ActionType.UPDATE, uri.getGraphURI(), userProvider.getUser());
    }

    /**
     * Updates references from draft to published version in other data models.
     * E.g. https://iri.suomi.fi/model/foo/resource -> https://iri.suomi.fi/model/foo/1.0.0/resource
     * @param graph published graph uri
     */
    public void updateDraftReferences(String graph, String newVersion) {
        var builder = new UpdateBuilder();
        var e = builder.getExprFactory();
        builder
                .addDelete("?g", "?s", "?p", "?o")
                .addInsert("?g", "?s", "?p", "?releaseUri")
                .addWhere(new WhereBuilder()
                        .addGraph("?g", "?s", "?p", "?o"))
                        .addFilter(e.regex(e.str("?o"), graph + "([a-zA-Z](.)*)?$", ""))
                        .addBind(e.iri(
                            e.replace(e.str("?o"), graph, graph + newVersion + "/")
                        ), "releaseUri")
                        .addFilter(e.not(e.strstarts(e.str("?g"), graph)));

        coreRepository.queryUpdate(builder.buildRequest());
    }

    public String copyDataModel(String oldPrefix, String version, String newPrefix) {
        var oldURI = DataModelURI.Factory.createModelURI(oldPrefix, version);
        var newURI = DataModelURI.Factory.createModelURI(newPrefix);

        logger.info("Copying graph {}, new prefix {}", oldURI.getGraphURI(), newPrefix);

        var model = coreRepository.fetch(oldURI.getGraphURI());

        // check that old graph exists and new prefix is not in use
        if (!coreRepository.graphExists(oldURI.getGraphURI())) {
            throw new ResourceNotFoundException(oldURI.getGraphURI());
        }
        if (coreRepository.graphExists(newURI.getGraphURI())) {
            throw new MappingError("Prefix in use");
        }

        var user = userProvider.getUser();
        check(authorizationManager.hasRightToModel(oldPrefix, model));

        var copy = DataModelMapperUtils.mapCopyModel(model, user, oldURI, newURI);

        coreRepository.put(newURI.getGraphURI(), copy);
        visualisationService.copyVisualization(oldPrefix, version, newPrefix);

        indexService.createModelToIndex(mapper.mapToIndexModel(newURI.getModelURI(), copy));
        indexService.indexGraphResource(copy);

        auditService.log(AuditService.ActionType.CREATE, newURI.getGraphURI(), userProvider.getUser());

        return newURI.getGraphURI();
    }

    /**
     * Creates a new draft from given version.
     * @param prefix model prefix
     * @param version model version
     */
    public void createDraft(String prefix, String version) {
        logger.info("Create new draft from datamodel {} version {}", prefix, version);

        var versionURI = DataModelURI.Factory.createModelURI(prefix, version);

        // the old draft graph should be removed before creating new one
        if (coreRepository.graphExists(versionURI.getDraftGraphURI())) {
            throw new MappingError("Draft graph exists");
        }

        var model = coreRepository.fetch(versionURI.getGraphURI());
        check(authorizationManager.hasAdminRightToModel(prefix, model));

        var newDraft = mapper.mapNewDraft(model, versionURI);
        coreRepository.put(versionURI.getDraftGraphURI(), newDraft);
        // copy version's visualization data
        visualisationService.copyVisualization(prefix, version, prefix);

        indexService.createModelToIndex(mapper.mapToIndexModel(versionURI.getDraftGraphURI(), newDraft));
        indexService.indexGraphResource(newDraft);
        auditService.log(AuditService.ActionType.CREATE, versionURI.getDraftGraphURI(), userProvider.getUser());
    }

    private void updatePriorVersions(String graphUri, String priorVersion) {
        var update = new UpdateBuilder();
        update.addDelete("?g", "?s", OWL.priorVersion, NodeFactory.createURI(graphUri));
        if (priorVersion != null) {
            update.addInsert("?g", "?s", OWL.priorVersion, NodeFactory.createURI(priorVersion));
        }
        update.addGraph("?g", new WhereBuilder()
                .addWhere("?s", OWL.priorVersion, NodeFactory.createURI(graphUri)));

        coreRepository.queryUpdate(update.buildRequest());
    }

    public List<String> getReferrerModels(String graphUri) {
        var refProperty = "?property";
        var construct = new ConstructBuilder()
                .addConstruct("?s", refProperty, NodeFactory.createURI(graphUri))
                .addWhere(new WhereBuilder()
                        .addWhere("?s", refProperty, NodeFactory.createURI(graphUri)))
                .addValueVar(refProperty, OWL.imports, DCTerms.requires);

        var result = coreRepository.queryConstruct(construct.build());

        return result.listSubjects().mapWith(Resource::getURI).toList();
    }

    /**
     * Updates all triples with an object referring to model with referencePrefix
     *
     * @param prefix model's prefix
     * @param referenceURI reference model's URI
     * @param newVersion new version (null = draft)
     */
    public void changeReferenceVersion(String prefix, String referenceURI, String newVersion) {
        var oldReferenceURI = DataModelURI.Factory.fromURI(referenceURI);

        if (Objects.equals(oldReferenceURI.getVersion(), newVersion)) {
            throw new MappingError("Same version given");
        }
        var datamodelURI = DataModelURI.Factory.createModelURI(prefix);
        var model = coreRepository.fetch(datamodelURI.getGraphURI());

        var newReferenceURI = DataModelURI.Factory.createModelURI(oldReferenceURI.getModelId(), newVersion).getGraphURI();

        if (!coreRepository.graphExists(newReferenceURI)) {
            throw new ResourceNotFoundException(newReferenceURI);
        }

        check(authorizationManager.hasRightToModel(prefix, model));

        // find all statements with old namespace and change them to refer to new one
        var stmtList = model.listStatements()
                .filterKeep(objectHasNamespace(oldReferenceURI.getGraphURI()))
                .toList();

        stmtList.forEach(s -> {
            var newStatement = ResourceFactory.createStatement(
                    s.getSubject(),
                    s.getPredicate(),
                    ResourceFactory.createResource(newReferenceURI + s.getObject().asResource().getLocalName())
            );
            if (!model.contains(newStatement)) {
                model.add(newStatement);
            }
        });

        // In some cases references are stored as literals instead of resources. Make sure that they are removed as well.
        List.of(OWL.imports, DCTerms.requires).forEach(property ->
                stmtList.add(ResourceFactory.createStatement(
                    ResourceFactory.createResource(datamodelURI.getModelResourceURI()),
                    property,
                    ResourceFactory.createPlainLiteral(oldReferenceURI.getGraphURI())
                ))
        );

        model.remove(stmtList);
        coreRepository.put(datamodelURI.getGraphURI(), model);
    }

    private void addPrefixes(Model model, DataModelDTO dto) {
        dto.getExternalNamespaces().forEach(ns -> model.getGraph().getPrefixMapping().setNsPrefix(ns.getPrefix(), ns.getNamespace()));
    }

    private void sendNotification(Model model, DataModelURI modelVersionURI) {
        try {
            var stream = DataModelService.class.getResourceAsStream("/release-notification-template.html");
            if (stream != null) {
                var label = MapperUtils.localizedPropertyToMap(model.getResource(modelVersionURI.getModelURI()), RDFS.label);
                var defaultLabel = label.getOrDefault("fi", label.entrySet().iterator().next().getValue());
                var message = new String(stream.readAllBytes(), StandardCharsets.UTF_8)
                        .replace("%VERSION%", modelVersionURI.getVersion())
                        .replace("%URI%", modelVersionURI.getGraphURI())
                        .replace("%LABEL_FI%", defaultLabel)
                        .replace("%LABEL_SV%", label.getOrDefault("sv", defaultLabel))
                        .replace("%LABEL_EN%", label.getOrDefault("en", defaultLabel));

                dataModelSubscriptionService.publish(modelVersionURI.getModelId(), ModelConstants.EMAIL_NOTIFICATION_TITLE, message);
            }
        } catch (Exception e) {
            logger.error("Error sending notification", e);
        }
    }

    private static void handleNamespaceRemove(Model model,
                                               List<DataModelURI> newNamespaces,
                                               List<DataModelURI> oldNamespaces) {
        Set<String> removedNamespaces = new HashSet<>();
        if (newNamespaces.isEmpty()) {
            removedNamespaces = oldNamespaces.stream()
                    .map(DataModelURI::getGraphURI)
                    .collect(Collectors.toSet());
        } else if (!oldNamespaces.isEmpty()) {
            oldNamespaces.stream()
                    .filter(oldNs -> !newNamespaces.contains(oldNs))
                    .map(DataModelURI::getGraphURI)
                    .forEach(removedNamespaces::add);
        }

        // throw an error if there are references to removed linked model
        removedNamespaces.forEach(removed -> {
            var stmtList = model.listStatements()
                    .filterDrop(s -> s.getObject().toString().equals(removed))
                    .filterKeep(objectHasNamespace(removed))
                    .toList();

            if (!stmtList.isEmpty()) {
                throw new MappingError(getNamespaceRemoveErrorMessage(model, stmtList));
            }
        });
    }

    private static String getNamespaceRemoveErrorMessage(Model model, List<Statement> stmtList) {
        var resources = stmtList.stream()
                .limit(5)
                .map(s -> {
                    var subj = DataModelUtils.findSubjectForObject(s, model);
                    return subj.getURI();
                })
                .collect(Collectors.toSet());

        var message = new StringBuilder("Cannot remove namespace, because it has existing references: ")
                .append(String.join(", ", resources));
        if (stmtList.size() > 5) {
            message.append(" ... ")
                    .append(stmtList.size() - 5)
                    .append(" other resources");
        }
        return message.toString();
    }

    private static Predicate<Statement> objectHasNamespace(String ns) {
        return stmt -> {
            var s = stmt.getObject();
            return s.isResource() && Objects.equals(s.asResource().getNameSpace(), ns);
        };
    }
}
