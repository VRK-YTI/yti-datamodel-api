package fi.vm.yti.datamodel.api.v2.mapper;

import fi.vm.yti.datamodel.api.v2.dto.*;
import fi.vm.yti.datamodel.api.v2.endpoint.error.MappingError;
import fi.vm.yti.datamodel.api.v2.opensearch.index.IndexResource;
import fi.vm.yti.security.YtiUser;
import org.apache.jena.datatypes.xsd.XSDDatatype;
import org.apache.jena.datatypes.xsd.XSDDateTime;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.vocabulary.*;
import org.topbraid.shacl.vocabulary.SH;

import java.util.Calendar;
import java.util.function.Consumer;

public class ResourceMapper {

    private ResourceMapper(){
        //Static class
    }

    public static String mapToResource(String graphUri, Model model, ResourceDTO dto, YtiUser user){
        var creationDate = new XSDDateTime(Calendar.getInstance());
        var resourceUri = graphUri + "#" + dto.getIdentifier();
        var resourceType = dto.getType().equals(ResourceType.ASSOCIATION) ? OWL.ObjectProperty : OWL.DatatypeProperty;

        var resourceResource = model.createResource(resourceUri)
                .addProperty(RDF.type, resourceType)
                .addProperty(OWL.versionInfo, dto.getStatus().name())
                .addProperty(DCTerms.modified, ResourceFactory.createTypedLiteral(creationDate))
                .addProperty(DCTerms.created, ResourceFactory.createTypedLiteral(creationDate))
                .addProperty(Iow.creator, user.getId().toString())
                .addProperty(Iow.modifier, user.getId().toString());

        resourceResource.addProperty(RDFS.isDefinedBy, ResourceFactory.createResource(graphUri));
        resourceResource.addProperty(DCTerms.identifier, ResourceFactory.createTypedLiteral(dto.getIdentifier(), XSDDatatype.XSDNCName));
        //Labels
        var modelResource = model.getResource(graphUri);
        var langs = MapperUtils.arrayPropertyToSet(modelResource, DCTerms.language);
        MapperUtils.addLocalizedProperty(langs, dto.getLabel(), resourceResource, RDFS.label, model);
        //Note
        MapperUtils.addLocalizedProperty(langs, dto.getNote(), resourceResource, RDFS.comment, model);
        MapperUtils.addOptionalStringProperty(resourceResource, SKOS.editorialNote, dto.getEditorialNote());
        MapperUtils.addOptionalUriProperty(resourceResource, DCTerms.subject, dto.getSubject());

        var owlImports = MapperUtils.arrayPropertyToSet(modelResource, OWL.imports);
        var dcTermsRequires = MapperUtils.arrayPropertyToSet(modelResource, DCTerms.requires);
        //Equivalent class
        if(dto.getEquivalentResource() != null){
            dto.getEquivalentResource().forEach(eq -> MapperUtils.addResourceRelationship(owlImports, dcTermsRequires, resourceResource, OWL.equivalentProperty, eq));
        }
        //Sub Class
        if(dto.getSubResourceOf() == null || dto.getSubResourceOf().isEmpty()){
            if(dto.getType().equals(ResourceType.ASSOCIATION)){
                resourceResource.addProperty(RDFS.subPropertyOf, OWL2.topObjectProperty); //Add OWL:TopObjectProperty if nothing else is specified
            }else{
                resourceResource.addProperty(RDFS.subPropertyOf, OWL2.topDataProperty); //Add OWL:TopDataProperty if nothing else is specified
            }
        }else{
            dto.getSubResourceOf().forEach(sub -> MapperUtils.addResourceRelationship(owlImports, dcTermsRequires, resourceResource, RDFS.subPropertyOf, sub));
        }

        MapperUtils.addOptionalUriProperty(resourceResource, RDFS.domain, dto.getDomain());
        MapperUtils.addOptionalUriProperty(resourceResource, RDFS.range, dto.getRange());

        modelResource.addProperty(DCTerms.hasPart, resourceResource);
        return resourceUri;
    }

    public static void mapToUpdateResource(String graphUri, Model model, String resourceIdentifier, ResourceDTO dto, YtiUser user) {
        var updateDate = new XSDDateTime(Calendar.getInstance());
        var modelResource = model.getResource(graphUri);
        var languages = MapperUtils.arrayPropertyToSet(modelResource, DCTerms.language);
        var resource = model.getResource(graphUri + "#" + resourceIdentifier);

        var type = resource.getProperty(RDF.type).getResource();
        if(type.equals(OWL.Class)){
            throw new MappingError("Class cannot be updated through this endpoint");
        }

        MapperUtils.updateLocalizedProperty(languages, dto.getLabel(), resource, RDFS.label, model);
        MapperUtils.updateLocalizedProperty(languages, dto.getNote(), resource, RDFS.comment, model);
        MapperUtils.updateStringProperty(resource, SKOS.editorialNote, dto.getEditorialNote());
        MapperUtils.updateUriProperty(resource, DCTerms.subject, dto.getSubject());

        var status = dto.getStatus();
        if (status != null) {
            MapperUtils.updateStringProperty(resource, OWL.versionInfo, status.name());
        }

        var owlImports = MapperUtils.arrayPropertyToSet(modelResource, OWL.imports);
        var dcTermsRequires = MapperUtils.arrayPropertyToSet(modelResource, DCTerms.requires);

        var equivalentResources = dto.getEquivalentResource();
        if(equivalentResources != null){
            resource.removeAll(OWL.equivalentProperty);
            equivalentResources.forEach(eq -> MapperUtils.addResourceRelationship(owlImports, dcTermsRequires, resource, OWL.equivalentProperty, eq));
        }

        var getSubResourceOf = dto.getSubResourceOf();
        if (getSubResourceOf != null){
            resource.removeAll(RDFS.subPropertyOf);
            if(getSubResourceOf.isEmpty()){
                if(type.equals(OWL.ObjectProperty)){
                    resource.addProperty(RDFS.subPropertyOf, OWL2.topObjectProperty); //Add OWL:TopObjectProperty if nothing else is specified
                }else{
                    resource.addProperty(RDFS.subPropertyOf, OWL2.topDataProperty); //Add OWL:TopDataProperty if nothing else is specified
                }
            }else{
                getSubResourceOf.forEach(sub -> MapperUtils.addResourceRelationship(owlImports, dcTermsRequires, resource, RDFS.subPropertyOf, sub));
            }
        }

        MapperUtils.updateUriProperty(resource, RDFS.domain, dto.getDomain());
        MapperUtils.updateUriProperty(resource, RDFS.range, dto.getRange());

        resource.removeAll(DCTerms.modified);
        resource.addProperty(DCTerms.modified, ResourceFactory.createTypedLiteral(updateDate));
        resource.removeAll(Iow.modifier);
        resource.addProperty(Iow.modifier, user.getId().toString());
    }

    public static IndexResource mapToIndexResource(Model model, String resourceUri){
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
        indexResource.setSubject(MapperUtils.propertyToString(resource, DCTerms.subject));
        var note = MapperUtils.localizedPropertyToMap(resource, RDFS.comment);
        if(!note.isEmpty()){
            indexResource.setNote(note);
        }

        var contentModified = resource.getProperty(Iow.contentModified);
        if(contentModified != null){
            indexResource.setContentModified(contentModified.getString());
        }

        if (MapperUtils.hasType(resource, OWL.ObjectProperty)) {
            indexResource.setResourceType(ResourceType.ASSOCIATION);
        } else if (MapperUtils.hasType(resource, OWL.DatatypeProperty)) {
            indexResource.setResourceType(ResourceType.ATTRIBUTE);
        } else {
            indexResource.setResourceType(ResourceType.CLASS);
        }

        indexResource.setDomain(MapperUtils.propertyToString(resource, RDFS.domain));
        indexResource.setRange(MapperUtils.propertyToString(resource, RDFS.range));

        indexResource.setTargetClass(MapperUtils.propertyToString(resource, SH.targetClass));

        return indexResource;
    }

    public static ResourceInfoDTO mapToResourceInfoDTO(Model model, String modelUri,
                                                       String resourceIdentifier, Model orgModel,
                                                       boolean hasRightToModel, Consumer<ResourceCommonDTO> userMapper) {
        var dto = new ResourceInfoDTO();
        var resourceUri = modelUri + "#" + resourceIdentifier;
        var resourceResource = model.getResource(resourceUri);
        if(MapperUtils.hasType(resourceResource, OWL.ObjectProperty)){
            dto.setType(ResourceType.ASSOCIATION);
        }else if(MapperUtils.hasType(resourceResource, OWL.DatatypeProperty)){
            dto.setType(ResourceType.ATTRIBUTE);
        }else{
            throw new MappingError("Unsupported rdf:type");
        }

        dto.setUri(resourceUri);
        dto.setLabel(MapperUtils.localizedPropertyToMap(resourceResource, RDFS.label));
        var status = Status.valueOf(resourceResource.getProperty(OWL.versionInfo).getObject().toString().toUpperCase());
        dto.setStatus(status);
        dto.setSubResourceOf(MapperUtils.arrayPropertyToSet(resourceResource, RDFS.subPropertyOf));
        dto.setEquivalentResource(MapperUtils.arrayPropertyToSet(resourceResource, OWL.equivalentProperty));
        String subject = MapperUtils.propertyToString(resourceResource, DCTerms.subject);
        if (subject != null) {
            var conceptDTO = new ConceptDTO();
            conceptDTO.setConceptURI(subject);
            dto.setSubject(conceptDTO);
        }
        dto.setIdentifier(resourceResource.getLocalName());
        dto.setNote(MapperUtils.localizedPropertyToMap(resourceResource, RDFS.comment));
        if (hasRightToModel) {
            dto.setEditorialNote(MapperUtils.propertyToString(resourceResource, SKOS.editorialNote));
        }

        var created = resourceResource.getProperty(DCTerms.created).getLiteral().getString();
        var modified = resourceResource.getProperty(DCTerms.modified).getLiteral().getString();
        dto.setCreated(created);
        dto.setModified(modified);
        var modelResource = model.getResource(modelUri);
        var contributors = MapperUtils.arrayPropertyToSet(modelResource, DCTerms.contributor);
        dto.setContributor(OrganizationMapper.mapOrganizationsToDTO(contributors, orgModel));
        dto.setContact(MapperUtils.propertyToString(modelResource, Iow.contact));
        dto.setCreator(new UserDTO(MapperUtils.propertyToString(resourceResource, Iow.creator)));
        dto.setModifier(new UserDTO(MapperUtils.propertyToString(resourceResource, Iow.modifier)));
        if (userMapper != null) {
            userMapper.accept(dto);
        }

        dto.setDomain(MapperUtils.propertyToString(resourceResource, RDFS.domain));
        dto.setRange(MapperUtils.propertyToString(resourceResource, RDFS.range));
        return dto;
    }

}