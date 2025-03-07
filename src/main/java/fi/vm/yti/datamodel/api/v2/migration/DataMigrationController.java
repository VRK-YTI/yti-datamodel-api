package fi.vm.yti.datamodel.api.v2.migration;

import fi.vm.yti.datamodel.api.v2.security.DataModelAuthorizationManager;
import fi.vm.yti.datamodel.api.v2.utils.DataModelUtils;
import io.swagger.v3.oas.annotations.Hidden;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import static fi.vm.yti.datamodel.api.v2.migration.V1DataMigrationService.OLD_NAMESPACE;
import static fi.vm.yti.security.AuthorizationException.check;

@RestController
@RequestMapping("v2/migration")
@Hidden
public class DataMigrationController {

    private static final Logger LOG = LoggerFactory.getLogger(DataMigrationController.class);

    /**
     * curl -X POST -H 'content-type: application/json' 'https://tietomallit.suomi.fi/datamodel-api/api/v1/searchModels' -d '
     * {
     *   "status": [
     *     "VALID","DRAFT"
     *   ],
     *   "type": [
     *     "library"
     *   ],
     *   "pageSize": 100,
     *   "pageFrom": 0
     * }' | jq '.models | map(.prefix) | join(",")'
     *
     * 'cmlife' is incomplete, added manually to list
     */
    private static final String PREFIXES = "kmr,busdoc,edu,eftivoc,en16931-1,etvtkuhy,etvttyol,fi-eauther,fi-eauthpe," +
            "fi-eauthun,ipvs,isa2core,jhs210,matike,ntp,rakht,rakmatulo,raktkk,ryyo,tiha,tihatos,vrtfin,xbrlje," +
            "zoneatlas,kultymp,rytj-raklu,kiintsena,laatuke,lupaha,tutkimus,etvtilym,etvttyok,fi-eauth,fi-eauthor," +
            "forests,jhs,laatu,livy,merialsuun,ncdrm,ncv,ntp_v2,opiskh_01x,saftdk09,suomisdk,thk,tihao," +
            "vaka,vcdm,xbrl-gl,ysd,aav,digiv,digione,rak,jtt,aedu,cmlife";

    @Value("${datamodel.v1.migration.url:https://tietomallit.dev.yti.cloud.dvv.fi}")
    String serviceURL;

    private final V1DataMigrationService migrationService;
    private final DataModelAuthorizationManager authorizationManager;

    public DataMigrationController(V1DataMigrationService migrationService,
                                   DataModelAuthorizationManager authorizationManager) {
        this.migrationService = migrationService;
        this.authorizationManager = authorizationManager;
    }

    @PostMapping
    public ResponseEntity<Void> migrateAll() {
        return migrate(PREFIXES);
    }

    @PostMapping("/prefix")
    public ResponseEntity<Void> migrate(@RequestParam String prefix) {
        check(authorizationManager.hasRightToDoMigration());
        migrationService.startMigrationAsync(prefix);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/positions")
    public ResponseEntity<Void> migrateVisualization(@RequestParam String prefix) {
        LOG.info("Migrate visualization {}", prefix);
        check(authorizationManager.hasRightToDoMigration());

        var oldVisualization = ModelFactory.createDefaultModel();
        var oldData = ModelFactory.createDefaultModel();

        var modelURI = OLD_NAMESPACE + prefix;
        LOG.info("Fetching model {}", modelURI);
        RDFParser.create()
                .source(serviceURL + "/datamodel-api/api/v1/modelPositions?model=" + DataModelUtils.encode(modelURI))
                .lang(Lang.JSONLD)
                .acceptHeader("application/ld+json")
                .parse(oldVisualization);

        RDFParser.create()
                .source(serviceURL + "/datamodel-api/api/v1/exportModel?graph=" + DataModelUtils.encode(modelURI))
                .lang(Lang.JSONLD)
                .acceptHeader("application/ld+json")
                .parse(oldData);

        migrationService.migratePositions(prefix, oldVisualization, oldData.getGraph().getPrefixMapping());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/version")
    public ResponseEntity<Void> createVersionAndUpdateReferences(@RequestParam String prefix) {
        check(authorizationManager.hasRightToDoMigration());
        migrationService.createVersions(prefix);
        return ResponseEntity.noContent().build();
    }
}
