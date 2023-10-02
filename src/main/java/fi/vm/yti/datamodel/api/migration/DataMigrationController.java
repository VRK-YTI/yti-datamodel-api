package fi.vm.yti.datamodel.api.migration;

import fi.vm.yti.datamodel.api.security.AuthorizationManager;
import fi.vm.yti.datamodel.api.v2.dto.ModelConstants;
import fi.vm.yti.datamodel.api.v2.utils.DataModelUtils;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.riot.RDFLanguages;
import org.apache.jena.riot.RDFParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import static fi.vm.yti.security.AuthorizationException.check;

@RestController
@RequestMapping("v2/migration")
public class DataMigrationController {

    private static final Logger LOG = LoggerFactory.getLogger(DataMigrationController.class);

    @Value("${datamodel.v1.migration.url:https://tietomallit.dev.yti.cloud.dvv.fi}")
    String serviceURL;

    private final V1DataMigrationService migrationService;
    private final AuthorizationManager authorizationManager;

    public DataMigrationController(V1DataMigrationService migrationService,
                                   AuthorizationManager authorizationManager) {
        this.migrationService = migrationService;
        this.authorizationManager = authorizationManager;
    }

    @PostMapping
    public ResponseEntity<Void> migrate(@RequestParam String prefix) {
        check(authorizationManager.hasRightToDoMigration());

        var oldData = ModelFactory.createDefaultModel();

        /* Use static file
        var stream = getClass().getResourceAsStream("/merialsuun_simple.ttl");
        RDFDataMgr.read(oldData, stream, RDFLanguages.TURTLE);
        */

        var modelURI = ModelConstants.SUOMI_FI_NAMESPACE + prefix;
        LOG.info("Fetching model {}", modelURI);
        RDFParser.create()
                .source(serviceURL + "/datamodel-api/api/v1/exportModel?graph=" + DataModelUtils.encode(modelURI))
                .lang(Lang.JSONLD)
                .acceptHeader("application/ld+json")
                .parse(oldData);

        try {
            migrationService.migrateLibrary(prefix, oldData);
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @PostMapping("/positions")
    public ResponseEntity<Void> migrateVisualization(@RequestParam String prefix) {
        check(authorizationManager.hasRightToDoMigration());

        var oldVisualization = ModelFactory.createDefaultModel();
        var modelURI = ModelConstants.SUOMI_FI_NAMESPACE + prefix;
        LOG.info("Fetching model {}", modelURI);
        RDFParser.create()
                .source(serviceURL + "/datamodel-api/api/v1/modelPositions?model=" + DataModelUtils.encode(modelURI))
                .lang(Lang.JSONLD)
                .acceptHeader("application/ld+json")
                .parse(oldVisualization);

        migrationService.migratePositions(prefix, oldVisualization);
        return ResponseEntity.noContent().build();
    }
}
