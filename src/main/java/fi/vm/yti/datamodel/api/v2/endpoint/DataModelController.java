package fi.vm.yti.datamodel.api.v2.endpoint;

import fi.vm.yti.datamodel.api.security.AuthorizationManager;
import fi.vm.yti.datamodel.api.v2.dto.DataModelDTO;
import fi.vm.yti.datamodel.api.v2.dto.DataModelInfoDTO;
import fi.vm.yti.datamodel.api.v2.dto.ModelConstants;
import fi.vm.yti.datamodel.api.v2.dto.ModelType;
import fi.vm.yti.datamodel.api.v2.endpoint.error.ResourceNotFoundException;
import fi.vm.yti.datamodel.api.v2.mapper.ModelMapper;
import fi.vm.yti.datamodel.api.v2.opensearch.index.OpenSearchIndexer;
import fi.vm.yti.datamodel.api.v2.service.CodeListService;
import fi.vm.yti.datamodel.api.v2.service.GroupManagementService;
import fi.vm.yti.datamodel.api.v2.service.JenaService;
import fi.vm.yti.datamodel.api.v2.service.TerminologyService;
import fi.vm.yti.datamodel.api.v2.validator.ValidDatamodel;
import fi.vm.yti.datamodel.api.v2.validator.ValidationConstants;
import fi.vm.yti.security.AuthenticatedUserProvider;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.net.URISyntaxException;

import static fi.vm.yti.security.AuthorizationException.check;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@RestController
@RequestMapping("v2/model")
@Tag(name = "Model" )
@Validated
public class DataModelController {

    private static final Logger logger = LoggerFactory.getLogger(DataModelController.class);

    private final AuthorizationManager authorizationManager;

    private final OpenSearchIndexer openSearchIndexer;

    private final JenaService jenaService;

    private final ModelMapper mapper;

    private final TerminologyService terminologyService;

    private final CodeListService codelistService;

    private final AuthenticatedUserProvider userProvider;

    private final GroupManagementService groupManagementService;

    public DataModelController(JenaService jenaService,
                               AuthorizationManager authorizationManager,
                               OpenSearchIndexer openSearchIndexer,
                               ModelMapper modelMapper,
                               TerminologyService terminologyService,
                               CodeListService codelistService,
                               AuthenticatedUserProvider userProvider,
                               GroupManagementService groupManagementService) {
        this.authorizationManager = authorizationManager;
        this.mapper = modelMapper;
        this.openSearchIndexer = openSearchIndexer;
        this.jenaService = jenaService;
        this.terminologyService = terminologyService;
        this.codelistService = codelistService;
        this.userProvider = userProvider;
        this.groupManagementService = groupManagementService;
    }

    private String createModel(DataModelDTO modelDTO, ModelType modelType) {
        logger.info("Create model {}", modelDTO);
        check(authorizationManager.hasRightToAnyOrganization(modelDTO.getOrganizations()));
        var graphUri = ModelConstants.SUOMI_FI_NAMESPACE + modelDTO.getPrefix();

        terminologyService.resolveTerminology(modelDTO.getTerminologies());
        codelistService.resolveCodelistScheme(modelDTO.getCodeLists());
        var jenaModel = mapper.mapToJenaModel(modelDTO, modelType, userProvider.getUser());

        jenaService.putDataModelToCore(graphUri, jenaModel);

        var indexModel = mapper.mapToIndexModel(modelDTO.getPrefix(), jenaModel);
        openSearchIndexer.createModelToIndex(indexModel);
        return graphUri;
    }

    @Operation(summary = "Create a new library")
    @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "The JSON data for the new core model")
    @ApiResponse(responseCode = "201", description = "The ID for the newly created model")
    @PostMapping(path = "/library", produces = APPLICATION_JSON_VALUE, consumes = APPLICATION_JSON_VALUE)
    public ResponseEntity<String> createLibrary(@ValidDatamodel(modelType = ModelType.LIBRARY) @RequestBody DataModelDTO modelDTO) throws URISyntaxException {
        var uri = createModel(modelDTO, ModelType.LIBRARY);
        return ResponseEntity.created(new URI(uri)).build();
    }

    @Operation(summary = "Create a new application profile")
    @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "The JSON data for the new application profile")
    @ApiResponse(responseCode = "201", description = "The ID for the newly created model")
    @PostMapping(path = "/profile", produces = APPLICATION_JSON_VALUE, consumes = APPLICATION_JSON_VALUE)
    public ResponseEntity<String> createProfile(@ValidDatamodel(modelType = ModelType.PROFILE) @RequestBody DataModelDTO modelDTO) throws URISyntaxException {
        var uri = createModel(modelDTO, ModelType.PROFILE);
        return ResponseEntity.created(new URI(uri)).build();
    }

    @Operation(summary = "Modify library")
    @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "The JSON data for the new model node")
    @ApiResponse(responseCode = "204", description = "The ID for the newly created model")
    @PutMapping(path = "/library/{prefix}", produces = APPLICATION_JSON_VALUE, consumes = APPLICATION_JSON_VALUE)
    public ResponseEntity<Void> updateLibrary(@ValidDatamodel(modelType = ModelType.LIBRARY, updateModel = true) @RequestBody DataModelDTO modelDTO,
                                           @PathVariable String prefix) {
        updateModel(modelDTO, prefix);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Modify application profile")
    @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "The JSON data for the new model node")
    @ApiResponse(responseCode = "204", description = "The ID for the newly created model")
    @PutMapping(path = "/profile/{prefix}", produces = APPLICATION_JSON_VALUE, consumes = APPLICATION_JSON_VALUE)
    public ResponseEntity<Void> updateProfile(@ValidDatamodel(modelType = ModelType.PROFILE, updateModel = true) @RequestBody DataModelDTO modelDTO,
                              @PathVariable String prefix) {
        updateModel(modelDTO, prefix);
        return ResponseEntity.noContent().build();
    }

    public void updateModel(DataModelDTO modelDTO, String prefix) {
        logger.info("Updating model {}", modelDTO);

        var oldModel = jenaService.getDataModel(ModelConstants.SUOMI_FI_NAMESPACE + prefix);

        check(authorizationManager.hasRightToModel(prefix, oldModel));

        terminologyService.resolveTerminology(modelDTO.getTerminologies());
        codelistService.resolveCodelistScheme(modelDTO.getCodeLists());

        var jenaModel = mapper.mapToUpdateJenaModel(prefix, modelDTO, oldModel, userProvider.getUser());

        jenaService.putDataModelToCore(ModelConstants.SUOMI_FI_NAMESPACE + prefix, jenaModel);


        var indexModel = mapper.mapToIndexModel(prefix, jenaModel);
        openSearchIndexer.updateModelToIndex(indexModel);
    }

    @Operation(summary = "Get a model from fuseki")
    @ApiResponse(responseCode = "200", description = "Datamodel object for the found model")
    @GetMapping(value = "/{prefix}", produces = APPLICATION_JSON_VALUE)
    public DataModelInfoDTO getModel(@PathVariable String prefix){
        var model = jenaService.getDataModel(ModelConstants.SUOMI_FI_NAMESPACE + prefix);
        var hasRightsToModel = authorizationManager.hasRightToModel(prefix, model);

        var userMapper = hasRightsToModel ? groupManagementService.mapUser() : null;
        return mapper.mapToDataModelDTO(prefix, model, userMapper);
    }

    @Operation(summary = "Check if prefix already exists")
    @ApiResponse(responseCode = "200", description = "Boolean value indicating whether prefix")
    @GetMapping(value = "/{prefix}/exists", produces = APPLICATION_JSON_VALUE)
    public Boolean freePrefix(@PathVariable String prefix) {
        if (ValidationConstants.RESERVED_WORDS.contains(prefix)) {
            return true;
        }
        return jenaService.doesDataModelExist(ModelConstants.SUOMI_FI_NAMESPACE + prefix);
    }

    @Operation(summary = "Delete a model from fuseki")
    @ApiResponse(responseCode = "200", description = "Model deleted successfully")
    @DeleteMapping(value = "/{prefix}")
    public void deleteModel(@PathVariable String prefix) {
        var modelUri = ModelConstants.SUOMI_FI_NAMESPACE + prefix;
        if(!jenaService.doesDataModelExist(modelUri)){
            throw new ResourceNotFoundException(modelUri);
        }
        var model = jenaService.getDataModel(modelUri);
        check(authorizationManager.hasRightToModel(prefix, model));

        jenaService.deleteDataModel(modelUri);
        openSearchIndexer.deleteModelFromIndex(modelUri);
    }
}
