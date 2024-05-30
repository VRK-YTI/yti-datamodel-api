package fi.vm.yti.datamodel.api.migration;

import fi.vm.yti.datamodel.api.v2.dto.*;
import fi.vm.yti.datamodel.api.v2.dto.visualization.PositionDataDTO;
import fi.vm.yti.datamodel.api.v2.mapper.MapperUtils;
import fi.vm.yti.datamodel.api.v2.properties.DCAP;
import fi.vm.yti.datamodel.api.v2.utils.DataModelURI;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.rdf.model.*;
import org.apache.jena.shared.PrefixMapping;
import org.apache.jena.sparql.vocabulary.FOAF;
import org.apache.jena.vocabulary.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.topbraid.shacl.vocabulary.SH;

import java.util.*;
import java.util.stream.Collectors;

import static fi.vm.yti.datamodel.api.migration.V1DataMigrationService.OLD_NAMESPACE;

public class V1DataMapper {

    private V1DataMapper() {
    }

    private static final Logger LOG = LoggerFactory.getLogger(V1DataMapper.class);

    private static final Property DOCUMENTATION =
            ResourceFactory.createProperty("http://uri.suomi.fi/datamodel/ns/iow#documentation");

    private static final Property POINT_XY =
            ResourceFactory.createProperty("http://uri.suomi.fi/datamodel/ns/iow#pointXY");

    private static final Property CODE_LIST =
            ResourceFactory.createProperty("http://purl.org/dc/dcam/memberOf");

    private static final Property LOCAL_NAME =
            ResourceFactory.createProperty("http://uri.suomi.fi/datamodel/ns/iow#localName");

    private static final Property preferredXMLNamespaceName =
            ResourceFactory.createProperty("http://purl.org/ws-mmi-dc/terms/preferredXMLNamespaceName");

    private static final Map<String, List<String>> ERRORS = new HashMap<>();

    public static DataModelDTO getLibraryMetadata(String modelURI, Model oldData, Model serviceCategories, String defaultOrganization) {
        var modelResource = oldData.getResource(modelURI);
        var dto = new DataModelDTO();
        dto.setPrefix(MapperUtils.getLiteral(modelResource, DCAP.preferredXMLNamespacePrefix, String.class));
        dto.setLabel(MapperUtils.localizedPropertyToMap(modelResource, RDFS.label));
        dto.setDescription(MapperUtils.localizedPropertyToMap(modelResource, RDFS.comment));

        var groups = new HashSet<String>();
        MapperUtils.arrayPropertyToSet(modelResource, DCTerms.isPartOf)
                .forEach(g -> groups.add(MapperUtils.propertyToString(serviceCategories.getResource(g), SKOS.notation)));
        dto.setGroups(groups.stream()
                .filter(Objects::nonNull)
                .collect(Collectors.toSet())
        );

        if (defaultOrganization == null || defaultOrganization.isBlank()) {
            var org = MapperUtils.arrayPropertyToSet(modelResource, DCTerms.contributor).stream()
                    .map(o -> UUID.fromString(o.replace("urn:uuid:", "")))
                    .collect(Collectors.toSet());
            dto.setOrganizations(org);
        } else {
            dto.setOrganizations(Set.of(UUID.fromString("7d3a3c00-5a6b-489b-a3ed-63bb58c26a63")));
        }

        var languages = MapperUtils.getList(oldData, modelResource, DCTerms.language)
                .asJavaList().stream()
                .map(l -> l.asLiteral().getString())
                .collect(Collectors.toSet());
        dto.setLanguages(languages);

        dto.setTerminologies(MapperUtils.arrayPropertyToSet(modelResource, DCTerms.references));

        var internalNamespaces = new HashSet<String>();
        var externalNamespaces = new HashSet<ExternalNamespaceDTO>();

        MapperUtils.arrayPropertyToSet(modelResource, DCTerms.requires)
                .forEach(ns -> {
                    if (!ns.contains("uri.suomi.fi")) {
                        var extResource = oldData.getResource(ns);
                        var ext = new ExternalNamespaceDTO();

                        if (!ns.endsWith("/") && !ns.endsWith("#")) {
                            var nsResource = oldData.getResource(ns);
                            if (nsResource.hasProperty(preferredXMLNamespaceName)) {
                                ns = MapperUtils.propertyToString(nsResource, preferredXMLNamespaceName);
                            } else {
                                ns = ns + "/";
                            }
                        }
                        ext.setNamespace(ns);
                        var label = MapperUtils.localizedPropertyToMap(extResource, RDFS.label);
                        var prefix = MapperUtils.propertyToString(extResource, DCAP.preferredXMLNamespacePrefix);

                        languages.forEach(l -> label.putIfAbsent(l, prefix));
                        label.keySet().removeIf(key -> !languages.contains(key));

                        ext.setName(label);
                        ext.setPrefix(prefix);
                        externalNamespaces.add(ext);
                    }
                });

        // add owl to linked models to avoid validation errors
        var c = externalNamespaces.stream().filter(ns -> ns.getNamespace().equals("http://www.w3.org/2002/07/owl#")).count();
        if (c == 0) {
            var e = new ExternalNamespaceDTO();
            e.setPrefix("owl");
            var name = new HashMap<String, String>();
            languages.forEach(l -> name.put(l, "owl"));
            e.setName(name);
            e.setNamespace("http://www.w3.org/2002/07/owl#");
            externalNamespaces.add(e);
        }
        dto.setInternalNamespaces(internalNamespaces);
        dto.setExternalNamespaces(externalNamespaces);

        if (modelResource.hasProperty(DCTerms.relation)) {
            var links = MapperUtils.getList(oldData, modelResource, DCTerms.relation).asJavaList().stream().map(link -> {
                var linkDTO = new LinkDTO();
                linkDTO.setUri(MapperUtils.propertyToString(link.asResource(), FOAF.homepage));
                linkDTO.setName(MapperUtils.localizedPropertyToMap(link.asResource(), DCTerms.title));
                linkDTO.setDescription(MapperUtils.localizedPropertyToMap(link.asResource(), DCTerms.description));
                return linkDTO;
            }).collect(Collectors.toSet());
            dto.setLinks(links);
        }

        dto.setDocumentation(MapperUtils.localizedPropertyToMap(modelResource, DOCUMENTATION));

        // make sure that label exists in all languages
        String defaultName;
        if (dto.getLabel().containsKey("fi")) {
            defaultName = dto.getLabel().get("fi");
        } else {
            defaultName = dto.getLabel().getOrDefault("en", "Name placeholder");
        }
        languages.forEach(lang -> dto.getLabel().putIfAbsent(lang, defaultName));

        return dto;
    }

    public static ClassDTO mapLibraryClass(Resource classResource) {
        var dto = new ClassDTO();
        mapCommon(dto, classResource);
        var subClasses = MapperUtils.arrayPropertyToSet(classResource, RDFS.subClassOf).stream()
                .map(V1DataMapper::fixNamespace)
                .collect(Collectors.toSet());
        dto.setSubClassOf(subClasses);
        return dto;
    }

    public static ResourceDTO mapLibraryResource(Resource resource) {
        var dto = new ResourceDTO();
        mapCommon(dto, resource);
        var range = fixNamespace(MapperUtils.propertyToString(resource, RDFS.range));
        dto.setRange(range);
        return dto;
    }

    public static NodeShapeDTO mapProfileClass(Resource nodeShapeResource) {
        var dto = new NodeShapeDTO();
        mapCommon(dto, nodeShapeResource);
        dto.setProperties(new HashSet<>());

        if (MapperUtils.hasType(nodeShapeResource, SH.NodeShape)) {
            dto.setTargetClass(fixNamespace(MapperUtils.propertyToString(nodeShapeResource, SH.targetClass)));
        } else if (nodeShapeResource.hasProperty(RDFS.subClassOf)) {
            dto.setTargetClass(fixNamespace(MapperUtils.propertyToString(nodeShapeResource, RDFS.subClassOf)));
        } else {
            dto.setTargetClass(OWL.Thing.getURI());
        }
        return dto;
    }

    public static PropertyShapeDTO mapProfileResource(Model oldData, Resource propertyShape, String prefix, String classResource) {
        PropertyShapeDTO dto;

        if (!propertyShape.hasProperty(SH.path)) {
            LOG.warn("MIGRATION ERROR: No path found for property shape {}, model {}", propertyShape.getURI(), prefix);
            addError(prefix, String.format("No path found for property shape %s, class %s", propertyShape.getURI(), classResource));
            return null;
        }

        if (propertyShape.hasProperty(DCTerms.type)) {
            var type = propertyShape.getProperty(DCTerms.type).getObject();
            if (OWL.DatatypeProperty.equals(type)) {
                dto = mapProfileAttribute(propertyShape);
            } else if (OWL.ObjectProperty.equals(type)) {
                dto = mapProfileAssociation(oldData, propertyShape, classResource, prefix);
            } else {
                LOG.warn("MIGRATION ERROR: Invalid type for property shape {}, model {}", propertyShape.getURI(), prefix);
                addError(prefix, String.format("Invalid type for property shape %s, class %s", propertyShape.getURI(), classResource));
                return null;
            }
        } else {
            if (propertyShape.hasProperty(SH.datatype)) {
                dto = mapProfileAttribute(propertyShape);
            } else {
                dto = mapProfileAssociation(oldData, propertyShape, classResource, prefix);
            }
        }

        Resource defaultPath = dto instanceof AttributeRestriction
                ? OWL2.topDataProperty
                : OWL2.topObjectProperty;

        mapCommon(dto, propertyShape);

        var path = MapperUtils.propertyToString(propertyShape, SH.path);
        var pathRes = oldData.getResource(path);

        if (pathRes.listProperties().hasNext()) {
            dto.setPath(defaultPath.toString());
        } else {
            dto.setPath(fixNamespace(path));
        }
        dto.setMaxCount(MapperUtils.getLiteral(propertyShape, SH.maxCount, Integer.class));
        dto.setMinCount(MapperUtils.getLiteral(propertyShape, SH.minCount, Integer.class));

        return dto;
    }

    private static AttributeRestriction mapProfileAttribute(Resource resource) {
        var attr = new AttributeRestriction();
        attr.setDataType(MapperUtils.propertyToString(resource, SH.datatype));
        attr.setCodeLists(MapperUtils.arrayPropertyToList(resource, CODE_LIST));
        attr.setDefaultValue(MapperUtils.propertyToString(resource, SH.defaultValue));
        attr.setHasValue(MapperUtils.propertyToString(resource, SH.hasValue));
        attr.setLanguageIn(MapperUtils.arrayPropertyToSet(resource, SH.languageIn));
        attr.setMaxInclusive(MapperUtils.getLiteral(resource, SH.maxInclusive, Integer.class));
        attr.setMinInclusive(MapperUtils.getLiteral(resource, SH.minInclusive, Integer.class));
        attr.setMaxExclusive(MapperUtils.getLiteral(resource, SH.maxExclusive, Integer.class));
        attr.setMinExclusive(MapperUtils.getLiteral(resource, SH.minExclusive, Integer.class));
        attr.setPattern(MapperUtils.propertyToString(resource, SH.pattern));
        attr.setMaxLength(MapperUtils.getLiteral(resource, SH.maxLength, Integer.class));
        attr.setMinLength(MapperUtils.getLiteral(resource, SH.minLength, Integer.class));

        if (resource.hasProperty(SH.in)) {
            attr.setAllowedValues(resource.getProperty(SH.in)
                    .getList()
                    .asJavaList().stream()
                    .map(RDFNode::toString)
                    .toList());
        }

        return attr;
    }

    private static AssociationRestriction mapProfileAssociation(Model oldData, Resource resource, String classRes, String prefix) {
        var assoc = new AssociationRestriction();
        var node = MapperUtils.propertyToString(resource, SH.node);

        if (node != null) {
            var nodeResource = oldData.getResource(node);
            var targetClass = MapperUtils.propertyToString(nodeResource, SH.targetClass);
            var subClassOf = MapperUtils.propertyToString(nodeResource, RDFS.subClassOf);

            // add sh:class if sh:targetClass or sh:node not in the same model
            if (targetClass != null && !oldData.getResource(targetClass).listProperties().hasNext()) {
                assoc.setClassType(fixNamespace(targetClass));
            } else if (subClassOf != null && !oldData.getResource(subClassOf).listProperties().hasNext()) {
                assoc.setClassType(fixNamespace(subClassOf));
            } else if (!nodeResource.listProperties().hasNext()) {
                assoc.setClassType(fixNamespace(node));
            } else {
                LOG.warn("MIGRATION ERROR: missing association target, class {}, resource {}", classRes, resource.getURI());
                addError(prefix, String.format("missing association target in %s, class %s", resource.getURI(), classRes));
            }
        }
        return assoc;
    }

    public static String getUriForOldResource(String prefix, Resource resource) {
        String id;
        if (resource.hasProperty(LOCAL_NAME)) {
            id = MapperUtils.propertyToString(resource, LOCAL_NAME);
        } else {
            id = resource.getURI()
                    .replace(ModelConstants.URN_UUID, "uuid-");
        }
        return DataModelURI.createResourceURI(prefix, id).getResourceURI();
    }

    public static List<Resource> findResourcesByType(Model oldData, Resource type) {
        return oldData.listSubjectsWithProperty(RDF.type, type)
                .mapWith(s -> oldData.getResource(s.getURI()))
                .toList();
    }

    public static List<PositionDataDTO> mapPositions(String modelURI, Model oldVisualization, PrefixMapping prefixMapping) {
        var positions = new ArrayList<PositionDataDTO>();

        oldVisualization.listSubjectsWithProperty(RDF.type, RDFS.Class).forEach(subj -> {
            var dto = new PositionDataDTO();
            var uri = NodeFactory.createURI(subj.getURI());

            if (uri.toString().startsWith(modelURI)) {
                dto.setIdentifier(uri.getLocalName());
            } else {
                var prefix = prefixMapping.getNsURIPrefix(uri.getNameSpace());
                dto.setIdentifier(prefix + ":" + uri.getLocalName());
            }
            var coordinates = MapperUtils.propertyToString(subj, POINT_XY);
            if (coordinates == null) {
                return;
            }
            var xy = coordinates.split(",");
            dto.setX(Double.parseDouble(xy[0]) * 1.2);
            dto.setY(Double.parseDouble(xy[1]) * 1.2);

            positions.add(dto);
        });

        return positions;
    }

    private static void mapCommon(BaseDTO dto, Resource resource) {
        if (dto instanceof NodeShapeDTO || dto instanceof ClassDTO) {
            dto.setIdentifier(resource.getLocalName());
        } else if (resource.hasProperty(LOCAL_NAME)) {
            dto.setIdentifier(MapperUtils.propertyToString(resource, LOCAL_NAME));
        } else if (dto instanceof PropertyShapeDTO && !resource.hasProperty(LOCAL_NAME)) {
            // Property shapes without localName are in uuid format.
            // Add prefix 'uuid', because identifier cannot start with a number
            dto.setIdentifier(resource.getURI().replace(ModelConstants.URN_UUID, "uuid-"));
        } else {
            dto.setIdentifier(resource.getLocalName());
        }
        dto.setEditorialNote(MapperUtils.propertyToString(resource, SKOS.editorialNote));
        dto.setSubject(MapperUtils.propertyToString(resource, DCTerms.subject));

        if (resource.hasProperty(SH.name)) {
            dto.setLabel(MapperUtils.localizedPropertyToMap(resource, SH.name));

        } else if (resource.hasProperty(RDFS.label)) {
            dto.setLabel(MapperUtils.localizedPropertyToMap(resource, RDFS.label));
        }

        if (resource.hasProperty(SH.description)) {
            dto.setNote(MapperUtils.localizedPropertyToMap(resource, SH.description));
        } else if (resource.hasProperty(RDFS.comment)) {
            dto.setNote(MapperUtils.localizedPropertyToMap(resource, RDFS.comment));
        }
    }

    public static String fixNamespace(String uri) {
        if (uri != null && uri.contains("uri.suomi.fi")) {
            return uri
                    .replace(OLD_NAMESPACE, ModelConstants.SUOMI_FI_NAMESPACE)
                    .replace("#", ModelConstants.RESOURCE_SEPARATOR);
        }
        return uri;
    }

    public static void addError(String prefix, String message) {
        var errorList = ERRORS.getOrDefault(prefix, new ArrayList<>());
        errorList.add(message);
        ERRORS.put(prefix, errorList);
    }

    public static void clearErrors() {
        ERRORS.clear();
    }

    public static Map<String, List<String>> getErrors() {
        return ERRORS;
    }
}