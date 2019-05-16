package fi.vm.yti.datamodel.api.index;

import java.io.IOException;
import java.util.Map;

import javax.inject.Singleton;

import org.apache.jena.rdf.model.Model;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import fi.vm.yti.datamodel.api.index.model.IndexClass;
import fi.vm.yti.datamodel.api.index.model.IndexModel;
import fi.vm.yti.datamodel.api.index.model.IndexPredicate;
import fi.vm.yti.datamodel.api.model.AbstractClass;
import fi.vm.yti.datamodel.api.model.AbstractPredicate;
import fi.vm.yti.datamodel.api.model.DataModel;
import fi.vm.yti.datamodel.api.service.JenaClient;
import fi.vm.yti.datamodel.api.service.ModelManager;
import fi.vm.yti.datamodel.api.utils.Frames;
import fi.vm.yti.datamodel.api.utils.LDHelper;

@Singleton
@Service
public class SearchIndexManager {

    private RestHighLevelClient esClient;
    private final ElasticConnector esManager;
    private final JenaClient jenaClient;
    private final ObjectMapper objectMapper;
    private final ModelManager modelManager;

    private static final Logger logger = LoggerFactory.getLogger(SearchIndexManager.class.getName());

    @Autowired
    public SearchIndexManager(final ElasticConnector esManager,
                              final JenaClient jenaClient,
                              final ObjectMapper objectMapper,
                              final ModelManager modelManager) {
        this.esManager = esManager;
        this.esClient = esManager.getEsClient();
        this.jenaClient = jenaClient;
        this.objectMapper = objectMapper;
        this.modelManager = modelManager;
    }

    public void bulkInsert(String indexName,
                           JsonNode resourceList) throws IOException {
        BulkRequest bulkRequest = new BulkRequest();
        resourceList = resourceList.get("@graph");
        resourceList.forEach(resource -> {
            IndexRequest indexRequest = new IndexRequest(indexName, "doc", resource.get("id").asText()).
                source(objectMapper.convertValue(resource, Map.class));
            bulkRequest.add(indexRequest);
        });
        esClient.bulk(bulkRequest, RequestOptions.DEFAULT);
    }

    public void initModelIndex() throws IOException {
        String qry = LDHelper.prefix + "CONSTRUCT {" +
            "?model rdfs:label ?prefLabel . " +
            "?model rdfs:comment ?comment . " +
            "?model dcterms:description ?definition . " +
            "?model dcterms:modified ?modified . " +
            "?model a ?modelType . " +
            "?model owl:versionInfo ?versionInfo . " +
            "?model iow:useContext ?useContext . " +
            "?model dcap:preferredXMLNamespaceName ?namespace . " +
            "?model dcap:preferredXMLNamespacePrefix ?prefix .  " +
            "?model dcterms:contributor ?org . " +
            "?model dcterms:isPartOf ?group . " +
            "} WHERE { " +
            "GRAPH ?model { " +
            "?model rdfs:label ?prefLabel . " +
            "OPTIONAL {?model rdfs:comment ?comment . FILTER(lang(?comment)!='') }" +
            "OPTIONAL { ?model iow:useContext ?useContext . }" +
            "?model owl:versionInfo ?versionInfo . " +
            "?model dcap:preferredXMLNamespaceName ?namespace . " +
            "?model dcap:preferredXMLNamespacePrefix ?prefix .  " +
            "?model a ?modelType . VALUES ?modelType { dcap:MetadataVocabulary dcap:DCAP }" +
            "?model dcterms:modified ?modified . " +
            "?model dcterms:contributor ?org . " +
            "?model dcterms:isPartOf ?group . }}";

        Model model = jenaClient.constructFromCore(qry);
        if (model.size() < 1) {
            logger.warn("Could not find any models to index!");
            return;
        }
        JsonNode nodes = modelManager.toFramedJsonNode(model, Frames.esModelFrame);
        if (nodes == null) {
            logger.warn("Could not parse JSON");
            return;
        }
        bulkInsert(esManager.ELASTIC_INDEX_MODEL, nodes);
    }

    public void initClassIndex() throws IOException {
        String qry = LDHelper.prefix + " CONSTRUCT {" +
            "?class sh:name ?prefLabel . " +
            "?class sh:description ?definition . " +
            "?class rdfs:isDefinedBy ?model . " +
            "?class dcterms:modified ?modified . " +
            "?class owl:versionInfo ?status . " +
            "?class a ?type . " +
            "} WHERE { " +
            "GRAPH ?class { ?class rdf:type ?classType . VALUES ?classType { sh:NodeShape rdfs:Class }" +
            "?class sh:name ?prefLabel . " +
            "?class owl:versionInfo ?status . " +
            "OPTIONAL { ?class sh:description ?definition . FILTER(lang(?definition)!='')}" +
            "?class a ?type . " +
            "?class dcterms:modified ?modified . " +
            "?class rdfs:isDefinedBy ?model . }" +
            "GRAPH ?model {?model a owl:Ontology  . ?model rdfs:label ?label . ?model a ?modelType . VALUES ?modelType { dcap:MetadataVocabulary dcap:DCAP }}}";
        Model model = jenaClient.constructFromCore(qry);
        if (model.size() < 1) {
            logger.warn("Could not find any classes to index!");
            return;
        }
        JsonNode nodes = modelManager.toFramedJsonNode(model, Frames.esClassFrame);
        if (nodes == null) {
            logger.warn("Could not parse JSON");
            return;
        }
        bulkInsert(esManager.ELASTIC_INDEX_CLASS, nodes);
    }

    public void initPredicateIndex() throws IOException {
        String qry = LDHelper.prefix + " CONSTRUCT {" +
            "?predicate rdfs:label ?prefLabel . " +
            "?predicate a ?predicateType . " +
            "?predicate dcterms:modified ?modified . " +
            "?predicate rdfs:range ?range . " +
            "?predicate rdfs:comment ?definition . " +
            "?predicate rdfs:isDefinedBy ?model . " +
            "?predicate owl:versionInfo ?status . " +
            "} WHERE { " +
            "GRAPH ?predicate { ?predicate a ?predicateType . VALUES ?predicateType { owl:ObjectProperty owl:DatatypeProperty }" +
            "?predicate rdfs:label ?prefLabel . " +
            "?predicate rdfs:range ?range . " +
            "?predicate owl:versionInfo ?status . " +
            "?predicate dcterms:modified ?modified . " +
            "OPTIONAL { ?predicate rdfs:comment ?definition . FILTER(lang(?definition)!='')}" +
            "?class rdfs:isDefinedBy ?model . }" +
            "GRAPH ?model {?model a owl:Ontology  . ?model rdfs:label ?label . ?model a ?modelType . VALUES ?modelType { dcap:MetadataVocabulary dcap:DCAP }}}";
        Model model = jenaClient.constructFromCore(qry);
        if (model.size() < 1) {
            logger.warn("Could not find any associations to index!");
            return;
        }
        JsonNode nodes = modelManager.toFramedJsonNode(model, Frames.esPredicateFrame);
        if (nodes == null) {
            logger.warn("Could not parse JSON");
            return;
        }
        logger.debug("Using BULK API: " + esManager.ELASTIC_INDEX_PREDICATE);
        bulkInsert(esManager.ELASTIC_INDEX_PREDICATE, nodes);
    }

    public void initSearchIndexes() throws IOException {
        initClassIndex();
        initPredicateIndex();
        initModelIndex();
    }

    public void reindex() {
        try {
            esManager.cleanIndex(esManager.ELASTIC_INDEX_PREDICATE);
            esManager.cleanIndex(esManager.ELASTIC_INDEX_CLASS);
            esManager.cleanIndex(esManager.ELASTIC_INDEX_MODEL);
            initSearchIndexes();
        } catch(IOException ex) {
            logger.warn(ex.toString());
        }
    }

    public void indexClass(String id,
                           Object obj) {
        esManager.addToIndex(esManager.ELASTIC_INDEX_CLASS, id, obj);
    }

    public void indexClass(AbstractClass classResource) {
        IndexClass indexClass = new IndexClass(classResource);
        try {
            logger.debug(modelManager.mapObjectToString(indexClass));
        } catch (IOException ex) {
            logger.warn(ex.toString());
        }
        esManager.addToIndex(ElasticConnector.ELASTIC_INDEX_CLASS, LDHelper.encode(indexClass.getId()), indexClass);
    }

    public void removeClass(String id) {
        esManager.removeFromIndex(LDHelper.encode(id), esManager.ELASTIC_INDEX_CLASS);
    }

    public void indexPredicate(String id,
                               Object obj) {
        esManager.addToIndex(esManager.ELASTIC_INDEX_PREDICATE, id, obj);
    }

    public void indexPredicate(AbstractPredicate predicateResource) {
        IndexPredicate indexPredicate = new IndexPredicate(predicateResource);
        try {
            logger.debug(modelManager.mapObjectToString(indexPredicate));
        } catch (IOException ex) {
            logger.warn(ex.toString());
        }
        esManager.addToIndex(ElasticConnector.ELASTIC_INDEX_PREDICATE, LDHelper.encode(indexPredicate.getId()), indexPredicate);
    }

    public void removePredicate(String id) {
        esManager.removeFromIndex(LDHelper.encode(id), esManager.ELASTIC_INDEX_PREDICATE);
    }

    public void removeModel(String id) {
        esManager.removeFromIndex(LDHelper.encode(id), esManager.ELASTIC_INDEX_MODEL);
    }

    public void indexModel(DataModel model) {
        IndexModel indexModel = new IndexModel(model);
        try {
            logger.debug(modelManager.mapObjectToString(indexModel));
        } catch (IOException ex) {
            logger.warn(ex.toString());
        }
        esManager.addToIndex(ElasticConnector.ELASTIC_INDEX_MODEL, LDHelper.encode(indexModel.getId()), indexModel);
    }

    public void indexModel(String id,
                           Object obj) {
        esManager.addToIndex(esManager.ELASTIC_INDEX_MODEL, id, obj);
    }

}
