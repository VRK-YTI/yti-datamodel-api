package fi.vm.yti.datamodel.api.migration.task;

import fi.vm.yti.datamodel.api.v2.repository.ConceptRepository;
import fi.vm.yti.datamodel.api.v2.repository.CoreRepository;
import fi.vm.yti.datamodel.api.v2.repository.SchemesRepository;
import fi.vm.yti.migration.MigrationTask;
import org.springframework.stereotype.Component;

@Component
public class V12_Iow_To_SuomiMeta implements MigrationTask {

    private final CoreRepository coreRepository;
    private final SchemesRepository schemesRepository;
    private final ConceptRepository conceptRepository;


    public V12_Iow_To_SuomiMeta(CoreRepository coreRepository,
                                SchemesRepository schemesRepository,
                                ConceptRepository conceptRepository) {
        this.coreRepository = coreRepository;
        this.schemesRepository = schemesRepository;
        this.conceptRepository = conceptRepository;
    }

    @Override
    public void migrate() {
        //get all predicates starting with iow and replace with suomi-meta
        var iowToSuomiMeta = """
                        DELETE {
                          GRAPH ?g { ?sub ?pred ?obj }
                        }INSERT{
                          GRAPH ?g { ?sub ?pred2 ?obj }
                        }WHERE {
                          graph ?g {
                          ?sub ?pred ?obj .
                          FILTER(isURI(?pred) && STRSTARTS(str(?pred), "https://iri.suomi.fi/model/iow"))
                          BIND(iri(REPLACE(str(?pred), "iow", "suomi-meta", "i")) AS ?pred2)
                          }
                        }
                """;

        coreRepository.queryUpdate(iowToSuomiMeta);
        schemesRepository.queryUpdate(iowToSuomiMeta);
        conceptRepository.queryUpdate(iowToSuomiMeta);
    }
}
