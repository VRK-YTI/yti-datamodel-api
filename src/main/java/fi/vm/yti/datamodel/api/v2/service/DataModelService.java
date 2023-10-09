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
import fi.vm.yti.datamodel.api.v2.repository.CoreRepository;
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
        var graphUri = ModelConstants.SUOMI_FI_NAMESPACE + prefix;
        if(version != null) {
            graphUri += ModelConstants.RESOURCE_SEPARATOR + version;
        }
        var model = coreRepository.fetch(graphUri);
        var hasRightsToModel = authorizationManager.hasRightToModel(prefix, model);

        var userMapper = hasRightsToModel ? groupManagementService.mapUser() : null;
        return mapper.mapToDataModelDTO(prefix, model, userMapper);
    }

    public URI create(DataModelDTO dto, ModelType modelType) throws URISyntaxException {
        check(authorizationManager.hasRightToAnyOrganization(dto.getOrganizations()));
        var graphUri = ModelConstants.SUOMI_FI_NAMESPACE + dto.getPrefix();

        terminologyService.resolveTerminology(dto.getTerminologies());
        codeListService.resolveCodelistScheme(dto.getCodeLists());
        var jenaModel = mapper.mapToJenaModel(dto, modelType, userProvider.getUser());

        coreRepository.put(graphUri, jenaModel);

        var indexModel = mapper.mapToIndexModel(graphUri, jenaModel);
        openSearchIndexer.createModelToIndex(indexModel);
        return new URI(graphUri);
    }

    public void update(String prefix, DataModelDTO dto) {
        var graphUri = ModelConstants.SUOMI_FI_NAMESPACE + prefix;
        var oldModel = coreRepository.fetch(graphUri);

        check(authorizationManager.hasRightToModel(prefix, oldModel));

        terminologyService.resolveTerminology(dto.getTerminologies());
        codeListService.resolveCodelistScheme(dto.getCodeLists());
        var jenaModel = mapper.mapToUpdateJenaModel(prefix, dto, oldModel, userProvider.getUser());

        coreRepository.put(graphUri, jenaModel);

        var indexModel = mapper.mapToIndexModel(graphUri, jenaModel);
        openSearchIndexer.updateModelToIndex(indexModel);
    }

    public void delete(String prefix) {
        var modelUri = ModelConstants.SUOMI_FI_NAMESPACE + prefix;
        if(!coreRepository.graphExists(modelUri)){
            throw new ResourceNotFoundException(modelUri);
        }
        var model = coreRepository.fetch(modelUri);
        check(authorizationManager.hasRightToModel(prefix, model));

        coreRepository.delete(modelUri);
        openSearchIndexer.deleteModelFromIndex(modelUri);
    }

    public boolean exists(String prefix) {
        if (ValidationConstants.RESERVED_WORDS.contains(prefix)) {
            return true;
        }
        return coreRepository.graphExists(ModelConstants.SUOMI_FI_NAMESPACE + prefix);
    }

    public ResponseEntity<String> export(String prefix, String version, String resource, String accept) {
        var modelURI = ModelConstants.SUOMI_FI_NAMESPACE + prefix;
        var versionUri = modelURI;
        if(version != null) {
            versionUri += ModelConstants.RESOURCE_SEPARATOR + version;
        }
        logger.info("Exporting datamodel {}, {}", versionUri, accept);

        Model exportedModel;
        Model model;

        try {
            model = coreRepository.fetch(versionUri);
        } catch (ResourceNotFoundException e) {
            // cannot throw ResourceNotFoundException because accept header is not application/json
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            logger.error("Error exporting datamodel", e);
            return ResponseEntity.internalServerError().build();
        }

        if (resource != null) {
            var res = model.getResource(modelURI + ModelConstants.RESOURCE_SEPARATOR + resource);
            var properties = res.listProperties();
            if (!properties.hasNext()) {
                return ResponseEntity.notFound().build();
            }
            exportedModel = properties.toModel();
        } else {
            exportedModel = model;
        }

        DataModelUtils.addPrefixesToModel(modelURI, exportedModel);

        // remove editorial notes from resources
        if (!authorizationManager.hasRightToModel(prefix, model)) {
            var hiddenValues = model.listStatements(
                    new SimpleSelector(null, SKOS.editorialNote, (String) null)).toList();
            exportedModel.remove(hiddenValues);
        }

        var stringWriter = new StringWriter();
        switch (accept) {
            case "text/turtle":
                RDFDataMgr.write(stringWriter, exportedModel, Lang.TURTLE);
                break;
            case "application/rdf+xml":
                RDFDataMgr.write(stringWriter, exportedModel, Lang.RDFXML);
                break;
            case "application/ld+json":
            default:
                RDFDataMgr.write(stringWriter, exportedModel, Lang.JSONLD);
        }
        return ResponseEntity.ok(stringWriter.toString());
    }


    public URI createRelease(String prefix, String version, Status status) throws URISyntaxException {
        var modelUri = ModelConstants.SUOMI_FI_NAMESPACE + prefix;
        if(!status.equals(Status.SUGGESTED) && !status.equals(Status.VALID)){
            throw new MappingError("Status has to be SUGGESTED or VALID");
        }

        if(!version.matches(SemVer.VALID_REGEX)){
            throw new MappingError("Not valid Semantic version string");
        }

        //This gets the current "DRAFT" version
        var model = coreRepository.fetch(modelUri);
        check(authorizationManager.hasRightToModel(prefix, model));
        var priorVersionUri = MapperUtils.propertyToString(model.getResource(modelUri), OWL.priorVersion);
        if(priorVersionUri != null){
            var priorVersion = priorVersionUri.substring(priorVersionUri.lastIndexOf("/")  + 1);
            var result = SemVer.compareSemVers(priorVersion, version);
            if(result == 0){
                throw new MappingError("Same version number");
            }
            if(result > 0) {
                throw new MappingError("Older version given");
            }
        }

        var newDraft = ModelFactory.createDefaultModel().add(model);

        var versionUri = mapper.mapReleaseProperties(model, modelUri, version, status);
        //Map new newest release to draft model
        mapper.mapPriorVersion(newDraft, modelUri, versionUri);
        coreRepository.put(modelUri, newDraft);
        coreRepository.put(versionUri, model);

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
        //Draft model does not need to be indexed since opensearch specific properties on it did not change
        return new URI(versionUri);

    }


    public List<ModelVersionInfo> getPreviousVersions(String prefix, String version){
        var modelUri = ModelConstants.SUOMI_FI_NAMESPACE + prefix;
        //Would be nice to do traverse the graphs using SPARQL starting from the correct versionIRI but currently that traversing named graphs is not supported
        var constructBuilder = new ConstructBuilder()
                .addConstruct("?g", OWL2.versionInfo, "?versionInfo")
                .addConstruct("?g", OWL2.versionIRI, "?versionIRI")
                .addConstruct("?g", RDFS.label, "?label");
        var uri = NodeFactory.createURI(modelUri);
        var whereBuilder = new WhereBuilder()
                .addWhere(uri, OWL2.versionInfo, "?versionInfo")
                .addWhere(uri, OWL2.versionIRI, "?versionIRI")
                .addWhere(uri, RDFS.label, "?label");
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
}
