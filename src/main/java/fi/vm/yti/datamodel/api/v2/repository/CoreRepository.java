package fi.vm.yti.datamodel.api.v2.repository;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import fi.vm.yti.datamodel.api.v2.dto.ModelConstants;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdfconnection.RDFConnection;
import org.apache.jena.riot.RDFDataMgr;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;

import java.util.concurrent.TimeUnit;

@Repository
public class CoreRepository extends BaseRepository{

    private final Cache<String, Model> modelCache;



    public CoreRepository(@Value(("${endpoint}")) String endpoint, @Value("${model.cache.expiration:1800}") Long cacheExpireTime){
        super(RDFConnection.connect(endpoint + "/core/get"),
              RDFConnection.connect(endpoint + "/core/data"),
              RDFConnection.connect(endpoint + "/core/sparql"),
              RDFConnection.connect(endpoint + "/core/update"));

        this.modelCache = CacheBuilder.newBuilder()
                .expireAfterWrite(cacheExpireTime, TimeUnit.SECONDS)
                .maximumSize(1000)
                .build();
    }

    public void initServiceCategories() {
        var model = RDFDataMgr.loadModel("ptvl-skos.rdf");
        write.put(ModelConstants.SERVICE_CATEGORY_GRAPH, model);
    }

    public Model getOrganizations(){
        var organizations = modelCache.getIfPresent("organizations");

        if(organizations != null){
            return organizations;
        }

        organizations = fetch("urn:yti:organizations");

        modelCache.put("organizations", organizations);
        return organizations;
    }
}
