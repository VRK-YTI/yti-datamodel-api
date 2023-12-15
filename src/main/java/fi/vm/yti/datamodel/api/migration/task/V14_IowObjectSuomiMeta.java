package fi.vm.yti.datamodel.api.migration.task;

import fi.vm.yti.datamodel.api.v2.repository.ConceptRepository;
import fi.vm.yti.datamodel.api.v2.repository.CoreRepository;
import fi.vm.yti.datamodel.api.v2.repository.SchemesRepository;
import fi.vm.yti.migration.MigrationTask;
import org.springframework.stereotype.Component;

@Component
public class V14_IowObjectSuomiMeta implements MigrationTask {

    private final CoreRepository coreRepository;
    private final SchemesRepository schemesRepository;
    private final ConceptRepository conceptRepository;


    public V14_IowObjectSuomiMeta(CoreRepository coreRepository,
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
                          GRAPH ?g { ?sub ?pred ?obj2 }
                        }WHERE {
                          graph ?g {
                          ?sub ?pred ?obj .
                          FILTER(isURI(?obj) && STRSTARTS(str(?obj), "https://iri.suomi.fi/model/iow"))
                          BIND(iri(REPLACE(str(?obj), "iow", "suomi-meta", "i")) AS ?obj2)
                          }
                        }
                """;

        coreRepository.queryUpdate(iowToSuomiMeta);
        schemesRepository.queryUpdate(iowToSuomiMeta);
        conceptRepository.queryUpdate(iowToSuomiMeta);
    }
}