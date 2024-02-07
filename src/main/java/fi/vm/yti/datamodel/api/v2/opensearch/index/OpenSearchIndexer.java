package fi.vm.yti.datamodel.api.v2.opensearch.index;

import com.google.common.collect.Iterables;
import fi.vm.yti.datamodel.api.index.OpenSearchConnector;
import fi.vm.yti.datamodel.api.security.AuthorizationManager;
import fi.vm.yti.datamodel.api.v2.dto.ModelConstants;
import fi.vm.yti.datamodel.api.v2.dto.ResourceType;
import fi.vm.yti.datamodel.api.v2.mapper.ModelMapper;
import fi.vm.yti.datamodel.api.v2.mapper.ResourceMapper;
import fi.vm.yti.datamodel.api.v2.opensearch.queries.QueryFactoryUtils;
import fi.vm.yti.datamodel.api.v2.properties.DCAP;
import fi.vm.yti.datamodel.api.v2.properties.SuomiMeta;
import fi.vm.yti.datamodel.api.v2.repository.CoreRepository;
import fi.vm.yti.datamodel.api.v2.repository.ImportsRepository;
import fi.vm.yti.datamodel.api.v2.service.AuditService;
import fi.vm.yti.datamodel.api.v2.service.NamespaceService;
import fi.vm.yti.datamodel.api.v2.utils.DataModelUtils;
import fi.vm.yti.datamodel.api.v2.utils.SparqlUtils;
import fi.vm.yti.security.AuthenticatedUserProvider;
import org.apache.jena.arq.querybuilder.ConstructBuilder;
import org.apache.jena.arq.querybuilder.SelectBuilder;
import org.apache.jena.arq.querybuilder.WhereBuilder;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.vocabulary.*;
import org.opensearch.client.opensearch.OpenSearchClient;
import org.opensearch.client.opensearch._types.mapping.*;
import org.opensearch.client.opensearch._types.query_dsl.Query;
import org.opensearch.client.opensearch._types.query_dsl.QueryBuilders;
import org.opensearch.client.opensearch.core.BulkRequest;
import org.opensearch.client.opensearch.core.bulk.BulkOperation;
import org.opensearch.client.opensearch.core.bulk.IndexOperation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.topbraid.shacl.vocabulary.SH;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static fi.vm.yti.security.AuthorizationException.check;


@Service
public class OpenSearchIndexer {

    private final AuditService auditService = new AuditService("INDEX");

    public static final String OPEN_SEARCH_INDEX_MODEL = "models_v2";
    public static final String OPEN_SEARCH_INDEX_RESOURCE = "resources_v2";
    public static final String OPEN_SEARCH_INDEX_EXTERNAL = "external_v2";

    private final Logger logger = LoggerFactory.getLogger(OpenSearchIndexer.class);
    private static final String GRAPH_VARIABLE = "?model";
    private final OpenSearchConnector openSearchConnector;
    private final CoreRepository coreRepository;
    private final ImportsRepository importsRepository;
    private final AuthorizationManager authorizationManager;
    private final AuthenticatedUserProvider userProvider;
    private final ModelMapper modelMapper;
    private final OpenSearchClient client;

    public OpenSearchIndexer(OpenSearchConnector openSearchConnector,
                             CoreRepository coreRepository,
                             ImportsRepository importsRepository,
                             AuthorizationManager authorizationManager,
                             AuthenticatedUserProvider userProvider, ModelMapper modelMapper,
                             OpenSearchClient client) {
        this.openSearchConnector = openSearchConnector;
        this.coreRepository = coreRepository;
        this.importsRepository = importsRepository;
        this.authorizationManager = authorizationManager;
        this.userProvider = userProvider;
        this.modelMapper = modelMapper;
        this.client = client;
    }

    public void initIndexes(){
        try {
            openSearchConnector.cleanIndex(OPEN_SEARCH_INDEX_MODEL);
            openSearchConnector.cleanIndex(OPEN_SEARCH_INDEX_RESOURCE);
            logger.info("v2 Indexes cleaned");
            openSearchConnector.createIndex(OPEN_SEARCH_INDEX_MODEL, getModelMappings());
            openSearchConnector.createIndex(OPEN_SEARCH_INDEX_RESOURCE, getResourceMappings());
            initModelIndex();
            initResourceIndex();

            if (!openSearchConnector.indexExists(OPEN_SEARCH_INDEX_EXTERNAL)) {
                openSearchConnector.createIndex(OPEN_SEARCH_INDEX_EXTERNAL, getExternalResourceMappings());
                initExternalResourceIndex();
            }

            logger.info("Indexes initialized");
        } catch (IOException ex) {
            logger.warn("Index initialization failed!", ex);
        }
    }

    public void reindex(String index){
        check(authorizationManager.hasRightToDropDatabase());
        if(index == null){
            reindexAll();
            auditService.log(AuditService.ActionType.UPDATE, "all_indexes", userProvider.getUser());
            return;
        }
        switch (index){
            case OpenSearchIndexer.OPEN_SEARCH_INDEX_MODEL -> initModelIndex();
            case OpenSearchIndexer.OPEN_SEARCH_INDEX_RESOURCE -> initResourceIndex();
            default -> throw new IllegalArgumentException("Given value not allowed");
        }
        auditService.log(AuditService.ActionType.UPDATE, index, userProvider.getUser());
    }

    public void reindexAll() {
        try {
            openSearchConnector.cleanIndex(OPEN_SEARCH_INDEX_MODEL);
            openSearchConnector.cleanIndex(OPEN_SEARCH_INDEX_RESOURCE);
            openSearchConnector.cleanIndex(OPEN_SEARCH_INDEX_EXTERNAL);
            logger.info("v2 Indexes cleaned");
            openSearchConnector.createIndex(OPEN_SEARCH_INDEX_MODEL, getModelMappings());
            openSearchConnector.createIndex(OPEN_SEARCH_INDEX_RESOURCE, getResourceMappings());
            openSearchConnector.createIndex(OPEN_SEARCH_INDEX_EXTERNAL, getExternalResourceMappings());
            initModelIndex();
            initResourceIndex();
            initExternalResourceIndex();

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

    private TypeMapping getExternalResourceMappings() {
        return new TypeMapping.Builder()
                .dynamicTemplates(geExternalResourcesDynamicTemplates())
                .properties(getExternalResourcesProperties())
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
     * Removes all resources that are based a certain data model
     *
     * @param modelUri Model URI
     * @param version  version number, will remove draft version resources if null or empty
     */
    public void removeResourceIndexesByDataModel(String modelUri, String version) {
        logger.info("Removing resource indexes from model: {}, version: {}", modelUri, version);
        var queries = new ArrayList<Query>();

        queries.add(QueryFactoryUtils.termQuery("isDefinedBy", modelUri));

        if(version != null && !version.isBlank()) {
            queries.add(QueryFactoryUtils.termQuery("fromVersion", version));
        } else {
            queries.add(QueryFactoryUtils.existsQuery("fromVersion", true));
        }

        var finalQuery = QueryBuilders.bool()
                .must(queries)
                .build()
                ._toQuery();

        openSearchConnector.removeFromIndexWithQuery(OPEN_SEARCH_INDEX_RESOURCE, finalQuery);
    }

    /**
     * Init model index
     */
    public void initModelIndex() {
        var constructBuilder = new ConstructBuilder()
                .addPrefixes(ModelConstants.PREFIXES);

        var whereBuilder = new WhereBuilder();
        Stream.of(RDFS.label, DCTerms.language, DCAP.preferredXMLNamespacePrefix, RDF.type, SuomiMeta.publicationStatus, DCTerms.modified, DCTerms.created, DCTerms.contributor, DCTerms.isPartOf)
                .forEach(property -> SparqlUtils.addRequiredToGraphConstruct(GRAPH_VARIABLE, constructBuilder, whereBuilder, property));
        Stream.of(RDFS.comment, OWL2.versionIRI, SuomiMeta.contentModified, SuomiMeta.documentation, OWL.versionInfo)
                .forEach(property -> SparqlUtils.addOptionalToGraphConstruct(GRAPH_VARIABLE, constructBuilder, whereBuilder, property));
        constructBuilder.addGraph("?g", whereBuilder);
        var indexModels = coreRepository.queryConstruct(constructBuilder.build());
        var list = new ArrayList<IndexModel>();
        indexModels.listSubjects().forEach(next -> {
            var newModel = ModelFactory.createDefaultModel()
                    .add(next.listProperties());
            var indexModel = modelMapper.mapToIndexModel(next.getURI(), newModel);
            list.add(indexModel);
        });
        bulkInsert(OPEN_SEARCH_INDEX_MODEL, list);
    }

    public void initResourceIndex() {

        var selectBuilder = new SelectBuilder();
        selectBuilder.addPrefixes(ModelConstants.PREFIXES);
        var exprFactory = selectBuilder.getExprFactory();
        var expr = exprFactory.strstarts(exprFactory.str("?g"), ModelConstants.SUOMI_FI_NAMESPACE);
        selectBuilder.addFilter(expr);
        selectBuilder.addGraph("?g", new WhereBuilder());

        var graphs = new ArrayList<String>();
        coreRepository.querySelect(selectBuilder.build(), res -> graphs.add(res.get("g").toString()));

        graphs.forEach(graph -> {
            var model = coreRepository.fetch(graph);
            //list resources with type Class, Property, DatatypeProperty, NodeShape
            var resources = model.listSubjectsWithProperty(RDF.type, OWL.Class)
                    .andThen(model.listSubjectsWithProperty(RDF.type, OWL.ObjectProperty))
                    .andThen(model.listSubjectsWithProperty(RDF.type, OWL.DatatypeProperty))
                    .andThen(model.listSubjectsWithProperty(RDF.type, SH.NodeShape))
                    .filterDrop(RDFNode::isAnon);
            var list = resources.mapWith(resource -> ResourceMapper.mapToIndexResource(model, resource.getURI())).toList();
            //we should bulk insert resources for each model separately if there are a lot of resources otherwise might cause memory issues
            if(!list.isEmpty()){
                bulkInsert(OPEN_SEARCH_INDEX_RESOURCE, list);
            }
        });
    }

    public void initExternalResourceIndex() {
        NamespaceService.DEFAULT_NAMESPACES.forEach((prefix, graphURI) -> {
            logger.info("Indexing external namespace {}", graphURI);
            var builder = new ConstructBuilder()
                    .addPrefix(prefix, graphURI)
                    .addPrefixes(ModelConstants.PREFIXES)
                    .from(graphURI);

            SparqlUtils.addConstructOptional("?s", builder, RDFS.label, "?label");
            SparqlUtils.addConstructOptional("?s", builder, RDFS.isDefinedBy, "?isDefinedBy");
            SparqlUtils.addConstructOptional("?s", builder, RDFS.comment, "?comment");
            SparqlUtils.addConstructOptional("?s", builder, RDF.type, "?type");
            SparqlUtils.addConstructOptional("?s", builder, OWL.inverseOf, "?inverseOf");

            var result = importsRepository.queryConstruct(builder.build());
            var list = new ArrayList<IndexBase>();
            result.listSubjects().forEach(resource -> {
                var indexResource = ResourceMapper.mapExternalToIndexResource(resource);
                if (indexResource == null) {
                    logger.info("Could not determine required properties for resource {}", resource.getURI());
                    return;
                }

                // try to find type from other graphs in imports dataset
                if (indexResource.getResourceType() == null && resource.hasProperty(RDF.type)) {
                    var select = new SelectBuilder()
                        .addPrefixes(ModelConstants.PREFIXES)
                        .addVar("?t");

                    var where = new WhereBuilder();
                    var typeProperties = resource.listProperties(RDF.type);
                    typeProperties.forEach(t -> where.addUnion(
                            new WhereBuilder().addWhere(t.getObject(), RDF.type, "?t")
                    ));
                    select.addWhere(where).addGroupBy("?t");

                    var types = new ArrayList<RDFNode>();
                    importsRepository.querySelect(select.build(), (res -> types.add(res.get("t"))));

                    if (types.contains(OWL.Class) || types.contains(RDFS.Class)) {
                        indexResource.setResourceType(ResourceType.CLASS);
                    } else if (types.contains(OWL.DatatypeProperty)) {
                        indexResource.setResourceType(ResourceType.ATTRIBUTE);
                    } else if (types.contains(OWL.ObjectProperty)) {
                        indexResource.setResourceType(ResourceType.ASSOCIATION);
                    } else {
                        logger.warn("Cannot determine type for resource {}, types: {}", resource,
                                String.join(", ", typeProperties
                                        .mapWith(t -> t.getObject().toString())
                                        .toList())
                        );
                        return;
                    }
                }
                list.add(indexResource);
            });
            logger.info("Indexing {} items to index {},", list.size(), OPEN_SEARCH_INDEX_EXTERNAL);
            bulkInsert(OPEN_SEARCH_INDEX_EXTERNAL, list);
        });
    }

    public <T extends IndexBase> void bulkInsert(String indexName,
                                                 List<T> documents) {
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

        Iterables.partition(bulkOperations, 300).forEach(batch -> {
            var bulkRequest = new BulkRequest.Builder()
                    .operations(batch);
            try {
                var response = client.bulk(bulkRequest.build());

                if (response.errors()) {
                    logger.warn("Errors occurred in bulk operation");
                    response.items().stream()
                            .filter(i -> i.error() != null)
                            .forEach(i -> logger.warn("Error in document {}, caused by {}", i.id(), i.error().reason()));
                }
                logger.debug("Bulk insert status for {}: errors: {}, items: {}, took: {}ms",
                        indexName, response.errors(), response.items().size(), response.took());
            } catch (IOException e) {
                logger.warn("Error in bulk operation", e);
            }
        });
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

    private List<Map<String, DynamicTemplate>> geExternalResourcesDynamicTemplates() {
        return List.of(
                getDynamicTemplate("label", "label.*")
        );
    }

    private Map<String, Property> getModelProperties() {
        return Map.ofEntries(
                Map.entry("id", getKeywordProperty()),
                Map.entry("status", getKeywordProperty()),
                Map.entry("type", getKeywordProperty()),
                Map.entry("prefix", getKeywordProperty()),
                Map.entry("contributor", getKeywordProperty()),
                Map.entry("language", getKeywordProperty()),
                Map.entry("isPartOf", getKeywordProperty()),
                Map.entry("uri", getKeywordProperty()),
                Map.entry("versionIri", getKeywordProperty()),
                Map.entry("version", getKeywordProperty()),
                Map.entry("created", getDateProperty()),
                Map.entry("contentModified", getDateProperty())
        );
    }

    private Map<String, Property> getClassProperties() {
        return Map.ofEntries(
                Map.entry("id", getKeywordProperty()),
                Map.entry("uri", getKeywordProperty()),
                Map.entry("status", getKeywordProperty()),
                Map.entry("isDefinedBy", getKeywordProperty()),
                Map.entry("comment", getKeywordProperty()),
                Map.entry("namespace", getKeywordProperty()),
                Map.entry("identifier", getKeywordProperty()),
                Map.entry("created", getDateProperty()),
                Map.entry("modified", getDateProperty()),
                Map.entry("fromVersion", getKeywordProperty()),
                Map.entry("resourceType", getKeywordProperty()),
                Map.entry("targetClass", getKeywordProperty()),
                Map.entry("versionIri", getKeywordProperty())
        );
    }

    private Map<String, Property> getExternalResourcesProperties() {
        return Map.of("id", getKeywordProperty(),
                "status", getKeywordProperty(),
                "isDefinedBy", getKeywordProperty(),
                "namespace", getKeywordProperty(),
                "identifier", getKeywordProperty(),
                "resourceType", getKeywordProperty());
    }

    private static Map<String, DynamicTemplate> getDynamicTemplate(String name, String pathMatch) {
        return Map.of(name, new DynamicTemplate.Builder()
                .pathMatch(pathMatch)
                .mapping(getTextKeyWordProperty()).build());
    }

    private static Property getTextKeyWordProperty() {
        return new Property.Builder()
                .text(new TextProperty.Builder()
                        .fields("keyword",
                                new KeywordProperty.Builder()
                                        .normalizer("lowercase")
                                        .ignoreAbove(256)
                                        .build()
                                        ._toProperty())
                                        .build()
                     )
                .build();
    }

    private static Property getKeywordProperty() {
        return new Property.Builder()
                .keyword(new KeywordProperty.Builder().build())
                .build();
    }

    private static Property getDateProperty() {
        return new Property.Builder()
                .date(new DateProperty.Builder().build()).build();
    }
}
