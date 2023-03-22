package fi.vm.yti.datamodel.api.v2.endpoint;

import fi.vm.yti.datamodel.api.security.AuthorizationManager;
import fi.vm.yti.datamodel.api.v2.dto.ClassDTO;
import fi.vm.yti.datamodel.api.v2.dto.ClassInfoDTO;
import fi.vm.yti.datamodel.api.v2.dto.ModelConstants;
import fi.vm.yti.datamodel.api.v2.endpoint.error.MappingError;
import fi.vm.yti.datamodel.api.v2.endpoint.error.ResourceNotFoundException;
import fi.vm.yti.datamodel.api.v2.mapper.ClassMapper;
import fi.vm.yti.datamodel.api.v2.mapper.ResourceMapper;
import fi.vm.yti.datamodel.api.v2.opensearch.index.OpenSearchIndexer;
import fi.vm.yti.datamodel.api.v2.service.JenaService;
import fi.vm.yti.datamodel.api.v2.validator.ValidClass;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

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

    public ClassController(AuthorizationManager authorizationManager,
                           JenaService jenaService,
                           OpenSearchIndexer openSearchIndexer){
        this.authorizationManager = authorizationManager;
        this.jenaService = jenaService;
        this.openSearchIndexer = openSearchIndexer;
    }

    @Operation(summary = "Add a class to a model")
    @ApiResponse(responseCode = "200", description = "Class added to model successfully")
    @PutMapping(value = "/{prefix}", consumes = APPLICATION_JSON_VALUE)
    public void createClass(@PathVariable String prefix, @RequestBody @ValidClass ClassDTO classDTO){
        var modelURI = ModelConstants.SUOMI_FI_NAMESPACE + prefix;
        if(jenaService.doesResourceExistInGraph(modelURI, modelURI + "#" + classDTO.getIdentifier())){
            throw new MappingError("Class already exists");
        }
        var model = jenaService.getDataModel(modelURI);
        if(model == null){
            throw new ResourceNotFoundException(modelURI);
        }
        check(authorizationManager.hasRightToModel(prefix, model));

        var classURi = ClassMapper.createClassAndMapToModel(modelURI, model, classDTO);
        jenaService.putDataModelToCore(modelURI, model);
        var indexClass = ResourceMapper.mapToIndexResource(model, classURi);
        openSearchIndexer.createResourceToIndex(indexClass);
    }

    @Operation(summary = "Update a class in a model")
    @ApiResponse(responseCode =  "200", description = "Class updated in model successfully")
    @PutMapping(value = "/{prefix}/{classIdentifier}", consumes = APPLICATION_JSON_VALUE)
    public void updateClass(@PathVariable String prefix, @PathVariable String classIdentifier, @RequestBody @ValidClass(updateClass = true) ClassDTO classDTO){
        logger.info("Updating class {}", classIdentifier);

        var graph = ModelConstants.SUOMI_FI_NAMESPACE + prefix;
        var classURI = graph + "#" + classIdentifier;
        if(!jenaService.doesResourceExistInGraph(graph, graph + "#" + classIdentifier)){
            throw new ResourceNotFoundException(classIdentifier);
        }

        var model = jenaService.getDataModel(graph);
        check(authorizationManager.hasRightToModel(prefix, model));

        var classResource = model.getResource(classURI);

        ClassMapper.mapToUpdateClass(model, graph, classResource, classDTO);
        jenaService.putDataModelToCore(graph, model);

        var indexClass = ResourceMapper.mapToIndexResource(model, classURI);
        openSearchIndexer.updateResourceToIndex(indexClass);
    }

    @Operation(summary = "Get a class from a data model")
    @ApiResponse(responseCode = "200", description = "Class found successfully")
    @GetMapping(value = "/{prefix}/{classIdentifier}", produces = APPLICATION_JSON_VALUE)
    public ClassInfoDTO getClass(@PathVariable String prefix, @PathVariable String classIdentifier){
        var modelURI = ModelConstants.SUOMI_FI_NAMESPACE + prefix;
        if(!jenaService.doesResourceExistInGraph(modelURI , modelURI + "#" + classIdentifier)){
            throw new ResourceNotFoundException(modelURI + "#" + classIdentifier);
        }
        var model = jenaService.getDataModel(modelURI);
        if(model == null){
            throw new ResourceNotFoundException(modelURI);
        }
        var hasRightToModel = authorizationManager.hasRightToModel(prefix, model);

        var orgModel = jenaService.getOrganizations();

        return ClassMapper.mapToClassDTO(model, modelURI, classIdentifier, orgModel, hasRightToModel);
    }
}
