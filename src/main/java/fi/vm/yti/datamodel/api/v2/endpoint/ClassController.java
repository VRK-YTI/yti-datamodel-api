package fi.vm.yti.datamodel.api.v2.endpoint;

import fi.vm.yti.datamodel.api.security.AuthorizationManager;
import fi.vm.yti.datamodel.api.v2.dto.*;
import fi.vm.yti.datamodel.api.v2.endpoint.error.MappingError;
import fi.vm.yti.datamodel.api.v2.endpoint.error.ResourceNotFoundException;
import fi.vm.yti.datamodel.api.v2.mapper.ClassMapper;
import fi.vm.yti.datamodel.api.v2.mapper.MapperUtils;
import fi.vm.yti.datamodel.api.v2.mapper.ResourceMapper;
import fi.vm.yti.datamodel.api.v2.opensearch.dto.ResourceSearchRequest;
import fi.vm.yti.datamodel.api.v2.opensearch.index.IndexResource;
import fi.vm.yti.datamodel.api.v2.opensearch.index.OpenSearchIndexer;
import fi.vm.yti.datamodel.api.v2.service.GroupManagementService;
import fi.vm.yti.datamodel.api.v2.service.JenaService;
import fi.vm.yti.datamodel.api.v2.service.SearchIndexService;
import fi.vm.yti.datamodel.api.v2.service.TerminologyService;
import fi.vm.yti.datamodel.api.v2.validator.ValidClass;
import fi.vm.yti.datamodel.api.v2.validator.ValidNodeShape;
import fi.vm.yti.security.AuthenticatedUserProvider;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.topbraid.shacl.vocabulary.SH;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static fi.vm.yti.security.AuthorizationException.check;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@RestController
@RequestMapping("v2/class")
@Tag(name = "Class" )
@Validated
public class ClassController {

    private final Logger logger = LoggerFactory.getLogger(ClassController.class);

    private final AuthorizationManager authorizationManager;
    private final JenaService jenaService;
    private final OpenSearchIndexer openSearchIndexer;
    private final GroupManagementService groupManagementService;
    private final AuthenticatedUserProvider userProvider;
    private final TerminologyService terminologyService;
    private final SearchIndexService searchIndexService;

    public ClassController(AuthorizationManager authorizationManager,
                           JenaService jenaService,
                           OpenSearchIndexer openSearchIndexer,
                           GroupManagementService groupManagementService,
                           AuthenticatedUserProvider userProvider,
                           TerminologyService terminologyService,
                           SearchIndexService searchIndexService){
        this.authorizationManager = authorizationManager;
        this.jenaService = jenaService;
        this.openSearchIndexer = openSearchIndexer;
        this.groupManagementService = groupManagementService;
        this.userProvider = userProvider;
        this.terminologyService = terminologyService;
        this.searchIndexService = searchIndexService;
    }

    @Operation(summary = "Add a class to a model")
    @ApiResponse(responseCode = "200", description = "Class added to model successfully")
    @PutMapping(value = "/ontology/{prefix}", consumes = APPLICATION_JSON_VALUE)
    public void createClass(@PathVariable String prefix, @RequestBody @ValidClass ClassDTO classDTO){
        var modelURI = ModelConstants.SUOMI_FI_NAMESPACE + prefix;
        var model = handleCreateClassOrNodeShape(modelURI, prefix, classDTO);
        var classURI = ClassMapper.createOntologyClassAndMapToModel(modelURI, model, classDTO, userProvider.getUser());
        jenaService.putDataModelToCore(modelURI, model);
        openSearchIndexer.createResourceToIndex(ResourceMapper.mapToIndexResource(model, classURI));
    }

    @Operation(summary = "Add a node shape to a model")
    @ApiResponse(responseCode = "200", description = "Class added to model successfully")
    @PutMapping(value = "/profile/{prefix}", consumes = APPLICATION_JSON_VALUE)
    public void createNodeShape(@PathVariable String prefix, @RequestBody @ValidNodeShape NodeShapeDTO nodeShapeDTO){
        var modelURI = ModelConstants.SUOMI_FI_NAMESPACE + prefix;
        var model = handleCreateClassOrNodeShape(modelURI, prefix, nodeShapeDTO);

        var indexedResources = new ArrayList<String>();
        var classURI = ClassMapper.createNodeShapeAndMapToModel(modelURI, model, nodeShapeDTO, userProvider.getUser());
        indexedResources.add(classURI);

        var propertiesModel = jenaService.findResources(nodeShapeDTO.getProperties());
        var properties = ClassMapper.mapPlaceholderPropertyShapes(model, classURI, propertiesModel, userProvider.getUser());
        indexedResources.addAll(properties);

        // Node shape based on an existing node shape
        var propertyURIs = handleTargetNodeProperties(nodeShapeDTO.getTargetNode());
        var referencePropertiesModel = jenaService.findResources(new ArrayList<>(propertyURIs));
        ClassMapper.mapReferencePropertyShapes(model, classURI,
                referencePropertiesModel);

        jenaService.putDataModelToCore(modelURI, model);

        openSearchIndexer.bulkInsert(OpenSearchIndexer.OPEN_SEARCH_INDEX_RESOURCE,
                indexedResources.stream()
                        .map(p -> ResourceMapper.mapToIndexResource(model, p))
                        .toList());
    }

    Model handleCreateClassOrNodeShape(String modelURI, String prefix, BaseDTO dto) {
        if(jenaService.doesResourceExistInGraph(modelURI, modelURI + ModelConstants.RESOURCE_SEPARATOR + dto.getIdentifier())){
            throw new MappingError("Class already exists");
        }
        var model = jenaService.getDataModel(modelURI);
        check(authorizationManager.hasRightToModel(prefix, model));
        checkDataModelType(model.getResource(modelURI), dto);

        terminologyService.resolveConcept(dto.getSubject());
        return model;
    }


    @Operation(summary = "Update a class in a model")
    @ApiResponse(responseCode =  "200", description = "Class updated in model successfully")
    @PutMapping(value = "/ontology/{prefix}/{classIdentifier}", consumes = APPLICATION_JSON_VALUE)
    public void updateClass(@PathVariable String prefix, @PathVariable String classIdentifier, @RequestBody @ValidClass(updateClass = true) ClassDTO classDTO){
        handleUpdateClassOrNodeShape(prefix, classIdentifier, classDTO);
    }

    @Operation(summary = "Update a node shape in a model")
    @ApiResponse(responseCode =  "200", description = "Class updated in model successfully")
    @PutMapping(value = "/profile/{prefix}/{nodeShapeIdentifier}", consumes = APPLICATION_JSON_VALUE)
    public void updateNodeShape(@PathVariable String prefix, @PathVariable String nodeShapeIdentifier,
                                @RequestBody @ValidNodeShape(updateNodeShape = true) NodeShapeDTO nodeShapeDTO){
        handleUpdateClassOrNodeShape(prefix, nodeShapeIdentifier, nodeShapeDTO);
    }

    void handleUpdateClassOrNodeShape(String prefix, String classIdentifier, BaseDTO dto) {
        logger.info("Updating class {}", classIdentifier);

        var graph = ModelConstants.SUOMI_FI_NAMESPACE + prefix;
        var classURI = graph + ModelConstants.RESOURCE_SEPARATOR + classIdentifier;
        if(!jenaService.doesResourceExistInGraph(graph, graph + ModelConstants.RESOURCE_SEPARATOR + classIdentifier)){
            throw new ResourceNotFoundException(classIdentifier);
        }

        var model = jenaService.getDataModel(graph);

        checkDataModelType(model.getResource(graph), dto);
        check(authorizationManager.hasRightToModel(prefix, model));

        var classResource = model.getResource(classURI);

        terminologyService.resolveConcept(dto.getSubject());

        if (MapperUtils.isOntology(model.getResource(graph))) {
            ClassMapper.mapToUpdateOntologyClass(model, graph, classResource, (ClassDTO) dto, userProvider.getUser());
        } else {
            ClassMapper.mapToUpdateNodeShape(model, graph, classResource, (NodeShapeDTO) dto, userProvider.getUser());
        }
        jenaService.putDataModelToCore(graph, model);

        var indexClass = ResourceMapper.mapToIndexResource(model, classURI);
        openSearchIndexer.updateResourceToIndex(indexClass);
    }

    @Operation(summary = "Get a class from a data model")
    @ApiResponse(responseCode = "200", description = "Class found successfully")
    @GetMapping(value = "/ontology/{prefix}/{classIdentifier}", produces = APPLICATION_JSON_VALUE)
    public ClassInfoDTO getClass(@PathVariable String prefix, @PathVariable String classIdentifier){
        return (ClassInfoDTO) handleGetClassOrNodeShape(prefix, classIdentifier);
    }

    @Operation(summary = "Add a class to a model")
    @ApiResponse(responseCode = "200", description = "Node shape found successfully")
    @GetMapping(value = "/profile/{prefix}/{shapeIdentifier}", produces = APPLICATION_JSON_VALUE)
    public NodeShapeInfoDTO getNodeShape(@PathVariable String prefix, @PathVariable String shapeIdentifier){
        return (NodeShapeInfoDTO) handleGetClassOrNodeShape(prefix, shapeIdentifier);
    }

    private ResourceInfoBaseDTO handleGetClassOrNodeShape(String prefix, String classIdentifier) {
        var modelURI = ModelConstants.SUOMI_FI_NAMESPACE + prefix;
        var classURI = modelURI + ModelConstants.RESOURCE_SEPARATOR + classIdentifier;
        if(!jenaService.doesResourceExistInGraph(modelURI , classURI)){
            throw new ResourceNotFoundException(classURI);
        }
        var model = jenaService.getDataModel(modelURI);
        var hasRightToModel = authorizationManager.hasRightToModel(prefix, model);

        var orgModel = jenaService.getOrganizations();
        var userMapper = hasRightToModel ? groupManagementService.mapUser() : null;

        ResourceInfoBaseDTO dto;
        if (MapperUtils.isOntology(model.getResource(modelURI))) {
            var classResources = jenaService.constructWithQuery(ClassMapper.getClassResourcesQuery(classURI, false));
            dto = ClassMapper.mapToClassDTO(model, modelURI, classIdentifier, orgModel,
                hasRightToModel, userMapper);
            ClassMapper.addClassResourcesToDTO(classResources, (ClassInfoDTO) dto);
        } else {
            dto = ClassMapper.mapToNodeShapeDTO(model, modelURI, classIdentifier, orgModel,
                    hasRightToModel, userMapper);
            ClassMapper.addNodeShapeResourcesToDTO(model, (NodeShapeInfoDTO) dto);
        }

        terminologyService.mapConcept().accept(dto);
        return dto;
    }

    @Operation(summary = "Delete a class from a data model")
    @ApiResponse(responseCode = "200", description = "Class deleted successfully")
    @DeleteMapping(value = "/ontology/{prefix}/{classIdentifier}")
    public void deleteClass(@PathVariable String prefix, @PathVariable String classIdentifier){
        handleDeleteClassOrNodeShape(prefix, classIdentifier);
    }

    @Operation(summary = "Delete a class from a data model")
    @ApiResponse(responseCode = "200", description = "Class deleted successfully")
    @DeleteMapping(value = "/profile/{prefix}/{classIdentifier}")
    public void deleteNodeShape(@PathVariable String prefix, @PathVariable String classIdentifier){
        handleDeleteClassOrNodeShape(prefix, classIdentifier);
    }

    void handleDeleteClassOrNodeShape(String prefix, String identifier) {
        var modelURI = ModelConstants.SUOMI_FI_NAMESPACE + prefix;
        var classURI  = modelURI + ModelConstants.RESOURCE_SEPARATOR + identifier;
        if(!jenaService.doesResourceExistInGraph(modelURI , classURI)){
            throw new ResourceNotFoundException(classURI);
        }
        var model = jenaService.getDataModel(modelURI);
        check(authorizationManager.hasRightToModel(prefix, model));
        jenaService.deleteResource(classURI);
        openSearchIndexer.deleteResourceFromIndex(classURI);
    }

    @Operation(summary = "Get an external class from imports")
    @ApiResponse(responseCode = "200", description = "External class found successfully")
    @GetMapping(value = "/external", produces = APPLICATION_JSON_VALUE)
    public ExternalClassDTO getExternalClass(@RequestParam String uri) {
        var namespace = NodeFactory.createURI(uri).getNameSpace();
        var model = jenaService.getNamespaceFromImports(namespace);

        var dto = ClassMapper.mapExternalClassToDTO(model, uri);
        var resources = jenaService.constructWithQueryImports(
                ClassMapper.getClassResourcesQuery(uri, true));

        ClassMapper.addExternalClassResourcesToDTO(resources, dto);

        return dto;
    }

    @Operation(summary = "Get all node shapes based on given targetClass")
    @ApiResponse(responseCode = "200", description = "List of node shapes fetched successfully")
    @GetMapping(value = "/nodeshapes", produces = APPLICATION_JSON_VALUE)
    public List<IndexResource> getNodeShapes(@RequestParam String targetClass) throws IOException {
        var request = new ResourceSearchRequest();
        request.setStatus(Set.of(Status.VALID, Status.DRAFT));
        request.setTargetClass(targetClass);
        return searchIndexService
                .searchInternalResources(request, userProvider.getUser())
                .getResponseObjects();
    }

    @Operation(summary = "Toggles deactivation of a single property shape")
    @ApiResponse(responseCode = "200", description = "Deactivation has changes successfully")
    @PutMapping(value = "/toggleDeactivate/{prefix}", produces = APPLICATION_JSON_VALUE)
    public void deactivatePropertyShape(@PathVariable String prefix, @RequestParam String propertyUri) {
        var modelURI = ModelConstants.SUOMI_FI_NAMESPACE + prefix;
        var model = jenaService.getDataModel(modelURI);

        if(!jenaService.doesResourceExistInGraph(modelURI, propertyUri)){
            throw new ResourceNotFoundException(propertyUri);
        }
        check(authorizationManager.hasRightToModel(prefix, model));
        ClassMapper.toggleAndMapDeactivatedProperty(model, propertyUri);
        jenaService.putDataModelToCore(modelURI, model);
    }

    private Set<String> handleTargetNodeProperties(String targetNode) {
        var propertyShapes = new HashSet<String>();
        var handledNodeShapes = new HashSet<String>();

        // collect recursively all property shape uris from target node
        while (targetNode != null) {
            if (handledNodeShapes.contains(targetNode)) {
                throw new MappingError("Circular dependency, cannot add sh:node reference");
            }
            handledNodeShapes.add(targetNode);

            var nodeModel = jenaService.findResources(List.of(targetNode));
            var nodeResource = nodeModel.getResource(targetNode);

            propertyShapes.addAll(nodeResource.listProperties(SH.property)
                    .mapWith((var stmt) -> stmt.getResource().getURI())
                    .toList());

            if (!nodeResource.hasProperty(SH.node)) {
                break;
            }
            targetNode = nodeResource.getProperty(SH.node).getObject().toString();
        }
        return propertyShapes;
    }

    private void checkDataModelType(Resource modelResource, BaseDTO dto) {
        if (dto instanceof NodeShapeDTO && MapperUtils.isOntology(modelResource)) {
            throw new MappingError("Cannot add node shape to ontology");
        } else if (dto instanceof ClassDTO && MapperUtils.isApplicationProfile(modelResource)) {
            throw new MappingError("Cannot add ontology class to application profile");
        }
    }
}
