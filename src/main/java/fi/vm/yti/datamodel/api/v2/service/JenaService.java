package fi.vm.yti.datamodel.api.v2.service;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import fi.vm.yti.datamodel.api.v2.endpoint.error.ResourceNotFoundException;
import fi.vm.yti.datamodel.api.v2.dto.ModelConstants;
import org.apache.jena.arq.querybuilder.*;
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

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

@Service
public class JenaService {

    private final Logger logger = LoggerFactory.getLogger(JenaService.class);

    private final RDFConnection coreWrite;
    private final RDFConnection coreRead;
    private final RDFConnection coreSparql;

    private final RDFConnection importWrite;
    private final RDFConnection importRead;
    private final RDFConnection importSparql;
    private final RDFConnection conceptRead;
    private final RDFConnection conceptWrite;
    private final RDFConnection conceptSparql;

    private final Cache<String, Model> modelCache;

    public JenaService(@Value("${model.cache.expiration:1800}") Long cacheExpireTime,
                       @Value(("${endpoint}")) String endpoint) {
        this.coreWrite = RDFConnection.connect(endpoint + "/core/data");
        this.coreRead = RDFConnection.connect(endpoint + "/core/get");
        this.coreSparql = RDFConnection.connect( endpoint + "/core/sparql");
        this.importWrite = RDFConnection.connect(endpoint + "/imports/data");
        this.importRead = RDFConnection.connect(endpoint + "/imports/get");
        this.importSparql = RDFConnection.connect(endpoint + "/imports/sparql");
        this.conceptWrite = RDFConnection.connect(endpoint + "/concept/data");
        this.conceptRead = RDFConnection.connect(endpoint + "/concept/get");
        this.conceptSparql = RDFConnection.connect(endpoint + "/concept/sparql");

        this.modelCache = CacheBuilder.newBuilder()
                .expireAfterWrite(cacheExpireTime, TimeUnit.SECONDS)
                .maximumSize(1000)
                .build();
    }

    public void createDataModel(String graphName, Model model) {
        coreWrite.put(graphName, model);
    }

    public void initServiceCategories() {
        var model = RDFDataMgr.loadModel("ptvl-skos.rdf");
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
                throw new JenaQueryException();
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

    /**
     * Check if Data model exists, this method just asks if a given graph can be found.
     * @param graph Graph url of data model
     * @return exists
     */
    public boolean doesDataModelExist(String graph){
        var askBuilder = new AskBuilder()
                .addGraph(NodeFactory.createURI(graph), "?s", "?p", "?o");
        try {
            return coreSparql.queryAsk(askBuilder.build());
        }catch(HttpException ex){
            throw new JenaQueryException();
        }
    }

    /**
     * Check if given classUri (subject) found in graph
     * @param graph Graph
     * @param classUri Class URI
     * @return exists
     */
    public boolean doesClassExistInGraph(String graph, String classUri){
        var askBuilder = new AskBuilder()
                .addGraph(NodeFactory.createURI(graph),
                        NodeFactory.createURI(classUri), "?p", "?o");
        try{
            return coreSparql.queryAsk(askBuilder.build());
        }catch(HttpException ex){
            throw new JenaQueryException();
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

    public void putNamespaceToImports(String graphName, Model model){
        importWrite.put(graphName, model);
    }

    public Model getNamespaceFromImports(String graphName){
        logger.debug("Getting model from core {}", graphName);
        try {
            return importRead.fetch(graphName);
        } catch (HttpException ex) {
            if (ex.getStatusCode() == HttpStatus.NOT_FOUND.value()) {
                logger.warn("Namespace not found: {}", graphName);
                throw new ResourceNotFoundException(graphName);
            } else {
                throw new JenaQueryException();
            }
        }
    }

    public boolean doesResolvedNamespaceExist(String namespace){
        var askBuilder = new AskBuilder()
                .addGraph(NodeFactory.createURI(namespace), "?s", "?p", "?o");
        try {
            return importSparql.queryAsk(askBuilder.build());
        }catch(HttpException ex){
            throw new JenaQueryException();
        }
    }

    public boolean doesClassExistInNamespace(String namespace, String classUri){
        var askBuilder = new AskBuilder()
                .addGraph(NodeFactory.createURI(namespace),
                        NodeFactory.createURI(classUri), "?p", "?o");
        try{
            return importSparql.queryAsk(askBuilder.build());
        }catch(HttpException ex){
            throw new JenaQueryException();
        }
    }

    public void putTerminologyToConcepts(String graph, Model model) {
        conceptWrite.put(graph, model);
    }

    public Model getTerminology(String graph) {
        try {
            return modelCache.get(graph, () -> {
                logger.debug("Fetch terminology from Fuseki {}", graph);
                return conceptRead.fetch(graph);
            });
        } catch (Exception e) {
            logger.warn("Error fetching terminology information {}", e.getMessage());
            return null;
        }
    }

    public int getVersionNumber() {
        // TODO: migrations
        return 1;
    }

    public void setVersionNumber(int version) {
        // TODO: migrations
    }
}
