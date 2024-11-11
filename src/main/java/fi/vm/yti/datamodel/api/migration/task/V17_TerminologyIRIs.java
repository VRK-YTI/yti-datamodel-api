package fi.vm.yti.datamodel.api.migration.task;

import fi.vm.yti.datamodel.api.v2.repository.CoreRepository;
import fi.vm.yti.migration.MigrationTask;
import org.springframework.stereotype.Component;

@Component
@SuppressWarnings("java:S101")
public class V17_TerminologyIRIs implements MigrationTask {

    private final CoreRepository coreRepository;

    public V17_TerminologyIRIs(CoreRepository coreRepository) {
        this.coreRepository = coreRepository;
    }

    /**
     * Change terminology and concept URIs to new format, e.g. https://iri.suomi.fi/terminology/test/concept-1
     */
    @Override
    public void migrate() {

        var queryTerminologies = """
                PREFIX dcterms: <http://purl.org/dc/terms/>
                DELETE { GRAPH ?g {
                    ?s dcterms:requires ?terminology .
                   }
                }
                INSERT { GRAPH ?g {
                     ?s dcterms:requires ?newTerminologyURI
                   }
                }
                WHERE { GRAPH ?g {
                     ?s dcterms:requires ?terminology .
                     filter (strstarts(str(?terminology), "http://uri.suomi.fi/terminology")) .
                     bind (iri(
                             replace(str(?terminology), "http://uri.suomi.fi/terminology", "https://iri.suomi.fi/terminology")
                         ) as ?newTerminologyURI
                     )
                   }
                }
                """;

        var queryConcepts = """
                PREFIX dcterms: <http://purl.org/dc/terms/>
                DELETE { GRAPH ?g {
                    ?s dcterms:subject ?concept .
                   }
                }
                INSERT { GRAPH ?g {
                     ?s dcterms:subject ?newConceptURI
                   }
                }
                WHERE { GRAPH ?g {
                     ?s dcterms:subject ?concept .
                     bind (iri(
                             replace(str(?concept), "http://uri.suomi.fi/terminology", "https://iri.suomi.fi/terminology")
                         ) as ?newConceptURI
                     )
                   }
                }
                """;

        coreRepository.queryUpdate(queryConcepts);
        coreRepository.queryUpdate(queryTerminologies);
    }
}
