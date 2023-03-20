package fi.vm.yti.datamodel.api.v2.mapper;

import fi.vm.yti.datamodel.api.v2.dto.*;
import org.apache.jena.datatypes.xsd.XSDDatatype;
import org.apache.jena.datatypes.xsd.XSDDateTime;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.vocabulary.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Calendar;

@Service
public class ClassMapper {

    private static final Logger logger = LoggerFactory.getLogger(ClassMapper.class);

    public String createClassAndMapToModel(String modelURI, Model model, ClassDTO dto){
        logger.info("Adding class to {}", modelURI);

        var creationDate = new XSDDateTime(Calendar.getInstance());
        var classUri = modelURI + "#" + dto.getIdentifier();
        var classResource = model.createResource(classUri)
                .addProperty(RDF.type, OWL.Class)
                .addProperty(OWL.versionInfo, dto.getStatus().name())
                .addProperty(DCTerms.modified, ResourceFactory.createTypedLiteral(creationDate))
                .addProperty(DCTerms.created, ResourceFactory.createTypedLiteral(creationDate))
                .addProperty(RDFS.isDefinedBy, modelURI);

        classResource.addProperty(DCTerms.identifier, ResourceFactory.createTypedLiteral(dto.getIdentifier(), XSDDatatype.XSDNCName));
        //Labels
        var modelResource = model.getResource(modelURI);
        var langs = MapperUtils.arrayPropertyToSet(modelResource, DCTerms.language);
        MapperUtils.addLocalizedProperty(langs, dto.getLabel(), classResource, RDFS.label, model);
        //Note
        MapperUtils.addLocalizedProperty(langs, dto.getNote(), classResource, SKOS.note, model);
        MapperUtils.addOptionalStringProperty(classResource, SKOS.editorialNote, dto.getEditorialNote());
        MapperUtils.addOptionalStringProperty(classResource, DCTerms.subject, dto.getSubject());

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

        modelResource.addProperty(DCTerms.hasPart, classUri);

        return classUri;
    }

    public void mapToUpdateClass(Model model, String graph, Resource classResource, ClassDTO classDTO) {
        logger.info("Updating class in graph {}", graph);
        var updateDate = new XSDDateTime(Calendar.getInstance());
        var modelResource = model.getResource(graph);
        var languages = MapperUtils.arrayPropertyToSet(modelResource, DCTerms.language);

        MapperUtils.updateLocalizedProperty(languages, classDTO.getLabel(), classResource, RDFS.label, model);
        MapperUtils.updateLocalizedProperty(languages, classDTO.getNote(), classResource, SKOS.note, model);
        MapperUtils.updateStringProperty(classResource, SKOS.editorialNote, classDTO.getEditorialNote());
        MapperUtils.updateStringProperty(classResource, DCTerms.subject, classDTO.getSubject());

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
        modelResource.removeAll(DCTerms.modified);
        modelResource.addProperty(DCTerms.modified, ResourceFactory.createTypedLiteral(updateDate));
    }

    /**
     * Map model with given prefix and class identifier
     *
     * @param modelPrefix     Model prefix
     * @param classIdentifier class identifier
     * @param model           Model
     * @return Class DTO
     */
    public ClassDTO mapToClassDTO(String modelPrefix, String classIdentifier, Model model, boolean hasRightToModel){
        var classDTO = new ClassDTO();
        var classUri = ModelConstants.SUOMI_FI_NAMESPACE + modelPrefix + "#" + classIdentifier;
        var classResource = model.getResource(classUri);
        classDTO.setLabel(MapperUtils.localizedPropertyToMap(classResource, RDFS.label));
        var status = Status.valueOf(classResource.getProperty(OWL.versionInfo).getObject().toString().toUpperCase());
        classDTO.setStatus(status);
        classDTO.setSubClassOf(MapperUtils.arrayPropertyToSet(classResource, RDFS.subClassOf));
        classDTO.setEquivalentClass(MapperUtils.arrayPropertyToSet(classResource, OWL.equivalentClass));
        classDTO.setSubject(MapperUtils.propertyToString(classResource, DCTerms.subject));
        classDTO.setIdentifier(classResource.getLocalName());
        classDTO.setNote(MapperUtils.localizedPropertyToMap(classResource, SKOS.note));
        if (hasRightToModel) {
            classDTO.setEditorialNote(MapperUtils.propertyToString(classResource, SKOS.editorialNote));
        }
        return classDTO;
    }

}
