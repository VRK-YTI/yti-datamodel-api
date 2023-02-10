package fi.vm.yti.datamodel.api.v2.opensearch.index;

import fi.vm.yti.datamodel.api.index.OpenSearchConnector;
import fi.vm.yti.datamodel.api.v2.dto.Iow;
import fi.vm.yti.datamodel.api.v2.dto.ModelConstants;
import fi.vm.yti.datamodel.api.v2.mapper.ClassMapper;
import fi.vm.yti.datamodel.api.v2.mapper.ModelMapper;
import fi.vm.yti.datamodel.api.v2.service.JenaService;
import fi.vm.yti.datamodel.api.v2.utils.DataModelUtils;
import org.apache.jena.arq.querybuilder.ConstructBuilder;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.vocabulary.*;
import org.opensearch.client.opensearch.OpenSearchClient;
import org.opensearch.client.opensearch._types.mapping.*;
import org.opensearch.client.opensearch.core.BulkRequest;
import org.opensearch.client.opensearch.core.BulkResponse;
import org.opensearch.client.opensearch.core.bulk.BulkOperation;
import org.opensearch.client.opensearch.core.bulk.IndexOperation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;


@Service
public class OpenSearchIndexer {

    public static final String OPEN_SEARCH_INDEX_MODEL = "models_v2";
    private static final String OPEN_SEARCH_INDEX_CLASS = "class_v2";

    private final Logger logger = LoggerFactory.getLogger(OpenSearchIndexer.class);
    private static final String GRAPH_VARIABLE = "?model";
    private final OpenSearchConnector openSearchConnector;
    private final JenaService jenaService;
    private final ModelMapper modelMapper;
    private final ClassMapper classMapper;
    private final OpenSearchClient client;

    public OpenSearchIndexer(OpenSearchConnector openSearchConnector,
                             JenaService jenaService,
                             ModelMapper modelMapper,
                             ClassMapper classMapper,
                             OpenSearchClient client) {
        this.openSearchConnector = openSearchConnector;
        this.jenaService = jenaService;
        this.modelMapper = modelMapper;
        this.classMapper = classMapper;
        this.client = client;
    }

    public void reindex() {
        try {
            openSearchConnector.cleanIndex(OPEN_SEARCH_INDEX_MODEL);
            openSearchConnector.cleanIndex(OPEN_SEARCH_INDEX_CLASS);
            logger.info("v2 Indexes cleaned");
            openSearchConnector.createIndex(OPEN_SEARCH_INDEX_MODEL, getModelMappings());
            openSearchConnector.createIndex(OPEN_SEARCH_INDEX_CLASS, getClassMappings());
            initSearchIndexes();

            logger.info("Indexes initialized");
        } catch (IOException ex) {
            logger.warn("Reindex failed!", ex);
        }
    }

    private TypeMapping getModelMappings() {
        return new TypeMapping.Builder()
                .dynamicTemplates(List.of(getDynamicTemplate("label", "label.*")))
                .dynamicTemplates(List.of(getDynamicTemplate("comment", "comment.*")))
                .dynamicTemplates(List.of(getDynamicTemplate("documentation", "documentation.*")))
                .properties(getModelProperties())
                .build();
    }

    private TypeMapping getClassMappings() {
        return new TypeMapping.Builder()
                .dynamicTemplates(List.of(getDynamicTemplate("label", "label.*")))
                .properties(getClassProperties())
                .build();
    }

    /**
     * A new model to index
     *
     * @param model Model to index
     */
    public void createModelToIndex(IndexModel model) {
        logger.info("Indexing: {}", model.getId());
        openSearchConnector.putToIndex(OPEN_SEARCH_INDEX_MODEL, model.getId(), model);
    }

    /**
     * Update existing model in index
     *
     * @param model Model to index
     */
    public void updateModelToIndex(IndexModel model) {
        openSearchConnector.updateToIndex(OPEN_SEARCH_INDEX_MODEL, model.getId(), model);
    }

    /**
     * A new class to index
     *
     * @param indexClass Class to index
     */
    public void createClassToIndex(IndexClass indexClass) {
        logger.info("Indexing: {}", indexClass.getId());
        openSearchConnector.putToIndex(OPEN_SEARCH_INDEX_CLASS, indexClass.getId(), indexClass);
    }

    /**
     * Update existing class in index
     *
     * @param indexClass Class to index
     */
    public void updateClassToIndex(IndexClass indexClass) {
        logger.info("Updating index for: {}", indexClass.getId());
        openSearchConnector.updateToIndex(OPEN_SEARCH_INDEX_CLASS, indexClass.getId(), indexClass);
    }


    /**
     * Init search indexes
     */
    private void initSearchIndexes() throws IOException {
        initModelIndex();
        initClassIndex();
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
                .addWhereValueVar("?langTypes", DCTerms.language, "dcterms:language/rdf:rest*/rdf:first")
                .addWhere(GRAPH_VARIABLE, "?langTypes", "?language");

        var indexModels = jenaService.constructWithQuery(constructBuilder.build());
        var it = indexModels.listSubjects();
        var list = new ArrayList<IndexModel>();
        while (it.hasNext()) {
            var resource = it.next();
            var newModel = ModelFactory.createDefaultModel()
                    .add(resource.listProperties());
            var indexModel = modelMapper.mapToIndexModel(resource.getLocalName(), newModel);
            list.add(indexModel);
        }
        bulkInsert(OPEN_SEARCH_INDEX_MODEL, list);
    }

    public void initClassIndex() throws IOException {
        var constructBuilder = new ConstructBuilder()
                .addPrefixes(ModelConstants.PREFIXES)
                .addWhere(GRAPH_VARIABLE, RDF.type, "?classType")
                .addWhereValueVar("?classType", OWL.Class);
        addProperty(constructBuilder, RDFS.label, "?label");
        addProperty(constructBuilder, OWL.versionInfo, "?versionInfo");
        addProperty(constructBuilder, DCTerms.modified, "?modified");
        addProperty(constructBuilder, DCTerms.created, "?created");
        addOptional(constructBuilder, Iow.contentModified, "?contentModified");
        addProperty(constructBuilder, RDFS.isDefinedBy, "?isDefinedBy");
        addOptional(constructBuilder, SKOS.note, "?comment");
        addOptional(constructBuilder, RDFS.subClassOf, "?subClassOf");
        addOptional(constructBuilder, OWL.equivalentClass, "?equivalentClass");
        var indexClasses = jenaService.constructWithQuery(constructBuilder.build());
        var it = indexClasses.listSubjects();
        var list = new ArrayList<IndexClass>();
        while (it.hasNext()) {
            var resource = it.next();
            var newClass = ModelFactory.createDefaultModel()
                    .add(resource.listProperties());
            var indexClass = classMapper.mapToIndexClass(newClass, resource.getURI());
            list.add(indexClass);
        }
        bulkInsert(OPEN_SEARCH_INDEX_CLASS, list);
    }

    private void addProperty(ConstructBuilder builder, Property property, String propertyName) {
        builder.addConstruct(GRAPH_VARIABLE, property, propertyName)
                .addWhere(GRAPH_VARIABLE, property, propertyName);
    }

    private void addOptional(ConstructBuilder builder, Property property, String propertyName) {
        builder.addConstruct(GRAPH_VARIABLE, property, propertyName)
                .addOptional(GRAPH_VARIABLE, property, propertyName);
    }

    public <T extends IndexBase> void bulkInsert(String indexName,
                                                 List<T> documents) throws IOException {
        var bulkRequest = new BulkRequest.Builder();
        List<BulkOperation> bulkOperations = new ArrayList<>();
        documents.forEach(doc ->
                bulkOperations.add(new IndexOperation.Builder<IndexBase>()
                        .index(indexName)
                        .id(DataModelUtils.encode(doc.getId()))
                        .document(doc)
                        .build().
                        _toBulkOperation())
        );
        if (bulkOperations.isEmpty()) {
            logger.info("No data to index");
            return;
        }
        bulkRequest.operations(bulkOperations);
        BulkResponse response = client.bulk(bulkRequest.build());
        logger.debug("Bulk insert status: {}", response.toString());
    }


    private Map<String, org.opensearch.client.opensearch._types.mapping.Property> getModelProperties() {
        return Map.of("id", getKeywordProperty(),
                "status", getKeywordProperty(),
                "type", getKeywordProperty(),
                "prefix", getKeywordProperty(),
                "contributor", getKeywordProperty(),
                "language", getKeywordProperty(),
                "isPartOf", getKeywordProperty(),
                "created", getDateProperty(),
                "contentModified", getDateProperty());
    }

    private Map<String, org.opensearch.client.opensearch._types.mapping.Property> getClassProperties() {
        return Map.of("id", getKeywordProperty(),
                "status", getKeywordProperty(),
                "isDefinedBy", getKeywordProperty(),
                "comment", getKeywordProperty(),
                "namespace", getKeywordProperty(),
                "identifier", getKeywordProperty(),
                "created", getDateProperty(),
                "modified", getDateProperty(),
                "contentModified", getDateProperty());
    }

    private static Map<String, DynamicTemplate> getDynamicTemplate(String name, String pathMatch) {
        return Map.of(name, new DynamicTemplate.Builder()
                .pathMatch(pathMatch)
                .mapping(getTextProperty()).build());
    }

    private static org.opensearch.client.opensearch._types.mapping.Property getKeywordProperty() {
        return new org.opensearch.client.opensearch._types.mapping.Property.Builder()
                .keyword(new KeywordProperty.Builder().build())
                .build();
    }

    private static org.opensearch.client.opensearch._types.mapping.Property getDateProperty() {
        return new org.opensearch.client.opensearch._types.mapping.Property.Builder()
                .date(new DateProperty.Builder().build()).build();
    }

    private static org.opensearch.client.opensearch._types.mapping.Property getTextProperty() {
        return new org.opensearch.client.opensearch._types.mapping.Property.Builder()
                .text(new TextProperty.Builder().build()).build();
    }
}
