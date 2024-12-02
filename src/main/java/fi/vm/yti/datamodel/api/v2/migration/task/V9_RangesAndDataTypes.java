package fi.vm.yti.datamodel.api.v2.migration.task;

import fi.vm.yti.datamodel.api.v2.repository.CoreRepository;
import fi.vm.yti.migration.MigrationTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@SuppressWarnings("java:S101")
@Component
public class V9_RangesAndDataTypes implements MigrationTask {
    private static final Logger LOG = LoggerFactory.getLogger(V9_RangesAndDataTypes.class);

    private final CoreRepository coreRepository;

    public V9_RangesAndDataTypes(CoreRepository coreRepository) {
        this.coreRepository = coreRepository;
    }

    @Override
    public void migrate() {

        LOG.info("Fix ranges and data types");

        var rangesQuery = """
                PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>
                DELETE {
                  GRAPH ?g { ?s rdfs:range <rdfs:Literal>}
                }
                INSERT {
                  GRAPH ?g { ?s rdfs:range <http://www.w3.org/2000/01/rdf-schema#Literal>}
                }
                WHERE {
                  GRAPH ?g { ?s rdfs:range <rdfs:Literal>}
                }
                """;

        var dataTypesQuery = """
                PREFIX sh: <http://www.w3.org/ns/shacl#>
                PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>
                DELETE {
                  GRAPH ?g { ?s sh:datatype <rdfs:Literal>}
                }
                INSERT {
                  GRAPH ?g { ?s sh:datatype <http://www.w3.org/2000/01/rdf-schema#Literal>}
                }
                WHERE {
                  GRAPH ?g { ?s sh:datatype <rdfs:Literal>}
                }
                """;

        coreRepository.queryUpdate(rangesQuery);
        coreRepository.queryUpdate(dataTypesQuery);
    }
}
