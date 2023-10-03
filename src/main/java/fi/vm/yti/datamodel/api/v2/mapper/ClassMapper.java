package fi.vm.yti.datamodel.api.v2.mapper;

import fi.vm.yti.datamodel.api.v2.dto.*;
import fi.vm.yti.datamodel.api.v2.endpoint.error.MappingError;
import fi.vm.yti.datamodel.api.v2.utils.DataModelUtils;
import fi.vm.yti.datamodel.api.v2.utils.SparqlUtils;
import fi.vm.yti.security.YtiUser;
import org.apache.jena.arq.querybuilder.ConstructBuilder;
import org.apache.jena.arq.querybuilder.WhereBuilder;
import org.apache.jena.datatypes.xsd.XSDDatatype;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.query.Query;
import org.apache.jena.rdf.model.*;
import org.apache.jena.vocabulary.DCTerms;
import org.apache.jena.vocabulary.OWL;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;
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

    public static String createOntologyClassAndMapToModel(String modelURI, Model model, ClassDTO dto, YtiUser user) {
        logger.info("Adding class to {}", modelURI);
        var modelResource = model.getResource(modelURI);

        var resource = MapperUtils.addCommonResourceInfo(model, modelURI, dto);
        MapperUtils.addCreationMetadata(resource, user);

        resource.addProperty(RDF.type, OWL.Class);

        var owlImports = MapperUtils.arrayPropertyToSet(modelResource, OWL.imports);
        var dcTermsRequires = MapperUtils.arrayPropertyToSet(modelResource, DCTerms.requires);
        //Equivalent class
        dto.getEquivalentClass().forEach(eq -> MapperUtils.addResourceRelationship(owlImports, dcTermsRequires, resource, OWL.equivalentClass, eq));

        if(dto.getSubClassOf().isEmpty()){
            resource.addProperty(RDFS.subClassOf, OWL.Thing); //Add OWL:Thing if nothing else is specified
        }else{
            dto.getSubClassOf().forEach(sub -> MapperUtils.addResourceRelationship(owlImports, dcTermsRequires, resource, RDFS.subClassOf, sub));
        }
        dto.getDisjointWith().forEach(disjoint -> MapperUtils.addResourceRelationship(owlImports, dcTermsRequires, resource, OWL.disjointWith, disjoint));

        modelResource.addProperty(DCTerms.hasPart, resource);
        return resource.getURI();
    }

    public static String createNodeShapeAndMapToModel(String modelURI, Model model, NodeShapeDTO dto, YtiUser user) {
        logger.info("Adding node shape to {}", modelURI);

        var nodeShapeResource = MapperUtils.addCommonResourceInfo(model, modelURI, dto);
        MapperUtils.addCreationMetadata(nodeShapeResource, user);

        nodeShapeResource.addProperty(RDF.type, SH.NodeShape);
        MapperUtils.addOptionalUriProperty(nodeShapeResource, SH.targetClass, dto.getTargetClass());
        MapperUtils.addOptionalUriProperty(nodeShapeResource, SH.node, dto.getTargetNode());

        return nodeShapeResource.getURI();
    }

    public static List<String> mapPlaceholderPropertyShapes(Model applicationProfileModel, String classURI,
                                                            Model propertiesModel, YtiUser user,
                                                            Predicate<String> checkFreeIdentifier) {
        var iterator = propertiesModel.listSubjects();
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
                if (label.getLanguage().equals("")) {
                    MapperUtils.addLocalizedProperty(Set.of("en"),
                            Map.of("en", label.getObject().toString()),
                            propertyShapeResource,
                            RDFS.label,
                            applicationProfileModel);
                } else {
                    propertyShapeResource.addProperty(RDFS.label, label.getObject());
                }
            }

            propertyShapeResource.addProperty(SH.path, ResourceFactory.createResource(uri))
                    .addProperty(DCTerms.identifier, ResourceFactory.createTypedLiteral(currentIdentifier, XSDDatatype.XSDNCName))
                    .addProperty(RDF.type, SH.PropertyShape)
                    .addProperty(RDF.type, targetResource.getProperty(RDF.type).getObject())
                    .addProperty(RDFS.isDefinedBy, classResource.getProperty(RDFS.isDefinedBy).getObject())
                    .addProperty(OWL.versionInfo, Status.DRAFT.name());

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

    public static void mapToUpdateOntologyClass(Model model, String graph, Resource classResource, ClassDTO classDTO, YtiUser user) {
        logger.info("Updating class in graph {}", graph);
        var modelResource = model.getResource(graph);

        MapperUtils.updateCommonResourceInfo(model, classResource, modelResource, classDTO);

        var owlImports = MapperUtils.arrayPropertyToSet(modelResource, OWL.imports);
        var dcTermsRequires = MapperUtils.arrayPropertyToSet(modelResource, DCTerms.requires);

        var equivalentClasses = classDTO.getEquivalentClass();

        var eqProps = classResource.listProperties(OWL.equivalentClass)
                .filterDrop(p -> p.getObject().isAnon())
                .toList();
        model.remove(eqProps);

        equivalentClasses.forEach(eq -> MapperUtils.addResourceRelationship(owlImports, dcTermsRequires, classResource, OWL.equivalentClass, eq));

        var subClassOf = classDTO.getSubClassOf();
        classResource.removeAll(RDFS.subClassOf);
        if(subClassOf.isEmpty()){
            classResource.addProperty(RDFS.subClassOf, OWL.Thing); //Add OWL:Thing if no subClassOf is specified
        }else{
            subClassOf.forEach(sub -> MapperUtils.addResourceRelationship(owlImports, dcTermsRequires, classResource, RDFS.subClassOf, sub));
        }

        classResource.removeAll(OWL.disjointWith);
        classDTO.getDisjointWith().forEach(disjoint -> MapperUtils.addResourceRelationship(owlImports, dcTermsRequires, classResource, OWL.disjointWith, disjoint));
        MapperUtils.addUpdateMetadata(classResource, user);
    }

    public static void mapToUpdateNodeShape(Model model, String graph, Resource classResource, NodeShapeDTO nodeShapeDTO, Set<String> properties, YtiUser user) {
        logger.info("Updating node shape in graph {}", graph);
        var modelResource = model.getResource(graph);

        MapperUtils.updateCommonResourceInfo(model, classResource, modelResource, nodeShapeDTO);

        if (nodeShapeDTO.getTargetClass() == null) {
            classResource.removeAll(SH.targetClass);
        } else {
            MapperUtils.updateUriProperty(classResource, SH.targetClass, nodeShapeDTO.getTargetClass());
        }

        if (nodeShapeDTO.getTargetNode() == null) {
            classResource.removeAll(SH.node);
        } else {
            MapperUtils.updateUriProperty(classResource, SH.node, nodeShapeDTO.getTargetNode());
        }

        classResource.removeAll(SH.property);
        mapNodeShapeProperties(model, classResource.getURI(), properties);

        MapperUtils.addUpdateMetadata(classResource, user);
    }

    /**
     * Map model with given prefix and class identifier
     *
     * @param model Model
     * @param modelUri Model uri
     * @param classIdentifier class identifier
     * @param orgModel Model of organisations
     * @param hasRightToModel does current user have right to model
     * @return Class DTO
     */
    public static ClassInfoDTO mapToClassDTO(Model model, String modelUri,
                                             String classIdentifier,
                                             Model orgModel,
                                             boolean hasRightToModel,
                                             Consumer<ResourceCommonDTO> userMapper){
        var dto = new ClassInfoDTO();
        var classUri = modelUri + ModelConstants.RESOURCE_SEPARATOR + classIdentifier;
        var classResource = model.getResource(classUri);
        var modelResource = model.getResource(modelUri);

        DataModelUtils.addPrefixesToModel(modelResource.getURI(), model);

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

    public static NodeShapeInfoDTO mapToNodeShapeDTO(Model model, String modelUri,
                                                     String identifier,
                                                     Model orgModel,
                                                     boolean hasRightToModel,
                                                     Consumer<ResourceCommonDTO> userMapper) {
        var dto = new NodeShapeInfoDTO();
        var nodeShapeURI = modelUri + ModelConstants.RESOURCE_SEPARATOR + identifier;
        var nodeShapeResource = model.getResource(nodeShapeURI);
        var modelResource = model.getResource(modelUri);

        DataModelUtils.addPrefixesToModel(modelUri, model);

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

    public static void addClassResourcesToDTO(Model classResources, ClassInfoDTO dto, Consumer<SimpleResourceDTO> subjectMapper){
        var associations = new ArrayList<SimpleResourceDTO>();
        var attributes = new ArrayList<SimpleResourceDTO>();
        classResources.listSubjects().forEach(res -> {
            var resDTO = new SimpleResourceDTO();
            resDTO.setUri(res.getURI());
            resDTO.setIdentifier(MapperUtils.getLiteral(res, DCTerms.identifier, String.class));
            resDTO.setLabel(MapperUtils.localizedPropertyToMap(res, RDFS.label));
            resDTO.setNote(MapperUtils.localizedPropertyToMap(res, RDFS.comment));
            resDTO.setCurie(MapperUtils.uriToURIDTO(res.getURI(), classResources).getCurie());
            var conceptDTO = new ConceptDTO();
            conceptDTO.setConceptURI(MapperUtils.propertyToString(res, DCTerms.subject));
            resDTO.setConcept(conceptDTO);
            subjectMapper.accept(resDTO);
            var modelUri = MapperUtils.propertyToString(res, RDFS.isDefinedBy);
            if(modelUri == null){
                throw new MappingError("ModelUri null for resource");
            }
            resDTO.setModelId(MapperUtils.getModelIdFromNamespace(modelUri));
            if (MapperUtils.hasType(res, OWL.DatatypeProperty)) {
                attributes.add(resDTO);
            } else if (MapperUtils.hasType(res, OWL.ObjectProperty)) {
                associations.add(resDTO);
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

            if(restrictedProperties.contains(resource.getURI())){
                dto.setFromShNode(true);
            }

            dto.setUri(resource.getURI());
            dto.setModelId(MapperUtils.getModelIdFromNamespace(modelUri));
            dto.setLabel(MapperUtils.localizedPropertyToMap(resource, RDFS.label));
            dto.setIdentifier(resource.getLocalName());
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

        var restrictionResource = model.createResource();
        restrictionResource.addProperty(RDF.type, OWL.Restriction);
        restrictionResource.addProperty(OWL.onProperty, propertyResource);

        if (propertyResource.hasProperty(RDFS.range)) {
            restrictionResource.addProperty(OWL.someValuesFrom,
                    propertyResource.getProperty(RDFS.range).getObject());
        }

        RDFList rdfList;

        if (existingEqClassResource.hasNext()) {
            var res = existingEqClassResource.next();
            rdfList = MapperUtils.getList(model, res, OWL.intersectionOf);

            if (rdfList.asJavaList().stream()
                    .anyMatch(r -> r.asResource().getProperty(OWL.onProperty).getObject().equals(propertyResource))) {
                throw new MappingError(String.format("Property %s already added", propertyResource.getURI()));
            }

            rdfList.add(restrictionResource);
        } else {
            Resource equvalentClassResource = model.createResource();
            equvalentClassResource.addProperty(RDF.type, OWL.Class);

            rdfList = model.createList().cons(restrictionResource);
            equvalentClassResource.addProperty(OWL.intersectionOf, rdfList);

            classResource.addProperty(OWL.equivalentClass, equvalentClassResource);
        }
    }

    public static void mapRemoveClassRestrictionProperty(Model model, Resource classResource, String propertyResource) {
        var eqResource = classResource.listProperties(OWL.equivalentClass)
                .filterKeep(p -> p.getObject().isAnon())
                .mapWith(r -> r.getObject().asResource());

        if (eqResource.hasNext() && propertyResource != null) {
            // construct new list of anonymous owl:restriction resources,
            // because RDFList.remove() doesn't work as expected in this scenario
            var eq = eqResource.next();
            var restrictionList = MapperUtils.getList(model, eq, OWL.intersectionOf);

            var remainingNodes = new ArrayList<RDFNode>();
            RDFNode removed = null;

            for (var rdfNode : restrictionList.asJavaList()) {
                if (propertyResource.equals(MapperUtils.propertyToString(rdfNode.asResource(), OWL.onProperty))) {
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
        } else {
            throw new MappingError(String.format("No restrictions found from class %s", classResource.getURI()));
        }
    }

    public static List<RDFNode> getClassRestrictionList(Model model, Resource classResource) {
        var eqResource = classResource.listProperties(OWL.equivalentClass)
                .filterKeep(p -> p.getObject().isAnon())
                .mapWith(r -> r.getObject().asResource());

        if (eqResource.hasNext()) {
            var eq = eqResource.next();
            var restrictionList = MapperUtils.getList(model, eq, OWL.intersectionOf);
            return restrictionList.asJavaList();
        } else {
            return List.of();
        }
    }
}
