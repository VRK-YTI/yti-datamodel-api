package fi.vm.yti.datamodel.api.v2.elasticsearch.index;

import com.fasterxml.jackson.databind.ObjectMapper;
import fi.vm.yti.datamodel.api.index.ElasticConnector;
import fi.vm.yti.datamodel.api.index.SearchIndexManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;

@Service
public class ElasticIndexer {

    private final Logger logger = LoggerFactory.getLogger(ElasticIndexer.class);
    private static final String ELASTIC_INDEX_MODEL = "models_v2";
    private final ElasticConnector elasticConnector;
    private final ObjectMapper objectMapper;


    public ElasticIndexer(ElasticConnector elasticConnector,
                          ObjectMapper objectMapper){
        this.elasticConnector = elasticConnector;
        this.objectMapper = objectMapper;
    }

    public void reindex() throws IOException {
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
        //TODO create model mappings
        InputStream is = SearchIndexManager.class.getClassLoader().getResourceAsStream("model_mapping.json");
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
     * @throws IOException
     */
    private void initSearchIndexes() throws IOException {
        initModelIndex();
    }

    /**
     * Init model index
     */
    private void initModelIndex(){
        //TODO get all models from fuseki
    }
}
