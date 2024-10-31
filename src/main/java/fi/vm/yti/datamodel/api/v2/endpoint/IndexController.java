package fi.vm.yti.datamodel.api.v2.endpoint;

import fi.vm.yti.datamodel.api.v2.service.IndexService;
import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("v2/index")
@Tag(name = "Index" )
public class IndexController {

    private final IndexService indexService;

    public IndexController(IndexService indexService) {
        this.indexService = indexService;
    }

    @Hidden
    @Operation(summary = "Reindex all indexes or a certain index")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "401", description = "Reindexed successfully"),
            @ApiResponse(responseCode = "401", description = "Current user does not have rights for this action"),
    })
    @PostMapping(value = "/reindex")
    public void reIndex(@RequestParam(required = false) @Parameter(description = "OpenSearch index") String index) {
        indexService.reindex(index);
    }
}
