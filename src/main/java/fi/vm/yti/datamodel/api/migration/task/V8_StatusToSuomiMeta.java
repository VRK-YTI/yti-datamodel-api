package fi.vm.yti.datamodel.api.migration.task;

import fi.vm.yti.datamodel.api.v2.repository.ConceptRepository;
import fi.vm.yti.datamodel.api.v2.repository.CoreRepository;
import fi.vm.yti.datamodel.api.v2.repository.SchemesRepository;
import fi.vm.yti.migration.MigrationTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@SuppressWarnings("java:S101")
@Component
public class V8_StatusToSuomiMeta implements MigrationTask {

    private final Logger logger = LoggerFactory.getLogger(V8_StatusToSuomiMeta.class);

    private final CoreRepository coreRepository;
    private final ConceptRepository conceptRepository;
    private final SchemesRepository schemesRepository;

    public V8_StatusToSuomiMeta(CoreRepository coreRepository, ConceptRepository conceptRepository, SchemesRepository schemesRepository) {
        this.coreRepository = coreRepository;
        this.conceptRepository = conceptRepository;
        this.schemesRepository = schemesRepository;
    }


    @Override
    public void migrate() {
        logger.info("Migrating statuses to SuomiMeta");

        //VALUES are same as in https://koodistot.suomi.fi/codescheme;registryCode=interoperabilityplatform;schemeCode=interoperabilityplatform_status
        var statusToSuomiMeta = """
                PREFIX status: <http://www.w3.org/2003/06/sw-vocab-status/ns#>
                PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>
                PREFIX owl: <http://www.w3.org/2002/07/owl#>
                PREFIX suomi-meta: <http://uri.suomi.fi/datamodel/ns/suomi-meta/>
                DELETE {
                  GRAPH ?g {
                    ?s owl:versionInfo ?status
                  }
                }
                INSERT{
                  GRAPH ?g {
                    ?s suomi-meta:publicationStatus ?status
                  }
                }
                WHERE {
                  GRAPH ?g {
                    ?s owl:versionInfo ?status .
                    VALUES ?status {"DRAFT" "VALID" "SUPERSEDED" "RETIRED" "SUGGESTED" "INVALID" "INCOMPLETE" "RECOMMENDED" "HIDDEN"} .
                  }
                }
                """;
        //update every YTI-related repository
        coreRepository.queryUpdate(statusToSuomiMeta);
        schemesRepository.queryUpdate(statusToSuomiMeta);
        conceptRepository.queryUpdate(statusToSuomiMeta);
    }
}
