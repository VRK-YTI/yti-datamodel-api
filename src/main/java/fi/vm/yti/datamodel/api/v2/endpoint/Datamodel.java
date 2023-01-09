package fi.vm.yti.datamodel.api.v2.endpoint;

import fi.vm.yti.datamodel.api.security.AuthorizationManager;
import fi.vm.yti.datamodel.api.v2.dto.DataModelDTO;
import fi.vm.yti.datamodel.api.v2.dto.ModelConstants;
import fi.vm.yti.datamodel.api.v2.elasticsearch.index.ElasticIndexer;
import fi.vm.yti.datamodel.api.v2.mapper.ModelMapper;
import fi.vm.yti.datamodel.api.v2.service.JenaService;
import fi.vm.yti.datamodel.api.v2.validator.ValidDatamodel;
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
//FIXME api/v2/model will not work at all, possible interference from Jersey?
@RequestMapping("v2/model")
@Tag(name = "Model" )
@Validated
public class Datamodel {

    private static final Logger logger = LoggerFactory.getLogger(Datamodel.class);

    private final AuthorizationManager authorizationManager;

    private final ElasticIndexer elasticIndexer;

    private final JenaService jenaService;

    private final ModelMapper mapper;

    public Datamodel(JenaService jenaService, AuthorizationManager authorizationManager, ElasticIndexer elasticIndexer, ModelMapper modelMapper) {
        this.authorizationManager = authorizationManager;
        this.mapper = modelMapper;
        this.elasticIndexer = elasticIndexer;
        this.jenaService = jenaService;
    }

    @Operation(summary = "Create a new model")
    @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "The JSON data for the new model node")
    @ApiResponse(responseCode = "200", description = "The ID for the newly created model")
    @PutMapping(produces = APPLICATION_JSON_VALUE, consumes = APPLICATION_JSON_VALUE)
    public void createModel(@ValidDatamodel @RequestBody DataModelDTO modelDTO) {
        logger.info("Create model {}", modelDTO);
        check(authorizationManager.hasRightToAnyOrganization(modelDTO.getOrganizations()));

        var jenaModel = mapper.mapToJenaModel(modelDTO);

        jenaService.createDataModel(ModelConstants.SUOMI_FI_NAMESPACE + modelDTO.getPrefix(), jenaModel);

        var indexModel = mapper.mapToIndexModel(modelDTO.getPrefix(), jenaModel);
        elasticIndexer.createModelToIndex(indexModel);
    }

    @Operation(summary = "Create a new model")
    @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "The JSON data for the new model node")
    @ApiResponse(responseCode = "200", description = "The ID for the newly created model")
    @PostMapping(path = "/{prefix}", produces = APPLICATION_JSON_VALUE, consumes = APPLICATION_JSON_VALUE)
    public void updateModel(@ValidDatamodel(updateModel = true) @RequestBody DataModelDTO modelDTO,
                            @PathVariable String prefix) {
        logger.info("Updating model {}", modelDTO);
        check(authorizationManager.hasRightToAnyOrganization(modelDTO.getOrganizations()));

        var jenaModel = mapper.mapToUpdateJenaModel(prefix, modelDTO);

        jenaService.createDataModel(ModelConstants.SUOMI_FI_NAMESPACE + modelDTO.getPrefix(), jenaModel);

        var indexModel = mapper.mapToIndexModel(prefix, jenaModel);
        elasticIndexer.updateModelToIndex(indexModel);
    }

    @Operation(summary = "Get a model from fuseki")
    @ApiResponse(responseCode = "200", description = "Datamodel object for the found model")
    @GetMapping(value = "/{prefix}", produces = APPLICATION_JSON_VALUE)
    public DataModelDTO getModel(@PathVariable String prefix){
        var model = jenaService.getDataModel(ModelConstants.SUOMI_FI_NAMESPACE + prefix);
        return mapper.mapToDataModelDTO(prefix, model);
    }

}
