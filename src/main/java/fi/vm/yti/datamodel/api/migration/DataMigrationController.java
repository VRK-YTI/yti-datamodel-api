package fi.vm.yti.datamodel.api.migration;

import fi.vm.yti.datamodel.api.security.AuthorizationManager;
import fi.vm.yti.datamodel.api.v2.dto.ModelConstants;
import fi.vm.yti.datamodel.api.v2.utils.DataModelUtils;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

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

    @GetMapping
    public void migrate(@RequestParam String prefix) {
        check(authorizationManager.hasRightToDoMigration());

        var oldData = ModelFactory.createDefaultModel();

        /*
        var stream = getClass().getResourceAsStream("/merialsuun_simple.ttl");
        RDFDataMgr.read(oldData, stream, RDFLanguages.TURTLE);
        */

        var modelURI = ModelConstants.SUOMI_FI_NAMESPACE + prefix;
        RDFParser.create()
                .source(serviceURL + "/datamodel-api/api/v1/exportModel?graph=" + DataModelUtils.encode(modelURI))
                .lang(Lang.JSONLD)
                .acceptHeader("application/ld+json")
                .parse(oldData);

        try {
            migrationService.migrateLibrary(prefix, oldData);
        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
        }
    }
}
