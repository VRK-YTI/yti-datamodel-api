package fi.vm.yti.datamodel.api.v2.mapper;

import fi.vm.yti.datamodel.api.v2.dto.*;
import fi.vm.yti.datamodel.api.v2.endpoint.error.MappingError;
import fi.vm.yti.datamodel.api.v2.properties.DCAP;
import fi.vm.yti.datamodel.api.v2.properties.SuomiMeta;
import fi.vm.yti.datamodel.api.v2.utils.DataModelURI;
import fi.vm.yti.datamodel.api.v2.utils.DataModelUtils;
import fi.vm.yti.security.YtiUser;
import org.apache.jena.datatypes.xsd.XSDDatatype;
import org.apache.jena.datatypes.xsd.XSDDateTime;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.rdf.model.*;
import org.apache.jena.shared.JenaException;
import org.apache.jena.util.ResourceUtils;
import org.apache.jena.vocabulary.*;
import org.topbraid.shacl.vocabulary.SH;

import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class MapperUtils {

    private MapperUtils(){
        //Util class so we need to hide constructor
    }

    /**
     * Get UUID from urn
     * Will return null if urn cannot be parsed
     * @param urn URN string formatted as urn:uuid:{uuid}
     * @return UUID
     */
    public static UUID getUUID(String urn) {
        try {
            return UUID.fromString(urn.replace("urn:uuid:", ""));
        } catch (Exception e) {
            return null;
        }
    }

    public static String getStatusUri(Status status) {
        return "http://uri.suomi.fi/codelist/interoperabilityplatform/interoperabilityplatform_status/code/" + status.name();
    }

    public static Status getStatusFromUri(String uri) {
        if(uri == null || uri.isBlank()) {
            throw new MappingError("Could not get status from uri");
        }
        return Status.valueOf(uri.substring(uri.lastIndexOf("/") + 1));
    }

    public static ModelType getModelTypeFromResource(Resource resource){
        if(isApplicationProfile(resource)) {
            return ModelType.PROFILE;
        }else if(isLibrary(resource)) {
            return ModelType.LIBRARY;
        }
        return ModelType.PROFILE;
    }

    /**
     * Localized property to Map of (language, value). If no language specified for property
     * (e.g. external classes), handle that value as an english content
     * @param resource Resource to get property from
     * @param property Property type
     * @return Map of (language, value)
     */
    public static Map<String, String> localizedPropertyToMap(Resource resource, Property property){
        var map = new HashMap<String, String>();
        resource.listProperties(property).forEach(prop -> {
            var lang = prop.getLanguage();
            var value = prop.getString();
            if (lang == null || lang.isBlank()) {
                map.put("en", value);
            } else {
                map.put(lang, value);
            }
        });
        return map;
    }

    /**
     * Add localized property to Jena model
     * @param data Map of (language, value)
     * @param resource Resource to add to
     * @param property Property to add
     * @param model Jena model to add to
     */
    public static void addLocalizedProperty(Set<String> languages,
                                            Map<String, String> data,
                                            Resource resource,
                                            Property property,
                                            Model model) {
        if (data == null || languages == null || languages.isEmpty()) {
            return;
        }

        data.forEach((lang, value) -> {
            if(!languages.contains(lang)){
                throw new MappingError("Model missing language for localized property {" + lang + "}");
            }
            resource.addProperty(property, model.createLiteral(value, lang));
        });
    }

    /**
     * Updates localized property
     * @param languages Languages of the datamodel, localized property has to be in language
     * @param data Data to add
     * @param resource Resource
     * @param property Property
     * @param model Model
     */
    public static void updateLocalizedProperty(Set<String> languages,
                                               Map<String, String> data,
                                               Resource resource,
                                               Property property,
                                               Model model) {
        resource.removeAll(property);
        if (data != null && languages != null && !languages.isEmpty()) {
            addLocalizedProperty(languages, data, resource, property, model);
        }
    }

    /**
     * Convert array property to list of strings
     * @param resource Resource to get property from
     * @param property Property type
     * @return List of property values
     */
    public static List<String> arrayPropertyToList(Resource resource, Property property){
        var list = new ArrayList<String>();
        try{
            var statement = resource.listProperties(property)
                    .filterDrop(p -> p.getObject().isAnon())
                    .toList();
            if (statement.isEmpty()) {
                return list;
            }
            statement.get(0)
                    .getList()
                    .asJavaList()
                    .forEach(node -> list.add(node.toString()));
        }catch(JenaException ex){
            //if item could not be gotten as list it means it is multiple statements of the property
            resource.listProperties(property)
                    .filterDrop(p -> p.getObject().isAnon())
                    .forEach(val -> list.add(val.getObject().toString()));
        }
        return list;
    }

    /**
     * Convert array property to set of strings
     * @param resource Resource to get property from
     * @param property Property type
     * @return Set of property values, empty if property is not found
     */
    public static Set<String> arrayPropertyToSet(Resource resource, Property property){
        return new HashSet<>(arrayPropertyToList(resource, property));
    }

    /**
     * Convert property to String, with null checks to ensure no NullPointerException
     * @param resource Resource to get property from
     * @param property Property
     * @return String if property is found, null if not
     */
    public static String propertyToString(Resource resource, Property property){
        var prop = resource.getProperty(property);
        //null check for property
        if(prop == null){
            return null;
        }
        var object = prop.getObject();
        //null check for object
        return object == null ? null : object.toString();
    }

    public static <T> T getLiteral(Resource resource, Property property, Class<T> type) {
        var prop = resource.getProperty(property);
        if (prop == null){
            return null;
        }
        var literal = prop.getLiteral();

        if (type.equals(Integer.class)) {
            return type.cast(literal.getInt());
        } else if (type.equals(Boolean.class)) {
            return type.cast(literal.getBoolean());
        } else if (type.equals(Double.class)) {
            return type.cast(literal.getDouble());
        } else if (type.equals(String.class)) {
            return type.cast(literal.getString());
        }
        return null;
    }

    /**
     * Update string property
     * If string is empty|blank value is removed
     * @param resource Resource
     * @param property Property
     * @param value Value
     */
    public static void updateStringProperty(Resource resource, Property property, String value){
        resource.removeAll(property);
        if (value != null && !value.isBlank()){
            resource.addProperty(property, value);
        }
    }

    public static void addLiteral(Resource resource, Property property, Object value) {
        if (value != null) {
            resource.addLiteral(property, value);
        }
    }
    public static void updateLiteral(Resource resource, Property property, Object value){
        resource.removeAll(property);
        if (value != null) {
            resource.addLiteral(property, value);
        }
    }

    /**
     * Update Uri property
     * If string is empty|blank value is removed
     * @param resource Resource
     * @param property Property
     * @param value Value
     */
    public static void updateUriProperty(Resource resource, Property property, String value) {
        resource.removeAll(property);
        if (value != null && !value.isBlank()) {
            resource.addProperty(property, ResourceFactory.createResource(value));
        }
    }

    public static void updateUriPropertyAndAddReferenceNamespaces(Resource modelResource, Resource resource, Property property, String value) {
        resource.removeAll(property);
        if (value != null && !value.isBlank()) {
            addResourceRelationship(modelResource, resource, property, value);
        }
    }

    /**
     * Adds an optional string property
     * This has a null check, so it does not need to be separately added
     * @param resource Resource
     * @param property Property
     * @param value Value
     */
    public static void addOptionalStringProperty(Resource resource, Property property, String value){
        if(value != null && !value.isBlank()){
            resource.addProperty(property, value);
        }
    }

    public static void addOptionalUriProperty(Resource resource, Property property, String value){
        if(value != null && !value.isBlank()){
            resource.addProperty(property, ResourceFactory.createResource(value));
        }
    }

    public static void addBooleanResourceType(Resource resource, Resource type, Boolean value) {
        if(value != null && value) {
            resource.addProperty(RDF.type, type);
        }
    }

    public static void updateBooleanTypeProperty(Model model, Resource resource, Resource type, Boolean value) {
        model.remove(resource, RDF.type, type);
        if(value != null && value) {
            resource.addProperty(RDF.type, type);
        }
    }


    /**
     * Adds resource relationship to resource.
     * Adds reference to the model (owl:imports or dcterms:requires) if not exists
     */
    public static void addResourceRelationship(Resource modelResource, Resource resource, Property property, String resourceURI) {
        if (resourceURI == null) {
            return;
        }

        var namespaces = new HashSet<String>();
        namespaces.addAll(arrayPropertyToSet(modelResource, OWL.imports));
        namespaces.addAll(arrayPropertyToSet(modelResource, DCTerms.requires));
        namespaces.add(modelResource.getURI());

        var refNamespace = DataModelURI.fromURI(resourceURI).getGraphURI();

        // reference already added
        if (namespaces.contains(refNamespace)) {
            resource.addProperty(property, ResourceFactory.createResource(resourceURI));
            return;
        }

        Property referenceProperty;
        if (refNamespace.startsWith(ModelConstants.SUOMI_FI_NAMESPACE)) {
            if (isLibrary(modelResource)) {
                referenceProperty = OWL.imports;
            } else {
                // object of these properties belongs to library
                var importsProperties = List.of(SH.path, SH.targetClass, SH.class_);
                referenceProperty = importsProperties.contains(property)
                        ? DCTerms.requires
                        : OWL.imports;
            }
        } else {
            // external namespaces are not added automatically
            throw new MappingError(String.format("Namespace %s not in owl:imports or dcterms:requires", refNamespace));
        }

        modelResource.addProperty(referenceProperty, refNamespace);
        resource.addProperty(property, ResourceFactory.createResource(resourceURI));
    }

    /**
     * Checks if the type property (RDF:type) of the resource is particular type
     * @param resource Resource to check
     * @param type Type to check
     * @return if resource has given type
     */
    public static boolean hasType(Resource resource, Resource... type) {
        if (!resource.hasProperty(RDF.type)) {
            return false;
        }
        var typeList = resource.listProperties(RDF.type).toList();
        return Arrays.stream(type)
                .anyMatch(t -> typeList.stream().anyMatch(r -> r.getResource().equals(t)));
    }

    public static boolean isApplicationProfile(Resource resource) {
        return hasType(resource, SuomiMeta.ApplicationProfile);
    }

    public static boolean isLibrary(Resource resource) {
        return hasType(resource, OWL.Ontology) && !hasType(resource, SuomiMeta.ApplicationProfile);
    }

    public static void addCreationMetadata(Resource resource, YtiUser user) {
        var creationDate = new XSDDateTime(Calendar.getInstance());
        resource.addProperty(DCTerms.modified, ResourceFactory.createTypedLiteral(creationDate))
                .addProperty(DCTerms.created, ResourceFactory.createTypedLiteral(creationDate))
                .addProperty(SuomiMeta.creator, user.getId().toString())
                .addProperty(SuomiMeta.modifier, user.getId().toString());
    }

    public static void addUpdateMetadata(Resource resource, YtiUser user) {
        var updateDate = new XSDDateTime(Calendar.getInstance());
        resource.removeAll(DCTerms.modified);
        resource.addProperty(DCTerms.modified, ResourceFactory.createTypedLiteral(updateDate));
        resource.removeAll(SuomiMeta.modifier);
        resource.addProperty(SuomiMeta.modifier, user.getId().toString());
    }

    public static void mapCreationInfo(ResourceCommonDTO dto,
                                        Resource resource,
                                        Consumer<ResourceCommonDTO> userMapper) {
        var created = resource.getProperty(DCTerms.created).getLiteral().getString();
        var modified = resource.getProperty(DCTerms.modified).getLiteral().getString();
        dto.setCreated(created);
        dto.setModified(modified);
        dto.setCreator(new UserDTO(MapperUtils.propertyToString(resource, SuomiMeta.creator)));
        dto.setModifier(new UserDTO(MapperUtils.propertyToString(resource, SuomiMeta.modifier)));

        if (userMapper != null) {
            userMapper.accept(dto);
        }
    }

    public static Resource addCommonResourceInfo(Model model, DataModelURI uri, BaseDTO dto) {
        var modelResource = model.getResource(uri.getModelURI());
        var languages = MapperUtils.arrayPropertyToSet(modelResource, DCTerms.language);
        var status = MapperUtils.propertyToString(modelResource, SuomiMeta.publicationStatus);

        var resource = model.createResource(uri.getResourceURI())
                .addProperty(SuomiMeta.publicationStatus, ResourceFactory.createResource(status))
                .addProperty(RDFS.isDefinedBy, modelResource)
                .addProperty(DCTerms.identifier, ResourceFactory.createTypedLiteral(dto.getIdentifier(), XSDDatatype.XSDNCName));
        MapperUtils.addLocalizedProperty(languages, dto.getLabel(), resource, RDFS.label, model);
        MapperUtils.addLocalizedProperty(languages, dto.getNote(), resource, RDFS.comment, model);
        MapperUtils.addOptionalStringProperty(resource, SKOS.editorialNote, dto.getEditorialNote());
        MapperUtils.addOptionalUriProperty(resource, DCTerms.subject, dto.getSubject());
        return resource;
    }

    public static void updateCommonResourceInfo(Model model, Resource resource, Resource modelResource, BaseDTO dto) {
        var languages = MapperUtils.arrayPropertyToSet(modelResource, DCTerms.language);

        MapperUtils.updateLocalizedProperty(languages, dto.getLabel(), resource, RDFS.label, model);
        MapperUtils.updateLocalizedProperty(languages, dto.getNote(), resource, RDFS.comment, model);
        MapperUtils.updateStringProperty(resource, SKOS.editorialNote, dto.getEditorialNote());
        MapperUtils.updateUriProperty(resource, DCTerms.subject, dto.getSubject());
    }

    public static void addCommonResourceDtoInfo(ResourceInfoBaseDTO dto, Resource resource, Resource modelResource, Model orgModel, boolean hasRightToModel) {
        var uriDTO = MapperUtils.uriToURIDTO(resource.getURI(), resource.getModel());
        dto.setUri(uriDTO.getUri());
        dto.setCurie(uriDTO.getCurie());

        dto.setLabel(MapperUtils.localizedPropertyToMap(resource, RDFS.label));
        dto.setStatus(MapperUtils.getStatusFromUri(MapperUtils.propertyToString(resource, SuomiMeta.publicationStatus)));

        var subject = MapperUtils.propertyToString(resource, DCTerms.subject);
        if (subject != null) {
            var conceptDTO = new ConceptDTO();
            conceptDTO.setConceptURI(subject);
            dto.setSubject(conceptDTO);
        }
        dto.setIdentifier(resource.getLocalName());
        if (hasRightToModel) {
            dto.setEditorialNote(MapperUtils.propertyToString(resource, SKOS.editorialNote));
        }

        var contributors = MapperUtils.arrayPropertyToSet(modelResource, DCTerms.contributor);
        dto.setContributor(OrganizationMapper.mapOrganizationsToDTO(contributors, orgModel));
        dto.setContact(MapperUtils.propertyToString(modelResource, SuomiMeta.contact));
        dto.setNote(MapperUtils.localizedPropertyToMap(resource, RDFS.comment));
    }

    public static UriDTO uriToURIDTO(String uri, Model model) {
        if (uri == null) {
            return null;
        }
        var u = DataModelURI.fromURI(uri);

        Map<String,String> label = null;
        var resource = model.getResource(uri);
        if (resource.hasProperty(RDFS.label)) {
            label = localizedPropertyToMap(resource, RDFS.label);
        }

        // if resource is part of the model, add version information to URI (if not draft version)
        if (resource.listProperties().hasNext()
                && model.getResource(u.getModelURI()).hasProperty(OWL2.versionInfo)) {
            u = DataModelURI.createResourceURI(u.getModelId(),
                    u.getResourceId(),
                    propertyToString(model.getResource(u.getModelURI()), OWL2.versionInfo));
        }

        return new UriDTO(u.getResourceVersionURI(), u.getCurie(model.getGraph().getPrefixMapping()), label);
    }

    public static Set<UriDTO> uriToURIDTOs(Collection<String> uris, Model model) {
        return uris.stream()
                .map(s -> uriToURIDTO(s, model))
                .collect(Collectors.toSet());
    }

    public static void addLabelsToURIs(ResourceInfoBaseDTO dto, Consumer<Set<UriDTO>> mapper) {
        var uris = new HashSet<UriDTO>();

        if (dto instanceof ClassInfoDTO classInfoDTO) {
            uris.addAll(classInfoDTO.getSubClassOf());
            uris.addAll(classInfoDTO.getEquivalentClass());
            uris.addAll(classInfoDTO.getDisjointWith());
        } else if (dto instanceof NodeShapeInfoDTO nodeShapeInfoDTO) {
            uris.add(nodeShapeInfoDTO.getTargetNode());
            uris.add(nodeShapeInfoDTO.getTargetClass());
        } else if (dto instanceof PropertyShapeInfoDTO propertyShapeInfoDTO) {
            uris.add(propertyShapeInfoDTO.getClassType());
            uris.add(propertyShapeInfoDTO.getPath());
        } else if (dto instanceof ResourceInfoDTO resourceInfoDTO) {
            uris.add(resourceInfoDTO.getRange());
            uris.add(resourceInfoDTO.getDomain());
            uris.addAll(resourceInfoDTO.getEquivalentResource());
            uris.addAll(resourceInfoDTO.getSubResourceOf());
        }

        mapper.accept(uris);
    }

    public static RDFList getList(Model model, Resource resource, Property property) {
        var obj = resource.getProperty(property).getObject();

        if (!obj.canAs(RDFList.class)) {
            throw new MappingError(String.format("Property %s in resource %s is not RDFList",
                    property, resource));
        }
        return model.getList(obj.asResource());
    }

    public static Resource getModelResourceFromVersion(Model model) {
        var typeStmt = model.listStatements(null, RDF.type, OWL.Ontology);
        if (!typeStmt.hasNext()) {
            throw new MappingError("Invalid datamodel added to internal namespaces");
        }
        return model.getResource(typeStmt.next().getSubject().getURI());
    }

    public static void addTerminologyReference(BaseDTO dto, Resource modelResource) {
        if (dto.getSubject() != null) {
            var terminologyURI = DataModelUtils.removeTrailingSlash(NodeFactory.createURI(dto.getSubject()).getNameSpace());
            var requires = MapperUtils.arrayPropertyToSet(modelResource, DCTerms.requires);
            if (!requires.contains(terminologyURI)) {
                modelResource.addProperty(DCTerms.requires, terminologyURI);
            }
        }
    }

    /**
     * Rename a resource in a datamodel
     * This will change the identifier of the resource and the URI
     *
     * @param resource      Resource to rename
     * @param newIdentifier New identifier of resource
     * @return Renamed resource
     */
    public static Resource renameResource(Resource resource, String newIdentifier) {
        resource.removeAll(DCTerms.identifier);
        resource.addProperty(DCTerms.identifier, ResourceFactory.createTypedLiteral(newIdentifier, XSDDatatype.XSDNCName));
        return ResourceUtils.renameResource(resource, resource.getNameSpace() + newIdentifier);
    }

    /**
     * Return curie for any resource based on model's prefix mapping. If prefix not found, return resource's uri
     * @param resource resource
     * @param model model with prefix mapping
     * @return curie e.g. prefix:localName
     */
    public static String getCurie(Resource resource, Model model) {
        var prefixMapping = model.getGraph().getPrefixMapping();
        var prefix = prefixMapping.getNsURIPrefix(resource.getNameSpace());
        if (prefix != null) {
            return String.format("%s:%s", prefix, resource.getLocalName());
        } else {
            return resource.getURI();
        }
    }

    /**
     * Creates a copy of given model and renames all resource URIs to given new prefix.
     * All version related information and creating / modifying metadata will be reset.
     * @param model model to be copied
     * @param user copier user
     * @param oldURI old graph
     * @param newURI new graph
     * @return copied model
     */
    public static Model mapCopyModel(Model model, YtiUser user, DataModelURI oldURI, DataModelURI newURI) {
        var copy = ModelFactory.createDefaultModel().add(model);

        var now = ResourceFactory.createTypedLiteral(new XSDDateTime(Calendar.getInstance()));
        var newStatus = MapperUtils.getStatusUri(Status.DRAFT);

        // rename all resources with new prefix
        copy.listSubjects()
                .filterDrop(RDFNode::isAnon)
                .filterKeep(subject -> subject.getNameSpace().equals(oldURI.getNamespace()))
                .forEach(subject -> {
                    var newSubject = DataModelURI.createResourceURI(newURI.getModelId(), subject.getLocalName()).getResourceURI();
                    MapperUtils.addUpdateMetadata(subject, user);
                    subject.removeAll(DCTerms.created);
                    subject.removeAll(SuomiMeta.creator);
                    subject.addProperty(DCTerms.created, now);
                    subject.addProperty(SuomiMeta.creator, user.getId().toString());
                    MapperUtils.updateUriProperty(subject, SuomiMeta.publicationStatus, newStatus);

                    ResourceUtils.renameResource(subject, newSubject);
                });

        var modelResource = copy.getResource(newURI.getModelURI());

        // add suffix (Copy) to data model's label
        var label = MapperUtils.localizedPropertyToMap(modelResource, RDFS.label);
        modelResource.removeAll(RDFS.label);
        label.forEach((lang, value) ->
                modelResource.addProperty(RDFS.label, ResourceFactory.createLangLiteral(value + " (Copy)", lang)));

        MapperUtils.updateStringProperty(modelResource, DCAP.preferredXMLNamespace, newURI.getGraphURI());
        MapperUtils.updateStringProperty(modelResource, DCAP.preferredXMLNamespacePrefix, newURI.getModelId());
        MapperUtils.updateUriProperty(modelResource, SuomiMeta.publicationStatus, newStatus);

        // remove creator and all version related information
        modelResource.removeAll(OWL.priorVersion)
                .removeAll(OWL.versionInfo)
                .removeAll(OWL2.versionIRI)
                .removeAll(DCTerms.created)
                .removeAll(SuomiMeta.creator)
                .removeAll(SuomiMeta.contentModified);

        MapperUtils.addUpdateMetadata(modelResource, user);
        modelResource.addProperty(SuomiMeta.contentModified, now);
        modelResource.addProperty(DCTerms.created, now);
        modelResource.addProperty(SuomiMeta.creator, user.getId().toString());

        MapperUtils.updateStringProperty(modelResource, SuomiMeta.copiedFrom, oldURI.getGraphURI());
        return copy;
    }
}
