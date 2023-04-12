package fi.vm.yti.datamodel.api.v2.opensearch.index;

import fi.vm.yti.datamodel.api.index.OpenSearchConnector;
import fi.vm.yti.datamodel.api.v2.dto.Iow;
import fi.vm.yti.datamodel.api.v2.dto.ModelConstants;
import fi.vm.yti.datamodel.api.v2.mapper.ModelMapper;
import fi.vm.yti.datamodel.api.v2.mapper.ResourceMapper;
import fi.vm.yti.datamodel.api.v2.service.JenaService;
import fi.vm.yti.datamodel.api.v2.utils.DataModelUtils;
import fi.vm.yti.datamodel.api.v2.utils.SparqlUtils;
import org.apache.jena.arq.querybuilder.ConstructBuilder;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.vocabulary.*;
import org.opensearch.client.opensearch.OpenSearchClient;
import org.opensearch.client.opensearch._types.mapping.*;
import org.opensearch.client.opensearch.core.BulkRequest;
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
    private final OpenSearchClient client;

    public OpenSearchIndexer(OpenSearchConnector openSearchConnector,
                             JenaService jenaService,
                             ModelMapper modelMapper,
                             OpenSearchClient client) {
        this.openSearchConnector = openSearchConnector;
        this.jenaService = jenaService;
        this.modelMapper = modelMapper;
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

    public void deleteModelFromIndex(String graph) {
        openSearchConnector.removeFromIndex(OPEN_SEARCH_INDEX_MODEL, graph);
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

    public void deleteResourceFromIndex(String id){
        logger.info("Removing index for: {}", id);
        openSearchConnector.removeFromIndex(OPEN_SEARCH_INDEX_RESOURCE, id);
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
        SparqlUtils.addConstructProperty(GRAPH_VARIABLE, constructBuilder, RDFS.label, "?prefLabel");
        SparqlUtils.addConstructOptional(GRAPH_VARIABLE, constructBuilder, RDFS.comment, "?comment");
        SparqlUtils.addConstructProperty(GRAPH_VARIABLE, constructBuilder, RDF.type, "?modelType");
        SparqlUtils.addConstructProperty(GRAPH_VARIABLE, constructBuilder, OWL.versionInfo, "?versionInfo");
        SparqlUtils.addConstructProperty(GRAPH_VARIABLE, constructBuilder, DCTerms.modified, "?modified");
        SparqlUtils.addConstructProperty(GRAPH_VARIABLE, constructBuilder, DCTerms.created, "?created");
        SparqlUtils.addConstructProperty(GRAPH_VARIABLE, constructBuilder, DCTerms.contributor, "?contributor");
        SparqlUtils.addConstructProperty(GRAPH_VARIABLE, constructBuilder, DCTerms.isPartOf, "?isPartOf");
        SparqlUtils.addConstructOptional(GRAPH_VARIABLE, constructBuilder, Iow.contentModified, "?contentModified");
        SparqlUtils.addConstructOptional(GRAPH_VARIABLE, constructBuilder, Iow.documentation, "?documentation");
        //TODO swap to commented text once older migration is ready
        //addProperty(constructBuilder, DCTerms.language, "?language");
        constructBuilder.addConstruct(GRAPH_VARIABLE, DCTerms.language, "?language")
                .addOptional(GRAPH_VARIABLE, "dcterms:language/rdf:rest*/rdf:first", "?language")
                .addOptional(GRAPH_VARIABLE, DCTerms.language, "?language");
        var indexModels = jenaService.constructWithQuery(constructBuilder.build());
        var list = new ArrayList<IndexModel>();
        indexModels.listSubjects().forEach(next -> {
            var newModel = ModelFactory.createDefaultModel()
                    .add(next.listProperties());
            var indexModel = modelMapper.mapToIndexModel(next.getLocalName(), newModel);
            list.add(indexModel);
        });
        bulkInsert(OPEN_SEARCH_INDEX_MODEL, list);
    }

    public void initResourceIndex() throws IOException {
        var constructBuilder = new ConstructBuilder()
                .addPrefixes(ModelConstants.PREFIXES)
                .addConstruct(GRAPH_VARIABLE, RDF.type, "?resourceType")
                .addWhere(GRAPH_VARIABLE, RDF.type, "?resourceType")
                .addWhereValueVar("?resourceType", OWL.Class, OWL.ObjectProperty, OWL.DatatypeProperty);
        SparqlUtils.addConstructProperty(GRAPH_VARIABLE, constructBuilder, RDFS.label, "?label");
        SparqlUtils.addConstructProperty(GRAPH_VARIABLE, constructBuilder, OWL.versionInfo, "?versionInfo");
        SparqlUtils.addConstructProperty(GRAPH_VARIABLE, constructBuilder, DCTerms.modified, "?modified");
        SparqlUtils.addConstructProperty(GRAPH_VARIABLE, constructBuilder, DCTerms.created, "?created");
        SparqlUtils.addConstructOptional(GRAPH_VARIABLE, constructBuilder, Iow.contentModified, "?contentModified");
        SparqlUtils.addConstructProperty(GRAPH_VARIABLE, constructBuilder, RDFS.isDefinedBy, "?isDefinedBy");
        SparqlUtils.addConstructOptional(GRAPH_VARIABLE, constructBuilder, SKOS.note, "?note");
        SparqlUtils.addConstructOptional(GRAPH_VARIABLE, constructBuilder, RDFS.subClassOf, "?subClassOf");
        SparqlUtils.addConstructOptional(GRAPH_VARIABLE, constructBuilder, RDFS.subPropertyOf, "?subPropertyOf");
        SparqlUtils.addConstructOptional(GRAPH_VARIABLE, constructBuilder, OWL.equivalentClass, "?equivalentClass");
        SparqlUtils.addConstructOptional(GRAPH_VARIABLE, constructBuilder, OWL.equivalentProperty, "?equivalentProperty");
        SparqlUtils.addConstructOptional(GRAPH_VARIABLE, constructBuilder, DCTerms.subject, "?subject");

        var indexClasses = jenaService.constructWithQuery(constructBuilder.build());
        var list = new ArrayList<IndexResource>();
        indexClasses.listSubjects().forEach(next -> {
            var newClass = ModelFactory.createDefaultModel()
                    .setNsPrefixes(indexClasses.getNsPrefixMap())
                    .add(next.listProperties());
            var indexClass = ResourceMapper.mapToIndexResource(newClass, next.getURI());
            list.add(indexClass);
        });
        bulkInsert(OPEN_SEARCH_INDEX_RESOURCE, list);
    }

    public <T extends IndexBase> void bulkInsert(String indexName,
                                                 List<T> documents) throws IOException {
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
        var bulkRequest = new BulkRequest.Builder()
                                .operations(bulkOperations);
        var response = client.bulk(bulkRequest.build());
        logger.debug("Bulk insert status for {}: errors: {}, items: {}, took: {}", indexName, response.errors(), response.items().size(), response.took());
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
