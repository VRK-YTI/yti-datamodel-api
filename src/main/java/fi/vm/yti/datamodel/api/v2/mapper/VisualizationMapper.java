package fi.vm.yti.datamodel.api.v2.mapper;

import fi.vm.yti.datamodel.api.v2.dto.ModelConstants;
import fi.vm.yti.datamodel.api.v2.dto.ResourceType;
import fi.vm.yti.datamodel.api.v2.dto.visualization.*;
import fi.vm.yti.datamodel.api.v2.opensearch.index.IndexResource;
import fi.vm.yti.datamodel.api.v2.properties.SuomiMeta;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.rdf.model.*;
import org.apache.jena.vocabulary.DCTerms;
import org.apache.jena.vocabulary.OWL;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.topbraid.shacl.vocabulary.SH;

import java.util.*;

public class VisualizationMapper {
    private static final Logger LOG = LoggerFactory.getLogger(VisualizationMapper.class);

    private VisualizationMapper() {
    }

    public static VisualizationClassDTO mapClass(String classURI, Model model,
                                                 Map<String, String> namespaces) {
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
        getParentReferences(classResource, namespaces).forEach(classDTO::addReference);
        classDTO.setType(VisualizationNodeType.CLASS);
        return classDTO;
    }

    public static void mapLibraryClassResources(VisualizationClassDTO classDTO, Model model,
                                                Resource classResource,
                                                Set<Resource> externalResources,
                                                Map<String, String> namespaces) {
        ClassMapper.getClassRestrictionList(model, classResource).stream()
                .filter(r -> r.asResource().hasProperty(OWL.onProperty))
                .forEach(r -> {
                    var resource = model.getResource(MapperUtils.propertyToString(r.asResource(), OWL.onProperty));
                    if (!model.contains(resource, null)) {
                        externalResources.add(r.asResource());
                        return;
                    }
                    mapLibraryJenaResource(classDTO, r.asResource(), resource, namespaces);
                });
    }

    public static void mapNodeShapeResources(VisualizationClassDTO classDTO,  Resource nodeShapeResource, Model model,
                                             Set<Resource> externalResources, Map<String, String> namespaces) {
        nodeShapeResource.listProperties(SH.property)
                .forEach(s -> {
                    var resource = model.getResource(s.getResource().getURI());
                    if (!model.contains(resource, null)) {
                        externalResources.add(s.getResource());
                        return;
                    }
                    mapProfileResource(classDTO, resource, model, namespaces);
                });
    }

    public static List<VisualizationAttributeNodeDTO> mapAttributesWithDomain(Model model) {
        return model.listStatements(null, RDFS.domain, (RDFNode) null)
                .filterKeep(r -> MapperUtils.hasType(r.getSubject(), OWL.DatatypeProperty))
                .mapWith(r -> {
                    var nodeDTO = new VisualizationAttributeNodeDTO();
                    nodeDTO.setType(VisualizationNodeType.ATTRIBUTE);
                    var range = MapperUtils.uriToURIDTO(MapperUtils.propertyToString(r.getSubject(), RDFS.range), model);
                    if (range != null) {
                        nodeDTO.setDataType(range.getCurie());
                    }
                    mapCommon(nodeDTO, r.getSubject(), Map.of());
                    var domain = r.getObject().asResource();
                    var reference = new VisualizationReferenceDTO();
                    var referenceIdentifier = getReferenceIdentifier(domain.getURI(), Map.of());
                    reference.setIdentifier(referenceIdentifier);
                    reference.setReferenceTarget(referenceIdentifier);
                    reference.setReferenceType(VisualizationReferenceType.ATTRIBUTE_DOMAIN);
                    nodeDTO.setReferences(Set.of(reference));
                    return nodeDTO;
                }).toList();
    }

    public static void mapAssociationsWithDomain(VisualizationClassDTO classDTO,
                                                 Model model,
                                                 Resource classResource,
                                                 Map<String, String> namespaces) {
        model.listStatements(null, RDFS.domain, classResource)
                .filterKeep(r -> MapperUtils.hasType(r.getSubject(), OWL.ObjectProperty))
                .forEach(r -> {
                    var range = MapperUtils.propertyToString(r.getSubject(), RDFS.range);
                    if (range == null) {
                        return;
                    }
                    var dto = new VisualizationReferenceDTO();
                    dto.setLabel(MapperUtils.localizedPropertyToMap(r.getSubject(), RDFS.label));
                    dto.setUri(r.getSubject().getURI());
                    dto.setReferenceType(VisualizationReferenceType.ASSOCIATION);
                    dto.setIdentifier(MapperUtils.getLiteral(r.getSubject(), DCTerms.identifier, String.class));
                    dto.setReferenceTarget(getReferenceIdentifier(range, namespaces));
                    classDTO.addReference(dto);
                });
    }

    public static VisualizationNodeDTO mapExternalClass(String identifier, Set<String> languages) {
        var dto = new VisualizationClassDTO();
        dto.setIdentifier(identifier);
        dto.setType(VisualizationNodeType.EXTERNAL_CLASS);

        var label = new HashMap<String, String>();
        languages.forEach(key -> label.put(key, identifier));
        dto.setLabel(label);

        return dto;
    }

    public static void mapLibraryJenaResource(VisualizationClassDTO classDTO,
                                              Resource restrictionResource,
                                              Resource resource,
                                              Map<String, String> namespaces) {
        var label = MapperUtils.localizedPropertyToMap(resource, RDFS.label);
        if (MapperUtils.hasType(resource, OWL.DatatypeProperty, OWL.AnnotationProperty)) {
            mapLibraryResource(classDTO, restrictionResource, namespaces, label, true);
        } else if (MapperUtils.hasType(resource, OWL.ObjectProperty, RDF.Property)) {
            mapLibraryResource(classDTO, restrictionResource, namespaces, label, false);
        }
    }

    public static void mapLibraryIndexResource(VisualizationClassDTO classDTO,
                                               Resource restrictionResource,
                                               IndexResource resource,
                                               Map<String, String> namespaces) {
        var label = resource.getLabel();
        if (ResourceType.ATTRIBUTE.equals(resource.getResourceType())) {
            mapLibraryResource(classDTO, restrictionResource, namespaces, label, true);
        } else if (ResourceType.ASSOCIATION.equals(resource.getResourceType())) {
            mapLibraryResource(classDTO, restrictionResource, namespaces, label, false);
        }
    }

    private static void mapLibraryResource(VisualizationClassDTO classDTO,
                                          Resource restrictionResource,
                                          Map<String, String> namespaces,
                                          Map<String, String> label,
                                          boolean isAttribute) {
        var someValuesFrom = MapperUtils.propertyToString(restrictionResource, OWL.someValuesFrom);
        var resourceURI = MapperUtils.propertyToString(restrictionResource, OWL.onProperty);
        if (isAttribute) {
            var attribute = new VisualizationAttributeDTO();
            attribute.setIdentifier(getReferenceIdentifier(resourceURI, namespaces));
            attribute.setLabel(label);
            attribute.setUri(resourceURI);
            if (someValuesFrom != null) {
                var uriDTO = MapperUtils.uriToURIDTO(someValuesFrom, restrictionResource.getModel());
                attribute.setDataType(uriDTO.getCurie());
            }
            classDTO.addAttribute(attribute);
        } else {
            var association = new VisualizationReferenceDTO();
            association.setIdentifier(getReferenceIdentifier(resourceURI, namespaces));
            association.setLabel(label);
            association.setUri(resourceURI);
            association.setReferenceType(VisualizationReferenceType.ASSOCIATION);
            association.setReferenceTarget(getReferenceIdentifier(someValuesFrom, namespaces));
            classDTO.addAssociation(association);
        }
    }

    public static void mapProfileResource(VisualizationClassDTO dto, Resource resource,
                                          Model model, Map<String, String> namespaces) {
        mapProfileResource(dto, resource, resource.getURI(), model, namespaces);
    }

    public static void mapProfileResource(VisualizationClassDTO dto, Resource resource, String versionedResourceURI,
                                          Model model, Map<String, String> namespaces) {
        var label = MapperUtils.localizedPropertyToMap(resource, RDFS.label);
        if (MapperUtils.hasType(resource, OWL.DatatypeProperty)) {
            var attribute = new VisualizationPropertyShapeAttributeDTO();
            attribute.setIdentifier(getReferenceIdentifier(versionedResourceURI, namespaces));
            attribute.setLabel(label);
            attribute.setUri(versionedResourceURI);
            var dataType = MapperUtils.propertyToString(resource, SH.datatype);
            if (dataType != null) {
                var uriDTO = MapperUtils.uriToURIDTO(dataType, model);
                attribute.setDataType(uriDTO.getCurie());
            }
            attribute.setMaxCount(MapperUtils.getLiteral(resource, SH.maxCount, Integer.class));
            attribute.setMinCount(MapperUtils.getLiteral(resource, SH.minCount, Integer.class));
            attribute.setCodeLists(MapperUtils.arrayPropertyToSet(resource, SuomiMeta.codeList));
            dto.addAttribute(attribute);
        } else if (MapperUtils.hasType(resource, OWL.ObjectProperty)) {
            var association = new VisualizationPropertyShapeAssociationDTO();
            association.setIdentifier(getReferenceIdentifier(versionedResourceURI, namespaces));
            association.setLabel(label);
            association.setUri(versionedResourceURI);
            association.setMaxCount(MapperUtils.getLiteral(resource, SH.maxCount, Integer.class));
            association.setMinCount(MapperUtils.getLiteral(resource, SH.minCount, Integer.class));
            association.setReferenceTarget(getPropertyShapeAssociationTarget(model, resource, namespaces));
            association.setReferenceType(VisualizationReferenceType.ASSOCIATION);
            dto.addAssociation(association);
        }
    }

    public static Model mapPositionDataToModel(String positionBaseURI, List<PositionDataDTO> positions) {
        var positionModel = ModelFactory.createDefaultModel();
        positions.forEach(node -> {
            var resource = mapPosition(positionModel, positionBaseURI, node);
            node.getReferenceTargets().forEach(t -> {
                var targetResource = positionModel.createResource();
                targetResource.addProperty(SuomiMeta.target, t.target());
                handleTargetOrigin(positions, t, targetResource);
                resource.addProperty(SuomiMeta.referenceTarget, targetResource);
            });
        });
        return positionModel;
    }

    public static Resource mapPosition(Model positionModel, String positionBaseURI, PositionDataDTO node) {
        var resource = positionModel.createResource(positionBaseURI + node.getIdentifier());
        resource.addLiteral(SuomiMeta.posX, node.getX());
        resource.addLiteral(SuomiMeta.posY, node.getY());
        resource.addProperty(DCTerms.identifier, node.getIdentifier());
        return resource;
    }

    public static Set<VisualizationHiddenNodeDTO> mapPositionsDataToDTOsAndCreateHiddenNodes(
            Model positions, String modelPrefix, Set<VisualizationNodeDTO> classes) {
        var hiddenElements = new HashSet<VisualizationHiddenNodeDTO>();

        classes.forEach(dto -> {
            var positionResource = positions.getResource(ModelConstants.MODEL_POSITIONS_NAMESPACE + modelPrefix
                    + ModelConstants.RESOURCE_SEPARATOR
                    + dto.getIdentifier());
            dto.setPosition(getPositionFromResource(positionResource));

            // check if there are hidden nodes between parent relations
            if (dto.getReferences() != null && !dto.getReferences().isEmpty()) {
                dto.getReferences().forEach(reference -> {
                    var path = handlePath(positions, dto.getIdentifier(), reference, hiddenElements);
                    reference.setReferenceTarget(path.get(0));
                });
            }

            if (dto instanceof VisualizationClassDTO classDTO) {
                // check if there are hidden nodes between association relations
                classDTO.getAssociations().forEach(reference -> {
                    var target = reference.getReferenceTarget();
                    if (target != null) {
                        var path = handlePath(positions, dto.getIdentifier(), reference, hiddenElements);
                        reference.setReferenceTarget(path.get(0));
                    }
                });
            }
        });
        return hiddenElements;
    }

    private static List<String> handlePath(Model positions, String sourceIdentifier, VisualizationReferenceDTO reference,
                                           Set<VisualizationHiddenNodeDTO> hiddenElements) {
        var targetResources = positions.listSubjectsWithProperty(SuomiMeta.target, reference.getReferenceTarget()).toList();
        for (Resource targetResource : targetResources) {
            if (!isSameOrigin(sourceIdentifier, reference, targetResource)) {
                continue;
            }

            var positionSubject = positions.listSubjectsWithProperty(SuomiMeta.referenceTarget, targetResource).next();

            var path = new LinkedList<String>();
            path.add(reference.getReferenceTarget());

            while (positionSubject.hasProperty(SuomiMeta.referenceTarget)) {
                var identifier = MapperUtils.propertyToString(positionSubject, DCTerms.identifier);

                // if there is an element in the path, that is not a hidden node and not the same as source node,
                // this is the wrong path. Jump to the next resource
                if (identifier != null
                        && !identifier.startsWith(ModelConstants.CORNER_PREFIX)
                        && !sourceIdentifier.equals(identifier)) {
                    break;
                }

                // reached the source node
                if (sourceIdentifier.equals(identifier)) {
                    return path;
                }

                path.addFirst(positionSubject.getLocalName());

                if (positionSubject.getLocalName().startsWith(ModelConstants.CORNER_PREFIX)) {
                    hiddenElements.add(mapHiddenNode(positionSubject, reference, sourceIdentifier));
                }

                var references = positions.listSubjectsWithProperty(SuomiMeta.target, positionSubject.getLocalName()).toList();

                // no more references found, return path
                // should not happen as each hidden node should have references
                // and otherwise should have returned earlier
                if (references.isEmpty()) {
                    return path;
                }

                // assume, that there is only one reference for hidden nodes
                positionSubject = positions.listSubjectsWithProperty(SuomiMeta.referenceTarget, references.get(0)).next();
            }
        }
        // no hidden nodes between source and target
        return List.of(reference.getReferenceTarget());
    }

    private static boolean isSameOrigin(String sourceIdentifier, VisualizationReferenceDTO reference, Resource targetResource) {
        var origin = MapperUtils.propertyToString(targetResource, SuomiMeta.origin);

        if (reference.getReferenceType().equals(VisualizationReferenceType.PARENT_CLASS) && sourceIdentifier.equals(origin)) {
            return true;
        } else {
            return reference.getReferenceType().equals(VisualizationReferenceType.ASSOCIATION) && reference.getIdentifier().equals(origin);
        }
    }

    private static VisualizationHiddenNodeDTO mapHiddenNode(Resource position, VisualizationReferenceDTO reference, String sourceIdentifier) {
        var dto = new VisualizationHiddenNodeDTO();

        dto.setIdentifier(MapperUtils.propertyToString(position, DCTerms.identifier));
        dto.setPosition(getPositionFromResource(position));

        dto.setReferenceTarget(MapperUtils.propertyToString(
                position.getProperty(SuomiMeta.referenceTarget).getObject().asResource(), SuomiMeta.target));

        dto.setReferenceType(reference.getReferenceType());

        if (reference.getReferenceType().equals(VisualizationReferenceType.ASSOCIATION)) {
            dto.setOrigin(reference.getIdentifier());
        } else {
            dto.setOrigin(sourceIdentifier);
        }

        return dto;
    }

    private static PositionDTO getPositionFromResource(Resource positionResource) {
        var x = MapperUtils.getLiteral(positionResource, SuomiMeta.posX, Double.class);
        var y = MapperUtils.getLiteral(positionResource, SuomiMeta.posY, Double.class);
        return new PositionDTO(
                x != null ? x : 0.0,
                y != null ? y : 0.0
        );
    }

    private static void mapCommon(VisualizationItemDTO item, Resource resource, Map<String, String> namespaces) {
        item.setLabel(MapperUtils.localizedPropertyToMap(resource, RDFS.label));
        item.setIdentifier(getReferenceIdentifier(resource.getURI(), namespaces));
        item.setUri(resource.getURI());
    }

    private static String getPropertyShapeAssociationTarget(Model model, Resource resource, Map<String, String> namespaces) {
        var target = MapperUtils.propertyToString(resource, SH.class_);
            if (target != null) {
            var iterator = model.listSubjectsWithProperty(SH.targetClass,
                    ResourceFactory.createResource(target));
            if (iterator.hasNext()) {
                return getReferenceIdentifier(iterator.next().getURI(), namespaces);
            } else {
                return getReferenceIdentifier(target, namespaces);
            }
        }
        return null;
    }

    @NotNull
    private static Set<VisualizationReferenceDTO> getParentReferences(Resource resource, Map<String, String> namespaceMap) {
        var parentReferences = new HashSet<VisualizationReferenceDTO>();

        if (MapperUtils.hasType(resource, OWL.Class)) {
            resource.listProperties(RDFS.subClassOf).forEach(parent -> {
                String classURI = parent.getObject().toString();
                // skip default parent class (owl#Thing)
                if ("http://www.w3.org/2002/07/owl#Thing".equals(classURI)) {
                    return;
                }
                var ref = new VisualizationReferenceDTO();
                var referenceIdentifier = getReferenceIdentifier(classURI, namespaceMap);
                ref.setIdentifier(referenceIdentifier);
                ref.setReferenceType(VisualizationReferenceType.PARENT_CLASS);
                ref.setReferenceTarget(referenceIdentifier);
                parentReferences.add(ref);
            });
        } else {
            var node = MapperUtils.propertyToString(resource, SH.node);
            if (node != null) {
                var ref = new VisualizationReferenceDTO();
                var referenceIdentifier = getReferenceIdentifier(node, namespaceMap);
                ref.setIdentifier(referenceIdentifier);
                ref.setReferenceType(VisualizationReferenceType.PARENT_CLASS);
                ref.setReferenceTarget(referenceIdentifier);
                parentReferences.add(ref);
            }
        }
        return parentReferences;
    }

    private static String getReferenceIdentifier(String uri, Map<String, String> namespaces) {
        if (uri == null) {
            return null;
        }
        try {
            var u = NodeFactory.createURI(uri);
            String prefix = namespaces.get(u.getNameSpace());

            if (prefix == null) {
                // referenced class in the same model
                return u.getLocalName();
            } else {
                // referenced class in the external namespace or other model in Interoperability platform
                return prefix + ":" + u.getLocalName();
            }
        } catch (Exception e) {
            LOG.warn("Invalid uri reference {}", uri);
            return null;
        }
    }

    private static void handleTargetOrigin(List<PositionDataDTO> positions, PositionDataDTO.ReferenceTarget t, Resource res) {
        var origin = t.origin();
        if (origin == null) {
            positions.stream().filter(p -> p.getIdentifier().equals(t.target())).findFirst().ifPresent(target -> {
                var refs = target.getReferenceTargets().iterator();
                if (refs.hasNext()) {
                    res.addProperty(SuomiMeta.origin, refs.next().origin());
                }
            });
        } else {
            res.addProperty(SuomiMeta.origin, origin);
        }
    }
}
