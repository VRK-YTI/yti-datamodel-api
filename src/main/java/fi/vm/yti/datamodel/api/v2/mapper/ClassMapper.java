package fi.vm.yti.datamodel.api.v2.mapper;

import fi.vm.yti.datamodel.api.v2.dto.*;
import fi.vm.yti.datamodel.api.v2.endpoint.error.MappingError;
import fi.vm.yti.datamodel.api.v2.opensearch.index.IndexResource;
import fi.vm.yti.datamodel.api.v2.properties.SuomiMeta;
import fi.vm.yti.datamodel.api.v2.utils.DataModelURI;
import fi.vm.yti.datamodel.api.v2.utils.SparqlUtils;
import fi.vm.yti.security.YtiUser;
import org.apache.jena.arq.querybuilder.ConstructBuilder;
import org.apache.jena.arq.querybuilder.WhereBuilder;
import org.apache.jena.datatypes.xsd.XSDDatatype;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.query.Query;
import org.apache.jena.rdf.model.*;
import org.apache.jena.vocabulary.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.topbraid.shacl.vocabulary.SH;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Predicate;

public class ClassMapper {

    private ClassMapper(){
        //Static class
    }

    private static final Logger logger = LoggerFactory.getLogger(ClassMapper.class);

    public static void createOntologyClassAndMapToModel(DataModelURI uri, Model model, ClassDTO dto, YtiUser user) {
        logger.info("Adding class to {}", uri.getModelURI());
        var modelResource = model.getResource(uri.getModelURI());

        var resource = MapperUtils.addCommonResourceInfo(model, uri, dto);
        MapperUtils.addCreationMetadata(resource, user);

        resource.addProperty(RDF.type, OWL.Class);

        //Equivalent class
        dto.getEquivalentClass().forEach(eq -> MapperUtils.addResourceRelationship(modelResource, resource, OWL.equivalentClass, eq));

        if(dto.getSubClassOf().isEmpty()){
            resource.addProperty(RDFS.subClassOf, OWL.Thing); //Add OWL:Thing if nothing else is specified
        }else{
            dto.getSubClassOf().forEach(sub -> MapperUtils.addResourceRelationship(modelResource, resource, RDFS.subClassOf, sub));
        }
        dto.getDisjointWith().forEach(disjoint -> MapperUtils.addResourceRelationship(modelResource, resource, OWL.disjointWith, disjoint));
        modelResource.addProperty(DCTerms.hasPart, resource);

        MapperUtils.addTerminologyReference(dto, modelResource);
    }

    public static void createNodeShapeAndMapToModel(DataModelURI uri, Model model, NodeShapeDTO dto, YtiUser user) {
        logger.info("Adding node shape to {}", uri.getGraphURI());

        // var uri = DataModelURI.createResourceURI()
        var nodeShapeResource = MapperUtils.addCommonResourceInfo(model, uri, dto);
        MapperUtils.addCreationMetadata(nodeShapeResource, user);

        nodeShapeResource.addProperty(RDF.type, SH.NodeShape);

        var modelResource = model.getResource(uri.getModelURI());
        MapperUtils.addResourceRelationship(modelResource, nodeShapeResource, SH.targetClass, dto.getTargetClass());
        MapperUtils.addResourceRelationship(modelResource, nodeShapeResource, SH.node, dto.getTargetNode());

        MapperUtils.addTerminologyReference(dto, modelResource);
    }

    public static List<String> mapPlaceholderPropertyShapes(Model applicationProfileModel, String classURI,
                                                            Model propertiesModel, YtiUser user,
                                                            Predicate<String> checkFreeIdentifier) {
        var iterator = propertiesModel.listSubjectsWithProperty(RDFS.isDefinedBy);
        var classResource = applicationProfileModel.getResource(classURI);
        var propertyResourceURIs = new ArrayList<String>();
        while (iterator.hasNext()) {
            var uri = iterator.next().getURI();
            var identifier = NodeFactory.createURI(uri).getLocalName();

            var currentIdentifier = identifier;
            var count = 0;
            while (checkFreeIdentifier.test(classResource.getNameSpace() + currentIdentifier)) {
                currentIdentifier = identifier + String.format("-%d", ++count);
            }
            var targetResource = propertiesModel.getResource(uri);
            var propertyShapeResource = applicationProfileModel.createResource(classResource.getNameSpace() + currentIdentifier);
            var label = targetResource.getProperty(RDFS.label);

            if (label != null) {
                // external class labels are defined often in only one language
                if (label.getLanguage().isEmpty()) {
                    MapperUtils.addLocalizedProperty(Set.of("en"),
                            Map.of("en", label.getObject().toString()),
                            propertyShapeResource,
                            RDFS.label,
                            applicationProfileModel);
                } else {
                    propertyShapeResource.addProperty(RDFS.label, label.getObject());
                }
            }

            var u = NodeFactory.createURI(uri);
            var versionResource = propertiesModel.listSubjectsWithProperty(OWL2.versionIRI);
            String pathURI;
            if (versionResource.hasNext()) {
                pathURI = versionResource.next().getProperty(OWL2.versionIRI).getObject().toString() + u.getLocalName();
            } else {
                pathURI = uri;
            }
            propertyShapeResource.addProperty(SH.path, ResourceFactory.createResource(pathURI))
                    .addProperty(DCTerms.identifier, ResourceFactory.createTypedLiteral(currentIdentifier, XSDDatatype.XSDNCName))
                    .addProperty(RDF.type, SH.PropertyShape)
                    .addProperty(RDF.type, targetResource.getProperty(RDF.type).getObject())
                    .addProperty(RDFS.isDefinedBy, classResource.getProperty(RDFS.isDefinedBy).getObject())
                    .addProperty(SuomiMeta.publicationStatus, ResourceFactory.createResource(MapperUtils.getStatusUri(Status.DRAFT)));

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

        MapperUtils.updateCommonResourceInfo(model, classResource, modelResource, classDTO);

        var eqProps = classResource.listProperties(OWL.equivalentClass)
                .filterDrop(p -> p.getObject().isAnon())
                .toList();
        model.remove(eqProps);

        classDTO.getEquivalentClass().forEach(eq ->
                MapperUtils.addResourceRelationship(modelResource, classResource, OWL.equivalentClass, eq));

        var subClassOf = classDTO.getSubClassOf();
        classResource.removeAll(RDFS.subClassOf);
        if(subClassOf.isEmpty()){
            classResource.addProperty(RDFS.subClassOf, OWL.Thing); //Add OWL:Thing if no subClassOf is specified
        }else{
            subClassOf.forEach(sub -> MapperUtils.addResourceRelationship(modelResource, classResource, RDFS.subClassOf, sub));
        }

        classResource.removeAll(OWL.disjointWith);
        classDTO.getDisjointWith().forEach(disjoint ->
                MapperUtils.addResourceRelationship(modelResource, classResource, OWL.disjointWith, disjoint));
        MapperUtils.addUpdateMetadata(classResource, user);
    }

    public static void mapToUpdateNodeShape(Model model, String graph, Resource classResource, NodeShapeDTO nodeShapeDTO, Set<String> properties, YtiUser user) {
        logger.info("Updating node shape in graph {}", graph);
        var modelResource = model.getResource(graph);

        MapperUtils.updateCommonResourceInfo(model, classResource, modelResource, nodeShapeDTO);

        if (nodeShapeDTO.getTargetClass() == null) {
            classResource.removeAll(SH.targetClass);
        } else {
            MapperUtils.updateUriPropertyAndAddReferenceNamespaces(modelResource, classResource, SH.targetClass, nodeShapeDTO.getTargetClass());
        }

        if (nodeShapeDTO.getTargetNode() == null) {
            classResource.removeAll(SH.node);
        } else {
            MapperUtils.updateUriPropertyAndAddReferenceNamespaces(modelResource, classResource, SH.node, nodeShapeDTO.getTargetNode());
        }

        classResource.removeAll(SH.property);
        mapNodeShapeProperties(model, classResource.getURI(), properties);

        MapperUtils.addUpdateMetadata(classResource, user);
    }

    /**
     * Map model with given prefix and class identifier
     *
     * @param model Model
     * @param uri DataModelURI
     * @param orgModel Model of organisations
     * @param hasRightToModel does current user have right to model
     * @return Class DTO
     */
    public static ClassInfoDTO mapToClassDTO(Model model, DataModelURI uri,
                                             Model orgModel,
                                             boolean hasRightToModel,
                                             Consumer<ResourceCommonDTO> userMapper){
        var dto = new ClassInfoDTO();
        var classResource = model.getResource(uri.getResourceURI());
        var modelResource = model.getResource(uri.getModelURI());

        MapperUtils.addCommonResourceDtoInfo(dto, classResource, modelResource, orgModel, hasRightToModel);
        MapperUtils.mapCreationInfo(dto, classResource, userMapper);

        var subClasses = MapperUtils.arrayPropertyToSet(classResource, RDFS.subClassOf);
        var equivalentClasses = MapperUtils.arrayPropertyToSet(classResource, OWL.equivalentClass);
        var disjointWith = MapperUtils.arrayPropertyToSet(classResource, OWL.disjointWith);

        dto.setSubClassOf(MapperUtils.uriToURIDTOs(subClasses, model));
        dto.setEquivalentClass(MapperUtils.uriToURIDTOs(equivalentClasses, model));
        dto.setDisjointWith(MapperUtils.uriToURIDTOs(disjointWith, model));

        return dto;
    }

    public static NodeShapeInfoDTO mapToNodeShapeDTO(Model model, DataModelURI uri,
                                                     Model orgModel,
                                                     boolean hasRightToModel,
                                                     Consumer<ResourceCommonDTO> userMapper) {
        var dto = new NodeShapeInfoDTO();
        var nodeShapeURI = uri.getResourceURI();
        var nodeShapeResource = model.getResource(nodeShapeURI);
        var modelResource = model.getResource(uri.getModelURI());

        MapperUtils.addCommonResourceDtoInfo(dto, nodeShapeResource, modelResource, orgModel, hasRightToModel);
        MapperUtils.mapCreationInfo(dto, nodeShapeResource, userMapper);

        dto.setTargetClass(MapperUtils.uriToURIDTO(
                MapperUtils.propertyToString(nodeShapeResource, SH.targetClass), model));
        dto.setTargetNode(MapperUtils.uriToURIDTO(
                MapperUtils.propertyToString(nodeShapeResource, SH.node), model));

        return dto;
    }

    public static ExternalClassDTO mapExternalClassToDTO(Model model, String uri) {
        var resource = model.getResource(uri);

        var dto = new ExternalClassDTO();
        dto.setUri(uri);
        dto.setLabel(MapperUtils.localizedPropertyToMap(resource, RDFS.label));
        return dto;
    }

    public static Query getClassResourcesQuery(String classUri, boolean isExternal){
        var constructBuilder = new ConstructBuilder();
        var resourceName = "?resource";
        var uri = NodeFactory.createURI(classUri);
        SparqlUtils.addConstructProperty(resourceName, constructBuilder, RDF.type, "?type");
        SparqlUtils.addConstructProperty(resourceName, constructBuilder, RDFS.label, "?label");
        SparqlUtils.addConstructOptional(resourceName, constructBuilder, RDFS.comment, "?note");
        SparqlUtils.addConstructOptional(resourceName, constructBuilder, DCTerms.subject, "?subject");
        if (!isExternal) {
            SparqlUtils.addConstructProperty(resourceName, constructBuilder, DCTerms.identifier, "?identifier");
        }
        SparqlUtils.addConstructProperty(resourceName, constructBuilder, RDFS.isDefinedBy, "?isDefinedBy");
        var domainQuery = new WhereBuilder().addWhere(resourceName, RDFS.domain, uri)
                .addWhere(resourceName, RDFS.domain, "?domain");
        var rangeQuery = new WhereBuilder().addWhere(resourceName, RDFS.range, uri)
                .addWhere(resourceName, RDFS.range, "?range");
        constructBuilder.addWhere(domainQuery.addUnion(rangeQuery))
                .addConstruct(resourceName, RDFS.range, "?range")
                .addConstruct(resourceName, RDFS.domain, "?domain");
        return constructBuilder.build();
    }

    public static Query getNodeShapeResourcesQuery(String nodeShapeURI) {
        var constructBuilder = new ConstructBuilder();
        var resourceName = "?resource";
        var uri = NodeFactory.createURI(nodeShapeURI);
        SparqlUtils.addConstructProperty(resourceName, constructBuilder, RDF.type, "?type");
        SparqlUtils.addConstructProperty(resourceName, constructBuilder, RDFS.label, "?label");
        SparqlUtils.addConstructOptional(resourceName, constructBuilder, DCTerms.identifier, "?identifier");
        SparqlUtils.addConstructProperty(resourceName, constructBuilder, RDFS.isDefinedBy, "?isDefinedBy");
        constructBuilder.addWhere(uri, SH.property, resourceName);

        return constructBuilder.build();
    }

    public static void addClassResourcesToDTO(List<IndexResource> uriResult, Set<SimpleResourceDTO> restrictions, ClassInfoDTO dto) {
        for (var restriction : restrictions) {
            var uri = DataModelURI.fromURI(restriction.getUri());
            var resource = uriResult.stream()
                    .filter(result -> result.getId().equals(restriction.getUri()))
                    .findFirst();

            resource.ifPresent(res -> {
                var modelUri = res.getIsDefinedBy();
                if (modelUri != null && modelUri.startsWith(ModelConstants.SUOMI_FI_NAMESPACE)) {
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

            if (modelUri != null && modelUri.startsWith(ModelConstants.SUOMI_FI_NAMESPACE)) {
                restriction.setModelId(uri.getModelId());
                var ns = uri.getModelURI();
                var versionIRI = MapperUtils.propertyToString(model.getResource(ns), OWL2.versionIRI);
                if (versionIRI != null) {
                    restriction.setVersionIri(versionIRI);
                    restriction.setVersion(DataModelURI.fromURI(versionIRI).getVersion());
                }
            }

            restriction.setNote(MapperUtils.localizedPropertyToMap(resource, RDFS.comment));
            restriction.setCurie(MapperUtils.uriToURIDTO(resource.getURI(), model).getCurie());
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

    public static void addNodeShapeResourcesToDTO(Model model, Model propertyShapeResources, NodeShapeInfoDTO nodeShapeDTO, Set<String> restrictedProperties) {
        var deactivatedURIs = model.listSubjectsWithProperty(SH.deactivated)
                .mapWith(Resource::getURI).toList();

        propertyShapeResources.listSubjects().forEach(resource -> {
            var dto = new SimplePropertyShapeDTO();

            var modelUri = MapperUtils.propertyToString(resource, RDFS.isDefinedBy);
            if (modelUri == null) {
                throw new MappingError("ModelUri null for resource");
            }

            var uri = DataModelURI.fromURI(modelUri);
            if(restrictedProperties.contains(resource.getURI())){
                dto.setFromShNode(true);
            }

            dto.setUri(resource.getURI());
            dto.setModelId(uri.getModelId());
            dto.setLabel(MapperUtils.localizedPropertyToMap(resource, RDFS.label));
            dto.setIdentifier(resource.getLocalName());
            dto.setCurie(String.format("%s:%s", uri.getModelId(), resource.getLocalName()));
            dto.setDeactivated(deactivatedURIs.contains(resource.getURI()));
            if (MapperUtils.hasType(resource, OWL.DatatypeProperty)) {
                nodeShapeDTO.getAttribute().add(dto);
            } else if (MapperUtils.hasType(resource, OWL.ObjectProperty)) {
                nodeShapeDTO.getAssociation().add(dto);
            }
        });
    }

    public static void addExternalClassResourcesToDTO(Model classResources, ExternalClassDTO dto) {
        var associations = new ArrayList<ExternalResourceDTO>();
        var attributes = new ArrayList<ExternalResourceDTO>();

        classResources.listSubjects().forEach(res -> {
            var resourceDTO = new ExternalResourceDTO();
            resourceDTO.setUri(res.getURI());
            resourceDTO.setLabel(MapperUtils.localizedPropertyToMap(res, RDFS.label));
            if (MapperUtils.hasType(res, OWL.ObjectProperty)) {
                associations.add(resourceDTO);
            } else if (MapperUtils.hasType(res, OWL.DatatypeProperty)) {
                attributes.add(resourceDTO);
            }
        });
        dto.setAttributes(attributes);
        dto.setAssociations(associations);
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

        // TODO after migration: enable type check
        if (range != null) { //  && !MapperUtils.hasType(propertyResource, OWL.ObjectProperty)) {
            restrictionResource.addProperty(OWL.someValuesFrom, ResourceFactory.createResource(range));
        }

        RDFList rdfList;

        if (existingEqClassResource.hasNext()) {
            var res = existingEqClassResource.next();
            rdfList = MapperUtils.getList(model, res, OWL.intersectionOf);
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
        var restrictionList = MapperUtils.getList(model, eq, OWL.intersectionOf);

        var remainingNodes = new ArrayList<RDFNode>();
        RDFNode removed = null;

        for (var rdfNode : restrictionList.asJavaList()) {
            String onProperty = MapperUtils.propertyToString(rdfNode.asResource(), OWL.onProperty);
            var someValuesFrom = MapperUtils.propertyToString(rdfNode.asResource(), OWL.someValuesFrom);

            if (propertyResource.getURI().equals(onProperty) && (
                    MapperUtils.hasType(propertyResource, OWL.DatatypeProperty) || Objects.equals(target, someValuesFrom))
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

        var modelResource = MapperUtils.getModelResourceFromVersion(model);
        MapperUtils.updateUriPropertyAndAddReferenceNamespaces(modelResource, updated, OWL.someValuesFrom, newTarget);
    }

    public static List<Resource> getClassRestrictionList(Model model, Resource classResource) {
        var eqResource = classResource.listProperties(OWL.equivalentClass)
                .filterKeep(p -> p.getObject().isAnon())
                .mapWith(r -> r.getObject().asResource());

        if (eqResource.hasNext()) {
            var eq = eqResource.next();
            return MapperUtils.getList(model, eq, OWL.intersectionOf)
                    .mapWith(RDFNode::asResource)
                    .toList();
        } else {
            return List.of();
        }
    }
}
