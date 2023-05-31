package fi.vm.yti.datamodel.api.v2.endpoint;

import fi.vm.yti.datamodel.api.security.AuthorizationManager;
import fi.vm.yti.datamodel.api.v2.dto.*;
import fi.vm.yti.datamodel.api.v2.endpoint.error.MappingError;
import fi.vm.yti.datamodel.api.v2.endpoint.error.ResourceNotFoundException;
import fi.vm.yti.datamodel.api.v2.mapper.MapperUtils;
import fi.vm.yti.datamodel.api.v2.mapper.ResourceMapper;
import fi.vm.yti.datamodel.api.v2.opensearch.index.OpenSearchIndexer;
import fi.vm.yti.datamodel.api.v2.service.GroupManagementService;
import fi.vm.yti.datamodel.api.v2.service.JenaService;
import fi.vm.yti.datamodel.api.v2.service.TerminologyService;
import fi.vm.yti.datamodel.api.v2.validator.ValidPropertyShape;
import fi.vm.yti.datamodel.api.v2.validator.ValidResource;
import fi.vm.yti.security.AuthenticatedUserProvider;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import static fi.vm.yti.security.AuthorizationException.check;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@RestController
@RequestMapping("v2/resource")
@Tag(name = "Resource" )
@Validated
public class ResourceController {

    private final JenaService jenaService;
    private final AuthorizationManager authorizationManager;
    private final OpenSearchIndexer openSearchIndexer;
    private final AuthenticatedUserProvider userProvider;
    private final GroupManagementService groupManagementService;
    private final TerminologyService terminologyService;

    public ResourceController(JenaService jenaService,
                              AuthorizationManager authorizationManager,
                              OpenSearchIndexer openSearchIndexer,
                              AuthenticatedUserProvider userProvider,
                              GroupManagementService groupManagementService,
                              TerminologyService terminologyService) {
        this.jenaService = jenaService;
        this.authorizationManager = authorizationManager;
        this.openSearchIndexer = openSearchIndexer;
        this.userProvider = userProvider;
        this.groupManagementService = groupManagementService;
        this.terminologyService = terminologyService;
    }

    @Operation(summary = "Add a resource (attribute or association) to a model")
    @ApiResponse(responseCode = "200", description = "Resource added to model successfully")
    @PutMapping(value = "/ontology/{prefix}", consumes = APPLICATION_JSON_VALUE)
    public void createResource(@PathVariable String prefix, @RequestBody @ValidResource ResourceDTO dto){
        var graphUri = ModelConstants.SUOMI_FI_NAMESPACE + prefix;
        var model = handleCreateResourceOrPropertyShape(prefix, dto);
        var resourceUri = ResourceMapper.mapToResource(graphUri, model, dto, userProvider.getUser());

        jenaService.putDataModelToCore(graphUri, model);
        var indexClass = ResourceMapper.mapToIndexResource(model, resourceUri);
        openSearchIndexer.createResourceToIndex(indexClass);
    }

    @Operation(summary = "Add a property shape to a profile")
    @ApiResponse(responseCode = "200", description = "Property shape added to profile successfully")
    @PutMapping(value = "/profile/{prefix}", consumes = APPLICATION_JSON_VALUE)
    public void createPropertyShape(@PathVariable String prefix, @ValidPropertyShape @RequestBody PropertyShapeDTO dto) {
        var modelURI = ModelConstants.SUOMI_FI_NAMESPACE + prefix;
        var model = handleCreateResourceOrPropertyShape(prefix, dto);
        var resourceURI = ResourceMapper.mapToPropertyShapeResource(modelURI, model, dto, userProvider.getUser());

        jenaService.putDataModelToCore(modelURI, model);
        var indexClass = ResourceMapper.mapToIndexResource(model, resourceURI);
        openSearchIndexer.createResourceToIndex(indexClass);
    }

    @Operation(summary = "Update a resource in a model")
    @ApiResponse(responseCode = "200", description = "Resource updated to model successfully")
    @PutMapping(value = "/ontology/{prefix}/{resourceIdentifier}", consumes = APPLICATION_JSON_VALUE)
    public void updateResource(@PathVariable String prefix, @PathVariable String resourceIdentifier,
                               @RequestBody @ValidResource(updateProperty = true) ResourceDTO dto){
        handleUpdateResourceOrPropertyShape(prefix, resourceIdentifier, dto);
    }

    @Operation(summary = "Update a property shape in a model")
    @ApiResponse(responseCode = "200", description = "Resource updated to model successfully")
    @PutMapping(value = "/profile/{prefix}/{resourceIdentifier}", consumes = APPLICATION_JSON_VALUE)
    public void updatePropertyShape(@PathVariable String prefix, @PathVariable String resourceIdentifier,
                                    @RequestBody @ValidPropertyShape(updateProperty = true) PropertyShapeDTO dto){
        handleUpdateResourceOrPropertyShape(prefix, resourceIdentifier, dto);
    }

    @Operation(summary = "Find an attribute or association from a model")
    @ApiResponse(responseCode = "200", description = "Attribute or association found")
    @GetMapping(value = "/ontology/{prefix}/{resourceIdentifier}", produces = APPLICATION_JSON_VALUE)
    public ResourceInfoDTO getResource(@PathVariable String prefix, @PathVariable String resourceIdentifier){
        return (ResourceInfoDTO) handleGetResourceOrPropertyShape(prefix, resourceIdentifier);
    }

    @Operation(summary = "Find a property shape from a profile")
    @ApiResponse(responseCode = "200", description = "Property shape found")
    @GetMapping(value = "/profile/{prefix}/{identifier}", produces = APPLICATION_JSON_VALUE)
    public PropertyShapeInfoDTO getPropertyShape(@PathVariable String prefix, @PathVariable String identifier){
        return (PropertyShapeInfoDTO) handleGetResourceOrPropertyShape(prefix, identifier);
    }

    @Operation(summary = "Get an external resource from imports")
    @ApiResponse(responseCode = "200", description = "External class found successfully")
    @GetMapping(value = "/external", produces = APPLICATION_JSON_VALUE)
    public ExternalResourceDTO getExternalClass(@RequestParam String uri) {
        var namespace = NodeFactory.createURI(uri).getNameSpace();
        var model = jenaService.getNamespaceFromImports(namespace);
        var resource = model.getResource(uri);
        if (!model.contains(resource, null, (RDFNode) null)) {
            throw new ResourceNotFoundException(uri);
        }
        return ResourceMapper.mapToExternalResource(resource);
    }

    @Operation(summary = "Check if identifier for resource already exists")
    @ApiResponse(responseCode = "200", description = "Boolean value indicating whether prefix")
    @GetMapping(value = "/{prefix}/free-identifier/{identifier}", produces = APPLICATION_JSON_VALUE)
    public Boolean freeIdentifier(@PathVariable String prefix, @PathVariable String identifier) {
        var graphUri = ModelConstants.SUOMI_FI_NAMESPACE + prefix;
        return !jenaService.doesResourceExistInGraph(graphUri, graphUri + ModelConstants.RESOURCE_SEPARATOR + identifier);
    }

    @Operation(summary = "Delete a resource from a data model")
    @ApiResponse(responseCode = "200", description = "Resource deleted successfully")
    @DeleteMapping(value = "/ontology/{prefix}/{resourceIdentifier}")
    public void deleteResource(@PathVariable String prefix, @PathVariable String resourceIdentifier){
        handleDeleteResourceOrPropertyShape(prefix, resourceIdentifier);
    }

    @Operation(summary = "Delete a resource from a data model")
    @ApiResponse(responseCode = "200", description = "Resource deleted successfully")
    @DeleteMapping(value = "/profile/{prefix}/{resourceIdentifier}")
    public void deletePropertyShape(@PathVariable String prefix, @PathVariable String resourceIdentifier) {
        // TODO: need to check node shapes' sh:property if resource is added there
        handleDeleteResourceOrPropertyShape(prefix, resourceIdentifier);
    }

    private void handleDeleteResourceOrPropertyShape(String prefix, String resourceIdentifier) {
        var modelURI = ModelConstants.SUOMI_FI_NAMESPACE + prefix;
        var resourceUri  = modelURI + ModelConstants.RESOURCE_SEPARATOR + resourceIdentifier;
        if(!jenaService.doesResourceExistInGraph(modelURI , resourceUri)){
            throw new ResourceNotFoundException(resourceUri);
        }
        var model = jenaService.getDataModel(modelURI);
        check(authorizationManager.hasRightToModel(prefix, model));

        jenaService.deleteResource(resourceUri);
        openSearchIndexer.deleteResourceFromIndex(resourceUri);
    }
    private Model handleCreateResourceOrPropertyShape(String prefix, BaseDTO dto) {
        var graphUri = ModelConstants.SUOMI_FI_NAMESPACE + prefix;
        if (jenaService.doesResourceExistInGraph(graphUri, graphUri + prefix + ModelConstants.RESOURCE_SEPARATOR + dto.getIdentifier())){
            throw new MappingError("Already exists");
        }
        var model = jenaService.getDataModel(graphUri);
        check(authorizationManager.hasRightToModel(prefix, model));
        checkDataModelType(model.getResource(graphUri), dto);

        terminologyService.resolveConcept(dto.getSubject());
        return model;
    }

    private void handleUpdateResourceOrPropertyShape(String prefix, String identifier, BaseDTO dto) {
        var graphUri = ModelConstants.SUOMI_FI_NAMESPACE + prefix;

        if(!jenaService.doesResourceExistInGraph(graphUri, graphUri + ModelConstants.RESOURCE_SEPARATOR + identifier)){
            throw new ResourceNotFoundException("Resource does not exist");
        }

        var model = jenaService.getDataModel(graphUri);
        check(authorizationManager.hasRightToModel(prefix, model));
        checkDataModelType(model.getResource(graphUri), dto);

        if (dto instanceof ResourceDTO resourceDTO) {
            ResourceMapper.mapToUpdateResource(graphUri, model, identifier, resourceDTO, userProvider.getUser());
        } else if (dto instanceof PropertyShapeDTO propertyShapeDTO) {
            ResourceMapper.mapToUpdatePropertyShape(graphUri, model, identifier, propertyShapeDTO, userProvider.getUser());
        } else {
            throw new MappingError("Invalid content type for mapping resource");
        }
        terminologyService.resolveConcept(dto.getSubject());

        jenaService.putDataModelToCore(graphUri, model);
        var indexResource = ResourceMapper.mapToIndexResource(model, graphUri + ModelConstants.RESOURCE_SEPARATOR + identifier);
        openSearchIndexer.updateResourceToIndex(indexResource);
    }

    private ResourceInfoBaseDTO handleGetResourceOrPropertyShape(String prefix, String identifier) {
        var graphUri = ModelConstants.SUOMI_FI_NAMESPACE + prefix;
        if(!jenaService.doesResourceExistInGraph(graphUri,graphUri + ModelConstants.RESOURCE_SEPARATOR + identifier)){
            throw new ResourceNotFoundException("Resource does not exist");
        }

        var model = jenaService.getDataModel(graphUri);
        var orgModel = jenaService.getOrganizations();
        var hasRightToModel = authorizationManager.hasRightToModel(prefix, model);
        var modelResource = model.getResource(graphUri);

        ResourceInfoBaseDTO dto;
        if (MapperUtils.isOntology(modelResource)) {
            dto = ResourceMapper.mapToResourceInfoDTO(model, graphUri, identifier, orgModel, hasRightToModel, groupManagementService.mapUser());
        } else if (MapperUtils.isApplicationProfile(modelResource)) {
            dto = ResourceMapper.mapToPropertyShapeInfoDTO(model, graphUri, identifier, orgModel, hasRightToModel, groupManagementService.mapUser());
        } else {
            throw new MappingError("Invalid model");
        }

        terminologyService.mapConcept().accept(dto);
        return dto;
    }

    private void checkDataModelType(Resource modelResource, BaseDTO dto) {
        if (dto instanceof PropertyShapeDTO && MapperUtils.isOntology(modelResource)) {
            throw new MappingError("Cannot add property shape to ontology");
        } else if (dto instanceof ResourceDTO && MapperUtils.isApplicationProfile(modelResource)) {
            throw new MappingError("Cannot add resource to application profile");
        }
    }
}
