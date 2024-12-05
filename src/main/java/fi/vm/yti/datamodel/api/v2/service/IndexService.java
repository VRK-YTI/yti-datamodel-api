package fi.vm.yti.datamodel.api.v2.service;

import fi.vm.yti.common.Constants;
import fi.vm.yti.common.opensearch.IndexBase;
import fi.vm.yti.common.opensearch.OpenSearchClientWrapper;
import fi.vm.yti.common.opensearch.OpenSearchInitializer;
import fi.vm.yti.common.opensearch.QueryFactoryUtils;
import fi.vm.yti.common.properties.SuomiMeta;
import fi.vm.yti.common.service.AuditService;
import fi.vm.yti.datamodel.api.v2.security.DataModelAuthorizationManager;
import fi.vm.yti.datamodel.api.v2.dto.ModelConstants;
import fi.vm.yti.datamodel.api.v2.dto.ResourceType;
import fi.vm.yti.datamodel.api.v2.mapper.ModelMapper;
import fi.vm.yti.datamodel.api.v2.mapper.ResourceMapper;
import fi.vm.yti.datamodel.api.v2.opensearch.index.IndexModel;
import fi.vm.yti.datamodel.api.v2.opensearch.index.IndexResource;
import fi.vm.yti.datamodel.api.v2.properties.DCAP;
import fi.vm.yti.datamodel.api.v2.repository.CoreRepository;
import fi.vm.yti.datamodel.api.v2.repository.ImportsRepository;
import fi.vm.yti.datamodel.api.v2.utils.SparqlUtils;
import fi.vm.yti.security.AuthenticatedUserProvider;
import org.apache.jena.arq.querybuilder.ConstructBuilder;
import org.apache.jena.arq.querybuilder.SelectBuilder;
import org.apache.jena.arq.querybuilder.WhereBuilder;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.vocabulary.*;
import org.opensearch.client.opensearch._types.mapping.DynamicTemplate;
import org.opensearch.client.opensearch._types.mapping.Property;
import org.opensearch.client.opensearch._types.mapping.TypeMapping;
import org.opensearch.client.opensearch._types.query_dsl.Query;
import org.opensearch.client.opensearch._types.query_dsl.QueryBuilders;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.topbraid.shacl.vocabulary.SH;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static fi.vm.yti.common.opensearch.OpenSearchUtil.*;
import static fi.vm.yti.security.AuthorizationException.check;

@Service
public class IndexService extends OpenSearchInitializer {

    private static final Logger LOG = LoggerFactory.getLogger(IndexService.class);
    public static final String OPEN_SEARCH_INDEX_MODEL = "models_v2";
    public static final String OPEN_SEARCH_INDEX_RESOURCE = "resources_v2";
    public static final String OPEN_SEARCH_INDEX_EXTERNAL = "external_v2";
    private static final String GRAPH_VARIABLE = "?model";

    private final AuditService auditService = new AuditService("INDEX");
    private final CoreRepository coreRepository;
    private final ImportsRepository importsRepository;
    private final DataModelAuthorizationManager authorizationManager;
    private final AuthenticatedUserProvider userProvider;
    private final ModelMapper modelMapper;
    private final OpenSearchClientWrapper client;
    public IndexService(OpenSearchClientWrapper client,
                        CoreRepository coreRepository,
                        ImportsRepository importsRepository,
                        DataModelAuthorizationManager authorizationManager,
                        AuthenticatedUserProvider userProvider,
                        ModelMapper modelMapper) {
        super(client);
        this.client = client;
        this.coreRepository = coreRepository;
        this.importsRepository = importsRepository;
        this.authorizationManager = authorizationManager;
        this.userProvider = userProvider;
        this.modelMapper = modelMapper;
    }

    public void initIndexes() {
        InitIndexesFunction fn = this::initDataModelIndexes;

        var indexConfig = Map.of(
                OPEN_SEARCH_INDEX_MODEL, getModelMappings(),
                OPEN_SEARCH_INDEX_RESOURCE, getResourceMappings()
        );
        super.initIndexes(fn, indexConfig);
    }

    public void initDataModelIndexes() {
        try {
            initModelIndex();
            initResourceIndex();

            if (!client.indexExists(OPEN_SEARCH_INDEX_EXTERNAL)) {
                client.createIndex(OPEN_SEARCH_INDEX_EXTERNAL, getExternalResourceMappings());
                initExternalResourceIndex();
            }

            LOG.info("Indexes initialized");
        } catch (IOException ex) {
            LOG.warn("Index initialization failed!", ex);
        }
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
        client.bulkInsert(OPEN_SEARCH_INDEX_MODEL, list);
    }

    public void initResourceIndex() {

        var selectBuilder = new SelectBuilder();
        selectBuilder.addPrefixes(ModelConstants.PREFIXES);
        var exprFactory = selectBuilder.getExprFactory();
        var expr = exprFactory.strstarts(exprFactory.str("?g"), Constants.DATA_MODEL_NAMESPACE);
        selectBuilder.addFilter(expr);
        selectBuilder.addGraph("?g", new WhereBuilder());

        var graphs = new ArrayList<String>();
        coreRepository.querySelect(selectBuilder.build(), res -> graphs.add(res.get("g").toString()));

        graphs.forEach(graph -> {
            var model = coreRepository.fetch(graph);
            indexGraphResource(model);
        });
    }

    public void indexGraphResource(Model model) {
        // list resources with type Class, Property, DatatypeProperty, NodeShape
        var resources = model.listSubjectsWithProperty(RDF.type, OWL.Class)
                .andThen(model.listSubjectsWithProperty(RDF.type, OWL.ObjectProperty))
                .andThen(model.listSubjectsWithProperty(RDF.type, OWL.DatatypeProperty))
                .andThen(model.listSubjectsWithProperty(RDF.type, SH.NodeShape))
                .filterDrop(RDFNode::isAnon);
        var list = resources.mapWith(resource -> ResourceMapper.mapToIndexResource(model, resource.getURI())).toList();
        // we should bulk insert resources for each model separately if there are a lot of resources otherwise might cause memory issues
        if (!list.isEmpty()) {
            client.bulkInsert(OPEN_SEARCH_INDEX_RESOURCE, list);
        }
    }

    public void initExternalResourceIndex() {
        NamespaceService.DEFAULT_NAMESPACES.forEach((prefix, graphURI) -> {
            if (!(graphURI.endsWith("#") || graphURI.endsWith("/"))) {
                graphURI = graphURI + Constants.RESOURCE_SEPARATOR;
            }
            LOG.info("Indexing external namespace {}", graphURI);
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
                    LOG.info("Could not determine required properties for resource {}", resource.getURI());
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
                        LOG.warn("Cannot determine type for resource {}, types: {}", resource,
                                String.join(", ", typeProperties
                                        .mapWith(t -> t.getObject().toString())
                                        .toList())
                        );
                        return;
                    }
                }
                list.add(indexResource);
            });
            LOG.info("Indexing {} items to index {},", list.size(), OPEN_SEARCH_INDEX_EXTERNAL);
            client.bulkInsert(OPEN_SEARCH_INDEX_EXTERNAL, list);
        });
    }

    /**
     * A new model to index
     *
     * @param model Model to index
     */
    public void createModelToIndex(IndexModel model) {
        LOG.debug("Indexing: {}", model.getId());
        client.putToIndex(OPEN_SEARCH_INDEX_MODEL, model);
    }

    /**
     * Update existing model in index
     *
     * @param model Model to index
     */
    public void updateModelToIndex(IndexModel model) {
        client.updateToIndex(OPEN_SEARCH_INDEX_MODEL, model);
    }

    public void deleteModelFromIndex(String graph) {
        client.removeFromIndex(OPEN_SEARCH_INDEX_MODEL, graph);
    }

    /**
     * A new class to index
     *
     * @param indexResource Class to index
     */
    public void createResourceToIndex(IndexResource indexResource) {
        LOG.info("Indexing: {}", indexResource.getId());
        client.putToIndex(OPEN_SEARCH_INDEX_RESOURCE, indexResource);
    }

    /**
     * Update existing class in index
     *
     * @param indexResource Class to index
     */
    public void updateResourceToIndex(IndexResource indexResource) {
        LOG.info("Updating index for: {}", indexResource.getId());
        client.updateToIndex(OPEN_SEARCH_INDEX_RESOURCE, indexResource);
    }

    public void deleteResourceFromIndex(String id){
        LOG.info("Removing index for: {}", id);
        client.removeFromIndex(OPEN_SEARCH_INDEX_RESOURCE, id);
    }

    /**
     * Removes all resources that are based a certain data model
     *
     * @param modelUri Model URI
     * @param version  version number, will remove draft version resources if null or empty
     */
    public void removeResourceIndexesByDataModel(String modelUri, String version) {
        LOG.info("Removing resource indexes from model: {}, version: {}", modelUri, version);
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
                .toQuery();

        client.removeFromIndexWithQuery(OPEN_SEARCH_INDEX_RESOURCE, finalQuery);
    }

    public <T extends IndexBase> void bulkInsert(String index, List<T> documents) {
        client.bulkInsert(index, documents);
    }

    public void reindex(String index){
        check(authorizationManager.hasRightToDropDatabase());

        var mappings = new HashMap<String, TypeMapping>();

        if(index == null){
            reindexAll();
            auditService.log(AuditService.ActionType.UPDATE, "all_indexes", userProvider.getUser());
            return;
        }
        switch (index){
            case OPEN_SEARCH_INDEX_MODEL: {
                mappings.put(OPEN_SEARCH_INDEX_MODEL, getModelMappings());
                super.initIndexes(this::initModelIndex, mappings, true);
                break;
            }
            case OPEN_SEARCH_INDEX_RESOURCE: {
                mappings.put(OPEN_SEARCH_INDEX_RESOURCE, getModelMappings());
                super.initIndexes(this::initResourceIndex, mappings, true);
                break;
            } case OPEN_SEARCH_INDEX_EXTERNAL: {
                mappings.put(OPEN_SEARCH_INDEX_EXTERNAL, getExternalResourceMappings());
                super.initIndexes(this::initExternalResourceIndex, mappings, true);
                break;
            }
            default: {
                throw new IllegalArgumentException("Given value not allowed");
            }
        }
        auditService.log(AuditService.ActionType.UPDATE, index, userProvider.getUser());
    }

    private void reindexAll() {
        InitIndexesFunction fn = this::initDataModelIndexes;

        var indexConfig = Map.of(
                OPEN_SEARCH_INDEX_MODEL, getModelMappings(),
                OPEN_SEARCH_INDEX_RESOURCE, getResourceMappings(),
                OPEN_SEARCH_INDEX_EXTERNAL, getExternalResourceMappings()
        );
        super.initIndexes(fn, indexConfig, true);
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

    private List<Map<String, DynamicTemplate>> getModelDynamicTemplates() {
        return List.of(
                getDynamicTemplateWithSortKey("label", "label.*"),
                getDynamicTemplate("comment", "comment.*"),
                getDynamicTemplate("documentation", "documentation.*")
        );
    }

    private List<Map<String, DynamicTemplate>> getClassDynamicTemplates() {
        return List.of(
                getDynamicTemplateWithSortKey("label", "label.*"),
                getDynamicTemplate("note", "note.*")
        );
    }

    private List<Map<String, DynamicTemplate>> geExternalResourcesDynamicTemplates() {
        return List.of(
                getDynamicTemplateWithSortKey("label", "label.*")
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

}
