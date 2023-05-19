package fi.vm.yti.datamodel.api.v2.mapper;

import fi.vm.yti.datamodel.api.v2.dto.*;
import fi.vm.yti.datamodel.api.v2.endpoint.error.MappingError;
import fi.vm.yti.datamodel.api.v2.utils.SparqlUtils;
import fi.vm.yti.security.YtiUser;
import org.apache.jena.arq.querybuilder.ConstructBuilder;
import org.apache.jena.arq.querybuilder.WhereBuilder;
import org.apache.jena.datatypes.xsd.XSDDatatype;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.query.Query;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.vocabulary.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.topbraid.shacl.vocabulary.SH;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

public class ClassMapper {

    private ClassMapper(){
        //Static class
    }

    private static final Logger logger = LoggerFactory.getLogger(ClassMapper.class);

    public static String createClassAndMapToModel(String modelURI, Model model, ClassDTO dto, YtiUser user){
        logger.info("Adding class to {}", modelURI);
        var modelResource = model.getResource(modelURI);
        var classUri = modelURI + "#" + dto.getIdentifier();
        var classResource = model.createResource(classUri)
                .addProperty(OWL.versionInfo, dto.getStatus().name());

        MapperUtils.addCreationMetadata(classResource, user);

        var langs = MapperUtils.arrayPropertyToSet(modelResource, DCTerms.language);

        classResource.addProperty(RDFS.isDefinedBy, modelResource);
        classResource.addProperty(DCTerms.identifier, ResourceFactory.createTypedLiteral(dto.getIdentifier(), XSDDatatype.XSDNCName));
        //Labels
        MapperUtils.addLocalizedProperty(langs, dto.getLabel(), classResource, RDFS.label, model);
        //Concept from terminology
        MapperUtils.addOptionalUriProperty(classResource, DCTerms.subject, dto.getSubject());

        //Editorial note, not visible for unauthenticated users
        MapperUtils.addOptionalStringProperty(classResource, SKOS.editorialNote, dto.getEditorialNote());

        var modelType = MapperUtils.getModelTypeFromResource(modelResource);
        if(modelType.equals(ModelType.LIBRARY)){
            classResource.addProperty(RDF.type, OWL.Class);
            MapperUtils.addLocalizedProperty(langs, dto.getNote(), classResource, SKOS.note, model);

            var owlImports = MapperUtils.arrayPropertyToSet(modelResource, OWL.imports);
            var dcTermsRequires = MapperUtils.arrayPropertyToSet(modelResource, DCTerms.requires);
            //Equivalent class
            if(dto.getEquivalentClass() != null){
                dto.getEquivalentClass().forEach(eq -> MapperUtils.addResourceRelationship(owlImports, dcTermsRequires, classResource, OWL.equivalentClass, eq));
            }
            //Sub Class
            if(dto.getSubClassOf() == null || dto.getSubClassOf().isEmpty()){
                classResource.addProperty(RDFS.subClassOf, OWL.Thing); //Add OWL:Thing if nothing else is specified
            }else{
                dto.getSubClassOf().forEach(sub -> MapperUtils.addResourceRelationship(owlImports, dcTermsRequires, classResource, RDFS.subClassOf, sub));
            }
        }else{
            classResource.addProperty(RDF.type, SH.NodeShape);
            classResource.addProperty(SH.targetClass, ResourceFactory.createResource(dto.getTargetClass()));
            classResource.addProperty(SH.node, ResourceFactory.createResource(dto.getTargetNode()));
            MapperUtils.addLocalizedProperty(langs, dto.getNote(), classResource, SH.description, model);
        }

        modelResource.addProperty(DCTerms.hasPart, classResource);

        return classUri;
    }

    public static List<String> mapPlaceholderPropertyShapes(Model applicationProfileModel, String classURI,
                                                            Model propertiesModel, YtiUser user) {
        var iterator = propertiesModel.listSubjects();
        var classResource = applicationProfileModel.getResource(classURI);
        var propertyResourceURIs = new ArrayList<String>();
        while (iterator.hasNext()) {
            var uri = iterator.next().getURI();
            var identifier = NodeFactory.createURI(uri).getLocalName();
            var targetResource = propertiesModel.getResource(uri);
            var propertyShapeResource = applicationProfileModel.createResource(classResource.getNameSpace() + identifier);
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
                    .addProperty(DCTerms.identifier, ResourceFactory.createTypedLiteral(identifier, XSDDatatype.XSDNCName))
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

    public static void mapToUpdateClass(Model model, String graph, Resource classResource, ClassDTO classDTO, YtiUser user) {
        logger.info("Updating class in graph {}", graph);
        var modelResource = model.getResource(graph);
        var languages = MapperUtils.arrayPropertyToSet(modelResource, DCTerms.language);
        MapperUtils.updateStringProperty(classResource, SKOS.editorialNote, classDTO.getEditorialNote());

        var modelType = MapperUtils.getModelTypeFromResource(modelResource);
        if(modelType.equals(ModelType.LIBRARY)){
            MapperUtils.updateLocalizedProperty(languages, classDTO.getNote(), classResource, SKOS.note, model);

            var owlImports = MapperUtils.arrayPropertyToSet(modelResource, OWL.imports);
            var dcTermsRequires = MapperUtils.arrayPropertyToSet(modelResource, DCTerms.requires);

            var equivalentClasses = classDTO.getEquivalentClass();
            if(equivalentClasses != null){
                classResource.removeAll(OWL.equivalentClass);
                equivalentClasses.forEach(eq -> MapperUtils.addResourceRelationship(owlImports, dcTermsRequires, classResource, OWL.equivalentClass, eq));
            }

            var subClassOf = classDTO.getSubClassOf();
            if (subClassOf != null){
                classResource.removeAll(RDFS.subClassOf);
                if(subClassOf.isEmpty()){
                    classResource.addProperty(RDFS.subClassOf, OWL.Thing); //Add OWL:Thing if no subClassOf is specified
                }else{
                    subClassOf.forEach(sub -> MapperUtils.addResourceRelationship(owlImports, dcTermsRequires, classResource, RDFS.subClassOf, sub));
                }
            }
        }else{
            MapperUtils.updateUriProperty(classResource, SH.targetClass, classDTO.getTargetClass());
            MapperUtils.updateLocalizedProperty(languages, classDTO.getNote(), classResource, SH.description, model);
        }
        MapperUtils.updateLocalizedProperty(languages, classDTO.getLabel(), classResource, RDFS.label, model);
        MapperUtils.updateUriProperty(classResource, DCTerms.subject, classDTO.getSubject());

        var status = classDTO.getStatus();
        if (status != null) {
            MapperUtils.updateStringProperty(classResource, OWL.versionInfo, status.name());
        }

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
                                             Consumer<ResourceInfoBaseDTO> userMapper){
        var dto = new ClassInfoDTO();
        var classUri = modelUri + "#" + classIdentifier;
        var classResource = model.getResource(classUri);
        dto.setUri(classUri);
        dto.setLabel(MapperUtils.localizedPropertyToMap(classResource, RDFS.label));
        dto.setStatus(Status.valueOf(MapperUtils.propertyToString(classResource, OWL.versionInfo)));
        dto.setSubClassOf(MapperUtils.arrayPropertyToSet(classResource, RDFS.subClassOf));
        dto.setEquivalentClass(MapperUtils.arrayPropertyToSet(classResource, OWL.equivalentClass));
        var subject = MapperUtils.propertyToString(classResource, DCTerms.subject);
        if (subject != null) {
            var conceptDTO = new ConceptDTO();
            conceptDTO.setConceptURI(subject);
            dto.setSubject(conceptDTO);
        }
        dto.setIdentifier(classResource.getLocalName());
        if (hasRightToModel) {
            dto.setEditorialNote(MapperUtils.propertyToString(classResource, SKOS.editorialNote));
        }
        dto.setTargetClass(MapperUtils.propertyToString(classResource, SH.targetClass));

        var created = classResource.getProperty(DCTerms.created).getLiteral().getString();
        var modified = classResource.getProperty(DCTerms.modified).getLiteral().getString();
        dto.setCreated(created);
        dto.setModified(modified);
        var modelResource = model.getResource(modelUri);
        var modelType = MapperUtils.getModelTypeFromResource(modelResource);
        if(modelType.equals(ModelType.LIBRARY)){
            dto.setNote(MapperUtils.localizedPropertyToMap(classResource, SKOS.note));
            MapperUtils.addOptionalStringProperty(classResource, SKOS.editorialNote, dto.getEditorialNote());
        }else{
            dto.setNote(MapperUtils.localizedPropertyToMap(classResource, SH.description));
            MapperUtils.addOptionalStringProperty(classResource, DCTerms.description, dto.getEditorialNote());
        }
        var contributors = MapperUtils.arrayPropertyToSet(modelResource, DCTerms.contributor);
        dto.setContributor(OrganizationMapper.mapOrganizationsToDTO(contributors, orgModel));
        dto.setContact(MapperUtils.propertyToString(modelResource, Iow.contact));
        dto.setCreator(new UserDTO(MapperUtils.propertyToString(modelResource, Iow.creator)));
        dto.setModifier(new UserDTO(MapperUtils.propertyToString(modelResource, Iow.modifier)));

        if (userMapper != null) {
            userMapper.accept(dto);
        }
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

    public static void addClassResourcesToDTO(Model classResources, ClassInfoDTO dto){
        var associations = new ArrayList<SimpleResourceDTO>();
        var attributes = new ArrayList<SimpleResourceDTO>();
        classResources.listSubjects().forEach(res -> {
            var resDTO = new SimpleResourceDTO();
            resDTO.setUri(res.getURI());
            resDTO.setIdentifier(res.getProperty(DCTerms.identifier).getString());
            resDTO.setLabel(MapperUtils.localizedPropertyToMap(res, RDFS.label));
            var modelUri = MapperUtils.propertyToString(res, RDFS.isDefinedBy);
            if(modelUri == null){
                throw new MappingError("ModelUri null for resource");
            }
            resDTO.setModelId(MapperUtils.getModelIdFromNamespace(modelUri));
            var type = res.getProperty(RDF.type).getResource();
            if(type.equals(OWL.ObjectProperty)){
                associations.add(resDTO);
            }else if(type.equals(OWL.DatatypeProperty)){
                attributes.add(resDTO);
            }
            dto.setAssociation(associations);
            dto.setAttribute(attributes);
        });
    }

    public static void addNodeShapeResourcesToDTO(Model model, ClassInfoDTO classDto) {
        var propertyShapeURIs = model.getResource(classDto.getUri())
                .listProperties(SH.property).toList().stream()
                .map(p -> p.getObject().toString())
                .toList();
        propertyShapeURIs.forEach(uri -> {
            var resource = model.getResource(uri);
            var resDTO = new SimpleResourceDTO();
            resDTO.setLabel(MapperUtils.localizedPropertyToMap(resource, RDFS.label));
            resDTO.setIdentifier(NodeFactory.createURI(uri).getLocalName());
            resDTO.setUri(MapperUtils.propertyToString(resource, RDFS.isDefinedBy));

            if (MapperUtils.hasType(resource, OWL.DatatypeProperty)) {
                classDto.getAttribute().add(resDTO);
            } else if (MapperUtils.hasType(resource, OWL.ObjectProperty)) {
                classDto.getAssociation().add(resDTO);
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

}
