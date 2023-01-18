package fi.vm.yti.datamodel.api.v2.service;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import fi.vm.yti.datamodel.api.v2.endpoint.error.ResourceNotFoundException;
import org.apache.jena.atlas.web.HttpException;
import org.apache.jena.query.Query;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdfconnection.RDFConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
public class JenaService {

    private final Logger logger = LoggerFactory.getLogger(JenaService.class);

    private final RDFConnection coreWrite;
    private final RDFConnection coreRead;
    private final RDFConnection coreSparql;

    private final Cache<String, Model> modelCache;

    public JenaService(@Value("${model.cache.expiration:1800}") Long cacheExpireTime,
                       @Value(("${endpoint}")) String endpoint) {
        this.coreWrite = RDFConnection.connect(endpoint + "/core/data");
        this.coreRead = RDFConnection.connect(endpoint + "/core/get");
        this.coreSparql = RDFConnection.connect( endpoint + "/core/sparql");
        this.modelCache = CacheBuilder.newBuilder()
                .expireAfterWrite(cacheExpireTime, TimeUnit.SECONDS)
                .maximumSize(1000)
                .build();
    }

    public void createDataModel(String graphName, Model model) {
        coreWrite.put(graphName, model);
    }

    public Model getDataModel(String graph) {
        logger.debug("Getting model from core {}", graph);
        try {
            return coreRead.fetch(graph);
        } catch (HttpException ex) {
            if (ex.getStatusCode() == HttpStatus.NOT_FOUND.value()) {
                logger.warn("Model not found with prefix {}", graph);
                throw new ResourceNotFoundException(graph);
            } else {
                throw new RuntimeException("Error fetching graph");
            }
        }
    }

    public Model constructWithQuery(Query query){
        try{
            return coreSparql.queryConstruct(query);
        }catch(HttpException ex){
            return null;
        }
    }

    public Model getServiceCategories(){
        var serviceCategories = modelCache.getIfPresent("serviceCategories");

        if(serviceCategories != null){
            logger.info("Used cache for servicecategories");
            return serviceCategories;
        }

        serviceCategories = getDataModel("urn:yti:servicecategories");
        modelCache.put("serviceCategories", serviceCategories);
        return serviceCategories;
    }

    public Model getOrganizations(){
        var serviceCategories = modelCache.getIfPresent("organizations");

        if(serviceCategories != null){
            logger.info("Used cache for organizations");
            return serviceCategories;
        }

        serviceCategories = getDataModel("urn:yti:organizations");
        modelCache.put("organizations", serviceCategories);
        return serviceCategories;
    }
}
