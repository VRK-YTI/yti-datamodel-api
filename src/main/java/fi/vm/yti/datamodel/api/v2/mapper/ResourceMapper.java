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
                .addProperty(RDFS.isDefinedBy, graphUri)
                .addProperty(Iow.creator, user.getId().toString())
                .addProperty(Iow.modifier, user.getId().toString());

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
                resourceResource.addProperty(RDFS.subPropertyOf, OWL2.topObjectProperty); //Add OWL:TopObjectProperty if nothing else is specified
            }else{
                resourceResource.addProperty(RDFS.subPropertyOf, OWL2.topDataProperty); //Add OWL:TopDataProperty if nothing else is specified
            }
        }else{
            dto.getSubResourceOf().forEach(sub -> MapperUtils.addResourceRelationship(owlImports, dcTermsRequires, resourceResource, RDFS.subPropertyOf, sub));
        }

        MapperUtils.addOptionalStringProperty(resourceResource, RDFS.domain, dto.getDomain());
        MapperUtils.addOptionalStringProperty(resourceResource, RDFS.range, dto.getRange());

        modelResource.addProperty(DCTerms.hasPart, resourceUri);
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
        MapperUtils.updateLocalizedProperty(languages, dto.getNote(), resource, SKOS.note, model);
        MapperUtils.updateStringProperty(resource, SKOS.editorialNote, dto.getEditorialNote());
        MapperUtils.updateStringProperty(resource, DCTerms.subject, dto.getSubject());

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

        MapperUtils.updateStringProperty(resource, RDFS.domain, dto.getDomain());
        MapperUtils.updateStringProperty(resource, RDFS.range, dto.getRange());

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

        indexResource.setDomain(MapperUtils.propertyToString(resource, RDFS.domain));
        indexResource.setRange(MapperUtils.propertyToString(resource, RDFS.range));

        return indexResource;
    }

    public static ResourceInfoDTO mapToResourceInfoDTO(Model model, String modelUri,
                                                       String resourceIdentifier, Model orgModel,
                                                       boolean hasRightToModel, Consumer<ResourceInfoBaseDTO> userMapper) {
        var dto = new ResourceInfoDTO();
        var resourceUri = modelUri + "#" + resourceIdentifier;
        var resourceResource = model.getResource(resourceUri);
        var type = resourceResource.getProperty(RDF.type).getResource();
        if(type.equals(OWL.ObjectProperty)){
            dto.setType(ResourceType.ASSOCIATION);
        }else if(type.equals(OWL.DatatypeProperty)){
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
        dto.setSubject(MapperUtils.propertyToString(resourceResource, DCTerms.subject));
        dto.setIdentifier(resourceResource.getLocalName());
        dto.setNote(MapperUtils.localizedPropertyToMap(resourceResource, SKOS.note));
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
