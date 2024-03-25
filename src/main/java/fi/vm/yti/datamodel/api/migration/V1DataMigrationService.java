package fi.vm.yti.datamodel.api.migration;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import fi.vm.yti.datamodel.api.v2.dto.*;
import fi.vm.yti.datamodel.api.v2.mapper.ClassMapper;
import fi.vm.yti.datamodel.api.v2.mapper.MapperUtils;
import fi.vm.yti.datamodel.api.v2.properties.SuomiMeta;
import fi.vm.yti.datamodel.api.v2.repository.CoreRepository;
import fi.vm.yti.datamodel.api.v2.service.ClassService;
import fi.vm.yti.datamodel.api.v2.service.DataModelService;
import fi.vm.yti.datamodel.api.v2.service.ResourceService;
import fi.vm.yti.datamodel.api.v2.service.VisualizationService;
import fi.vm.yti.datamodel.api.v2.utils.DataModelURI;
import fi.vm.yti.datamodel.api.v2.utils.DataModelUtils;
import org.apache.jena.arq.querybuilder.UpdateBuilder;
import org.apache.jena.arq.querybuilder.WhereBuilder;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.rdf.model.*;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFParser;
import org.apache.jena.shared.PrefixMapping;
import org.apache.jena.vocabulary.*;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.topbraid.shacl.vocabulary.SH;

import java.net.URISyntaxException;
import java.util.*;

@Service
public class V1DataMigrationService {

    private static final Logger LOG = LoggerFactory.getLogger(V1DataMigrationService.class);
    public static final String URI_SUOMI_FI = "uri.suomi.fi";
    public static final String OLD_NAMESPACE = "http://uri.suomi.fi/datamodel/ns/";

    private final DataModelService dataModelService;
    private final ClassService classService;
    private final ResourceService resourceService;
    private final CoreRepository coreRepository;
    private final VisualizationService visualizationService;

    private final LoadingCache<String, Model> modelCache = CacheBuilder.newBuilder().build(getLoader());

    @Value("${datamodel.v1.migration.defaultOrganization:}")
    private String defaultOrganization;

    private final Map<String, String> renamedResourcesMap = new HashMap<>();

    private final Map<String, String> propertyShapeIdMap = new HashMap<>();

    @Value("${datamodel.v1.migration.url:https://tietomallit.dev.yti.cloud.dvv.fi}")
    String serviceURL;

    private static final Property API_PATH =
            ResourceFactory.createProperty("http://www.w3.org/2011/http#absolutePath");

    public V1DataMigrationService(DataModelService dataModelService,
                                  ClassService classService,
                                  ResourceService resourceService,
                                  CoreRepository coreRepository,
                                  VisualizationService visualizationService) {
        this.dataModelService = dataModelService;
        this.classService = classService;
        this.resourceService = resourceService;
        this.coreRepository = coreRepository;
        this.visualizationService = visualizationService;
    }

    private void initMigration() {
        renamedResourcesMap.clear();
        propertyShapeIdMap.clear();
        V1DataMapper.clearErrors();
    }

    public void renameResources() {

        for (Map.Entry<String, String> entry : renamedResourcesMap.entrySet()) {
            var newURI = entry.getValue();
            var old = entry.getKey();
            LOG.info("Renaming references {} to {}", old, newURI);
            var query = String.format("""
                    delete { graph ?g {
                        ?s ?p ?o
                      }
                    }
                    insert { graph ?g {
                        ?s ?p ?newURI
                      }
                    }
                    where { graph ?g {
                        ?s ?p ?o .
                        filter(str(?o) = "%s")
                        bind (iri("%s") as ?newURI)
                      }
                    }""", old, newURI);
            coreRepository.queryUpdate(query);
        }
    }

    @Async
    public void startMigrationAsync(String prefix) {
        initMigration();
        var prefixes = prefix.split(",");

        /* Use static file
        var oldData = ModelFactory.createDefaultModel();
        var stream = getClass().getResourceAsStream("/isa2core-org.ttl");
        RDFDataMgr.read(oldData, stream, RDFLanguages.TURTLE);
        migrationService.migrateDatamodel(prefix, oldData);
        */

        for (var p : prefixes) {
            var oldData = ModelFactory.createDefaultModel();
            var oldVisualization = ModelFactory.createDefaultModel();
            var modelURI = OLD_NAMESPACE + p;
            LOG.info("Fetching model {}", modelURI);
            try {
                RDFParser.create()
                        .source(serviceURL + "/datamodel-api/api/v1/exportModel?graph=" + DataModelUtils.encode(modelURI))
                        .lang(Lang.JSONLD)
                        .acceptHeader("application/ld+json")
                        .parse(oldData);

                migrateDatamodel(p, oldData);
            } catch (Exception e) {
                LOG.error("Error migrating model " + p + ": " + e.getMessage(), e);
            }
            try {
                LOG.info("Fetching visualization for model {}", modelURI);
                RDFParser.create()
                        .source(serviceURL + "/datamodel-api/api/v1/modelPositions?model=" + DataModelUtils.encode(modelURI))
                        .lang(Lang.JSONLD)
                        .acceptHeader("application/ld+json")
                        .parse(oldVisualization);

                migratePositions(p, oldVisualization, oldData.getGraph().getPrefixMapping());
            } catch (Exception e) {
                LOG.error("Error migrating visualization " + p + ": " + e.getMessage(), e);
            }
        }

        renameResources();

        var errors = V1DataMapper.getErrors();
        LOG.info("Done, errors: {}", errors);
    }

    public void migrateDatamodel(String prefix, Model oldData) throws URISyntaxException {
        var modelURI = DataModelURI.createModelURI(prefix).getModelURI();
        var oldModelURI = OLD_NAMESPACE + prefix;

        if (coreRepository.graphExists(modelURI)) {
            throw new RuntimeException("Already exists");
        }
        var groupModel = coreRepository.getServiceCategories();
        var dataModel = V1DataMapper.getLibraryMetadata(oldModelURI, oldData, groupModel, defaultOrganization);

        addModelSpecificInfo(prefix, dataModel);

        var modelResource = oldData.getResource(oldModelURI);
        var languages = dataModel.getLanguages();

        // filter out localized information not included to languages added to the model
        var invalidStatements = oldData.listStatements().toList().stream()
                .filter(s -> s.getObject().isLiteral())
                .filter(s -> !"".equals(s.getLanguage()) && !languages.contains(s.getLanguage()))
                .toList();

        oldData.remove(invalidStatements);

        var modelType = MapperUtils.hasType(modelResource,
                ResourceFactory.createProperty("http://purl.org/ws-mmi-dc/terms/MetadataVocabulary"))
                ? ModelType.LIBRARY
                : ModelType.PROFILE;

        // create data model
        var dataModelURI = dataModelService.create(dataModel, modelType);

        // add references with separate query, because they might not exist yet (causes validation error)
        updateRequires(dataModelURI.toString(), modelResource);

        addCustomProperty(modelURI, modelResource, VOID.rootResource);

        // update created and modified info from the original resource. Remove creator and modifier properties,
        // because they are not defined in the older application version
        handleModifiedInfo(modelURI, modelResource);

        LOG.info("Created datamodel {}", dataModelURI);

        if (modelType.equals(ModelType.LIBRARY)) {
            // create library classes
            handleAddResources(prefix, oldData, oldModelURI, ResourceType.CLASS);

            // create library attributes and associations
            handleAddResources(prefix, oldData, oldModelURI, ResourceType.ATTRIBUTE);
            handleAddResources(prefix, oldData, oldModelURI, ResourceType.ASSOCIATION);

            // add class restrictions
            handleRestrictions(oldData, oldModelURI, modelURI);
        } else {
            // create node shapes
            handleAddNodeShapes(prefix, oldData);
            // create property shapes
            handleAddPropertyShapes(prefix, oldData);

            // attach property shapes to node shapes
            handleAddPropertiesToNodeShapes(prefix, oldData);
        }
        LOG.info("Migrated model {}", modelURI);
    }

    private void handleAddNodeShapes(String prefix, Model oldData) {
        var nodeShapes = V1DataMapper.findResourcesByType(oldData, SH.NodeShape);
        nodeShapes.addAll(V1DataMapper.findResourcesByType(oldData, RDFS.Class));

        var newModelURI = DataModelURI.createModelURI(prefix).getGraphURI();

        for (var nodeShape : nodeShapes) {
            var dto = V1DataMapper.mapProfileClass(nodeShape);
            try {
                classService.create(prefix, dto, true);
                handleModifiedInfo(newModelURI, nodeShape);
                addCustomProperty(newModelURI, nodeShape, API_PATH);
            } catch (Exception e) {
                LOG.warn("MIGRATION ERROR: Error creating node shape {} to model {}, cause: {}", dto.getIdentifier(), prefix, e.getMessage());
                V1DataMapper.addError(prefix, String.format("Error creating node shape %s, cause: %s", dto.getIdentifier(), e.getMessage()));
            }
        }
    }

    private void handleAddPropertyShapes(String prefix, Model oldData) {
        var propertyShapes = V1DataMapper.findResourcesByType(oldData, SH.PropertyShape);
        var newModelURI = DataModelURI.createModelURI(prefix).getGraphURI();

        for (var propertyShape : propertyShapes) {
            var classResource = oldData.listSubjectsWithProperty(SH.property, propertyShape).nextResource();

            var ps = V1DataMapper.mapProfileResource(oldData, propertyShape, prefix, classResource.getURI());
            if (ps == null) {
                continue;
            }
            var resourceType = ps instanceof AttributeRestriction
                    ? ResourceType.ATTRIBUTE
                    : ResourceType.ASSOCIATION;

            int n = 0;
            var originalId = ps.getIdentifier();
            while (resourceService.exists(prefix, ps.getIdentifier())) {
                ps.setIdentifier(originalId + "-" + ++n);
                propertyShapeIdMap.put(propertyShape.getURI(), ps.getIdentifier());
            }

            try {
                resourceService.create(prefix, ps, resourceType, true);
                handleModifiedInfo(newModelURI, propertyShape);
            } catch (Exception e) {
                LOG.warn("MIGRATION ERROR: Error creating property shape {} to model {}, cause: {}",
                        ps.getIdentifier(), prefix, e.getMessage());
                V1DataMapper.addError(prefix, String.format("Error creating property shape %s, cause: %s", ps.getIdentifier(), e.getMessage()));
            }
        }
    }

    private void handleAddPropertiesToNodeShapes(String prefix, Model oldData) {
        var nodeShapes = V1DataMapper.findResourcesByType(oldData, SH.NodeShape);
        nodeShapes.addAll(V1DataMapper.findResourcesByType(oldData, RDFS.Class));

        var dataModelURI = DataModelURI.createModelURI(prefix).getGraphURI();
        var newModel = coreRepository.fetch(dataModelURI);
        for (var nodeShape : nodeShapes) {
            var nodeShapeURI = dataModelURI + NodeFactory.createURI(nodeShape.getURI()).getLocalName();
            var nodeShapeResource = newModel.getResource(nodeShapeURI);
            var properties = nodeShape.listProperties(SH.property)
                    .mapWith(p -> {
                        var property = p.getObject().toString();

                        // if property shape is renamed with suffix, e.g. title-1
                        if (propertyShapeIdMap.containsKey(property)) {
                            return DataModelURI.createResourceURI(prefix, propertyShapeIdMap.get(property)).getResourceURI();
                        }
                        return V1DataMapper.getUriForOldResource(prefix, oldData.getResource(p.getObject().toString()));
                    })
                    .filterKeep(Objects::nonNull);

            properties.forEach(p -> nodeShapeResource.addProperty(SH.property, ResourceFactory.createResource(p)));
        }
        coreRepository.put(dataModelURI, newModel);
    }

    private void handleRestrictions(Model oldData, String oldModelURI, String newModelURI) {
        var newModel = coreRepository.fetch(newModelURI);
        var classes = V1DataMapper.findResourcesByType(oldData, RDFS.Class);
        for (var cls : classes) {

            if (!includesToModel(oldModelURI, cls)) {
                LOG.info("Not included: {}", cls.getURI());
                continue;
            }

            var classResource = newModel.getResource(cls.getURI()
                    .replace(OLD_NAMESPACE, ModelConstants.SUOMI_FI_NAMESPACE)
                    .replace("#", ModelConstants.RESOURCE_SEPARATOR));
            var properties = MapperUtils.arrayPropertyToList(cls, SH.property);

            for (var property : properties) {
                LOG.info("Handling restriction {}", property);
                var restrictionRes = oldData.getResource(property);
                var path = MapperUtils.propertyToString(restrictionRes, SH.path);

                if (path == null) {
                    LOG.warn("No path for restriction {}", property);
                    continue;
                }

                RDFNode type = getRestrictionType(oldData, restrictionRes, path);

                if (type == null) {
                    LOG.warn("Cannot determine type for {} in class {}", property, classResource.getURI());
                    continue;
                }

                // replace "yläluokka" and "perii" associations with subClassOf reference
                var label = MapperUtils.localizedPropertyToMap(restrictionRes, SH.name);
                if ("yläluokka".equalsIgnoreCase(label.get("fi")) || "perii".equalsIgnoreCase(label.get("fi"))) {
                    var subClassTarget = MapperUtils.propertyToString(restrictionRes, SH.node);
                    if (subClassTarget != null) {
                        classResource.addProperty(RDFS.subClassOf, ResourceFactory.createResource(subClassTarget
                                .replace(OLD_NAMESPACE, ModelConstants.SUOMI_FI_NAMESPACE)
                                .replace("#", ModelConstants.RESOURCE_SEPARATOR)));
                        newModel.remove(classResource, RDFS.subClassOf, OWL.Thing);
                        continue;
                    }
                }

                Resource targetRes;
                var ns = NodeFactory.createURI(path).getNameSpace().replace("#", "");

                if (ns.contains(URI_SUOMI_FI)) {
                    Resource origResource;
                    if (ns.equals(oldModelURI)) {
                        // current model
                        origResource = oldData.getResource(path);
                    } else {
                        // reference to other model in Interoperability platform
                        var modelFromCache = modelCache.getUnchecked(ns);
                        origResource = modelFromCache.getResource(path);
                    }

                    var tempModel = ModelFactory.createDefaultModel();
                    var tempResource = tempModel.createResource(path
                            .replace(OLD_NAMESPACE, ModelConstants.SUOMI_FI_NAMESPACE)
                            .replace("#", ModelConstants.RESOURCE_SEPARATOR));

                    String range;
                    if (restrictionRes.hasProperty(RDFS.range)) {
                        range = MapperUtils.propertyToString(restrictionRes, RDFS.range);
                    } else if (restrictionRes.hasProperty(SH.node)) {
                        range = MapperUtils.propertyToString(restrictionRes, SH.node);
                    } else if (origResource.hasProperty(RDFS.range)) {
                        range = MapperUtils.propertyToString(origResource, RDFS.range);
                    } else {
                        LOG.warn("No range property found for class restriction {}, class {}, path {}",
                                restrictionRes.getURI(), classResource.getURI(), path);
                        continue;
                    }

                    if (range != null && range.contains(URI_SUOMI_FI)) {
                        range = range
                                .replace(OLD_NAMESPACE, ModelConstants.SUOMI_FI_NAMESPACE)
                                .replace("#", ModelConstants.RESOURCE_SEPARATOR);
                    }
                    tempResource.addProperty(RDFS.range, ResourceFactory.createResource(range));
                    tempResource.addProperty(RDF.type, type);
                    targetRes = tempResource;

                } else {
                    // reference to external resource, no need to specify namespaces
                    targetRes = resourceService.findResources(Set.of(path), Set.of()).getResource(path);
                }

                LOG.info("add restriction {} to class {}", targetRes.getURI(), classResource.getURI());
                ClassMapper.mapClassRestrictionProperty(newModel, classResource, targetRes);
            }
        }
        LOG.info("Save restrictions to model");
        coreRepository.put(newModelURI, newModel);
    }

    private RDFNode getRestrictionType(Model oldData, Resource restrictionRes, String path) {
        RDFNode type;
        if (restrictionRes.hasProperty(DCTerms.type)) {
            return restrictionRes.getProperty(DCTerms.type).getObject();
        } else {
            var typeFromPath = oldData.getResource(path).getProperty(RDF.type);

            if (typeFromPath == null) {
                var refModel = modelCache.getUnchecked(NodeFactory.createURI(path).getNameSpace());
                var refModelResource = refModel.getResource(path);

                if (refModelResource.hasProperty(RDF.type)) {
                    type = refModelResource.getProperty(RDF.type).getObject();
                } else {
                    return null;
                }
            } else {
                type = typeFromPath.getObject();
            }
        }
        return type;
    }

    private void handleAddResources(String prefix, Model oldData, String modelURI, ResourceType type) {
        Resource property;

        if (type.equals(ResourceType.ASSOCIATION)) {
            property = OWL.ObjectProperty;
        } else if (type.equals(ResourceType.ATTRIBUTE)) {
            property = OWL.DatatypeProperty;
        } else {
            property = RDFS.Class;
        }

        var newModelURI = DataModelURI.createModelURI(prefix);

        V1DataMapper.findResourcesByType(oldData, property).forEach(resource -> {
            if (!includesToModel(modelURI, resource)) {
                LOG.info("Skip external resource {} added directly to the model", resource.getURI());
                return;
            }

            try {
                if (type.equals(ResourceType.CLASS)) {
                    var dto = V1DataMapper.mapLibraryClass(resource);
                    handleExists(prefix, resource, dto);
                    classService.create(prefix, dto, false);
                } else {
                    var dto = V1DataMapper.mapLibraryResource(resource);
                    handleExists(prefix, resource, dto);
                    resourceService.create(prefix, dto, type, false);
                }
                handleModifiedInfo(newModelURI.getModelURI(), resource);
            } catch (Exception e) {
                LOG.error("Error creating class {} to model {}: {}", resource.getURI(), modelURI, e.getMessage());
                V1DataMapper.addError(prefix, String.format("Error creating class %s, cause: %s", resource.getURI(), e.getMessage()));
            }
        });
    }

    private void handleExists(String prefix, Resource resource, BaseDTO dto) {
        var graphURI = DataModelURI.createModelURI(prefix).getGraphURI();
        var newResourceURI = DataModelURI.createResourceURI(prefix, dto.getIdentifier()).getResourceURI();

        var hasRenamed = false;
        while (coreRepository.resourceExistsInGraph(graphURI, newResourceURI, false)) {
            var newIdentifier = dto.getIdentifier() + "_";
            dto.setIdentifier(newIdentifier);
            newResourceURI = DataModelURI.createResourceURI(prefix, newIdentifier).getResourceURI();
            hasRenamed = true;
        }

        if (hasRenamed) {
            var oldResourceURI = DataModelURI.createResourceURI(prefix, resource.getLocalName()).getResourceURI();
            LOG.info("Renamed resource {}, new URI {}", resource.getURI(), newResourceURI);
            renamedResourcesMap.put(oldResourceURI, newResourceURI);
        }
    }

    private CacheLoader<String, Model> getLoader() {

        return new CacheLoader<>() {
            @Override
            public @NotNull Model load(@NotNull String ns) {
                var temp = ModelFactory.createDefaultModel();
                LOG.info("Fetch reference datamodel and add to cache {}", ns);
                try {
                    RDFParser.create()
                            .source(ns)
                            .lang(Lang.JSONLD)
                            .acceptHeader("application/ld+json")
                            .parse(temp);
                } catch (Exception e) {
                    LOG.error("Error fetching model {}", ns);
                }
                return temp;
            }
        };
    }

    private static boolean includesToModel(String modelURI, Resource resource) {
        return modelURI.equals(MapperUtils.propertyToString(resource, RDFS.isDefinedBy));
    }

    private void updateRequires(String newModelURI, Resource modelResource) {
        var builder = new UpdateBuilder();
        var requires = MapperUtils.arrayPropertyToSet(modelResource, DCTerms.requires);

        var internalRequires = requires.stream()
                .filter(ns -> ns.contains(URI_SUOMI_FI))
                .map(ns -> {
                    ns = ns.replace(OLD_NAMESPACE, ModelConstants.SUOMI_FI_NAMESPACE);
                    if (!ns.endsWith("#") && !ns.endsWith("/")) {
                        ns = ns + "/";
                    }
                    return ns;
                })
                .toList();
        var newURI = NodeFactory.createURI(newModelURI);
        if (internalRequires.isEmpty()) {
            return;
        }
        internalRequires.forEach(ns -> builder.addInsert(newURI, newURI, OWL.imports, NodeFactory.createURI(ns)));
        coreRepository.queryUpdate(builder.buildRequest());
    }

    public void handleModifiedInfo(String modelURI, Resource resource) {
        var hasModifiedInfo = resource.hasProperty(DCTerms.created) && resource.hasProperty(DCTerms.modified);

        var graph = NodeFactory.createURI(modelURI);

        var builder = new UpdateBuilder();
        builder.addPrefixes(ModelConstants.PREFIXES);

        var created = MapperUtils.getLiteral(resource, DCTerms.created, String.class);
        var modified = MapperUtils.getLiteral(resource, DCTerms.modified, String.class);

        builder.addDelete(graph, "?s", SuomiMeta.creator, "?creator")
                .addDelete(graph, "?s", SuomiMeta.modifier, "?modifier");

        if (hasModifiedInfo) {
            builder.addDelete(graph, "?s", DCTerms.created, "?created")
                    .addDelete(graph, "?s", DCTerms.modified, "?modified")
                    .addInsert(graph, "?s", DCTerms.created, created)
                    .addInsert(graph, "?s", DCTerms.modified, modified);
        }

        var whereBuilder = new WhereBuilder()
                .addWhere("?s", SuomiMeta.creator, "?creator")
                .addWhere("?s", SuomiMeta.modifier, "?modifier");

        if (hasModifiedInfo) {
            whereBuilder
                    .addWhere("?s", DCTerms.created, "?created")
                    .addWhere("?s", DCTerms.modified, "?modified");

        }

        builder.addGraph(graph, whereBuilder);

        coreRepository.queryUpdate(builder.buildRequest());
    }

    private void addCustomProperty(String graphURI, Resource resource, Property property) {
        if (!resource.hasProperty(property)) {
            return;
        }
        var object = resource.getProperty(property).getObject();

        Object value;
        if (object.isLiteral()) {
            value = object.toString();
        } else {
            value = NodeFactory.createURI(V1DataMapper.fixNamespace(object.toString()));
        }

        var resourceURI = V1DataMapper.fixNamespace(resource.getURI());
        if (MapperUtils.hasType(resource, OWL.Ontology)) {
            resourceURI += "/";
        }
        var builder = new UpdateBuilder();
        builder.addInsert(NodeFactory.createURI(graphURI), NodeFactory.createURI(resourceURI), property, value);

        coreRepository.queryUpdate(builder.buildRequest());
    }

    public void migratePositions(String prefix, Model oldVisualization, PrefixMapping prefixMapping) {
        var modelURI = OLD_NAMESPACE + prefix;
        var positions = V1DataMapper.mapPositions(modelURI, oldVisualization, prefixMapping);
        visualizationService.savePositionData(prefix, positions);
    }

    public void createVersions(String prefix) {
        try {
            var version = "1.0.0";
            dataModelService.createRelease(prefix, version, Status.VALID);
            var uri = DataModelURI.createModelURI(prefix).getGraphURI();
            dataModelService.updateDraftReferences(uri, version);
        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
        }
    }

    private void addModelSpecificInfo(String prefix, DataModelDTO dataModel) {
        var rdfs = new ExternalNamespaceDTO();
        rdfs.setName(Map.of("en", "rdfs"));
        rdfs.setNamespace("http://www.w3.org/2000/01/rdf-schema#");
        rdfs.setPrefix("rdfs");

        if (prefix.equals("fi-dcatap")) {
            var spdx = new ExternalNamespaceDTO();
            spdx.setName(Map.of("en", "spdx"));
            spdx.setNamespace("http://spdx.org/rdf/terms#");
            spdx.setPrefix("spdx");

            dataModel.getExternalNamespaces().addAll(Set.of(rdfs, spdx));
        } else if (prefix.equals("fi-nsipap")) {
            var skos = new ExternalNamespaceDTO();
            skos.setName(Map.of("en", "skos"));
            skos.setNamespace("http://www.w3.org/2004/02/skos/core#");
            skos.setPrefix("skos");

            dataModel.getExternalNamespaces().add(skos);
        } else if (prefix.startsWith("efti")) {
            var unece = new ExternalNamespaceDTO();
            unece.setName(Map.of("en", "unece"));
            unece.setNamespace("https://vocabulary.uncefact.org/");
            unece.setPrefix("unece");

            dataModel.getExternalNamespaces().add(unece);
        } else if (prefix.startsWith("ttv")) {
            var org = new ExternalNamespaceDTO();
            org.setName(Map.of("en", "org"));
            org.setNamespace("http://www.w3.org/ns/org#");
            org.setPrefix("org");

            var foaf = new ExternalNamespaceDTO();
            foaf.setName(Map.of("en", "foaf"));
            foaf.setNamespace("http://xmlns.com/foaf/0.1/");
            foaf.setPrefix("foaf");

            dataModel.getExternalNamespaces().addAll(Set.of(foaf, org));
        } else if (prefix.equals("agent2")) {
            var semic = new ExternalNamespaceDTO();
            semic.setName(Map.of("en", "locn"));
            semic.setNamespace("https://semiceu.github.io/Core-Location-Vocabulary/releases/w3c/#");
            semic.setPrefix("locn");

            dataModel.getExternalNamespaces().add(semic);
        } else if (prefix.equals("emrex")) {
            var vcard = new ExternalNamespaceDTO();
            vcard.setName(Map.of("en", "org"));
            vcard.setNamespace("http://www.w3.org/2001/vcard-rdf/");
            vcard.setPrefix("vcard");

            dataModel.getExternalNamespaces().addAll(Set.of(rdfs, vcard));
        }
    }
}
