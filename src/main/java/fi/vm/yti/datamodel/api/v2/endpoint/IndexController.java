package fi.vm.yti.datamodel.api.v2.endpoint;

import fi.vm.yti.datamodel.api.security.AuthorizationManager;
import fi.vm.yti.datamodel.api.v2.opensearch.index.OpenSearchIndexer;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import static fi.vm.yti.security.AuthorizationException.check;

@RestController
@RequestMapping("v2/index")
@Tag(name = "Index" )
public class IndexController {


    private final OpenSearchIndexer indexer;
    private final AuthorizationManager authorizationManager;

    public IndexController(OpenSearchIndexer indexer,
                           AuthorizationManager authorizationManager) {
        this.indexer = indexer;
        this.authorizationManager = authorizationManager;
    }



    @Operation(summary = "Reindex all datamodels")
    @GetMapping(value = "/reindex")
    public void reIndex(@RequestParam(required = false) String index) {
        check(authorizationManager.hasRightToDropDatabase());
        if(index == null){
            indexer.reindex();
            return;
        }
        switch (index){
            case OpenSearchIndexer.OPEN_SEARCH_INDEX_EXTERNAL -> indexer.initExternalResourceIndex();
            case OpenSearchIndexer.OPEN_SEARCH_INDEX_MODEL -> indexer.initModelIndex();
            case OpenSearchIndexer.OPEN_SEARCH_INDEX_RESOURCE -> indexer.initResourceIndex();
            default -> throw new IllegalArgumentException("Given value not allowed");
        }
    }
}
