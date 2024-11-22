package fi.vm.yti.datamodel.api.v2.repository;

import fi.vm.yti.common.repository.BaseRepository;
import org.apache.jena.rdfconnection.RDFConnection;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;

@Repository
public class TerminologyRepository extends BaseRepository {

        public TerminologyRepository(@Value(("${endpoint}")) String endpoint){
            super(RDFConnection.connect(endpoint + "/terminology/get"),
                    RDFConnection.connect(endpoint + "/terminology/data"),
                    RDFConnection.connect(endpoint + "/terminology/sparql"));
        }
}
