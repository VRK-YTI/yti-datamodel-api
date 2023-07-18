package fi.vm.yti.datamodel.api.v2.mapper;

import fi.vm.yti.datamodel.api.v2.dto.*;
import org.apache.jena.rdf.model.*;
import org.apache.jena.vocabulary.OWL;
import org.apache.jena.vocabulary.RDFS;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.topbraid.shacl.vocabulary.SH;

import java.net.URI;
import java.util.*;

public class VisualizationMapper {
    private static final Logger LOG = LoggerFactory.getLogger(VisualizationMapper.class);

    private VisualizationMapper() {
    }

    public static VisualizationClassDTO mapClass(String classURI, Model model, Model positions, Map<String, String> namespaces) {
        var classResource = model.getResource(classURI);
        VisualizationClassDTO classDTO;

        if (MapperUtils.hasType(classResource, OWL.Class)) {
            classDTO = new VisualizationClassDTO();
        } else {
            var nodeShapeDTO = new VisualizationNodeShapeDTO();
            var targetClass = MapperUtils.propertyToString(classResource, SH.targetClass);
            nodeShapeDTO.setTargetClass(getReferenceIdentifier(targetClass, namespaces));
            classDTO = nodeShapeDTO;
        }

        mapCommon(classDTO, classResource, namespaces);

        classDTO.setParentClasses(getParentClasses(classResource, namespaces));

        mapPositions(classURI, positions, classDTO);
        return classDTO;
    }

    public static VisualizationClassDTO mapExternalClass(String identifier, Set<String> languages, Model positions) {
        var dto = new VisualizationClassDTO();
        dto.setIdentifier(identifier);
        mapPositions(identifier, positions, dto);

        var label = new HashMap<String, String>();
        languages.forEach(key -> label.put(key, identifier));
        dto.setLabel(label);

        return dto;
    }
    public static void mapResource(VisualizationClassDTO dto, Resource resource, Model model, Model positions, Map<String, String> namespaces) {
        if (MapperUtils.hasType(resource, OWL.DatatypeProperty)) {
            dto.addAttribute(mapAttribute(resource, namespaces));
        } else if (MapperUtils.hasType(resource, OWL.ObjectProperty)) {
            dto.addAssociation(mapAssociation(resource, model, positions,namespaces));
        }
    }

    private static VisualizationAttributeDTO mapAttribute(Resource resource, Map<String, String> namespaces) {
        if (MapperUtils.hasType(resource, SH.PropertyShape)) {
            var dto = new VisualizationPropertyShapeAttributeDTO();
            mapCommon(dto, resource, namespaces);
            dto.setPath(getReferenceIdentifier(MapperUtils.propertyToString(resource, SH.path), namespaces));
            dto.setDataType(MapperUtils.propertyToString(resource, SH.datatype));
            dto.setMaxCount(MapperUtils.getLiteral(resource, SH.maxCount, Integer.class));
            dto.setMinCount(MapperUtils.getLiteral(resource, SH.minCount, Integer.class));
            dto.setCodeLists(MapperUtils.arrayPropertyToSet(resource, Iow.codeList));
            return dto;
        } else {
            var dto = new VisualizationAttributeDTO();
            mapCommon(dto, resource, namespaces);
            return dto;
        }
    }

    private static VisualizationAssociationDTO mapAssociation(Resource resource, Model model, Model positions, Map<String, String> namespaces) {
        if (MapperUtils.hasType(resource, SH.PropertyShape)) {
            var dto = new VisualizationPropertyShapeAssociationDTO();
            mapCommon(dto, resource, namespaces);
            dto.setPath(getReferenceIdentifier(MapperUtils.propertyToString(resource, SH.path), namespaces));
            dto.setMaxCount(MapperUtils.getLiteral(resource, SH.maxCount, Integer.class));
            dto.setMinCount(MapperUtils.getLiteral(resource, SH.minCount, Integer.class));
            dto.setRoute(getPropertyShapeRoute(model, resource, positions, namespaces));
            return dto;
        } else {
            var dto = new VisualizationAssociationDTO();
            var target = MapperUtils.propertyToString(resource, RDFS.range);
            mapCommon(dto, resource, namespaces);
            dto.setRoute(getRoute(positions, target, namespaces));
            return dto;
        }
    }

    private static void mapCommon(VisualizationItemDTO item, Resource resource, Map<String, String> namespaces) {
        item.setLabel(MapperUtils.localizedPropertyToMap(resource, RDFS.label));
        item.setIdentifier(getReferenceIdentifier(resource.getURI(), namespaces));
    }

    private static LinkedList<String> getRoute(Model positions, String target, Map<String, String> namespaces) {
        // TODO: store route from source to target via possible invisible nodes (with id #corner-12345678)
        var route = new LinkedList<String>();
        route.add(getReferenceIdentifier(target, namespaces));
        return route;
    }

    private static LinkedList<String> getPropertyShapeRoute(Model model, Resource resource,
                                                      Model positions, Map<String, String> namespaces) {
        var target = MapperUtils.propertyToString(resource, SH.class_);
        if (target != null) {
            // is it possible to have multiple routes per association?
            var iterator = model.listSubjectsWithProperty(SH.targetClass,
                    ResourceFactory.createResource(target));
            if (iterator.hasNext()) {
                return getRoute(positions, iterator.next().getURI(), namespaces);
            } else {
                return getRoute(positions, target, namespaces);
            }
        }
        return new LinkedList<>();
    }

    @NotNull
    private static HashSet<String> getParentClasses(Resource resource, Map<String, String> namespaceMap) {
        var parentClasses = new HashSet<String>();

        if (MapperUtils.hasType(resource, OWL.Class)) {
            var subClassOf = resource.listProperties(RDFS.subClassOf).toList();
            subClassOf.forEach(parent -> {
                String classURI = parent.getObject().toString();
                // skip default parent class (owl#Thing)
                if ("http://www.w3.org/2002/07/owl#Thing".equals(classURI)) {
                    return;
                }
                parentClasses.add(getReferenceIdentifier(classURI, namespaceMap));
            });
        } else {
            var node = MapperUtils.propertyToString(resource, SH.node);
            if (node != null) {
                parentClasses.add(getReferenceIdentifier(node, namespaceMap));
            }
        }
        return parentClasses;
    }

    private static String getReferenceIdentifier(String uri, Map<String, String> namespaces) {
        try {
            var uriPath = URI.create(uri).getPath();
            var fragment = uriPath.substring(uriPath.lastIndexOf("/") + 1);
            String prefix = namespaces.get(uri.substring(0, uri.lastIndexOf("/")));

            if (prefix == null) {
                // referenced class in the same model
                return fragment;
            } else {
                // referenced class in the external namespace or other model in Interoperability platform
                return prefix + ":" + fragment;
            }
        } catch (Exception e) {
            LOG.warn("Invalid uri reference {}", uri);
            return null;
        }
    }

    private static void mapPositions(String classURI, Model positions, VisualizationClassDTO classDTO) {
        // TODO save and map positions
        var positionResource = positions.getResource(classURI);
        if (positionResource == null) {
            classDTO.setPosition(new PositionDTO(0.0, 0.0));
        }
    }
}
