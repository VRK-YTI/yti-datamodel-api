package fi.vm.yti.datamodel.api.v2.endpoint;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import fi.vm.yti.datamodel.api.v2.dto.OrganizationDTO;
import fi.vm.yti.datamodel.api.v2.dto.ServiceCategoryDTO;
import fi.vm.yti.datamodel.api.v2.elasticsearch.dto.CountSearchResponse;
import fi.vm.yti.datamodel.api.v2.service.FrontendService;
import fi.vm.yti.datamodel.api.v2.service.SearchIndexService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("v2/frontend")
@Tag(name = "Frontend")
public class FrontendController {

    private static final Logger logger = LoggerFactory.getLogger(FrontendController.class);
    private final SearchIndexService searchIndexService;
    private final ObjectMapper objectMapper;
    private final FrontendService frontendService;

    @Autowired
    public FrontendController(SearchIndexService searchIndexService,
                              ObjectMapper objectMapper,
                              FrontendService frontendService) {
        this.searchIndexService = searchIndexService;
        this.objectMapper = objectMapper;
        this.frontendService = frontendService;
    }

    @Operation(summary = "Get counts", description = "List counts of data model grouped by different search results")
    @ApiResponse(responseCode = "200", description = "Counts response container object as JSON")
    @GetMapping(path = "/counts", produces = MediaType.APPLICATION_JSON_VALUE)
    public JsonNode getCounts() {
        logger.info("GET /counts requested");
        CountSearchResponse response = searchIndexService.getCounts();
        return objectMapper.valueToTree(response);
    }

    @Operation(summary = "Get organizations", description = "List of organizations sorted by name")
    @ApiResponse(responseCode = "200", description = "Organization list as JSON")
    @GetMapping(path = "/organizations", produces = MediaType.APPLICATION_JSON_VALUE)
    public List<OrganizationDTO> getOrganizations(
            @RequestParam("sortLang") String sortLang,
            @RequestParam(value = "includeChildOrganizations", required = false) boolean includeChildOrganizations) {
        logger.info("GET /organizations requested");
        return frontendService.getOrganizations(sortLang, includeChildOrganizations);
    }

    @Operation(summary = "Get service categories", description = "List of service categories sorted by name")
    @ApiResponse(responseCode = "200", description = "Service categories as JSON")
    @GetMapping(path = "/serviceCategories", produces = MediaType.APPLICATION_JSON_VALUE)
    public List<ServiceCategoryDTO> getServiceCategories(@RequestParam("sortLang") String sortLang) {
        logger.info("GET /serviceCategories requested");
        return frontendService.getServiceCategories(sortLang);
    }
}
