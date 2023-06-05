package fi.vm.yti.datamodel.api.v2.mapper;

import fi.vm.yti.datamodel.api.v2.dto.*;
import fi.vm.yti.datamodel.api.v2.endpoint.error.MappingError;
import fi.vm.yti.datamodel.api.v2.opensearch.index.*;
import fi.vm.yti.security.YtiUser;
import org.apache.jena.datatypes.xsd.XSDDatatype;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.vocabulary.*;
import org.topbraid.shacl.vocabulary.SH;

import java.util.Map;
import java.util.function.Consumer;

public class ResourceMapper {

    private ResourceMapper(){
        //Static class
    }

    public static String mapToResource(String graphUri, Model model, ResourceDTO dto, YtiUser user){
        var resourceResource = createAndMapCommonInfo(graphUri, model, dto);

        resourceResource.addProperty(RDF.type, dto.getType().equals(ResourceType.ASSOCIATION)
                ? OWL.ObjectProperty
                : OWL.DatatypeProperty);

        var modelResource = model.getResource(graphUri);
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
        MapperUtils.addCreationMetadata(resourceResource, user);

        return resourceResource.getURI();
    }

    public static String mapToPropertyShapeResource(String graphUri, Model model, PropertyShapeDTO dto, YtiUser user) {
        var resource = createAndMapCommonInfo(graphUri, model, dto);

        resource.addProperty(RDF.type, SH.PropertyShape);
        resource.addProperty(RDF.type, dto.getType().equals(ResourceType.ASSOCIATION)
                ? OWL.ObjectProperty
                : OWL.DatatypeProperty);
        MapperUtils.addOptionalUriProperty(resource, SH.path, dto.getPath());
        MapperUtils.addOptionalUriProperty(resource, SH.class_, dto.getClassType());
        dto.getAllowedValues().forEach(value -> resource.addProperty(SH.in, value));
        MapperUtils.addOptionalStringProperty(resource, SH.defaultValue, dto.getDefaultValue());
        MapperUtils.addOptionalStringProperty(resource, SH.hasValue, dto.getHasValue());
        MapperUtils.addOptionalStringProperty(resource, SH.datatype, dto.getDataType());
        MapperUtils.addLiteral(resource, SH.minCount, dto.getMinCount());
        MapperUtils.addLiteral(resource, SH.maxCount, dto.getMaxCount());
        MapperUtils.addLiteral(resource, SH.minLength, dto.getMinLength());
        MapperUtils.addLiteral(resource, SH.maxLength, dto.getMaxLength());

        MapperUtils.addCreationMetadata(resource, user);

        return resource.getURI();
    }

    public static void mapToUpdateResource(String graphUri, Model model, String resourceIdentifier, ResourceDTO dto, YtiUser user) {
        var modelResource = model.getResource(graphUri);
        var resource = model.getResource(graphUri + ModelConstants.RESOURCE_SEPARATOR + resourceIdentifier);

        updateAndMapCommonInfo(model, resource, modelResource, dto);

        var type = resource.getProperty(RDF.type).getResource();
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

        MapperUtils.addUpdateMetadata(resource, user);
    }

    public static void mapToUpdatePropertyShape(String graphUri, Model model, String resourceIdentifier, PropertyShapeDTO dto, YtiUser user) {
        var modelResource = model.getResource(graphUri);
        var resource = model.getResource(graphUri + ModelConstants.RESOURCE_SEPARATOR + resourceIdentifier);

        updateAndMapCommonInfo(model, resource, modelResource, dto);

        MapperUtils.updateUriProperty(resource, SH.path, dto.getPath());
        MapperUtils.updateUriProperty(resource, SH.class_, dto.getClassType());
        if (dto.getAllowedValues() != null) {
            resource.removeAll(SH.in);
            dto.getAllowedValues().forEach(value -> resource.addProperty(SH.in, value));
        }
        MapperUtils.updateStringProperty(resource, SH.defaultValue, dto.getDefaultValue());
        MapperUtils.updateStringProperty(resource, SH.hasValue, dto.getHasValue());
        MapperUtils.updateStringProperty(resource, SH.datatype, dto.getDataType());
        MapperUtils.updateLiteral(resource, SH.minCount, dto.getMinCount());
        MapperUtils.updateLiteral(resource, SH.maxCount, dto.getMaxCount());
        MapperUtils.updateLiteral(resource, SH.minLength, dto.getMinLength());
        MapperUtils.updateLiteral(resource, SH.maxLength, dto.getMaxLength());

        MapperUtils.addUpdateMetadata(resource, user);
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

    /**
     * Map data model and concept information to search result
     * @param resource data from index
     * @param dataModels all data models
     * @param concepts all concepts
     * @return enriched data
     */
    public static IndexResourceInfo mapIndexResourceInfo(IndexResource resource, Map<String, IndexModel> dataModels, Model concepts) {
        var indexResource = new IndexResourceInfo();
        indexResource.setId(resource.getId());
        indexResource.setLabel(resource.getLabel());
        indexResource.setStatus(resource.getStatus());
        indexResource.setNote(resource.getNote());
        indexResource.setResourceType(resource.getResourceType());
        indexResource.setNamespace(resource.getNamespace());
        indexResource.setIdentifier(resource.getIdentifier());

        var dataModel = dataModels.get(resource.getIsDefinedBy());
        var dataModelInfo = new DatamodelInfo();
        dataModelInfo.setModelType(dataModel.getType());
        dataModelInfo.setStatus(dataModel.getStatus());
        dataModelInfo.setLabel(dataModel.getLabel());
        dataModelInfo.setGroups(dataModel.getIsPartOf());
        indexResource.setDataModelInfo(dataModelInfo);

        if (resource.getSubject() != null) {
            var conceptResource = concepts.getResource(resource.getSubject());
            var terminologyResource = concepts.getResource(MapperUtils.propertyToString(conceptResource, SKOS.inScheme));

            var conceptInfo = new ConceptInfo();
            conceptInfo.setConceptURI(conceptResource.getURI());
            conceptInfo.setTerminologyLabel(MapperUtils.localizedPropertyToMap(terminologyResource, RDFS.label));
            conceptInfo.setConceptLabel(MapperUtils.localizedPropertyToMap(conceptResource, SKOS.prefLabel));
            indexResource.setConceptInfo(conceptInfo);
        }
        return indexResource;
    }

    public static ResourceInfoDTO mapToResourceInfoDTO(Model model, String modelUri,
                                                       String resourceIdentifier, Model orgModel,
                                                       boolean hasRightToModel, Consumer<ResourceCommonDTO> userMapper) {
        var dto = new ResourceInfoDTO();
        var resourceUri = modelUri + ModelConstants.RESOURCE_SEPARATOR + resourceIdentifier;
        var resourceResource = model.getResource(resourceUri);

        mapResourceBasicInfoDTO(dto, resourceResource, model.getResource(modelUri), orgModel, hasRightToModel);

        if(MapperUtils.hasType(resourceResource, OWL.ObjectProperty)){
            dto.setType(ResourceType.ASSOCIATION);
        }else if(MapperUtils.hasType(resourceResource, OWL.DatatypeProperty)){
            dto.setType(ResourceType.ATTRIBUTE);
        }else{
            throw new MappingError("Unsupported rdf:type");
        }

        dto.setSubResourceOf(MapperUtils.arrayPropertyToSet(resourceResource, RDFS.subPropertyOf));
        dto.setEquivalentResource(MapperUtils.arrayPropertyToSet(resourceResource, OWL.equivalentProperty));

        dto.setDomain(MapperUtils.propertyToString(resourceResource, RDFS.domain));
        dto.setRange(MapperUtils.propertyToString(resourceResource, RDFS.range));

        MapperUtils.mapCreationInfo(dto, resourceResource, userMapper);
        return dto;
    }

    public static PropertyShapeInfoDTO mapToPropertyShapeInfoDTO(Model model, String modelUri,
                                                                 String identifier, Model orgModel,
                                                                 boolean hasRightToModel, Consumer<ResourceCommonDTO> userMapper ) {
        var dto = new PropertyShapeInfoDTO();
        var resourceUri = modelUri + ModelConstants.RESOURCE_SEPARATOR + identifier;
        var resource = model.getResource(resourceUri);

        mapResourceBasicInfoDTO(dto, resource, model.getResource(modelUri), orgModel, hasRightToModel);

        if(MapperUtils.hasType(resource, OWL.ObjectProperty)){
            dto.setType(ResourceType.ASSOCIATION);
        }else if(MapperUtils.hasType(resource, OWL.DatatypeProperty)){
            dto.setType(ResourceType.ATTRIBUTE);
        }else{
            throw new MappingError("Unsupported rdf:type");
        }
        dto.setAllowedValues(MapperUtils.arrayPropertyToList(resource, SH.in));
        dto.setClassType(MapperUtils.propertyToString(resource, SH.class_));
        dto.setDataType(MapperUtils.propertyToString(resource, SH.datatype));
        dto.setDefaultValue(MapperUtils.propertyToString(resource, SH.defaultValue));
        dto.setHasValue(MapperUtils.propertyToString(resource, SH.hasValue));
        dto.setPath(MapperUtils.propertyToString(resource, SH.path));
        dto.setMaxCount(MapperUtils.getLiteral(resource, SH.maxCount, Integer.class));
        dto.setMinCount(MapperUtils.getLiteral(resource, SH.minCount, Integer.class));
        dto.setMaxLength(MapperUtils.getLiteral(resource, SH.maxLength, Integer.class));
        dto.setMinLength(MapperUtils.getLiteral(resource, SH.minLength, Integer.class));
        MapperUtils.mapCreationInfo(dto, resource, userMapper);

        return dto;
    }

    public static ExternalResourceDTO mapToExternalResource(Resource resource) {
        var external = new ExternalResourceDTO();
        external.setLabel(MapperUtils.localizedPropertyToMap(resource, RDFS.label));
        external.setUri(resource.getURI());
        return external;
    }

    private static void mapResourceBasicInfoDTO(ResourceInfoBaseDTO dto,
                                                Resource resourceResource,
                                                Resource modelResource,
                                                Model orgModel,
                                                boolean hasRightToModel) {
        dto.setUri(resourceResource.getURI());
        dto.setLabel(MapperUtils.localizedPropertyToMap(resourceResource, RDFS.label));
        var status = Status.valueOf(resourceResource.getProperty(OWL.versionInfo).getObject().toString().toUpperCase());
        dto.setStatus(status);
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
        var contributors = MapperUtils.arrayPropertyToSet(modelResource, DCTerms.contributor);
        dto.setContributor(OrganizationMapper.mapOrganizationsToDTO(contributors, orgModel));
        dto.setContact(MapperUtils.propertyToString(modelResource, Iow.contact));
    }

    private static Resource createAndMapCommonInfo(String graphUri, Model model, BaseDTO dto) {
        var resource = model.createResource(graphUri + ModelConstants.RESOURCE_SEPARATOR + dto.getIdentifier());
        var modelResource = model.getResource(graphUri);
        var languages = MapperUtils.arrayPropertyToSet(modelResource, DCTerms.language);

        resource.addProperty(OWL.versionInfo, dto.getStatus().name());
        resource.addProperty(RDFS.isDefinedBy, ResourceFactory.createResource(modelResource.getURI()));
        resource.addProperty(DCTerms.identifier, ResourceFactory.createTypedLiteral(dto.getIdentifier(), XSDDatatype.XSDNCName));
        MapperUtils.addLocalizedProperty(languages, dto.getLabel(), resource, RDFS.label, model);
        MapperUtils.addLocalizedProperty(languages, dto.getNote(), resource, RDFS.comment, model);
        MapperUtils.addOptionalStringProperty(resource, SKOS.editorialNote, dto.getEditorialNote());
        MapperUtils.addOptionalUriProperty(resource, DCTerms.subject, dto.getSubject());

        return resource;
    }

    private static void updateAndMapCommonInfo(Model model, Resource resource, Resource modelResource, BaseDTO dto) {
        var languages = MapperUtils.arrayPropertyToSet(modelResource, DCTerms.language);

        if(!MapperUtils.hasType(resource, OWL.DatatypeProperty, OWL.ObjectProperty)){
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
    }

}
