package fi.vm.yti.datamodel.api.v2.mapper;

import fi.vm.yti.common.Constants;
import fi.vm.yti.common.dto.ResourceCommonInfoDTO;
import fi.vm.yti.common.enums.Status;
import fi.vm.yti.common.properties.SuomiMeta;
import fi.vm.yti.common.util.MapperUtils;
import fi.vm.yti.datamodel.api.v2.utils.DataModelMapperUtils;
import fi.vm.yti.datamodel.api.v2.dto.*;
import fi.vm.yti.datamodel.api.v2.endpoint.error.MappingError;
import fi.vm.yti.datamodel.api.v2.opensearch.index.IndexResource;
import fi.vm.yti.datamodel.api.v2.properties.HTTP;
import fi.vm.yti.datamodel.api.v2.utils.DataModelURI;
import fi.vm.yti.datamodel.api.v2.utils.DataModelUtils;
import fi.vm.yti.security.YtiUser;
import org.apache.jena.datatypes.xsd.XSDDatatype;
import org.apache.jena.rdf.model.*;
import org.apache.jena.vocabulary.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.topbraid.shacl.vocabulary.SH;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class ClassMapper {

    private ClassMapper() {
        //Static class
    }

    private static final Logger logger = LoggerFactory.getLogger(ClassMapper.class);

    public static void createOntologyClassAndMapToModel(DataModelURI uri, Model model, ClassDTO dto, YtiUser user) {
        logger.info("Adding class to {}", uri.getModelURI());
        var modelResource = model.getResource(uri.getModelURI());

        var resource = DataModelMapperUtils.addCommonResourceInfo(model, uri, dto);
        MapperUtils.addCreationMetadata(resource, user);

        resource.addProperty(RDF.type, OWL.Class);

        //Equivalent class
        dto.getEquivalentClass().forEach(eq -> DataModelMapperUtils.addResourceRelationship(modelResource, resource, OWL.equivalentClass, eq));

        if (dto.getSubClassOf().isEmpty()) {
            resource.addProperty(RDFS.subClassOf, OWL.Thing); //Add OWL:Thing if nothing else is specified
        } else {
            dto.getSubClassOf().forEach(sub -> DataModelMapperUtils.addResourceRelationship(modelResource, resource, RDFS.subClassOf, sub));
        }
        dto.getDisjointWith().forEach(disjoint -> DataModelMapperUtils.addResourceRelationship(modelResource, resource, OWL.disjointWith, disjoint));
        modelResource.addProperty(DCTerms.hasPart, resource);

        DataModelMapperUtils.addTerminologyReference(dto, modelResource);
    }

    public static void createNodeShapeAndMapToModel(DataModelURI uri, Model model, NodeShapeDTO dto, YtiUser user) {
        logger.info("Adding node shape to {}", uri.getGraphURI());

        // var uri = DataModelURI.createResourceURI()
        var nodeShapeResource = DataModelMapperUtils.addCommonResourceInfo(model, uri, dto);
        MapperUtils.addCreationMetadata(nodeShapeResource, user);

        nodeShapeResource.addProperty(RDF.type, SH.NodeShape);

        var modelResource = model.getResource(uri.getModelURI());

        if (dto.getTargetClass() == null && dto.getTargetNode() == null) {
            nodeShapeResource.addProperty(SH.targetClass, OWL.Thing);
        } else {
            DataModelMapperUtils.addResourceRelationship(modelResource, nodeShapeResource, SH.targetClass, dto.getTargetClass());
        }
        DataModelMapperUtils.addResourceRelationship(modelResource, nodeShapeResource, SH.node, dto.getTargetNode());
        MapperUtils.addOptionalStringProperty(nodeShapeResource, HTTP.API_PATH, dto.getApiPath());
        DataModelMapperUtils.addTerminologyReference(dto, modelResource);
    }

    public static List<String> mapPlaceholderPropertyShapes(Model applicationProfileModel, String classURI, List<IndexResource> properties,
                                                            YtiUser user, Predicate<String> checkFreeIdentifier,
                                                            List<SimpleResourceDTO> attributeRestrictions) {
        var classResource = applicationProfileModel.getResource(classURI);
        var modelResource = applicationProfileModel.getResource(classResource.getNameSpace());
        var propertyResourceURIs = new ArrayList<String>();

        for (var property : properties) {
            var identifier = property.getIdentifier();

            var currentIdentifier = identifier;
            var count = 0;
            while (checkFreeIdentifier.test(classResource.getNameSpace() + currentIdentifier)) {
                currentIdentifier = identifier + String.format("-%d", ++count);
            }
            var propertyShapeResource = applicationProfileModel.createResource(classResource.getNameSpace() + currentIdentifier);

            var resourceType = property.getResourceType();
            if (ResourceType.ATTRIBUTE.equals(resourceType)) {
                propertyShapeResource.addProperty(RDF.type, OWL.DatatypeProperty);
            } else {
                propertyShapeResource.addProperty(RDF.type, OWL.ObjectProperty);
            }

            var label = property.getLabel();

            if (!label.isEmpty()) {
                var languages = MapperUtils.arrayPropertyToSet(modelResource, DCTerms.language);
                var labelValue = label.keySet().stream()
                        .filter(languages::contains)
                        .collect(Collectors.toMap(lang -> lang, label::get));

                // define label at least in one language to avoid validation errors
                if (labelValue.isEmpty()) {
                    labelValue.put(languages.iterator().next(), label.entrySet().iterator().next().getValue());
                }

                MapperUtils.addLocalizedProperty(languages,
                        labelValue,
                        propertyShapeResource,
                        RDFS.label);
            }

            propertyShapeResource.addProperty(SH.path, ResourceFactory.createResource(property.getId()))
                    .addProperty(DCTerms.identifier, ResourceFactory.createTypedLiteral(currentIdentifier, XSDDatatype.XSDNCName))
                    .addProperty(RDF.type, SH.PropertyShape)
                    .addProperty(RDFS.isDefinedBy, classResource.getProperty(RDFS.isDefinedBy).getObject())
                    .addProperty(SuomiMeta.publicationStatus, ResourceFactory.createResource(MapperUtils.getStatusUri(Status.DRAFT)));

            // add code list if added to library class' attribute restriction
            var attributeRestriction = attributeRestrictions.stream()
                    .filter(r -> property.getId().equals(r.getUri()))
                    .findFirst();
            attributeRestriction.ifPresent(
                    a -> a.getCodeLists().forEach(c -> propertyShapeResource.addProperty(SuomiMeta.codeList, c))
            );

            MapperUtils.addCreationMetadata(propertyShapeResource, user);

            classResource.addProperty(SH.property, propertyShapeResource);
            propertyResourceURIs.add(propertyShapeResource.getURI());
        }
        return propertyResourceURIs;
    }

    public static void mapNodeShapeProperties(Model model, String classURI, Set<String> propertyURIs) {
        var classRes = model.getResource(classURI);
        propertyURIs.forEach(
                (var uri) -> classRes.addProperty(SH.property, ResourceFactory.createResource(uri))
        );
    }

    public static void mapToUpdateOntologyClass(Model model, String modelUri, Resource classResource, ClassDTO classDTO, YtiUser user) {
        logger.info("Updating class in graph {}", modelUri);
        var modelResource = model.getResource(modelUri);

        DataModelMapperUtils.updateCommonResourceInfo(classResource, modelResource, classDTO);

        var eqProps = classResource.listProperties(OWL.equivalentClass)
                .filterDrop(p -> p.getObject().isAnon())
                .toList();
        model.remove(eqProps);

        classDTO.getEquivalentClass().forEach(eq ->
                DataModelMapperUtils.addResourceRelationship(modelResource, classResource, OWL.equivalentClass, eq));

        var subClassOf = classDTO.getSubClassOf();
        classResource.removeAll(RDFS.subClassOf);
        if (subClassOf.isEmpty()) {
            classResource.addProperty(RDFS.subClassOf, OWL.Thing); //Add OWL:Thing if no subClassOf is specified
        } else {
            subClassOf.forEach(sub -> DataModelMapperUtils.addResourceRelationship(modelResource, classResource, RDFS.subClassOf, sub));
        }

        classResource.removeAll(OWL.disjointWith);
        classDTO.getDisjointWith().forEach(disjoint ->
                DataModelMapperUtils.addResourceRelationship(modelResource, classResource, OWL.disjointWith, disjoint));
        MapperUtils.addUpdateMetadata(classResource, user);
    }

    public static void mapToUpdateNodeShape(Model model, String graph, Resource classResource, NodeShapeDTO nodeShapeDTO, Set<String> properties, YtiUser user) {
        logger.info("Updating node shape in graph {}", graph);
        var modelResource = model.getResource(graph);

        DataModelMapperUtils.updateCommonResourceInfo(classResource, modelResource, nodeShapeDTO);

        // either sh:node or sh:targetClass must be defined. If not, add owl:Thing as targetClass
        if (nodeShapeDTO.getTargetClass() == null && nodeShapeDTO.getTargetNode() == null) {
            classResource.addProperty(SH.targetClass, OWL.Thing);
        } else if (nodeShapeDTO.getTargetClass() == null && nodeShapeDTO.getTargetNode() != null) {
            classResource.removeAll(SH.targetClass);
        } else {
            DataModelMapperUtils.updateUriPropertyAndAddReferenceNamespaces(modelResource, classResource, SH.targetClass, nodeShapeDTO.getTargetClass());
        }

        if (nodeShapeDTO.getTargetNode() == null) {
            classResource.removeAll(SH.node);
        } else {
            DataModelMapperUtils.updateUriPropertyAndAddReferenceNamespaces(modelResource, classResource, SH.node, nodeShapeDTO.getTargetNode());
        }

        classResource.removeAll(SH.property);
        mapNodeShapeProperties(model, classResource.getURI(), properties);
        MapperUtils.updateStringProperty(classResource, HTTP.API_PATH, nodeShapeDTO.getApiPath());
        MapperUtils.addUpdateMetadata(classResource, user);
    }

    /**
     * Map model with given prefix and class identifier
     *
     * @param model           Model
     * @param uri             DataModelURI
     * @param orgModel        Model of organisations
     * @param hasRightToModel does current user have right to model
     * @return Class DTO
     */
    public static ClassInfoDTO mapToClassDTO(Model model, DataModelURI uri,
                                             Model orgModel,
                                             boolean hasRightToModel,
                                             Consumer<ResourceCommonInfoDTO> userMapper) {
        var dto = new ClassInfoDTO();
        var classResource = model.getResource(uri.getResourceURI());
        var modelResource = model.getResource(uri.getModelURI());

        DataModelMapperUtils.addCommonResourceDtoInfo(dto, classResource, modelResource, orgModel, hasRightToModel);
        MapperUtils.mapCreationInfo(dto, classResource, userMapper);

        var subClasses = MapperUtils.arrayPropertyToSet(classResource, RDFS.subClassOf);
        var equivalentClasses = MapperUtils.arrayPropertyToSet(classResource, OWL.equivalentClass);
        var disjointWith = MapperUtils.arrayPropertyToSet(classResource, OWL.disjointWith);

        dto.setSubClassOf(DataModelMapperUtils.uriToURIDTOs(subClasses, model));
        dto.setEquivalentClass(DataModelMapperUtils.uriToURIDTOs(equivalentClasses, model));
        dto.setDisjointWith(DataModelMapperUtils.uriToURIDTOs(disjointWith, model));

        return dto;
    }

    public static NodeShapeInfoDTO mapToNodeShapeDTO(Model model, DataModelURI uri,
                                                     Model orgModel,
                                                     boolean hasRightToModel,
                                                     Consumer<ResourceCommonInfoDTO> userMapper) {
        var dto = new NodeShapeInfoDTO();
        var nodeShapeURI = uri.getResourceURI();
        var nodeShapeResource = model.getResource(nodeShapeURI);
        var modelResource = model.getResource(uri.getModelURI());

        DataModelUtils.addPrefixesToModel(modelResource.getURI(), model);

        DataModelMapperUtils.addCommonResourceDtoInfo(dto, nodeShapeResource, modelResource, orgModel, hasRightToModel);
        MapperUtils.mapCreationInfo(dto, nodeShapeResource, userMapper);

        dto.setTargetClass(DataModelMapperUtils.uriToURIDTO(
                MapperUtils.propertyToString(nodeShapeResource, SH.targetClass), model));
        dto.setTargetNode(DataModelMapperUtils.uriToURIDTO(
                MapperUtils.propertyToString(nodeShapeResource, SH.node), model));
        dto.setApiPath(MapperUtils.propertyToString(nodeShapeResource, HTTP.API_PATH));
        return dto;
    }

    public static ExternalClassDTO mapExternalClassToDTO(Model result, String uri) {
        var resource = result.getResource(uri);

        var dto = new ExternalClassDTO();
        dto.setUri(uri);
        dto.setLabel(MapperUtils.localizedPropertyToMap(resource, RDFS.label));
        dto.setDescription(MapperUtils.localizedPropertyToMap(resource, RDFS.comment));

        var associations = new ArrayList<ExternalResourceDTO>();
        var attributes = new ArrayList<ExternalResourceDTO>();

        result.listSubjectsWithProperty(RDFS.domain).forEach(res -> {
            var resourceDTO = new ExternalResourceDTO();
            resourceDTO.setUri(res.getURI());
            resourceDTO.setLabel(MapperUtils.localizedPropertyToMap(res, RDFS.label));
            resourceDTO.setDescription(MapperUtils.localizedPropertyToMap(res, RDFS.comment));
            var resourceType = ResourceMapper.getExternalResourceType(res);
            if (ResourceType.ASSOCIATION.equals(resourceType)) {
                associations.add(resourceDTO);
            } else if (ResourceType.ATTRIBUTE.equals(resourceType)) {
                attributes.add(resourceDTO);
            }
        });
        dto.setAttributes(attributes);
        dto.setAssociations(associations);
        return dto;
    }

    public static void addClassResourcesToDTO(List<IndexResource> uriResult, Set<SimpleResourceDTO> restrictions, ClassInfoDTO dto) {
        for (var restriction : restrictions) {
            var uri = DataModelURI.fromURI(restriction.getUri());
            var resource = uriResult.stream()
                    .filter(result -> result.getId().equals(restriction.getUri()))
                    .findFirst();

            resource.ifPresent(res -> {
                var modelUri = res.getIsDefinedBy();
                if (modelUri != null && modelUri.startsWith(Constants.DATA_MODEL_NAMESPACE)) {
                    restriction.setModelId(uri.getModelId());
                    var versionIRI = res.getVersionIri();
                    if (versionIRI != null) {
                        restriction.setVersionIri(versionIRI);
                        restriction.setVersion(DataModelURI.fromURI(versionIRI).getVersion());
                    }
                }
                restriction.setCurie(res.getCurie());
                restriction.setLabel(res.getLabel());
                restriction.setIdentifier(res.getIdentifier());
                restriction.setNote(res.getNote());
                if (res.getResourceType().equals(ResourceType.ATTRIBUTE)) {
                    dto.getAttribute().add(restriction);
                } else if (res.getResourceType().equals(ResourceType.ASSOCIATION)) {
                    dto.getAssociation().add(restriction);
                }
            });
        }
    }

    public static void addClassResourcesToDTO(Model model, Set<SimpleResourceDTO> restrictions,
                                              ClassInfoDTO dto, Consumer<SimpleResourceDTO> subjectMapper) {
        var associations = new ArrayList<SimpleResourceDTO>();
        var attributes = new ArrayList<SimpleResourceDTO>();
        restrictions.forEach(restriction -> {
            var uri = DataModelURI.fromURI(restriction.getUri());
            var resource = model.getResource(uri.getResourceURI());
            var modelUri = MapperUtils.propertyToString(resource, RDFS.isDefinedBy);

            restriction.setIdentifier(MapperUtils.getLiteral(resource, DCTerms.identifier, String.class));
            restriction.setLabel(MapperUtils.localizedPropertyToMap(resource, RDFS.label));

            if (modelUri != null && modelUri.startsWith(Constants.DATA_MODEL_NAMESPACE)) {
                restriction.setModelId(uri.getModelId());
                var ns = uri.getModelURI();
                var versionIRI = MapperUtils.propertyToString(model.getResource(ns), OWL2.versionIRI);
                if (versionIRI != null) {
                    restriction.setVersionIri(versionIRI);
                    restriction.setVersion(DataModelURI.fromURI(versionIRI).getVersion());
                }
            }

            restriction.setNote(MapperUtils.localizedPropertyToMap(resource, RDFS.comment));
            restriction.setCurie(DataModelMapperUtils.uriToURIDTO(resource.getURI(), model).getCurie());
            var conceptDTO = new ConceptDTO();
            conceptDTO.setConceptURI(MapperUtils.propertyToString(resource, DCTerms.subject));
            restriction.setConcept(conceptDTO);
            subjectMapper.accept(restriction);

            if (MapperUtils.hasType(resource, OWL.DatatypeProperty, OWL.AnnotationProperty)) {
                attributes.add(restriction);
            } else if (MapperUtils.hasType(resource, OWL.ObjectProperty)) {
                associations.add(restriction);
            }

            dto.setAssociation(associations);
            dto.setAttribute(attributes);
        });
    }

    public static void addCurrentModelNodeShapeResources(Model model, Resource nodeShapeResource, NodeShapeInfoDTO nodeShapeDTO) {

        nodeShapeResource.listProperties(SH.property).forEach(p -> {
            var resource = model.getResource(p.getObject().toString());

            if (!resource.listProperties().hasNext()) {
                return;
            }

            var dto = new SimplePropertyShapeDTO();
            var uri = DataModelURI.fromURI(resource.getURI());

            dto.setUri(uri.getResourceURI());
            dto.setIdentifier(resource.getLocalName());
            dto.setModelId(uri.getModelId());
            dto.setCurie(uri.getCurie(model.getGraph().getPrefixMapping()));
            dto.setLabel(MapperUtils.localizedPropertyToMap(resource, RDFS.label));

            if (MapperUtils.hasType(resource, OWL.DatatypeProperty)) {
                nodeShapeDTO.getAttribute().add(dto);
            } else if (MapperUtils.hasType(resource, OWL.ObjectProperty)) {
                nodeShapeDTO.getAssociation().add(dto);
            }
        });
    }

    public static void addExternalNodeShapeResource(List<IndexResource> resources, NodeShapeInfoDTO nodeShapeDTO) {
        resources.forEach(r -> {
            var dto = new SimplePropertyShapeDTO();

            var modelURI = DataModelURI.fromURI(r.getUri());
            dto.setUri(r.getUri());
            dto.setIdentifier(r.getIdentifier());
            dto.setModelId(modelURI.getModelId());
            dto.setCurie(r.getCurie());
            dto.setLabel(r.getLabel());
            dto.setVersion(r.getFromVersion());

            if (r.getResourceType().equals(ResourceType.ATTRIBUTE)) {
                nodeShapeDTO.getAttribute().add(dto);
            } else if (r.getResourceType().equals(ResourceType.ASSOCIATION)) {
                nodeShapeDTO.getAssociation().add(dto);
            }
        });
    }

    public static void updateNodeShapeResourceRestrictions(Model model, List<SimplePropertyShapeDTO> properties, Set<String> shNodeProperties) {
        var deactivatedURIs = model.listSubjectsWithProperty(SH.deactivated)
                .mapWith(Resource::getURI).toList();

        properties.forEach(p -> {
            if (deactivatedURIs.contains(p.getUri())) {
                p.setDeactivated(true);
            }

            if (shNodeProperties.contains(p.getUri())) {
                p.setFromShNode(true);
            }
        });
    }

    public static void toggleAndMapDeactivatedProperty(Model model, String propertyURI, boolean external) {
        var resource = model.getResource(propertyURI);
        if (!external && !MapperUtils.hasType(resource, SH.PropertyShape, SH.NodeShape)) {
            throw new MappingError("Resource must be NodeShape or PropertyShape");
        }
        if (resource.hasProperty(SH.deactivated)) {
            resource.removeAll(SH.deactivated);
        } else {
            resource.addLiteral(SH.deactivated, true);
        }
    }

    public static void mapAppendNodeShapeProperty(Resource classResource, String propertyURI,
                                                  Set<String> restrictedProperties) {
        if (restrictedProperties.contains(propertyURI) || !MapperUtils.hasType(classResource, SH.NodeShape)) {
            throw new MappingError("Resource is not sh:PropertyShape or property already exists");
        }
        classResource.addProperty(SH.property, ResourceFactory.createResource(propertyURI));
    }

    public static void mapRemoveNodeShapeProperty(Model model, Resource classResource, String propertyURI,
                                                  Set<String> restrictedProperties) {
        if (restrictedProperties.contains(propertyURI) || !MapperUtils.hasType(classResource, SH.NodeShape)) {
            throw new MappingError("Resource is not sh:PropertyShape or property is added from sh:node reference");
        }
        model.remove(ResourceFactory.createStatement(classResource, SH.property, ResourceFactory.createResource(propertyURI)));
    }

    public static void mapClassRestrictionProperty(Model model, Resource classResource, Resource propertyResource) {

        var existingEqClassResource = classResource.listProperties(OWL.equivalentClass)
                .filterKeep(p -> p.getObject().isAnon())
                .mapWith(r -> r.getObject().asResource());

        var range = MapperUtils.propertyToString(propertyResource, RDFS.range);

        // Check duplicates. Duplicate attributes are not allowed, duplicate associations are allowed
        // as long as their target (owl:someValuesFrom) is different.
        // By default, association restriction's range is empty
        var hasDuplicate = getClassRestrictionList(model, classResource).stream().anyMatch(resource -> {
            var onProperty = MapperUtils.propertyToString(resource, OWL.onProperty);
            var someValuesFrom = MapperUtils.propertyToString(resource, OWL.someValuesFrom);

            return MapperUtils.hasType(propertyResource, OWL.ObjectProperty)
                    ? propertyResource.getURI().equals(onProperty) && someValuesFrom == null
                    : propertyResource.getURI().equals(onProperty);
        });

        if (hasDuplicate) {
            throw new MappingError(String.format("Restriction %s already exists in class %s", propertyResource.getURI(), classResource.getURI()));
        }

        var restrictionResource = model.createResource();
        restrictionResource.addProperty(RDF.type, OWL.Restriction);
        restrictionResource.addProperty(OWL.onProperty, propertyResource);

        if (range != null && !MapperUtils.hasType(propertyResource, OWL.ObjectProperty)) {
            restrictionResource.addProperty(OWL.someValuesFrom, ResourceFactory.createResource(range));
        }

        RDFList rdfList;

        if (existingEqClassResource.hasNext()) {
            var res = existingEqClassResource.next();
            rdfList = MapperUtils.getList(res, OWL.intersectionOf);
            rdfList.add(restrictionResource);
        } else {
            Resource equvalentClassResource = model.createResource();
            equvalentClassResource.addProperty(RDF.type, OWL.Class);

            rdfList = model.createList().cons(restrictionResource);
            equvalentClassResource.addProperty(OWL.intersectionOf, rdfList);

            classResource.addProperty(OWL.equivalentClass, equvalentClassResource);
        }
    }

    public static void mapRemoveClassRestrictionProperty(Model model, Resource classResource, Resource propertyResource, String target) {
        var eqResource = classResource.listProperties(OWL.equivalentClass)
                .filterKeep(p -> p.getObject().isAnon())
                .mapWith(r -> r.getObject().asResource());

        if (!eqResource.hasNext()) {
            throw new MappingError(String.format("No restrictions found from class %s", classResource.getURI()));
        }

        // construct new list of anonymous owl:restriction resources,
        // because RDFList.remove() doesn't work as expected in this scenario
        var eq = eqResource.next();
        var restrictionList = MapperUtils.getList(eq, OWL.intersectionOf);

        var remainingNodes = new ArrayList<RDFNode>();
        RDFNode removed = null;

        for (var rdfNode : restrictionList.asJavaList()) {
            String onProperty = MapperUtils.propertyToString(rdfNode.asResource(), OWL.onProperty);
            var someValuesFrom = MapperUtils.propertyToString(rdfNode.asResource(), OWL.someValuesFrom);

            // check that removed is still null, because in migrated data there might be more than one
            // association with the same uri and target
            if (removed == null && propertyResource.getURI().equals(onProperty) && (
                    MapperUtils.hasType(propertyResource, OWL.DatatypeProperty, OWL.AnnotationProperty)
                    || Objects.equals(target, someValuesFrom))
            ) {
                removed = rdfNode;
                continue;
            }
            remainingNodes.add(rdfNode);
        }

        if (removed == null) {
            throw new MappingError(String.format("Property %s not added to the class", propertyResource));
        }

        if (remainingNodes.isEmpty()) {
            model.removeAll(classResource, OWL.equivalentClass, eq);
            model.removeAll(eq, null, null);
        } else {
            eq.removeAll(OWL.intersectionOf);
            eq.addProperty(OWL.intersectionOf, model.createList(remainingNodes.iterator()));
        }

        model.removeAll(removed.asResource(), null, null);
        restrictionList.removeList();
    }

    public static void mapUpdateClassRestrictionProperty(Model model, Resource classResource, String restrictionURI,
                                                         String oldTarget, String newTarget, ResourceType resourceType) {
        var restrictions = getClassRestrictionList(model, classResource);
        if (restrictions.isEmpty()) {
            return;
        }

        // check for duplicates
        var hasDuplicate = restrictions.stream().anyMatch(r -> {
            var onProperty = MapperUtils.propertyToString(r, OWL.onProperty);
            var someValuesFrom = MapperUtils.propertyToString(r, OWL.someValuesFrom);

            return resourceType.equals(ResourceType.ASSOCIATION)
                    ? restrictionURI.equals(onProperty) && newTarget.equals(someValuesFrom)
                    : restrictionURI.equals(onProperty);
        });

        if (hasDuplicate) {
            throw new MappingError(String.format("Target %s already exists for restriction %s", newTarget, restrictionURI));
        }

        var updated = restrictions.stream().filter(r -> {
                    var onProperty = MapperUtils.propertyToString(r, OWL.onProperty);
                    var someValuesFrom = MapperUtils.propertyToString(r, OWL.someValuesFrom);

                    return oldTarget == null
                            ? restrictionURI.equals(onProperty) && someValuesFrom == null
                            : restrictionURI.equals(onProperty) && oldTarget.equals(someValuesFrom);
                })
                .findFirst()
                .orElseThrow(() ->
                        new MappingError(String.format("Restriction for %s not found with type %s", restrictionURI, oldTarget)));

        var modelResource = DataModelMapperUtils.getModelResourceFromVersion(model);
        DataModelMapperUtils.updateUriPropertyAndAddReferenceNamespaces(modelResource, updated, OWL.someValuesFrom, newTarget);
    }

    public static List<Resource> getClassRestrictionList(Model model, Resource classResource) {
        var eqResource = classResource.listProperties(OWL.equivalentClass)
                .filterKeep(p -> p.getObject().isAnon())
                .mapWith(r -> r.getObject().asResource());

        if (eqResource.hasNext()) {
            var eq = eqResource.next();
            return MapperUtils.getList(eq, OWL.intersectionOf)
                    .mapWith(RDFNode::asResource)
                    .toList();
        } else {
            return List.of();
        }
    }
}
