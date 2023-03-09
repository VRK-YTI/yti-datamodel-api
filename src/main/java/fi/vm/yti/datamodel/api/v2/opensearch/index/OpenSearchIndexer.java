package fi.vm.yti.datamodel.api.v2.opensearch.index;

import fi.vm.yti.datamodel.api.index.OpenSearchConnector;
import fi.vm.yti.datamodel.api.v2.dto.Iow;
import fi.vm.yti.datamodel.api.v2.dto.ModelConstants;
import fi.vm.yti.datamodel.api.v2.mapper.ModelMapper;
import fi.vm.yti.datamodel.api.v2.mapper.ResourceMapper;
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
    public static final String OPEN_SEARCH_INDEX_RESOURCE = "resources_v2";

    private final Logger logger = LoggerFactory.getLogger(OpenSearchIndexer.class);
    private static final String GRAPH_VARIABLE = "?model";
    private final OpenSearchConnector openSearchConnector;
    private final JenaService jenaService;
    private final ModelMapper modelMapper;
    private final ResourceMapper resourceMapper;
    private final OpenSearchClient client;

    public OpenSearchIndexer(OpenSearchConnector openSearchConnector,
                             JenaService jenaService,
                             ModelMapper modelMapper,
                             ResourceMapper resourceMapper, OpenSearchClient client) {
        this.openSearchConnector = openSearchConnector;
        this.jenaService = jenaService;
        this.modelMapper = modelMapper;
        this.resourceMapper = resourceMapper;
        this.client = client;
    }

    public void reindex() {
        try {
            openSearchConnector.cleanIndex(OPEN_SEARCH_INDEX_MODEL);
            openSearchConnector.cleanIndex(OPEN_SEARCH_INDEX_RESOURCE);
            logger.info("v2 Indexes cleaned");
            openSearchConnector.createIndex(OPEN_SEARCH_INDEX_MODEL, getModelMappings());
            openSearchConnector.createIndex(OPEN_SEARCH_INDEX_RESOURCE, getResourceMappings());
            initSearchIndexes();

            logger.info("Indexes initialized");
        } catch (IOException ex) {
            logger.warn("Reindex failed!", ex);
        }
    }

    private TypeMapping getModelMappings() {
        return new TypeMapping.Builder()
                .dynamicTemplates(getModelDynamicTemplates())
                .properties(getModelProperties())
                .build();
    }

    private TypeMapping getResourceMappings() {
        return new TypeMapping.Builder()
                .dynamicTemplates(getClassDynamicTemplates())
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
     * @param indexResource Class to index
     */
    public void createResourceToIndex(IndexResource indexResource) {
        logger.info("Indexing: {}", indexResource.getId());
        openSearchConnector.putToIndex(OPEN_SEARCH_INDEX_RESOURCE, indexResource.getId(), indexResource);
    }

    /**
     * Update existing class in index
     *
     * @param indexResource Class to index
     */
    public void updateResourceToIndex(IndexResource indexResource) {
        logger.info("Updating index for: {}", indexResource.getId());
        openSearchConnector.updateToIndex(OPEN_SEARCH_INDEX_RESOURCE, indexResource.getId(), indexResource);
    }


    /**
     * Init search indexes
     */
    private void initSearchIndexes() throws IOException {
        initModelIndex();
        initResourceIndex();
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
                .addOptional(GRAPH_VARIABLE, "dcterms:language/rdf:rest*/rdf:first", "?language")
                .addOptional(GRAPH_VARIABLE, DCTerms.language, "?language");
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

    public void initResourceIndex() throws IOException {
        var constructBuilder = new ConstructBuilder()
                .addPrefixes(ModelConstants.PREFIXES)
                .addConstruct(GRAPH_VARIABLE, RDF.type, "?resourceType")
                .addWhere(GRAPH_VARIABLE, RDF.type, "?resourceType")
                .addWhereValueVar("?resourceType", OWL.Class, OWL.ObjectProperty, OWL.DatatypeProperty);
        addProperty(constructBuilder, RDFS.label, "?label");
        addProperty(constructBuilder, OWL.versionInfo, "?versionInfo");
        addProperty(constructBuilder, DCTerms.modified, "?modified");
        addProperty(constructBuilder, DCTerms.created, "?created");
        addOptional(constructBuilder, Iow.contentModified, "?contentModified");
        addProperty(constructBuilder, RDFS.isDefinedBy, "?isDefinedBy");
        addOptional(constructBuilder, SKOS.note, "?note");
        addOptional(constructBuilder, RDFS.subClassOf, "?subClassOf");
        addOptional(constructBuilder, RDFS.subPropertyOf, "?subPropertyOf");
        addOptional(constructBuilder, OWL.equivalentClass, "?equivalentClass");
        addOptional(constructBuilder, OWL.equivalentProperty, "?equivalentProperty");
        var indexClasses = jenaService.constructWithQuery(constructBuilder.build());
        var it = indexClasses.listSubjects();
        var list = new ArrayList<IndexResource>();
        while (it.hasNext()) {
            var resource = it.next();
            var newClass = ModelFactory.createDefaultModel()
                    .setNsPrefixes(indexClasses.getNsPrefixMap())
                    .add(resource.listProperties());
            var indexClass = resourceMapper.mapToIndexResource(newClass, resource.getURI());
            list.add(indexClass);
        }
        bulkInsert(OPEN_SEARCH_INDEX_RESOURCE, list);
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
        logger.debug("Bulk insert status: errors: {}, items: {}, took: {}", response.errors(), response.items().size(), response.took());
    }

    private List<Map<String, DynamicTemplate>> getModelDynamicTemplates() {
        return List.of(
                getDynamicTemplate("label", "label.*"),
                getDynamicTemplate("comment", "comment.*"),
                getDynamicTemplate("documentation", "documentation.*")
        );
    }

    private List<Map<String, DynamicTemplate>> getClassDynamicTemplates() {
        return List.of(
                getDynamicTemplate("label", "label.*"),
                getDynamicTemplate("note", "note.*")
        );
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
                "contentModified", getDateProperty(),
                "resourceType", getKeywordProperty());
    }

    private static Map<String, DynamicTemplate> getDynamicTemplate(String name, String pathMatch) {
        return Map.of(name, new DynamicTemplate.Builder()
                .pathMatch(pathMatch)
                .mapping(getTextKeyWordProperty()).build());
    }

    private static org.opensearch.client.opensearch._types.mapping.Property getTextKeyWordProperty() {
        return new org.opensearch.client.opensearch._types.mapping.Property.Builder()
                .text(new TextProperty.Builder()
                        .fields("keyword",
                                new KeywordProperty.Builder()
                                        .ignoreAbove(256)
                                        .build()
                                        ._toProperty())
                                        .build()
                     )
                .build();
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
}
