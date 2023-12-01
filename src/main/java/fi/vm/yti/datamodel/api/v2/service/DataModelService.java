package fi.vm.yti.datamodel.api.v2.service;

import fi.vm.yti.datamodel.api.security.AuthorizationManager;
import fi.vm.yti.datamodel.api.v2.dto.*;
import fi.vm.yti.datamodel.api.v2.endpoint.error.MappingError;
import fi.vm.yti.datamodel.api.v2.endpoint.error.ResourceNotFoundException;
import fi.vm.yti.datamodel.api.v2.mapper.MapperUtils;
import fi.vm.yti.datamodel.api.v2.mapper.ModelMapper;
import fi.vm.yti.datamodel.api.v2.mapper.ResourceMapper;
import fi.vm.yti.datamodel.api.v2.opensearch.index.IndexResource;
import fi.vm.yti.datamodel.api.v2.opensearch.index.OpenSearchIndexer;
import fi.vm.yti.datamodel.api.v2.properties.SuomiMeta;
import fi.vm.yti.datamodel.api.v2.repository.CoreRepository;
import fi.vm.yti.datamodel.api.v2.utils.DataModelURI;
import fi.vm.yti.datamodel.api.v2.utils.DataModelUtils;
import fi.vm.yti.datamodel.api.v2.utils.SemVer;
import fi.vm.yti.datamodel.api.v2.validator.ValidationConstants;
import fi.vm.yti.security.AuthenticatedUserProvider;
import org.apache.jena.arq.querybuilder.ConstructBuilder;
import org.apache.jena.arq.querybuilder.WhereBuilder;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.SimpleSelector;
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
import java.util.ArrayList;
import java.util.List;

import static fi.vm.yti.security.AuthorizationException.check;

@Service
public class DataModelService {

    private final AuditService auditService = new AuditService("DATAMODEL");
    private final Logger logger = LoggerFactory.getLogger(DataModelService.class);

    private final CoreRepository coreRepository;
    private final AuthorizationManager authorizationManager;
    private final GroupManagementService groupManagementService;
    private final ModelMapper mapper;
    private final TerminologyService terminologyService;
    private final VisualizationService visualisationService;
    private final CodeListService codeListService;
    private final OpenSearchIndexer openSearchIndexer;
    private final AuthenticatedUserProvider userProvider;

    @Autowired
    public DataModelService(CoreRepository coreRepository,
                            AuthorizationManager authorizationManager,
                            GroupManagementService groupManagementService,
                            ModelMapper modelMapper,
                            TerminologyService terminologyService,
                            VisualizationService visualizationService,
                            CodeListService codeListService,
                            OpenSearchIndexer openSearchIndexer,
                            AuthenticatedUserProvider userProvider) {
        this.coreRepository = coreRepository;
        this.authorizationManager = authorizationManager;
        this.groupManagementService = groupManagementService;
        this.mapper = modelMapper;
        this.terminologyService = terminologyService;
        this.visualisationService = visualizationService;
        this.codeListService = codeListService;
        this.openSearchIndexer = openSearchIndexer;
        this.userProvider = userProvider;
    }

    public DataModelInfoDTO get(String prefix, String version) {
        var uri = DataModelURI.createModelURI(prefix, version);
        var model = coreRepository.fetch(uri.getGraphURI());
        var hasRightsToModel = authorizationManager.hasRightToModel(prefix, model);

        var userMapper = hasRightsToModel ? groupManagementService.mapUser() : null;
        return mapper.mapToDataModelDTO(prefix, model, userMapper);
    }

    public URI create(DataModelDTO dto, ModelType modelType) throws URISyntaxException {
        check(authorizationManager.hasRightToAnyOrganization(dto.getOrganizations()));
        var graphUri = DataModelURI.createModelURI(dto.getPrefix()).getGraphURI();

        terminologyService.resolveTerminology(dto.getTerminologies());
        codeListService.resolveCodelistScheme(dto.getCodeLists());
        var jenaModel = mapper.mapToJenaModel(dto, modelType, userProvider.getUser());

        coreRepository.put(graphUri, jenaModel);

        var indexModel = mapper.mapToIndexModel(graphUri, jenaModel);
        openSearchIndexer.createModelToIndex(indexModel);
        auditService.log(AuditService.ActionType.CREATE, graphUri, userProvider.getUser());
        return new URI(graphUri);
    }

    public void update(String prefix, DataModelDTO dto) {
        var graphUri = DataModelURI.createModelURI(prefix).getGraphURI();
        var model = coreRepository.fetch(graphUri);

        check(authorizationManager.hasRightToModel(prefix, model));

        terminologyService.resolveTerminology(dto.getTerminologies());
        codeListService.resolveCodelistScheme(dto.getCodeLists());
        mapper.mapToUpdateJenaModel(graphUri, dto, model, userProvider.getUser());

        coreRepository.put(graphUri, model);

        var indexModel = mapper.mapToIndexModel(graphUri, model);
        openSearchIndexer.updateModelToIndex(indexModel);
        auditService.log(AuditService.ActionType.UPDATE, graphUri, userProvider.getUser());
    }

    public void delete(String prefix, String version) {
        check(userProvider.getUser().isSuperuser());

        var modelUri = DataModelURI.createModelURI(prefix, version);
        var graphUri = modelUri.getGraphURI();
        if(!coreRepository.graphExists(graphUri)) {
            throw new ResourceNotFoundException(graphUri);
        }

        coreRepository.delete(graphUri);
        openSearchIndexer.deleteModelFromIndex(graphUri);
        openSearchIndexer.removeResourceIndexesByDataModel(modelUri.getModelURI(), version);
        auditService.log(AuditService.ActionType.DELETE, graphUri, userProvider.getUser());
    }

    public boolean exists(String prefix) {
        if (ValidationConstants.RESERVED_WORDS.contains(prefix)) {
            return true;
        }
        return coreRepository.graphExists(DataModelURI.createModelURI(prefix).getModelURI());
    }

    public ResponseEntity<String> export(String prefix, String version, String accept, boolean showAsFile) {
        var uri = DataModelURI.createModelURI(prefix, version);

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
        var modelVersionURI = DataModelURI.createModelURI(prefix, version);
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
            var priorVersion = DataModelURI.fromURI(priorVersionUri).getVersion();
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

        var newVersion = mapper.mapToIndexModel(modelUri, model);
        openSearchIndexer.createModelToIndex(newVersion);
        var list = new ArrayList<IndexResource>();
        var resources = model.listSubjectsWithProperty(RDF.type, OWL.Class)
                .andThen(model.listSubjectsWithProperty(RDF.type, OWL.ObjectProperty))
                .andThen(model.listSubjectsWithProperty(RDF.type, OWL.DatatypeProperty))
                .andThen(model.listSubjectsWithProperty(RDF.type, SH.NodeShape))
                .filterDrop(RDFNode::isAnon);
        resources.forEach(resource -> list.add(ResourceMapper.mapToIndexResource(model, resource.getURI())));
        openSearchIndexer.bulkInsert(OpenSearchIndexer.OPEN_SEARCH_INDEX_RESOURCE, list);
        visualisationService.saveVersionedPositions(prefix, version);
        auditService.log(AuditService.ActionType.CREATE, modelVersionURI.getGraphURI(), userProvider.getUser());
        //Draft model does not need to be indexed since opensearch specific properties on it did not change
        return new URI(modelVersionURI.getGraphURI());
    }


    public List<ModelVersionInfo> getPriorVersions(String prefix, String version){

        var modelUri = DataModelURI.createModelURI(prefix);
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
        var uri = DataModelURI.createModelURI(prefix, version);

        var model = coreRepository.fetch(uri.getGraphURI());
        check(authorizationManager.hasRightToModel(prefix, model));

        mapper.mapUpdateVersionedModel(model, uri.getModelURI(), dto, userProvider.getUser());

        coreRepository.put(uri.getGraphURI(), model);

        var indexModel = mapper.mapToIndexModel(uri.getModelId(), model);
        openSearchIndexer.updateModelToIndex(indexModel);
        auditService.log(AuditService.ActionType.UPDATE, uri.getGraphURI(), userProvider.getUser());
    }
}
