package fi.vm.yti.datamodel.api.v2.migration.task;

import fi.vm.yti.datamodel.api.v2.repository.ConceptRepository;
import fi.vm.yti.datamodel.api.v2.repository.CoreRepository;
import fi.vm.yti.datamodel.api.v2.repository.SchemesRepository;
import fi.vm.yti.migration.MigrationTask;
import org.springframework.stereotype.Component;

@Component
public class V13_StatusToUri implements MigrationTask {

    private final CoreRepository coreRepository;
    private final SchemesRepository schemesRepository;
    private final ConceptRepository conceptRepository;

    public V13_StatusToUri(CoreRepository coreRepository, SchemesRepository schemesRepository, ConceptRepository conceptRepository) {
        this.coreRepository = coreRepository;
        this.schemesRepository = schemesRepository;
        this.conceptRepository = conceptRepository;
    }


    @Override
    public void migrate() {
        var query = """
                PREFIX suomi-meta: <https://iri.suomi.fi/model/suomi-meta/>
                DELETE {
                  graph ?g { ?s suomi-meta:publicationStatus ?status }
                } INSERT{
                  graph ?g {?s suomi-meta:publicationStatus ?uri}
                }WHERE {
                graph ?g {
                 ?s suomi-meta:publicationStatus ?status
                  BIND(iri("http://uri.suomi.fi/codelist/interoperabilityplatform/interoperabilityplatform_status/code/" + str(?status)) as ?uri)
                  }
                }
                """;
        coreRepository.queryUpdate(query);
        schemesRepository.queryUpdate(query);
        conceptRepository.queryUpdate(query);
    }
}
