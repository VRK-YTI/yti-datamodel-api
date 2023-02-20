package fi.vm.yti.datamodel.api.v2.endpoint;

import fi.vm.yti.datamodel.api.v2.dto.ModelConstants;
import fi.vm.yti.datamodel.api.v2.dto.OrganizationDTO;
import fi.vm.yti.datamodel.api.v2.dto.ServiceCategoryDTO;
import fi.vm.yti.datamodel.api.v2.opensearch.dto.*;
import fi.vm.yti.datamodel.api.v2.opensearch.index.IndexClass;
import fi.vm.yti.datamodel.api.v2.opensearch.index.IndexModel;
import fi.vm.yti.datamodel.api.v2.service.FrontendService;
import fi.vm.yti.datamodel.api.v2.service.SearchIndexService;
import fi.vm.yti.security.AuthenticatedUserProvider;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@RestController
@RequestMapping("v2/frontend")
@Tag(name = "Frontend")
public class FrontendController {

    private static final Logger logger = LoggerFactory.getLogger(FrontendController.class);
    private final SearchIndexService searchIndexService;
    private final FrontendService frontendService;
    private final AuthenticatedUserProvider userProvider;

    @Autowired
    public FrontendController(SearchIndexService searchIndexService,
                              FrontendService frontendService,
                              AuthenticatedUserProvider userProvider) {
        this.searchIndexService = searchIndexService;
        this.frontendService = frontendService;
        this.userProvider = userProvider;
    }

    @Operation(summary = "Get counts", description = "List counts of data model grouped by different search results")
    @ApiResponse(responseCode = "200", description = "Counts response container object as JSON")
    @GetMapping(path = "/counts", produces = MediaType.APPLICATION_JSON_VALUE)
    public CountSearchResponse getCounts() {
        logger.info("GET /counts requested");
        return searchIndexService.getCounts();
    }

    @Operation(summary = "Get organizations", description = "List of organizations sorted by name")
    @ApiResponse(responseCode = "200", description = "Organization list as JSON")
    @GetMapping(path = "/organizations", produces = MediaType.APPLICATION_JSON_VALUE)
    public List<OrganizationDTO> getOrganizations(
            @RequestParam(value = "sortLang", required = false, defaultValue = ModelConstants.DEFAULT_LANGUAGE) String sortLang,
            @RequestParam(value = "includeChildOrganizations", required = false) boolean includeChildOrganizations) {
        logger.info("GET /organizations requested");
        return frontendService.getOrganizations(sortLang, includeChildOrganizations);
    }

    @Operation(summary = "Get service categories", description = "List of service categories sorted by name")
    @ApiResponse(responseCode = "200", description = "Service categories as JSON")
    @GetMapping(path = "/serviceCategories", produces = MediaType.APPLICATION_JSON_VALUE)
    public List<ServiceCategoryDTO> getServiceCategories(@RequestParam(value = "sortLang", required = false, defaultValue = ModelConstants.DEFAULT_LANGUAGE) String sortLang) {
        logger.info("GET /serviceCategories requested");
        return frontendService.getServiceCategories(sortLang);
    }

    @Operation(summary = "Search models")
    @ApiResponse(responseCode = "200", description = "List of data model objects")
    @PostMapping(value = "/searchModels", produces = APPLICATION_JSON_VALUE)
    public SearchResponseDTO<IndexModel> getModels(@RequestBody ModelSearchRequest request) {
        return searchIndexService.searchModels(request, userProvider.getUser());
    }

    @Operation(summary = "Search classes", description = "List of classes")
    @ApiResponse(responseCode = "200", description = "List of classes as JSON")
    @GetMapping(path = "/searchInternalClasses", produces = APPLICATION_JSON_VALUE, consumes = APPLICATION_JSON_VALUE)
    public SearchResponseDTO<IndexClass> getClasses(@RequestBody ClassSearchRequest request) throws IOException {
        return searchIndexService.searchInternalClasses(request);
    }
}
