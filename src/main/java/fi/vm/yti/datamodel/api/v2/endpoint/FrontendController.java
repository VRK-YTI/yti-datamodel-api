package fi.vm.yti.datamodel.api.v2.endpoint;

import fi.vm.yti.datamodel.api.v2.dto.ModelConstants;
import fi.vm.yti.datamodel.api.v2.dto.OrganizationDTO;
import fi.vm.yti.datamodel.api.v2.dto.ServiceCategoryDTO;
import fi.vm.yti.datamodel.api.v2.opensearch.dto.*;
import fi.vm.yti.datamodel.api.v2.opensearch.index.IndexModel;
import fi.vm.yti.datamodel.api.v2.opensearch.index.IndexResource;
import fi.vm.yti.datamodel.api.v2.opensearch.index.IndexResourceInfo;
import fi.vm.yti.datamodel.api.v2.service.FrontendService;
import fi.vm.yti.datamodel.api.v2.service.NamespaceService;
import fi.vm.yti.datamodel.api.v2.service.SearchIndexService;
import fi.vm.yti.security.AuthenticatedUserProvider;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.Collection;
import java.util.Set;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@RestController
@RequestMapping("v2/frontend")
@Tag(name = "Frontend")
public class FrontendController {

    private static final Logger logger = LoggerFactory.getLogger(FrontendController.class);
    private final SearchIndexService searchIndexService;
    private final FrontendService frontendService;
    private final AuthenticatedUserProvider userProvider;
    private final NamespaceService namespaceService;

    @Autowired
    public FrontendController(SearchIndexService searchIndexService,
                              FrontendService frontendService,
                              AuthenticatedUserProvider userProvider,
                              NamespaceService namespaceService) {
        this.searchIndexService = searchIndexService;
        this.frontendService = frontendService;
        this.userProvider = userProvider;
        this.namespaceService = namespaceService;
    }

    @Operation(summary = "Get counts", description = "List counts of data model grouped by different search results")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Counts response container object as JSON")
    })
    @GetMapping(path = "/counts", produces = MediaType.APPLICATION_JSON_VALUE)
    public CountSearchResponse getCounts(@Parameter(description = "Count request parameters") CountRequest request) {
        logger.info("GET /counts requested");
        return searchIndexService.getCounts(request, userProvider.getUser());
    }

    @Operation(summary = "Get organizations", description = "List of organizations sorted by name")
    @ApiResponse(responseCode = "200", description = "Organization list as JSON")
    @GetMapping(path = "/organizations", produces = MediaType.APPLICATION_JSON_VALUE)
    public Collection<OrganizationDTO> getOrganizations(
            @RequestParam(required = false, defaultValue = ModelConstants.DEFAULT_LANGUAGE) @Parameter(description = "Alphabetical sorting language") String sortLang,
            @RequestParam(required = false) @Parameter(description = "Include child organizations in response") boolean includeChildOrganizations) {
        logger.info("GET /organizations requested");
        return frontendService.getOrganizations(sortLang, includeChildOrganizations);
    }

    @Operation(summary = "Get service categories", description = "List of service categories sorted by name")
    @ApiResponse(responseCode = "200", description = "Service categories as JSON")
    @GetMapping(path = "/service-categories", produces = MediaType.APPLICATION_JSON_VALUE)
    public Collection<ServiceCategoryDTO> getServiceCategories(@RequestParam(required = false, defaultValue = ModelConstants.DEFAULT_LANGUAGE) @Parameter(description = "Alphabetical sorting language") String sortLang) {
        logger.info("GET /serviceCategories requested");
        return frontendService.getServiceCategories(sortLang);
    }

    @Operation(summary = "Search models")
    @ApiResponse(responseCode = "200", description = "List of data model objects")
    @GetMapping(value = "/search-models", produces = APPLICATION_JSON_VALUE)
    public SearchResponseDTO<IndexModel> getModels(@Parameter(description = "Data model search parameters") ModelSearchRequest request) {
        return searchIndexService.searchModels(request, userProvider.getUser());
    }

    @Operation(summary = "Search resources", description = "List of resources")
    @ApiResponse(responseCode = "200", description = "List of resources as JSON")
    @GetMapping(path = "/search-internal-resources", produces = APPLICATION_JSON_VALUE)
    public SearchResponseDTO<IndexResource> getInternalResources(@Parameter(description = "Resource search parameters") ResourceSearchRequest request) throws IOException {
        return searchIndexService.searchInternalResources(request, userProvider.getUser());
    }

    @Operation(summary = "Search resources with data model information", description = "List of resources")
    @ApiResponse(responseCode = "200", description = "List of resources as JSON")
    @GetMapping(path = "/search-internal-resources-info", produces = APPLICATION_JSON_VALUE)
    public SearchResponseDTO<IndexResourceInfo> getInternalResourcesInfo(@Parameter(description = "Resource search parameters") ResourceSearchRequest request) throws IOException {
        return searchIndexService.searchInternalResourcesWithInfo(request, userProvider.getUser());
    }

    @Operation(summary = "Get supported data types")
    @ApiResponse(responseCode = "200", description = "List of supported data types")
    @GetMapping(path = "/data-types", produces = APPLICATION_JSON_VALUE)
    public Collection<String> getSupportedDataTypes() {
        return ModelConstants.SUPPORTED_DATA_TYPES;
    }

    @Operation(summary = "Get resolved external namespaces")
    @ApiResponse(responseCode = "200", description = "List of resolved namespaces")
    @GetMapping(path = "/namespaces", produces = APPLICATION_JSON_VALUE)
    public Set<String> getResolvedNamespaces() {
        return namespaceService.getResolvedNamespaces();
    }
}
