package fi.vm.yti.datamodel.api.migration;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import fi.vm.yti.datamodel.api.v2.dto.Iow;
import fi.vm.yti.datamodel.api.v2.dto.ModelConstants;
import fi.vm.yti.datamodel.api.v2.dto.ModelType;
import fi.vm.yti.datamodel.api.v2.dto.ResourceType;
import fi.vm.yti.datamodel.api.v2.mapper.ClassMapper;
import fi.vm.yti.datamodel.api.v2.mapper.MapperUtils;
import fi.vm.yti.datamodel.api.v2.repository.CoreRepository;
import fi.vm.yti.datamodel.api.v2.service.ClassService;
import fi.vm.yti.datamodel.api.v2.service.DataModelService;
import fi.vm.yti.datamodel.api.v2.service.ResourceService;
import org.apache.jena.arq.querybuilder.UpdateBuilder;
import org.apache.jena.arq.querybuilder.WhereBuilder;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFParser;
import org.apache.jena.util.ResourceUtils;
import org.apache.jena.vocabulary.DCTerms;
import org.apache.jena.vocabulary.OWL;
import org.apache.jena.vocabulary.RDFS;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.topbraid.shacl.vocabulary.SH;

import java.net.URISyntaxException;
import java.util.Set;

@Service
public class V1DataMigrationService {

    private static final Logger LOG = LoggerFactory.getLogger(V1DataMigrationService.class);
    public static final String URI_SUOMI_FI = "uri.suomi.fi";

    private final DataModelService dataModelService;
    private final ClassService classService;
    private final ResourceService resourceService;
    private final CoreRepository coreRepository;

    private final LoadingCache<String, Model> modelCache = CacheBuilder.newBuilder().build(getLoader());

    @Value("${datamodel.v1.migration.defaultOrganization:}")
    private String defaultOrganization;

    public V1DataMigrationService(DataModelService dataModelService,
                                  ClassService classService,
                                  ResourceService resourceService,
                                  CoreRepository coreRepository) {
        this.dataModelService = dataModelService;
        this.classService = classService;
        this.resourceService = resourceService;
        this.coreRepository = coreRepository;
    }

    public void migrateLibrary(String prefix, Model oldData) throws URISyntaxException {

        var modelURI = ModelConstants.SUOMI_FI_NAMESPACE + prefix;
        var groupModel = coreRepository.getServiceCategories();
        var dataModel = V1DataMapper.getLibraryMetadata(modelURI, oldData, groupModel, defaultOrganization);

        Resource modelResource = oldData.getResource(modelURI);

        if (!MapperUtils.hasType(modelResource,
                ResourceFactory.createProperty("http://purl.org/ws-mmi-dc/terms/MetadataVocabulary"))) {
            throw new RuntimeException("Not a library");
        }

        // create data model
        var dataModelURI = dataModelService.create(dataModel, ModelType.LIBRARY);

        updateRequires(modelURI, modelResource);
        handleModifiedInfo(modelURI, modelResource);

        LOG.info("Created datamodel {}", dataModelURI);

        // create classes
        handleAddResources(prefix, oldData, modelURI, ResourceType.CLASS);

        // create attributes and associations
        handleAddResources(prefix, oldData, modelURI, ResourceType.ATTRIBUTE);
        handleAddResources(prefix, oldData, modelURI, ResourceType.ASSOCIATION);

        // add class restrictions
        handleRestrictions(oldData, modelURI);
    }

    private void handleRestrictions(Model oldData, String modelURI) {
        var newModel = coreRepository.fetch(modelURI);
        V1DataMapper.findResourcesByType(oldData, RDFS.Class).forEach(cls -> {

            if (!includesToModel(modelURI, cls)) {
                return;
            }

            var classResource = newModel.getResource(cls.getURI().replace("#", ModelConstants.RESOURCE_SEPARATOR));
            MapperUtils.arrayPropertyToList(cls, SH.property).forEach(property -> {
                var restrictionRes = oldData.getResource(property);
                var path = MapperUtils.propertyToString(restrictionRes, SH.path);
                if (path == null) {
                    return;
                }

                Resource targetRes;
                var ns = NodeFactory.createURI(path).getNameSpace().replace("#", "");

                if (ns.contains(URI_SUOMI_FI)) {
                    Resource temp;
                    if (ns.equals(modelURI)) {
                        // current model
                        temp = oldData.getResource(path);
                    } else {
                        // reference to other model in Interoperability platform
                        temp = modelCache.getUnchecked(ns).getResource(path);
                        if (temp == null) {
                            LOG.info("Resource {} not found from cached models", path);
                            return;
                        }
                        var range = MapperUtils.propertyToString(temp, RDFS.range);

                        if (range != null && range.contains(URI_SUOMI_FI)) {
                            temp.removeAll(RDFS.range);
                            temp.addProperty(RDFS.range, ResourceFactory.createResource(range.replace("#", ModelConstants.RESOURCE_SEPARATOR)));
                        }
                    }
                    if (temp.getURI().contains("#")) {
                        targetRes = ResourceUtils.renameResource(temp, temp.getURI().replace("#", ModelConstants.RESOURCE_SEPARATOR));
                    } else {
                        targetRes = temp;
                    }
                } else {
                    // reference to external resource
                    targetRes = resourceService.findResources(Set.of(path)).getResource(path);
                }

                LOG.info("add restriction {} to class {}", targetRes.getURI(), classResource.getURI());
                ClassMapper.mapClassRestrictionProperty(newModel, classResource, targetRes);
            });
        });
        coreRepository.put(modelURI, newModel);
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

        V1DataMapper.findResourcesByType(oldData, property).forEach(resource -> {
            if (!includesToModel(modelURI, resource)) {
                LOG.info("Skip external resource {} added directly to the model", resource.getURI());
                return;
            }

            try {
                if (type.equals(ResourceType.CLASS)) {
                    var dto = V1DataMapper.mapLibraryClass(resource);
                    var classURI = classService.create(prefix, dto, false);
                    LOG.info("Created class {}", classURI);
                } else {
                    var dto = V1DataMapper.mapLibraryResource(resource);
                    var resourceURI = resourceService.create(prefix, dto, type, false);
                    LOG.info("Created resource {}", resourceURI);
                }
                handleModifiedInfo(modelURI, oldData.getResource(modelURI));
            } catch (Exception e) {
                LOG.error("Error creating class {} to model {}: {}", resource.getURI(), modelURI, e.getMessage());
            }
        });
    }

    private CacheLoader<String, Model> getLoader() {

        return new CacheLoader<>() {
            @Override
            public @NotNull Model load(@NotNull String ns) {
                try {
                    LOG.info("Try to fetch model {} from Fuseki", ns);
                    return coreRepository.fetch(ns);
                } catch (Exception e) {
                    LOG.info("Model not exists {}", ns);
                }
                var temp = ModelFactory.createDefaultModel();
                LOG.info("Fetch reference datamodel and add to cache {}", ns);
                RDFParser.create()
                        .source(ns)
                        .lang(Lang.JSONLD)
                        .acceptHeader("application/ld+json")
                        .parse(temp);
                return temp;
            }
        };
    }

    private static boolean includesToModel(String modelURI, Resource resource) {
        return modelURI.equals(MapperUtils.propertyToString(resource, RDFS.isDefinedBy));
    }

    private void updateRequires(String modelURI, Resource modelResource) {
        var builder = new UpdateBuilder();
        var g = NodeFactory.createURI(modelURI);
        MapperUtils.arrayPropertyToSet(modelResource, DCTerms.requires)
                .forEach(ns -> {
                    if (ns.contains(URI_SUOMI_FI)) {
                        builder.addInsert(g, g, DCTerms.requires, NodeFactory.createURI(ns));
                    }
                });
        coreRepository.queryUpdate(builder.buildRequest());
    }

    public void handleModifiedInfo(String modelURI, Resource resource) {
        var graph = NodeFactory.createURI(modelURI);

        var builder = new UpdateBuilder();
        builder.addPrefixes(ModelConstants.PREFIXES);

        var created = resource.getProperty(DCTerms.created).getLiteral().getString();
        var modified = resource.getProperty(DCTerms.modified).getLiteral().getString();

        builder.addDelete(graph, "?s", DCTerms.created, "?created")
                .addDelete(graph, "?s", DCTerms.modified, "?modified")
                .addDelete(graph, "?s", Iow.creator, "?creator")
                .addDelete(graph, "?s", Iow.modifier, "?modifier")
                .addInsert(graph, "?s", DCTerms.created, created)
                .addInsert(graph, "?s", DCTerms.modified, modified)
                .addGraph(graph, new WhereBuilder()
                        .addWhere("?s", DCTerms.created, "?created")
                        .addWhere("?s", DCTerms.modified, "?modified")
                        .addWhere("?s", Iow.creator, "?creator")
                        .addWhere("?s", Iow.modifier, "?modifier"));

        LOG.info("Add created {} and modified {} for resource {}", created, modified, resource.getURI());

        coreRepository.queryUpdate(builder.buildRequest());
    }
}
