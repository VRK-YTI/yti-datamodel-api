package fi.vm.yti.datamodel.api.v2.service;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import fi.vm.yti.datamodel.api.v2.endpoint.error.ResourceNotFoundException;
import fi.vm.yti.datamodel.api.v2.dto.ModelConstants;
import org.apache.jena.arq.querybuilder.AskBuilder;
import org.apache.jena.arq.querybuilder.ConstructBuilder;
import org.apache.jena.arq.querybuilder.ExprFactory;
import org.apache.jena.arq.querybuilder.WhereBuilder;
import org.apache.jena.atlas.web.HttpException;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.query.Query;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdfconnection.RDFConnection;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.sparql.vocabulary.FOAF;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;
import org.apache.jena.vocabulary.SKOS;
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

    public void initServiceCategories() {
        Model model = RDFDataMgr.loadModel("ptvl-skos.rdf");
        coreWrite.put(ModelConstants.SERVICE_CATEGORY_GRAPH, model);
    }

    public void saveOrganizations(Model model) {
        coreWrite.put(ModelConstants.ORGANIZATION_GRAPH, model);
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

    public boolean doesDataModelExist(String graph){
        var askBuilder = new AskBuilder()
                .addGraph(NodeFactory.createURI(graph), "?s", "?p", "?o");
        try {
            return coreSparql.queryAsk(askBuilder.build());
        }catch(HttpException ex){
            throw new RuntimeException("Error querying graph");
        }
    }

    public Model getServiceCategories(){
        var serviceCategories = modelCache.getIfPresent("serviceCategories");

        if(serviceCategories != null){
            logger.info("Used cache for servicecategories");
            return serviceCategories;
        }

        var cat = "?category";
        ConstructBuilder builder = new ConstructBuilder()
                .addPrefixes(ModelConstants.PREFIXES)
                .addConstruct(cat, RDFS.label, "?label")
                .addConstruct(cat, RDF.type, FOAF.Group)
                .addConstruct(cat, SKOS.notation, "?id")
                .addConstruct(cat, SKOS.note, "?note")
                .addWhere(cat, RDF.type, SKOS.Concept)
                .addWhere(cat, SKOS.prefLabel, "?label")
                .addWhere(cat, SKOS.notation, "?id")
                .addWhere(cat, SKOS.note, "?note")
                .addFilter(new ExprFactory().notexists(
                        new WhereBuilder().addWhere(cat, SKOS.broader, "?topCategory")
                ));

        serviceCategories = constructWithQuery(builder.build());

        modelCache.put("serviceCategories", serviceCategories);
        return serviceCategories;
    }

    public Model getOrganizations(){
        var organizations = modelCache.getIfPresent("organizations");

        if(organizations != null){
            logger.info("Used cache for organizations");
            return organizations;
        }

        organizations = getDataModel("urn:yti:organizations");

        modelCache.put("organizations", organizations);
        return organizations;
    }

    public int getVersionNumber() {
        // TODO: migrations
        return 1;
    }

    public void setVersionNumber(int version) {
        // TODO: migrations
    }
}
