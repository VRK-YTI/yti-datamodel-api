package fi.vm.yti.datamodel.api.v2.migration.task;

import fi.vm.yti.datamodel.api.v2.repository.ConceptRepository;
import fi.vm.yti.datamodel.api.v2.repository.SchemesRepository;
import fi.vm.yti.migration.MigrationTask;
import org.springframework.stereotype.Component;

@SuppressWarnings("java:S101")
@Component
public class V11_FixConceptAndCodeListStatus implements MigrationTask {

    private final ConceptRepository conceptRepository;
    private final SchemesRepository schemesRepository;

    public V11_FixConceptAndCodeListStatus(ConceptRepository conceptRepository,
                                           SchemesRepository schemesRepository) {
        this.conceptRepository = conceptRepository;
        this.schemesRepository = schemesRepository;
    }

    @Override
    public void migrate() {
        var fixStatusQuery = """
                DELETE {
                  GRAPH ?g
                    { ?s <http://uri.suomi.fi/datamodel/ns/suomi-meta/publicationStatus> ?o }
                }
                INSERT {
                  GRAPH ?g
                    { ?s <https://iri.suomi.fi/model/suomi-meta/publicationStatus> ?o }
                }
                WHERE {
                  GRAPH ?g
                    { ?s <http://uri.suomi.fi/datamodel/ns/suomi-meta/publicationStatus> ?o }
                }
                """;

        var fixTypeQuery = """
                PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>
                DELETE {
                  GRAPH ?g
                    { ?s rdf:type <http://uri.suomi.fi/datamodel/ns/iow/CodeScheme> }
                }
                INSERT {
                  GRAPH ?g
                    { ?s rdf:type <https://iri.suomi.fi/model/iow/CodeScheme> }
                }
                WHERE {
                  GRAPH ?g
                    { ?s rdf:type <http://uri.suomi.fi/datamodel/ns/iow/CodeScheme> }
                }
                """;

        conceptRepository.queryUpdate(fixStatusQuery);
        schemesRepository.queryUpdate(fixStatusQuery);
        schemesRepository.queryUpdate(fixTypeQuery);
    }
}
