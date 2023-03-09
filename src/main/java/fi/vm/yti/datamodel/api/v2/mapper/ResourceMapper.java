package fi.vm.yti.datamodel.api.v2.mapper;

import fi.vm.yti.datamodel.api.v2.dto.Iow;
import fi.vm.yti.datamodel.api.v2.dto.ResourceDTO;
import fi.vm.yti.datamodel.api.v2.dto.ResourceType;
import fi.vm.yti.datamodel.api.v2.dto.Status;
import fi.vm.yti.datamodel.api.v2.endpoint.error.MappingError;
import fi.vm.yti.datamodel.api.v2.opensearch.index.IndexResource;
import fi.vm.yti.datamodel.api.v2.service.JenaService;
import org.apache.jena.datatypes.xsd.XSDDatatype;
import org.apache.jena.datatypes.xsd.XSDDateTime;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.vocabulary.*;
import org.springframework.stereotype.Service;

import java.util.Calendar;

@Service
public class ResourceMapper {

    private final JenaService jenaService;

    public ResourceMapper(JenaService jenaService) {
        this.jenaService = jenaService;
    }

    public String mapToResource(String graphUri, Model model, ResourceDTO dto){
        var creationDate = new XSDDateTime(Calendar.getInstance());
        var resourceUri = graphUri + "#" + dto.getIdentifier();
        if(jenaService.doesResourceExistInGraph(graphUri, resourceUri)){
            throw new MappingError("Resource already exists");
        }

        var resourceType = dto.getType().equals(ResourceType.ASSOCIATION) ? OWL.ObjectProperty : OWL.DatatypeProperty;

        var resourceResource = model.createResource(resourceUri)
                .addProperty(RDF.type, resourceType)
                .addProperty(OWL.versionInfo, dto.getStatus().name())
                .addProperty(DCTerms.modified, ResourceFactory.createTypedLiteral(creationDate))
                .addProperty(DCTerms.created, ResourceFactory.createTypedLiteral(creationDate))
                .addProperty(RDFS.isDefinedBy, graphUri);

        resourceResource.addProperty(DCTerms.identifier, ResourceFactory.createTypedLiteral(dto.getIdentifier(), XSDDatatype.XSDNCName));
        //Labels
        var modelResource = model.getResource(graphUri);
        var langs = MapperUtils.arrayPropertyToSet(modelResource, DCTerms.language);
        MapperUtils.addLocalizedProperty(langs, dto.getLabel(), resourceResource, RDFS.label, model);
        //Note
        MapperUtils.addLocalizedProperty(langs, dto.getNote(), resourceResource, SKOS.note, model);
        MapperUtils.addOptionalStringProperty(resourceResource, SKOS.editorialNote, dto.getEditorialNote());
        MapperUtils.addOptionalStringProperty(resourceResource, DCTerms.subject, dto.getSubject());

        var owlImports = MapperUtils.arrayPropertyToSet(modelResource, OWL.imports);
        var dcTermsRequires = MapperUtils.arrayPropertyToSet(modelResource, DCTerms.requires);
        //Equivalent class
        if(dto.getEquivalentResource() != null){
            dto.getEquivalentResource().forEach(eq -> MapperUtils.addResourceRelationship(owlImports, dcTermsRequires, resourceResource, OWL.equivalentProperty, eq));
        }
        //Sub Class
        if(dto.getSubResourceOf() == null || dto.getSubResourceOf().isEmpty()){
            if(dto.getType().equals(ResourceType.ASSOCIATION)){
                resourceResource.addProperty(RDFS.subPropertyOf, OWL2.topObjectProperty); //Add OWL:Thing if nothing else is specified
            }else{
                resourceResource.addProperty(RDFS.subPropertyOf, OWL2.topDataProperty); //Add OWL:Thing if nothing else is specified
            }
        }else{
            dto.getSubResourceOf().forEach(sub -> MapperUtils.addResourceRelationship(owlImports, dcTermsRequires, resourceResource, RDFS.subPropertyOf, sub));
        }

        modelResource.addProperty(DCTerms.hasPart, resourceUri);
        return resourceUri;
    }

    public IndexResource mapToIndexResource(Model model, String resourceUri){
        var indexResource = new IndexResource();
        var resource = model.getResource(resourceUri);

        indexResource.setId(resourceUri);
        indexResource.setLabel(MapperUtils.localizedPropertyToMap(resource, RDFS.label));
        indexResource.setStatus(Status.valueOf(MapperUtils.propertyToString(resource, OWL.versionInfo)));
        indexResource.setIsDefinedBy(MapperUtils.propertyToString(resource, RDFS.isDefinedBy));
        indexResource.setIdentifier(resource.getLocalName());
        indexResource.setNamespace(resource.getNameSpace());
        indexResource.setModified(resource.getProperty(DCTerms.modified).getString());
        indexResource.setCreated(resource.getProperty(DCTerms.created).getString());

        var note = MapperUtils.localizedPropertyToMap(resource, SKOS.note);
        if(!note.isEmpty()){
            indexResource.setNote(note);
        }

        var contentModified = resource.getProperty(Iow.contentModified);
        if(contentModified != null){
            indexResource.setContentModified(contentModified.getString());
        }

        var typeProperty = resource.getProperty(RDF.type).getResource();
        if(typeProperty.equals(OWL.ObjectProperty)){
            indexResource.setResourceType(ResourceType.ASSOCIATION);
        }else if(typeProperty.equals(OWL.DatatypeProperty)){
            indexResource.setResourceType(ResourceType.ATTRIBUTE);
        }else{
            indexResource.setResourceType(ResourceType.CLASS);
        }

        return indexResource;
    }

}
