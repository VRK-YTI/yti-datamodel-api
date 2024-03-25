package fi.vm.yti.datamodel.api.v2.mapper;

import fi.vm.yti.datamodel.api.v2.dto.*;
import fi.vm.yti.datamodel.api.v2.endpoint.error.MappingError;
import fi.vm.yti.datamodel.api.v2.opensearch.index.*;
import fi.vm.yti.datamodel.api.v2.properties.DCAP;
import fi.vm.yti.datamodel.api.v2.properties.SuomiMeta;
import fi.vm.yti.datamodel.api.v2.utils.DataModelURI;
import fi.vm.yti.security.YtiUser;
import org.apache.jena.datatypes.xsd.XSDDatatype;
import org.apache.jena.rdf.model.*;
import org.apache.jena.vocabulary.*;
import org.topbraid.shacl.vocabulary.SH;

import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class ResourceMapper {

    private ResourceMapper(){
        //Static class
    }

    public static String mapToResource(DataModelURI uri, Model model, ResourceDTO dto, ResourceType resourceType, YtiUser user){
        var resourceResource = MapperUtils.addCommonResourceInfo(model, uri, dto);

        resourceResource.addProperty(RDF.type, resourceType.equals(ResourceType.ASSOCIATION)
                ? OWL.ObjectProperty
                : OWL.DatatypeProperty);

        var modelResource = model.getResource(uri.getModelURI());

        //Equivalent resource
        if(dto.getEquivalentResource() != null){
            dto.getEquivalentResource().forEach(eq -> MapperUtils.addResourceRelationship(modelResource, resourceResource, OWL.equivalentProperty, eq));
        }
        //Sub Class
        if(dto.getSubResourceOf() == null || dto.getSubResourceOf().isEmpty()){
            if(resourceType.equals(ResourceType.ASSOCIATION)){
                resourceResource.addProperty(RDFS.subPropertyOf, OWL2.topObjectProperty); //Add OWL:TopObjectProperty if nothing else is specified
            }else{
                resourceResource.addProperty(RDFS.subPropertyOf, OWL2.topDataProperty); //Add OWL:TopDataProperty if nothing else is specified
            }
        }else{
            dto.getSubResourceOf().forEach(sub -> MapperUtils.addResourceRelationship(modelResource, resourceResource, RDFS.subPropertyOf, sub));
        }

        MapperUtils.addResourceRelationship(modelResource, resourceResource, RDFS.domain, dto.getDomain());
        MapperUtils.addBooleanResourceType(resourceResource, OWL.FunctionalProperty, dto.getFunctionalProperty());
        if (resourceType.equals(ResourceType.ASSOCIATION)) {
            MapperUtils.addResourceRelationship(modelResource, resourceResource, RDFS.range, dto.getRange());
            MapperUtils.addBooleanResourceType(resourceResource, OWL.TransitiveProperty, dto.getTransitiveProperty());
            MapperUtils.addBooleanResourceType(resourceResource, OWL2.ReflexiveProperty, dto.getReflexiveProperty());
        } else {
            MapperUtils.addOptionalUriProperty(resourceResource, RDFS.range, dto.getRange());
        }

        modelResource.addProperty(DCTerms.hasPart, resourceResource);
        MapperUtils.addCreationMetadata(resourceResource, user);

        return resourceResource.getURI();
    }

    public static String mapToPropertyShapeResource(DataModelURI uri, Model model, PropertyShapeDTO dto, ResourceType resourceType, YtiUser user) {
        var resource = MapperUtils.addCommonResourceInfo(model, uri, dto);
        var modelResource = model.getResource(uri.getModelURI());

        Resource typeResource = resourceType.equals(ResourceType.ASSOCIATION)
                ? OWL.ObjectProperty
                : OWL.DatatypeProperty;

        Resource defaultPathResource = resourceType.equals(ResourceType.ASSOCIATION)
                ? OWL2.topObjectProperty
                : OWL2.topDataProperty;

        resource.addProperty(RDF.type, SH.PropertyShape);
        resource.addProperty(RDF.type, typeResource);

        if (dto.getPath() == null) {
            resource.addProperty(SH.path, defaultPathResource);
        } else {
            MapperUtils.addResourceRelationship(modelResource, resource, SH.path, dto.getPath());
        }
        MapperUtils.addLiteral(resource, SH.minCount, dto.getMinCount());
        MapperUtils.addLiteral(resource, SH.maxCount, dto.getMaxCount());

        if(resourceType.equals(ResourceType.ASSOCIATION)){
            MapperUtils.addResourceRelationship(modelResource, resource, SH.class_, ((AssociationRestriction) dto).getClassType());
        }else{
            addAttributeRestrictionProperties(resource, (AttributeRestriction) dto, modelResource);
        }

        MapperUtils.addCreationMetadata(resource, user);

        return resource.getURI();
    }

    public static void addAttributeRestrictionProperties(Resource resource, AttributeRestriction dto, Resource modelResource) {
        dto.getAllowedValues().forEach(value -> resource.addProperty(SH.in, value));
        MapperUtils.addOptionalStringProperty(resource, SH.defaultValue, dto.getDefaultValue());
        MapperUtils.addOptionalStringProperty(resource, SH.hasValue, dto.getHasValue());
        MapperUtils.addOptionalStringProperty(resource, SH.datatype, dto.getDataType());
        MapperUtils.addLiteral(resource, SH.minLength, dto.getMinLength());
        MapperUtils.addLiteral(resource, SH.maxLength, dto.getMaxLength());
        MapperUtils.addLiteral(resource, SH.minInclusive, dto.getMinInclusive());
        MapperUtils.addLiteral(resource, SH.maxInclusive, dto.getMaxInclusive());
        MapperUtils.addLiteral(resource, SH.minExclusive, dto.getMinExclusive());
        MapperUtils.addLiteral(resource, SH.maxExclusive, dto.getMaxExclusive());
        MapperUtils.addOptionalStringProperty(resource, SH.pattern, dto.getPattern());
        dto.getLanguageIn().forEach(lang -> MapperUtils.addOptionalStringProperty(resource, SH.languageIn, lang));

        var modelCodeLists = MapperUtils.arrayPropertyToList(modelResource, DCTerms.requires);
        dto.getCodeLists().forEach(codeList -> {
            if(!modelCodeLists.contains(codeList)){
                MapperUtils.addOptionalUriProperty(modelResource, DCTerms.requires, codeList);
            }
            MapperUtils.addOptionalUriProperty(resource, SuomiMeta.codeList, codeList);
        });
    }

    public static void mapToUpdateResource(DataModelURI uri, Model model, ResourceDTO dto, YtiUser user) {
        var modelResource = model.getResource(uri.getModelURI());
        var resource = model.getResource(uri.getResourceURI());

        MapperUtils.updateCommonResourceInfo(model, resource, modelResource, dto);

        resource.removeAll(OWL.equivalentProperty);
        dto.getEquivalentResource().forEach(eq -> MapperUtils.addResourceRelationship(modelResource, resource, OWL.equivalentProperty, eq));

        var getSubResourceOf = dto.getSubResourceOf();
        resource.removeAll(RDFS.subPropertyOf);
        if(getSubResourceOf.isEmpty()){
            if(MapperUtils.hasType(resource, OWL.ObjectProperty)){
                resource.addProperty(RDFS.subPropertyOf, OWL2.topObjectProperty); //Add OWL:TopObjectProperty if nothing else is specified
            }else{
                resource.addProperty(RDFS.subPropertyOf, OWL2.topDataProperty); //Add OWL:TopDataProperty if nothing else is specified
            }
        }else{
            getSubResourceOf.forEach(sub -> MapperUtils.addResourceRelationship(modelResource, resource, RDFS.subPropertyOf, sub));
        }

        MapperUtils.addTerminologyReference(dto, modelResource);
        MapperUtils.updateUriPropertyAndAddReferenceNamespaces(modelResource, resource, RDFS.domain, dto.getDomain());

        MapperUtils.updateBooleanTypeProperty(model, resource, OWL.FunctionalProperty, dto.getFunctionalProperty());
        //Object property meaning association
        if (MapperUtils.hasType(resource, OWL.ObjectProperty)) {
            MapperUtils.updateUriPropertyAndAddReferenceNamespaces(modelResource, resource, RDFS.range, dto.getRange());
            MapperUtils.updateBooleanTypeProperty(model, resource, OWL.TransitiveProperty, dto.getTransitiveProperty());
            MapperUtils.updateBooleanTypeProperty(model, resource, OWL2.ReflexiveProperty, dto.getReflexiveProperty());
        } else {
            MapperUtils.updateUriProperty(resource, RDFS.range, dto.getRange());
        }

        // update range to attribute restrictions (owl:someValuesFrom)
        if (MapperUtils.hasType(resource, OWL.DatatypeProperty)) {
            model.listStatements(new SimpleSelector(null, OWL.equivalentClass, (RDFNode) null))
                    .filterKeep(p -> p.getObject().isAnon()).toList()
                    .forEach(r -> r.getProperty(OWL.intersectionOf).getObject().as(RDFList.class)
                            .asJavaList().stream()
                            .map(RDFNode::asResource)
                            .filter(p -> p.hasProperty(OWL.onProperty) && p.getProperty(OWL.onProperty).getObject().equals(resource))
                            .forEach(s -> {
                                s.removeAll(OWL.someValuesFrom);
                                if (dto.getRange() != null) {
                                    s.addProperty(OWL.someValuesFrom, ResourceFactory.createResource(dto.getRange()));
                                }
                            }));
        }
        MapperUtils.addUpdateMetadata(resource, user);
    }

    public static void mapToUpdatePropertyShape(DataModelURI uri, Model model, PropertyShapeDTO dto, YtiUser user) {
        var modelResource = model.getResource(uri.getModelURI());
        var resource = model.getResource(uri.getResourceURI());

        if(!MapperUtils.hasType(resource, OWL.DatatypeProperty, OWL.ObjectProperty)){
            throw new MappingError("Class cannot be updated through this endpoint");
        }

        MapperUtils.updateCommonResourceInfo(model,resource, modelResource, dto);

        var defaultPathResource = MapperUtils.hasType(resource, OWL.ObjectProperty)
                ? OWL2.topObjectProperty
                : OWL2.topDataProperty;

        if (dto.getPath() == null) {
            resource.addProperty(SH.path, defaultPathResource);
        } else {
            MapperUtils.updateUriPropertyAndAddReferenceNamespaces(modelResource, resource, SH.path, dto.getPath());
        }

        MapperUtils.updateLiteral(resource, SH.minCount, dto.getMinCount());
        MapperUtils.updateLiteral(resource, SH.maxCount, dto.getMaxCount());

        if(MapperUtils.hasType(resource, OWL.ObjectProperty)) {
            MapperUtils.updateUriPropertyAndAddReferenceNamespaces(modelResource, resource, SH.class_,
                    ((AssociationRestriction) dto).getClassType());
        }else{
            updateAttributeRestrictionProperties(resource, (AttributeRestriction) dto, modelResource);
        }

        MapperUtils.addTerminologyReference(dto, modelResource);
        MapperUtils.addUpdateMetadata(resource, user);
    }

    public static void updateAttributeRestrictionProperties(Resource resource, AttributeRestriction dto, Resource modelResource) {
        resource.removeAll(SH.in);
        dto.getAllowedValues().forEach(value -> resource.addProperty(SH.in, value));
        MapperUtils.updateStringProperty(resource, SH.defaultValue, dto.getDefaultValue());
        MapperUtils.updateStringProperty(resource, SH.hasValue, dto.getHasValue());
        MapperUtils.updateStringProperty(resource, SH.datatype, dto.getDataType());
        MapperUtils.updateLiteral(resource, SH.minLength, dto.getMinLength());
        MapperUtils.updateLiteral(resource, SH.maxLength, dto.getMaxLength());
        MapperUtils.updateLiteral(resource, SH.minInclusive, dto.getMinInclusive());
        MapperUtils.updateLiteral(resource, SH.maxInclusive, dto.getMaxInclusive());
        MapperUtils.updateLiteral(resource, SH.minExclusive, dto.getMinExclusive());
        MapperUtils.updateLiteral(resource, SH.maxExclusive, dto.getMaxExclusive());

        var requires = MapperUtils.arrayPropertyToList(modelResource, DCTerms.requires);
        resource.removeAll(SuomiMeta.codeList);
        dto.getCodeLists().forEach(codeList -> {
            if(!requires.contains(codeList)){
                MapperUtils.addOptionalUriProperty(modelResource, DCTerms.requires, codeList);
            }
            MapperUtils.addOptionalUriProperty(resource, SuomiMeta.codeList, codeList);
        });
        MapperUtils.updateStringProperty(resource, SH.pattern, dto.getPattern());
        resource.removeAll(SH.languageIn);
        dto.getLanguageIn().forEach(lang -> MapperUtils.addOptionalStringProperty(resource, SH.languageIn, lang));
    }

    public static void mapToCopyToLocalPropertyShape(String graphUri, Model model, String resourceIdentifier, Model targetModel, String targetGraph, String targetIdentifier, YtiUser user){
        var resource = model.getResource(graphUri + ModelConstants.RESOURCE_SEPARATOR + resourceIdentifier);
        var newResource = targetModel.createResource(targetGraph + ModelConstants.RESOURCE_SEPARATOR + targetIdentifier);
        resource.listProperties().forEach(prop -> {
            var pred = prop.getPredicate();
            var obj = prop.getObject();
            newResource.addProperty(pred, obj);
        }
        );

        MapperUtils.updateUriProperty(newResource, RDFS.isDefinedBy, targetGraph);
        MapperUtils.updateLiteral(newResource, DCTerms.identifier, XSDDatatype.XSDNCName.parse(targetIdentifier));

        newResource.removeAll(DCTerms.modified);
        newResource.removeAll(DCTerms.created);
        newResource.removeAll(SuomiMeta.modifier);
        newResource.removeAll(SuomiMeta.creator);
        MapperUtils.addCreationMetadata(newResource, user);
    }

    public static IndexResource mapToIndexResource(Model model, String resourceUri){
        var indexResource = new IndexResource();
        var resource = model.getResource(resourceUri);

        var isDefinedBy = MapperUtils.propertyToString(model.getResource(resourceUri), RDFS.isDefinedBy);
        var modelResource = model.getResource(isDefinedBy);
        var version = MapperUtils.propertyToString(modelResource, OWL.versionInfo);

        if(version != null){
            indexResource.setFromVersion(version);
            indexResource.setVersionIri(MapperUtils.propertyToString(modelResource, OWL2.versionIRI));
        }

        var id = DataModelURI.createResourceURI(MapperUtils.propertyToString(modelResource, DCAP.preferredXMLNamespacePrefix),
                resource.getLocalName(), version);

        indexResource.setId(id.getResourceVersionURI());
        indexResource.setUri(resourceUri);
        indexResource.setCurie(MapperUtils.uriToURIDTO(resourceUri, model).getCurie());
        indexResource.setLabel(MapperUtils.localizedPropertyToMap(resource, RDFS.label));
        indexResource.setStatus(MapperUtils.getStatusFromUri(MapperUtils.propertyToString(resource, SuomiMeta.publicationStatus)));
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

        var contentModified = resource.getProperty(SuomiMeta.contentModified);
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
        indexResource.setCurie(resource.getCurie());
        indexResource.setLabel(resource.getLabel());
        indexResource.setStatus(resource.getStatus());
        indexResource.setNote(resource.getNote());
        indexResource.setResourceType(resource.getResourceType());
        indexResource.setNamespace(resource.getNamespace());
        indexResource.setIdentifier(resource.getIdentifier());

        var modelIri = resource.getVersionIri() != null ? resource.getVersionIri() : resource.getIsDefinedBy();

        var dataModel = dataModels.get(modelIri);
        var dataModelInfo = new DatamodelInfo();
        if (dataModel != null) {
            dataModelInfo.setModelType(dataModel.getType());
            dataModelInfo.setStatus(dataModel.getStatus());
            dataModelInfo.setLabel(dataModel.getLabel());
            dataModelInfo.setGroups(dataModel.getIsPartOf());
            dataModelInfo.setVersion(dataModel.getVersion());
        }
        dataModelInfo.setUri(resource.getIsDefinedBy());
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

    public static IndexResource mapExternalToIndexResource(Resource resource) {
        var indexResource = new IndexResource();

        indexResource.setId(resource.getURI());
        indexResource.setUri(resource.getURI());
        indexResource.setIdentifier(resource.getLocalName());
        indexResource.setNamespace(resource.getNameSpace());
        indexResource.setIsDefinedBy(MapperUtils.propertyToString(resource, RDFS.isDefinedBy));
        indexResource.setStatus(Status.VALID);

        if (!resource.isAnon()) {
            var uri = DataModelURI.fromURI(resource.getURI());
            indexResource.setCurie(uri.getCurie(resource.getModel().getGraph().getPrefixMapping()));
        }

        if (resource.hasProperty(RDFS.comment)) {
            indexResource.setNote(MapperUtils.localizedPropertyToMap(resource, RDFS.comment));
        } else if (resource.hasProperty(SKOS.definition)) {
            indexResource.setNote(MapperUtils.localizedPropertyToMap(resource, SKOS.definition));
        }

        if (resource.hasProperty(RDFS.label)) {
            indexResource.setLabel(MapperUtils.localizedPropertyToMap(resource, RDFS.label));
        } else if (resource.hasProperty(SKOS.prefLabel)) {
            indexResource.setLabel(MapperUtils.localizedPropertyToMap(resource, SKOS.prefLabel));
        } else {
            return null;
        }

        var resourceType = getExternalResourceType(resource);

        indexResource.setResourceType(resourceType);
        return indexResource;
    }

    public static ResourceInfoDTO mapToResourceInfoDTO(Model model, DataModelURI uri, Model orgModel,
                                                       boolean hasRightToModel, Consumer<ResourceCommonDTO> userMapper) {
        var dto = new ResourceInfoDTO();
        var resourceUri = uri.getResourceURI();
        var resourceResource = model.getResource(resourceUri);

        MapperUtils.addCommonResourceDtoInfo(dto, resourceResource, model.getResource(uri.getModelURI()), orgModel, hasRightToModel);

        if(MapperUtils.hasType(resourceResource, OWL.ObjectProperty)){
            dto.setType(ResourceType.ASSOCIATION);
        }else if(MapperUtils.hasType(resourceResource, OWL.DatatypeProperty)){
            dto.setType(ResourceType.ATTRIBUTE);
        }else{
            throw new MappingError("Unsupported rdf:type");
        }

        var subProperties = MapperUtils.arrayPropertyToSet(resourceResource, RDFS.subPropertyOf);
        var equivalentProperties = MapperUtils.arrayPropertyToSet(resourceResource, OWL.equivalentProperty);
        var domain = MapperUtils.propertyToString(resourceResource, RDFS.domain);
        var range = MapperUtils.propertyToString(resourceResource, RDFS.range);

        dto.setSubResourceOf(MapperUtils.uriToURIDTOs(subProperties, model));
        dto.setEquivalentResource(MapperUtils.uriToURIDTOs(equivalentProperties, model));
        dto.setDomain(MapperUtils.uriToURIDTO(domain, model));
        dto.setRange(MapperUtils.uriToURIDTO(range, model));

        dto.setFunctionalProperty(MapperUtils.hasType(resourceResource, OWL.FunctionalProperty));

        if (dto.getType().equals(ResourceType.ASSOCIATION)) {
            dto.setTransitiveProperty(MapperUtils.hasType(resourceResource, OWL.TransitiveProperty));
            dto.setReflexiveProperty(MapperUtils.hasType(resourceResource, OWL2.ReflexiveProperty));
        }

        MapperUtils.mapCreationInfo(dto, resourceResource, userMapper);
        return dto;
    }

    public static PropertyShapeInfoDTO mapToPropertyShapeInfoDTO(Model model, DataModelURI uri, Model orgModel,
                                                                 boolean hasRightToModel, Consumer<ResourceCommonDTO> userMapper ) {
        var dto = new PropertyShapeInfoDTO();
        var resource = model.getResource(uri.getResourceURI());

        MapperUtils.addCommonResourceDtoInfo(dto, resource, model.getResource(uri.getModelURI()), orgModel, hasRightToModel);

        if(MapperUtils.hasType(resource, OWL.ObjectProperty)){
            dto.setType(ResourceType.ASSOCIATION);
        }else if(MapperUtils.hasType(resource, OWL.DatatypeProperty)){
            dto.setType(ResourceType.ATTRIBUTE);
        }else{
            throw new MappingError("Unsupported rdf:type");
        }
        dto.setAllowedValues(MapperUtils.arrayPropertyToList(resource, SH.in));
        dto.setClassType(MapperUtils.uriToURIDTO(
                MapperUtils.propertyToString(resource, SH.class_), model)
        );
        dto.setDataType(MapperUtils.uriToURIDTO(MapperUtils.propertyToString(resource, SH.datatype), model));
        dto.setDefaultValue(MapperUtils.propertyToString(resource, SH.defaultValue));
        dto.setHasValue(MapperUtils.propertyToString(resource, SH.hasValue));
        dto.setPath(MapperUtils.uriToURIDTO(
                MapperUtils.propertyToString(resource, SH.path), model)
        );
        dto.setMaxCount(MapperUtils.getLiteral(resource, SH.maxCount, Integer.class));
        dto.setMinCount(MapperUtils.getLiteral(resource, SH.minCount, Integer.class));
        dto.setMaxLength(MapperUtils.getLiteral(resource, SH.maxLength, Integer.class));
        dto.setMinLength(MapperUtils.getLiteral(resource, SH.minLength, Integer.class));
        dto.setMinInclusive(MapperUtils.getLiteral(resource, SH.minInclusive, Integer.class));
        dto.setMaxInclusive(MapperUtils.getLiteral(resource, SH.maxInclusive, Integer.class));
        dto.setMinExclusive(MapperUtils.getLiteral(resource, SH.minExclusive, Integer.class));
        dto.setMaxExclusive(MapperUtils.getLiteral(resource, SH.maxExclusive, Integer.class));
        dto.setPattern(MapperUtils.propertyToString(resource, SH.pattern));
        dto.setLanguageIn(MapperUtils.arrayPropertyToSet(resource, SH.languageIn));
        dto.setCodeLists(MapperUtils.arrayPropertyToList(resource, SuomiMeta.codeList));
        MapperUtils.mapCreationInfo(dto, resource, userMapper);

        return dto;
    }

    public static ExternalResourceDTO mapToExternalResource(Resource resource) {
        var external = new ExternalResourceDTO();
        external.setLabel(MapperUtils.localizedPropertyToMap(resource, RDFS.label));
        external.setUri(resource.getURI());
        return external;
    }

    private static boolean hasLiteralRange(Resource resource) {
        var range = resource.getProperty(RDFS.range);
        if (range == null) {
            return false;
        }
        var xsdNs = ModelConstants.PREFIXES.get("xsd");
        var rangeResource = range.getResource();
        if(rangeResource.hasProperty(OWL.unionOf)){
            return MapperUtils.arrayPropertyToList(rangeResource, OWL.unionOf).stream().anyMatch(item -> ResourceFactory.createResource(item).getNameSpace().equals(xsdNs));
        }
        return rangeResource.getNameSpace().equals(xsdNs);
    }

    public static ResourceType getExternalResourceType(Resource resource) {

        if (MapperUtils.hasType(resource, OWL.DatatypeProperty, OWL.AnnotationProperty) || hasLiteralRange(resource)) {
            // DatatypeProperties, AnnotationProperties and range with literal value (e.g. xsd:string)
            return ResourceType.ATTRIBUTE;
        } else if (MapperUtils.hasType(resource, OWL.ObjectProperty, RDF.Property)) {
            // ObjectProperties and RDF properties if not matched in previous block
            return ResourceType.ASSOCIATION;
        } else if (MapperUtils.hasType(resource, OWL.Class, RDFS.Class, RDFS.Resource)) {
            // OWL and RDFS classes and resources
            return ResourceType.CLASS;
        } else if (resource.hasProperty(RDF.type)) {
            // Try to find type from current ontology
            var type = resource.listProperties(RDF.type)
                    .mapWith(s -> getExternalResourceType(s.getResource()))
                    .filterKeep(Objects::nonNull);
            return type.hasNext() ? type.next() : null;
        } else if(resource.hasProperty(OWL.inverseOf)) {
            var inverseOf = resource.getProperty(OWL.inverseOf).getResource();
            return getExternalResourceType(inverseOf);
        }
        return null;
    }

    public static Map<ResourceType, List<ResourceReferenceDTO>> mapToResourceReference(Model model) {
        var result = new HashSet<ResourceReferenceDTO>();
        model.listSubjectsWithProperty(RDF.type).forEach((var subject) -> {
            var dto = new ResourceReferenceDTO();
            dto.setResourceURI(MapperUtils.uriToURIDTO(subject.getURI(), model));

            var pred = subject.getProperty(DCTerms.references).getObject().asResource();
            var prefix = model.getGraph().getPrefixMapping().getNsURIPrefix(pred.getNameSpace());

            if (prefix != null) {
                dto.setProperty(String.format("%s:%s", prefix, pred.getLocalName()));
            } else {
                dto.setProperty(pred.getURI());
            }

            if (MapperUtils.hasType(subject, OWL.DatatypeProperty)) {
                dto.setType(ResourceType.ATTRIBUTE);
            } else if (MapperUtils.hasType(subject, OWL.ObjectProperty)) {
                dto.setType(ResourceType.ASSOCIATION);
            } else if (MapperUtils.hasType(subject, OWL.Class, SH.NodeShape, OWL.Restriction)) {
                dto.setType(ResourceType.CLASS);
            }
            result.add(dto);
        });
        return result.stream()
                .collect(Collectors.groupingBy(ResourceReferenceDTO::getType));
    }
}
