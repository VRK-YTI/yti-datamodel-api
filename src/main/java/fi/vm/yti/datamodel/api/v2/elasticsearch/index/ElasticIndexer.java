package fi.vm.yti.datamodel.api.v2.elasticsearch.index;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import fi.vm.yti.datamodel.api.index.ElasticConnector;
import fi.vm.yti.datamodel.api.v2.dto.Iow;
import fi.vm.yti.datamodel.api.v2.dto.ModelConstants;
import fi.vm.yti.datamodel.api.v2.mapper.ModelMapper;
import fi.vm.yti.datamodel.api.v2.service.JenaService;
import org.apache.jena.arq.querybuilder.ConstructBuilder;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.vocabulary.DCTerms;
import org.apache.jena.vocabulary.OWL;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Map;


@Service
public class ElasticIndexer {

    private final Logger logger = LoggerFactory.getLogger(ElasticIndexer.class);
    private static final String ELASTIC_INDEX_MODEL = "models_v2";
    private static final String GRAPH_VARIABLE = "?model";
    private final ElasticConnector elasticConnector;
    private final ObjectMapper objectMapper;
    private final JenaService jenaService;
    private final ModelMapper modelMapper;
    private final RestHighLevelClient esClient;


    public ElasticIndexer(ElasticConnector elasticConnector,
                          ObjectMapper objectMapper,
                          JenaService jenaService,
                          ModelMapper modelMapper,
                          RestHighLevelClient esClient){
        this.elasticConnector = elasticConnector;
        this.objectMapper = objectMapper;
        this.jenaService = jenaService;
        this.modelMapper = modelMapper;
        this.esClient = esClient;
    }

    public void reindex() {
        try {
            elasticConnector.cleanIndex(ELASTIC_INDEX_MODEL);
            logger.info("v2 Indexes cleaned");
            elasticConnector.createIndex(ELASTIC_INDEX_MODEL, getModelMappings());
            initSearchIndexes();
            logger.info("Indexes initialized");
        } catch (IOException ex) {
            logger.warn("Reindex failed!", ex);
        }
    }

    private String getModelMappings() throws IOException {
        InputStream is = ElasticIndexer.class.getClassLoader().getResourceAsStream("model_v2_mapping.json");
        Object obj = objectMapper.readTree(is);
        return objectMapper.writeValueAsString(obj);
    }

    /**
     * A new model to index
     * @param model Model to index
     */
    public void createModelToIndex(IndexModel model){
        logger.info("Indexing: {}", model.getId());
        elasticConnector.putToIndex(ELASTIC_INDEX_MODEL, model.getId(), model);
    }

    /**
     * Update existing model in index
     * @param model Model to index
     */
    public void updateModelToIndex(IndexModel model){
        elasticConnector.updateToIndex(ELASTIC_INDEX_MODEL, model.getId(), model);
    }

    /**
     * Init search indexes
     */
    private void initSearchIndexes() throws IOException {
        initModelIndex();
    }

    /**
     * Init model index
     */
    public void initModelIndex() throws IOException {
        var constructBuilder = new ConstructBuilder()
                .addPrefixes(ModelConstants.PREFIXES);
        addProperty(constructBuilder, RDFS.label, "?prefLabel");
        addOptional(constructBuilder, RDFS.comment, "?comment");
        addProperty(constructBuilder, RDF.type, "?modelType");
        addProperty(constructBuilder, OWL.versionInfo, "?versionInfo");
        addProperty(constructBuilder, DCTerms.modified, "?modified");
        addProperty(constructBuilder, DCTerms.created, "?created");
        addProperty(constructBuilder, DCTerms.contributor, "?contributor");
        addProperty(constructBuilder, DCTerms.isPartOf, "?isPartOf");
        addOptional(constructBuilder, Iow.contentModified, "?contentModified");
        addOptional(constructBuilder, Iow.documentation, "?documentation");
        //TODO swap to commented text once older migration is ready
        //addProperty(constructBuilder, DCTerms.language, "?language");
        constructBuilder.addConstruct(GRAPH_VARIABLE, DCTerms.language, "?language")
                .addWhere(GRAPH_VARIABLE, "dcterms:language/rdf:rest*/rdf:first", "?language");

        var indexModels = jenaService.constructWithQuery(constructBuilder.build());
        var it = indexModels.listSubjects();
        var list = new ArrayList<IndexModel>();
        while(it.hasNext()){
            var resource = it.next();
            var newModel = ModelFactory.createDefaultModel()
                    .add(resource.listProperties());
            var indexModel = modelMapper.mapToIndexModel(resource.getLocalName(), newModel);
            list.add(indexModel);
        }
        //TODO check why http://uri.suomi/fi/datamodel/ns/aaa is not indexes
        var values = objectMapper.valueToTree(list);
        bulkInsert(ELASTIC_INDEX_MODEL, values);
    }

    private void addProperty(ConstructBuilder builder, Property property, String propertyName){
        builder.addConstruct(GRAPH_VARIABLE, property, propertyName)
                .addWhere(GRAPH_VARIABLE, property, propertyName);
    }

    private void addOptional(ConstructBuilder builder, Property property, String propertyName){
        builder.addConstruct(GRAPH_VARIABLE, property, propertyName)
                .addOptional(GRAPH_VARIABLE, property, propertyName);
    }

    public void bulkInsert(String indexName, JsonNode indexModels) throws IOException {
        var bulkrequest = new BulkRequest();
        indexModels.forEach(indexModel -> {
            var req = new IndexRequest(indexName, "doc", indexModel.get("id").asText())
                    .source(objectMapper.convertValue(indexModel, Map.class));
            bulkrequest.add(req);
        });
        if(bulkrequest.numberOfActions() > 0){
            var res = esClient.bulk(bulkrequest, RequestOptions.DEFAULT);
            logger.debug("Bulk insert status: {}", res.status().getStatus());
        }

    }
}
