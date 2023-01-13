package fi.vm.yti.datamodel.api.v2.endpoint;

import fi.vm.yti.datamodel.api.security.AuthorizationManager;
import fi.vm.yti.datamodel.api.v2.elasticsearch.index.ElasticIndexer;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static fi.vm.yti.security.AuthorizationException.check;

@RestController
@RequestMapping("v2/index")
@Tag(name = "Index" )
public class IndexController {


    private final ElasticIndexer indexer;
    private final AuthorizationManager authorizationManager;

    public IndexController(ElasticIndexer indexer,
                           AuthorizationManager authorizationManager) {
        this.indexer = indexer;
        this.authorizationManager = authorizationManager;
    }



    @Operation(summary = "Reindex all datamodels")
    @GetMapping(value = "/reindex")
    public void reIndex() {
        check(authorizationManager.hasRightToDropDatabase());

        indexer.reindex();
    }
}
