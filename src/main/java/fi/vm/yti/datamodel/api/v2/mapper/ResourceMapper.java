package fi.vm.yti.datamodel.api.v2.mapper;

import fi.vm.yti.datamodel.api.v2.dto.*;
import fi.vm.yti.datamodel.api.v2.endpoint.error.MappingError;
import fi.vm.yti.datamodel.api.v2.opensearch.index.IndexResource;
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
        MapperUtils.addLocalizedProperty(langs, dto.getNote(), resourceResource, SKOS.note, model);
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

        modelResource.addProperty(DCTerms.hasPart, ResourceFactory.createResource(resourceUri));
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

    public static Query buildDomainAndRangeResourceQuery(String classUri){
        var resourceName = "?resource";
        var isDefinedBy = "?isDefinedBy";
        var constructBuilder = new ConstructBuilder().addPrefixes(ModelConstants.PREFIXES);
        SparqlUtils.addConstructProperty(resourceName, constructBuilder, RDF.type, "?type");
        SparqlUtils.addConstructProperty(resourceName, constructBuilder, RDFS.label, "?label");
        SparqlUtils.addConstructProperty(resourceName, constructBuilder, OWL.versionInfo, "?versionInfo");
        SparqlUtils.addConstructProperty(resourceName, constructBuilder, DCTerms.modified, "?modified");
        SparqlUtils.addConstructProperty(resourceName, constructBuilder, DCTerms.created, "?created");
        SparqlUtils.addConstructProperty(resourceName, constructBuilder, Iow.modifier, "?modifier");
        SparqlUtils.addConstructProperty(resourceName, constructBuilder, Iow.creator, "?creator");
        SparqlUtils.addConstructProperty(resourceName, constructBuilder, RDFS.isDefinedBy, isDefinedBy);
        SparqlUtils.addConstructOptional(resourceName, constructBuilder, RDFS.subPropertyOf, "?subProperty");
        SparqlUtils.addConstructOptional(resourceName, constructBuilder, OWL.equivalentProperty, "?eqProperty");
        SparqlUtils.addConstructOptional(resourceName, constructBuilder, SKOS.note, "?note");
        SparqlUtils.addConstructOptional(resourceName, constructBuilder, SKOS.editorialNote, "?editorialNote");
        SparqlUtils.addConstructOptional(resourceName, constructBuilder, DCTerms.subject, "?subject");
        var uri = NodeFactory.createURI(classUri);
        var domainQuery = new WhereBuilder().addWhere(resourceName, RDFS.domain, uri)
                .addWhere(resourceName, RDFS.domain, "?domain");
        var rangeQuery = new WhereBuilder().addWhere(resourceName, RDFS.range, uri)
                .addWhere(resourceName, RDFS.range, "?range");
        constructBuilder.addWhere(domainQuery.addUnion(rangeQuery))
                .addConstruct(resourceName, RDFS.range, "?range")
                .addConstruct(resourceName, RDFS.domain, "?domain")
                .addConstruct(resourceName, DCTerms.contributor, "?contributor")
                .addWhere(isDefinedBy, DCTerms.contributor, "?contributor")
                .addConstruct(resourceName, Iow.contact, "?contact")
                .addOptional(isDefinedBy, Iow.contact, "?contact");
        return constructBuilder.build();
    }


    public static ResourceInfoDTO mapToResourceInfoDtoFromConstruct(Resource resource, boolean hasRightToModel, Consumer<ResourceInfoBaseDTO> userMapper, Model orgModel){
        var dto = new ResourceInfoDTO();
        var type = resource.getProperty(RDF.type).getResource();
        if(type.equals(OWL.ObjectProperty)){
            dto.setType(ResourceType.ASSOCIATION);
        }else if(type.equals(OWL.DatatypeProperty)){
            dto.setType(ResourceType.ATTRIBUTE);
        }else{
            throw new MappingError("Unsupported rdf:type");
        }
        dto.setUri(resource.getURI());
        dto.setLabel(MapperUtils.localizedPropertyToMap(resource, RDFS.label));
        var status = Status.valueOf(resource.getProperty(OWL.versionInfo).getObject().toString().toUpperCase());
        dto.setStatus(status);
        dto.setSubResourceOf(MapperUtils.arrayPropertyToSet(resource, RDFS.subPropertyOf));
        dto.setEquivalentResource(MapperUtils.arrayPropertyToSet(resource, OWL.equivalentProperty));
        dto.setSubject(MapperUtils.propertyToString(resource, DCTerms.subject));
        dto.setIdentifier(resource.getLocalName());
        dto.setNote(MapperUtils.localizedPropertyToMap(resource, SKOS.note));
        if (hasRightToModel) {
            dto.setEditorialNote(MapperUtils.propertyToString(resource, SKOS.editorialNote));
        }
        var created = resource.getProperty(DCTerms.created).getLiteral().getString();
        var modified = resource.getProperty(DCTerms.modified).getLiteral().getString();
        dto.setCreated(created);
        dto.setModified(modified);
        dto.setCreator(new UserDTO(MapperUtils.propertyToString(resource, Iow.creator)));
        dto.setModifier(new UserDTO(MapperUtils.propertyToString(resource, Iow.modifier)));
        if (userMapper != null) {
            userMapper.accept(dto);
        }
        dto.setDomain(MapperUtils.propertyToString(resource, RDFS.domain));
        dto.setRange(MapperUtils.propertyToString(resource, RDFS.range));

        //these are datamodel specific values, but we have pulled them into the resource using a construct query
        dto.setContact(MapperUtils.propertyToString(resource, Iow.contact));
        var contributors = MapperUtils.arrayPropertyToSet(resource, DCTerms.contributor);
        dto.setContributor(OrganizationMapper.mapOrganizationsToDTO(contributors, orgModel));

        return dto;
    }

}
