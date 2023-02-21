package fi.vm.yti.datamodel.api.v2.mapper;

import fi.vm.yti.datamodel.api.security.AuthorizationManager;
import fi.vm.yti.datamodel.api.v2.dto.ClassDTO;
import fi.vm.yti.datamodel.api.v2.dto.Iow;
import fi.vm.yti.datamodel.api.v2.dto.ModelConstants;
import fi.vm.yti.datamodel.api.v2.dto.Status;
import fi.vm.yti.datamodel.api.v2.opensearch.index.IndexClass;
import fi.vm.yti.datamodel.api.v2.endpoint.error.MappingError;
import fi.vm.yti.datamodel.api.v2.endpoint.error.ResourceNotFoundException;
import fi.vm.yti.datamodel.api.v2.service.JenaService;
import org.apache.jena.datatypes.xsd.XSDDatatype;
import org.apache.jena.datatypes.xsd.XSDDateTime;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.vocabulary.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Calendar;

@Service
public class ClassMapper {

    private static final Logger logger = LoggerFactory.getLogger(ClassMapper.class);

    private final JenaService jenaService;

    private final AuthorizationManager authorizationManager;

    public ClassMapper(JenaService jenaService, AuthorizationManager authorizationManager) {
        this.jenaService = jenaService;
        this.authorizationManager = authorizationManager;
    }

    public String createClassAndMapToModel(String prefix, Model model, ClassDTO dto){
        logger.info("Adding class to {}", prefix);

        var creationDate = new XSDDateTime(Calendar.getInstance());
        var classUri = ModelConstants.SUOMI_FI_NAMESPACE + prefix + "#" + dto.getIdentifier();
        if(jenaService.doesClassExistInGraph(ModelConstants.SUOMI_FI_NAMESPACE + prefix, classUri)){
            throw new MappingError("Class already exists");
        }
        var classResource = model.createResource(classUri)
                .addProperty(RDF.type, OWL.Class)
                .addProperty(OWL.versionInfo, dto.getStatus().name())
                .addProperty(DCTerms.modified, ResourceFactory.createTypedLiteral(creationDate))
                .addProperty(DCTerms.created, ResourceFactory.createTypedLiteral(creationDate))
                .addProperty(RDFS.isDefinedBy, ModelConstants.SUOMI_FI_NAMESPACE + prefix);

        classResource.addProperty(DCTerms.identifier, ResourceFactory.createTypedLiteral(dto.getIdentifier(), XSDDatatype.XSDNCName));
        //Labels
        var modelResource = model.getResource(ModelConstants.SUOMI_FI_NAMESPACE + prefix);
        var langs = MapperUtils.arrayPropertyToSet(modelResource, DCTerms.language);
        MapperUtils.addLocalizedProperty(langs, dto.getLabel(), classResource, RDFS.label, model);
        //Comment (not visible for unauthenticated users)
        classResource.addProperty(SKOS.editorialNote, dto.getComment());
        //Note
        MapperUtils.addLocalizedProperty(langs, dto.getNote(), classResource, SKOS.note, model);
        //Status
        classResource.addProperty(OWL.versionInfo, dto.getStatus().name());

        //namespaces so we don't add any that aren't in model.
        //user will have to add namespaces to model before adding class with these.
        var owlImports = MapperUtils.arrayPropertyToSet(modelResource, OWL.imports);
        var dcTermsRequires = MapperUtils.arrayPropertyToSet(modelResource, DCTerms.requires);
        //Equivalent class
        dto.getEquivalentClass().forEach(eq -> {
            var namespace = NodeFactory.createURI(eq).getNameSpace().replace("#", "");
            if(!owlImports.contains(namespace) && !dcTermsRequires.contains(namespace)){
                throw new MappingError("Equivalent class namespace not in owl:imports or dcterms:requires");
            }
            classResource.addProperty(OWL.equivalentClass, eq);
        });
        //Sub Class
        if(dto.getSubClassOf() == null || dto.getSubClassOf().isEmpty()){
            classResource.addProperty(RDFS.subClassOf, OWL.Thing);
        }
        dto.getSubClassOf().forEach(sub -> {
            var namespace = NodeFactory.createURI(sub).getNameSpace().replace("#", "");
            if(!owlImports.contains(namespace) && !dcTermsRequires.contains(namespace)){
                throw new MappingError("Sub class namespace not in owl:imports or dcterms:requires");
            }
            classResource.addProperty(RDFS.subClassOf, sub);
        });
        //Subject
        //TODO are all subjects resolved before hand or do we resolve on the fly?
        //This can be expanded when adding terminology functionality
        classResource.addProperty(DCTerms.subject, dto.getSubject());

        model.getResource(ModelConstants.SUOMI_FI_NAMESPACE + prefix)
                .addProperty(DCTerms.hasPart, classUri);

        return classUri;
    }

    /**
     * Map model with given prefix and class identifier
     *
     * @param modelPrefix     Model prefix
     * @param classIdentifier class identifier
     * @param model           Model
     * @return Class DTO
     */
    public ClassDTO mapToClassDTO(String modelPrefix, String classIdentifier, Model model){
        var classDTO = new ClassDTO();
        var classUri = ModelConstants.SUOMI_FI_NAMESPACE + modelPrefix + "#" + classIdentifier;
        if(!jenaService.doesClassExistInGraph(ModelConstants.SUOMI_FI_NAMESPACE + modelPrefix , classUri)){
            throw new ResourceNotFoundException(classUri);
        }
        var classResource = model.getResource(classUri);
        classDTO.setLabel(MapperUtils.localizedPropertyToMap(classResource, RDFS.label));
        var status = Status.valueOf(classResource.getProperty(OWL.versionInfo).getObject().toString().toUpperCase());
        classDTO.setStatus(status);
        classDTO.setSubClassOf(MapperUtils.arrayPropertyToSet(classResource, RDFS.subClassOf));
        classDTO.setEquivalentClass(MapperUtils.arrayPropertyToSet(classResource, OWL.equivalentClass));
        classDTO.setSubject(MapperUtils.propertyToString(classResource, DCTerms.subject));
        classDTO.setIdentifier(classResource.getLocalName());
        classDTO.setNote(MapperUtils.localizedPropertyToMap(classResource, SKOS.note));
        if (authorizationManager.hasRightToModel(modelPrefix, model)) {
            classDTO.setComment(MapperUtils.propertyToString(classResource, SKOS.editorialNote));
        }
        return classDTO;
    }

    public IndexClass mapToIndexClass(Model model, String classUri){
        var indexClass = new IndexClass();
        if(!jenaService.doesClassExistInGraph(classUri.substring(0, classUri.indexOf('#')), classUri)){
            throw new ResourceNotFoundException(classUri);
        }
        var classResource = model.getResource(classUri);
        indexClass.setId(classUri);
        indexClass.setLabel(MapperUtils.localizedPropertyToMap(classResource, RDFS.label));
        indexClass.setNote(MapperUtils.localizedPropertyToMap(classResource, SKOS.note));
        indexClass.setStatus(Status.valueOf(MapperUtils.propertyToString(classResource, OWL.versionInfo)));
        indexClass.setIsDefinedBy(MapperUtils.propertyToString(classResource, RDFS.isDefinedBy));
        indexClass.setIdentifier(classResource.getLocalName());
        indexClass.setNamespace(classResource.getNameSpace());
        indexClass.setModified(classResource.getProperty(DCTerms.modified).getString());
        indexClass.setCreated(classResource.getProperty(DCTerms.created).getString());

        var contentModified = classResource.getProperty(Iow.contentModified);
        if(contentModified != null){
            indexClass.setContentModified(contentModified.getString());
        }

        return indexClass;
    }

}
