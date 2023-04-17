package fi.vm.yti.datamodel.api.v2.mapper;

import fi.vm.yti.datamodel.api.v2.dto.*;
import fi.vm.yti.datamodel.api.v2.endpoint.error.MappingError;
import fi.vm.yti.datamodel.api.v2.utils.SparqlUtils;
import fi.vm.yti.security.YtiUser;
import org.apache.jena.arq.querybuilder.ConstructBuilder;
import org.apache.jena.arq.querybuilder.WhereBuilder;
import org.apache.jena.datatypes.xsd.XSDDatatype;
import org.apache.jena.datatypes.xsd.XSDDateTime;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.query.Query;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.vocabulary.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.function.Consumer;

public class ClassMapper {

    private ClassMapper(){
        //Static class
    }

    private static final Logger logger = LoggerFactory.getLogger(ClassMapper.class);

    public static String createClassAndMapToModel(String modelURI, Model model, ClassDTO dto, YtiUser user){
        logger.info("Adding class to {}", modelURI);

        var creationDate = new XSDDateTime(Calendar.getInstance());
        var classUri = modelURI + "#" + dto.getIdentifier();
        var classResource = model.createResource(classUri)
                .addProperty(RDF.type, OWL.Class)
                .addProperty(OWL.versionInfo, dto.getStatus().name())
                .addProperty(DCTerms.modified, ResourceFactory.createTypedLiteral(creationDate))
                .addProperty(DCTerms.created, ResourceFactory.createTypedLiteral(creationDate))
                .addProperty(Iow.creator, user.getId().toString())
                .addProperty(Iow.modifier, user.getId().toString());


        classResource.addProperty(RDFS.isDefinedBy, ResourceFactory.createResource(modelURI));
        classResource.addProperty(DCTerms.identifier, ResourceFactory.createTypedLiteral(dto.getIdentifier(), XSDDatatype.XSDNCName));
        //Labels
        var modelResource = model.getResource(modelURI);
        var langs = MapperUtils.arrayPropertyToSet(modelResource, DCTerms.language);
        MapperUtils.addLocalizedProperty(langs, dto.getLabel(), classResource, RDFS.label, model);
        //Note
        MapperUtils.addLocalizedProperty(langs, dto.getNote(), classResource, SKOS.note, model);
        MapperUtils.addOptionalStringProperty(classResource, SKOS.editorialNote, dto.getEditorialNote());
        MapperUtils.addOptionalUriProperty(classResource, DCTerms.subject, dto.getSubject());

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

        modelResource.addProperty(DCTerms.hasPart, classResource);

        return classUri;
    }

    public static void mapToUpdateClass(Model model, String graph, Resource classResource, ClassDTO classDTO, YtiUser user) {
        logger.info("Updating class in graph {}", graph);
        var updateDate = new XSDDateTime(Calendar.getInstance());
        var modelResource = model.getResource(graph);
        var languages = MapperUtils.arrayPropertyToSet(modelResource, DCTerms.language);

        MapperUtils.updateLocalizedProperty(languages, classDTO.getLabel(), classResource, RDFS.label, model);
        MapperUtils.updateLocalizedProperty(languages, classDTO.getNote(), classResource, SKOS.note, model);
        MapperUtils.updateStringProperty(classResource, SKOS.editorialNote, classDTO.getEditorialNote());
        MapperUtils.updateUriProperty(classResource, DCTerms.subject, classDTO.getSubject());

        var status = classDTO.getStatus();
        if (status != null) {
            MapperUtils.updateStringProperty(classResource, OWL.versionInfo, status.name());
        }

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
        classResource.removeAll(DCTerms.modified);
        classResource.addProperty(DCTerms.modified, ResourceFactory.createTypedLiteral(updateDate));
        classResource.removeAll(Iow.modifier);
        classResource.addProperty(Iow.modifier, user.getId().toString());
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
        dto.setNote(MapperUtils.localizedPropertyToMap(classResource, SKOS.note));
        if (hasRightToModel) {
            dto.setEditorialNote(MapperUtils.propertyToString(classResource, SKOS.editorialNote));
        }

        var created = classResource.getProperty(DCTerms.created).getLiteral().getString();
        var modified = classResource.getProperty(DCTerms.modified).getLiteral().getString();
        dto.setCreated(created);
        dto.setModified(modified);
        var modelResource = model.getResource(modelUri);
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

    public static Query getClassResourcesQuery(String classUri){
        var constructBuilder = new ConstructBuilder();
        var resourceName = "?resource";
        var uri = NodeFactory.createURI(classUri);
        SparqlUtils.addConstructProperty(resourceName, constructBuilder, RDF.type, "?type");
        SparqlUtils.addConstructProperty(resourceName, constructBuilder, RDFS.label, "?label");
        SparqlUtils.addConstructProperty(resourceName, constructBuilder, DCTerms.identifier, "?identifier");
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

}
