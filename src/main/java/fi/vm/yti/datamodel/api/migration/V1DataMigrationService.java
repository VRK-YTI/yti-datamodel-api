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
import org.apache.jena.arq.querybuilder.UpdateBuilder;
import org.apache.jena.arq.querybuilder.WhereBuilder;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.rdf.model.*;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFParser;
import org.apache.jena.vocabulary.DCTerms;
import org.apache.jena.vocabulary.OWL;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.topbraid.shacl.vocabulary.SH;

import java.net.URISyntaxException;
import java.util.HashSet;
import java.util.Set;

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

    private final Set<String> renamedResources = new HashSet<>();

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

    public void initRenamedResources() {
        renamedResources.clear();
    }

    public void renameResources() {

        for (String res : renamedResources) {
            LOG.info("Renaming references {}", res);
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
                    bind (iri(str(?o) + "_") as ?newURI)
                  }
                }""", res);
            coreRepository.queryUpdate(query);
        }
    }

    public void migrateLibrary(String prefix, Model oldData) throws URISyntaxException {

        var modelURI = DataModelURI.createModelURI(prefix).getModelURI();
        var oldModelURI = OLD_NAMESPACE + prefix;

        if (coreRepository.graphExists(modelURI)) {
            throw new RuntimeException("Already exists");
        }
        var groupModel = coreRepository.getServiceCategories();
        var dataModel = V1DataMapper.getLibraryMetadata(oldModelURI, oldData, groupModel, defaultOrganization);

        Resource modelResource = oldData.getResource(oldModelURI);

        if (!MapperUtils.hasType(modelResource,
                ResourceFactory.createProperty("http://purl.org/ws-mmi-dc/terms/MetadataVocabulary"))) {
            throw new RuntimeException("Not a library");
        }

        // create data model
        var dataModelURI = dataModelService.create(dataModel, ModelType.LIBRARY);

        // add references with separate query, because they might not exist yet (causes validation error)
        updateRequires(dataModelURI.toString(), modelResource);

        // update created and modified info from the original resource. Remove creator and modifier properties,
        // because they are not defined in the older application version
        handleModifiedInfo(modelURI, modelResource);

        LOG.info("Created datamodel {}", dataModelURI);

        // create classes
        handleAddResources(prefix, oldData, oldModelURI, ResourceType.CLASS);

        // create attributes and associations
        handleAddResources(prefix, oldData, oldModelURI, ResourceType.ATTRIBUTE);
        handleAddResources(prefix, oldData, oldModelURI, ResourceType.ASSOCIATION);

        // add class restrictions
        handleRestrictions(oldData, oldModelURI, modelURI);

        LOG.info("Migrated model {}", modelURI);
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

                // replace "yläluokka" associations with subClassOf reference
                var label = MapperUtils.localizedPropertyToMap(restrictionRes, SH.name);
                if ("yläluokka".equalsIgnoreCase(label.get("fi"))) {
                    var subClassTarget = MapperUtils.propertyToString(restrictionRes, SH.node);
                    if (subClassTarget != null) {
                        classResource.addProperty(RDFS.subClassOf, ResourceFactory.createResource(subClassTarget
                                .replace(OLD_NAMESPACE, ModelConstants.SUOMI_FI_NAMESPACE)
                                .replace("#", ModelConstants.RESOURCE_SEPARATOR)));
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
                    if (origResource.hasProperty(RDFS.range)) {
                        range = MapperUtils.propertyToString(origResource, RDFS.range);
                    } else if (restrictionRes.hasProperty(RDFS.range)) {
                        range = MapperUtils.propertyToString(restrictionRes, RDFS.range);
                    } else if (restrictionRes.hasProperty(SH.node)) {
                        range = MapperUtils.propertyToString(restrictionRes, SH.node);
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

            var newResourceURI = DataModelURI.createResourceURI(prefix, resource.getLocalName());

            try {
                var exists = coreRepository.resourceExistsInGraph(newModelURI.getGraphURI(), newResourceURI.getResourceURI(), false);

                if (type.equals(ResourceType.CLASS)) {
                    var dto = V1DataMapper.mapLibraryClass(resource);
                    handleExists(prefix, resource, exists, dto);
                    var classURI = classService.create(prefix, dto, false);
                    LOG.info("Created class {}", classURI);
                } else {
                    var dto = V1DataMapper.mapLibraryResource(resource);
                    handleExists(prefix, resource, exists, dto);
                    var resourceURI = resourceService.create(prefix, dto, type, false);
                    LOG.info("Created resource {}", resourceURI);
                }
                handleModifiedInfo(newModelURI.getModelURI(), resource);
            } catch (Exception e) {
                LOG.error("Error creating class {} to model {}: {}", resource.getURI(), modelURI, e.getMessage());
            }
        });
    }

    private void handleExists(String prefix, Resource resource, boolean exists, BaseDTO dto) {
        if (exists) {
            var newIdentifier = resource.getLocalName() + "_";
            dto.setIdentifier(newIdentifier);
            var renamedURI = DataModelURI.createResourceURI(prefix, resource.getLocalName()).getResourceURI();
            var renamedVersionURI = DataModelURI.createResourceURI(prefix, resource.getLocalName(), "1.0.0").getResourceVersionURI();
            LOG.info("Renamed resource {}, new URI {}", resource.getURI(), renamedURI);
            renamedResources.add(renamedURI);
            renamedResources.add(renamedVersionURI);
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
        var graph = NodeFactory.createURI(modelURI);

        var builder = new UpdateBuilder();
        builder.addPrefixes(ModelConstants.PREFIXES);

        var created = MapperUtils.getLiteral(resource, DCTerms.created, String.class);
        var modified = MapperUtils.getLiteral(resource, DCTerms.modified, String.class);

        builder.addDelete(graph, "?s", DCTerms.created, "?created")
                .addDelete(graph, "?s", DCTerms.modified, "?modified")
                .addDelete(graph, "?s", SuomiMeta.creator, "?creator")
                .addDelete(graph, "?s", SuomiMeta.modifier, "?modifier")
                .addInsert(graph, "?s", DCTerms.created, created)
                .addInsert(graph, "?s", DCTerms.modified, modified)
                .addGraph(graph, new WhereBuilder()
                        .addWhere("?s", DCTerms.created, "?created")
                        .addWhere("?s", DCTerms.modified, "?modified")
                        .addWhere("?s", SuomiMeta.creator, "?creator")
                        .addWhere("?s", SuomiMeta.modifier, "?modifier"));
        coreRepository.queryUpdate(builder.buildRequest());
    }

    public void migratePositions(String prefix, Model oldVisualization) {
        var modelURI = OLD_NAMESPACE + prefix;
        var positions = V1DataMapper.mapPositions(modelURI, oldVisualization);
        visualizationService.savePositionData(prefix, positions);
    }

    public void createVersions(String prefix) {
        try {
            dataModelService.createRelease(prefix, "1.0.0", Status.VALID);
            var uri = DataModelURI.createModelURI(prefix).getGraphURI();
            updateReferences(uri);
        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
        }
    }

    private void updateReferences(String graph) {
        var query = String.format("""
            delete {
              graph ?g {
                ?s ?p ?o
              }
            }
            insert {
              graph ?g {
                ?s ?p ?uri
              }
            }
            where {
              graph ?g {
                ?s ?p ?o
                filter(strstarts(str(?o), "%s"))
                bind(iri(
                    replace(str(?o), "%s", "%s1.0.0/")
                ) as ?uri)
              }
              filter(!strstarts(str(?g), "%s"))
            }""", graph, graph, graph, graph);

        coreRepository.queryUpdate(query);
    }
}
