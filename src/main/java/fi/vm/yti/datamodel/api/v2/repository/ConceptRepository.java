package fi.vm.yti.datamodel.api.v2.repository;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import fi.vm.yti.common.repository.BaseRepository;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdfconnection.RDFConnection;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;

import java.util.concurrent.TimeUnit;

@Repository
public class ConceptRepository extends BaseRepository {

    private final Cache<String, Model> modelCache;

    public ConceptRepository(@Value(("${fuseki.url}")) String endpoint,
                             @Value("${fuseki.cache.common.expiration:1800}") Long cacheExpireTime){
        super(RDFConnection.connect(endpoint + "/concept/get"),
                RDFConnection.connect(endpoint + "/concept/data"),
                RDFConnection.connect(endpoint + "/concept/sparql"),
                RDFConnection.connect(endpoint + "/concept/update")
        );

        this.modelCache = CacheBuilder.newBuilder()
                .expireAfterWrite(cacheExpireTime, TimeUnit.SECONDS)
                .maximumSize(1000)
                .build();
    }

    @Override
    public Model fetch(String graph) {
        try {
            return modelCache.get(graph, () -> super.fetch(graph));
        } catch (Exception e) {
            return null;
        }
    }
}
