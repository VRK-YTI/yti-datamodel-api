package fi.vm.yti.datamodel.api.migration.task;

import fi.vm.yti.datamodel.api.v2.repository.CoreRepository;
import fi.vm.yti.migration.MigrationTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class V6_VersionStatusResources implements MigrationTask {
    private static final Logger LOG = LoggerFactory.getLogger(V6_VersionStatusResources.class);

    private final CoreRepository coreRepository;

    public V6_VersionStatusResources(CoreRepository coreRepository) {
        this.coreRepository = coreRepository;
    }

    @Override
    public void migrate() {
        LOG.info("Migrating resource statuses");

        var draftToSuggestion = """
                PREFIX owl: <http://www.w3.org/2002/07/owl#>
                PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>
                PREFIX iow: <http://uri.suomi.fi/datamodel/ns/iow/>
                                
                DELETE {
                   GRAPH ?g {
                     ?s owl:versionInfo "DRAFT" .
                   }
                }
                INSERT {
                  GRAPH ?g {
                     ?s owl:versionInfo "SUGGESTED"
                  }
                }
                WHERE {
                   GRAPH ?g {
                       FILTER NOT EXISTS { ?s rdf:type owl:Ontology}
                       ?s owl:versionInfo "DRAFT" .
                   }
                }""";

        coreRepository.queryUpdate(draftToSuggestion);

        var incompleteToDraft = """
                PREFIX owl: <http://www.w3.org/2002/07/owl#>
                PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>
                PREFIX iow: <http://uri.suomi.fi/datamodel/ns/iow/>
                
                DELETE {
                   GRAPH ?g {
                     ?s owl:versionInfo "INCOMPLETE" .
                   }
                }
                INSERT {
                  GRAPH ?g {
                     ?s owl:versionInfo "DRAFT"
                  }
                }
                WHERE {
                   GRAPH ?g {
                       FILTER NOT EXISTS { ?s rdf:type owl:Ontology}
                     ?s owl:versionInfo "INCOMPLETE" .
                   }
                }""";

        coreRepository.queryUpdate(incompleteToDraft);

        var invalidToRetired = """
                PREFIX owl: <http://www.w3.org/2002/07/owl#>
                PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>
                PREFIX iow: <http://uri.suomi.fi/datamodel/ns/iow/>
                
                DELETE {
                   GRAPH ?g {
                     ?s owl:versionInfo "INVALID" .
                   }
                }
                INSERT {
                  GRAPH ?g {
                     ?s owl:versionInfo "RETIRED"
                  }
                }
                WHERE {
                   GRAPH ?g {
                     FILTER NOT EXISTS { ?s rdf:type owl:Ontology}
                     ?s owl:versionInfo "INVALID" .
                   }
                }""";

        coreRepository.queryUpdate(invalidToRetired);
    }
}
