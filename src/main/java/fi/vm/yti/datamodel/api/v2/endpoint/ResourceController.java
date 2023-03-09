package fi.vm.yti.datamodel.api.v2.endpoint;

import fi.vm.yti.datamodel.api.security.AuthorizationManager;
import fi.vm.yti.datamodel.api.v2.dto.ModelConstants;
import fi.vm.yti.datamodel.api.v2.dto.ResourceDTO;
import fi.vm.yti.datamodel.api.v2.endpoint.error.MappingError;
import fi.vm.yti.datamodel.api.v2.endpoint.error.ResourceNotFoundException;
import fi.vm.yti.datamodel.api.v2.mapper.ResourceMapper;
import fi.vm.yti.datamodel.api.v2.opensearch.index.OpenSearchIndexer;
import fi.vm.yti.datamodel.api.v2.service.JenaService;
import fi.vm.yti.datamodel.api.v2.validator.ValidResource;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
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

    private final ResourceMapper resourceMapper;

    public ResourceController(JenaService jenaService, AuthorizationManager authorizationManager, OpenSearchIndexer openSearchIndexer, ResourceMapper resourceMapper) {
        this.jenaService = jenaService;
        this.authorizationManager = authorizationManager;
        this.openSearchIndexer = openSearchIndexer;
        this.resourceMapper = resourceMapper;
    }

    @Operation(summary = "Add a class to a model")
    @ApiResponse(responseCode = "200", description = "Class added to model successfully")
    @PutMapping(value = "/{prefix}", consumes = APPLICATION_JSON_VALUE)
    public void createResource(@PathVariable String prefix, @RequestBody @ValidResource ResourceDTO dto){
        var graphUri = ModelConstants.SUOMI_FI_NAMESPACE + prefix;
        if(jenaService.doesResourceExistInGraph(graphUri, graphUri + prefix + "#" + dto.getIdentifier())){
            throw new MappingError("Already exists");
        }

        var model = jenaService.getDataModel(graphUri);
        if(model == null){
            throw new ResourceNotFoundException(prefix);
        }
        check(authorizationManager.hasRightToModel(prefix, model));

        var resourceUri = resourceMapper.mapToResource(graphUri, model, dto);
        jenaService.putDataModelToCore(graphUri, model);
        var indexClass = resourceMapper.mapToIndexResource(model, resourceUri);
        openSearchIndexer.createResourceToIndex(indexClass);
    }

    @Operation(summary = "Update a resource in a model")
    @ApiResponse(responseCode = "200", description = "Class added to model successfully")
    @PutMapping(value = "/{prefix}/{resourceIdentifier}", consumes = APPLICATION_JSON_VALUE)
    public void updateResource(@PathVariable String prefix, @RequestBody @ValidResource(updateProperty = true) ResourceDTO dto){
        if(!jenaService.doesResourceExistInGraph(ModelConstants.SUOMI_FI_NAMESPACE + prefix, ModelConstants.SUOMI_FI_NAMESPACE + prefix + "#" + dto.getIdentifier())){
            throw new ResourceNotFoundException("Resource does not exist");
        }

        var model = jenaService.getDataModel(ModelConstants.SUOMI_FI_NAMESPACE + prefix);
        if(model == null){
            throw new ResourceNotFoundException(prefix);
        }
        //check(authorizationManager.hasRightToModel(prefix, model));

        //resourceMapper.mapToResource(ModelConstants.SUOMI_FI_NAMESPACE + prefix, model, dto);
        //jenaService.createDataModel(ModelConstants.SUOMI_FI_NAMESPACE + prefix, model);
        // var indexClass = classMapper.mapToIndexClass(model, classURi);
        //openSearchIndexer.createClassToIndex(indexClass);
    }

}
