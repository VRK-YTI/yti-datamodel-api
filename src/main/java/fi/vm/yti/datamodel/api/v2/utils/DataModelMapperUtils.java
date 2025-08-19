package fi.vm.yti.datamodel.api.v2.utils;

import fi.vm.yti.common.Constants;
import fi.vm.yti.common.enums.GraphType;
import fi.vm.yti.common.enums.Status;
import fi.vm.yti.common.exception.MappingError;
import fi.vm.yti.common.mapper.OrganizationMapper;
import fi.vm.yti.common.properties.SuomiMeta;
import fi.vm.yti.common.util.MapperUtils;
import fi.vm.yti.datamodel.api.v2.dto.*;
import fi.vm.yti.datamodel.api.v2.properties.DCAP;
import fi.vm.yti.security.YtiUser;
import org.apache.jena.datatypes.xsd.XSDDatatype;
import org.apache.jena.datatypes.xsd.XSDDateTime;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.rdf.model.*;
import org.apache.jena.util.ResourceUtils;
import org.apache.jena.vocabulary.*;
import org.topbraid.shacl.vocabulary.SH;

import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class DataModelMapperUtils {

    private DataModelMapperUtils() {
        //Util class so we need to hide constructor
    }

    public static GraphType getModelTypeFromResource(Resource resource) {
        if (MapperUtils.isApplicationProfile(resource)) {
            return GraphType.PROFILE;
        } else if (MapperUtils.isLibrary(resource)) {
            return GraphType.LIBRARY;
        }
        return GraphType.PROFILE;
    }

    public static void updateUriPropertyAndAddReferenceNamespaces(Resource modelResource, Resource resource, Property property, String value) {
        resource.removeAll(property);
        if (value != null && !value.isBlank()) {
            addResourceRelationship(modelResource, resource, property, value);
        }
    }

    public static void addBooleanResourceType(Resource resource, Resource type, Boolean value) {
        if (value != null && value) {
            resource.addProperty(RDF.type, type);
        }
    }

    public static void updateBooleanTypeProperty(Model model, Resource resource, Resource type, Boolean value) {
        model.remove(resource, RDF.type, type);
        if (value != null && value) {
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
        namespaces.addAll(MapperUtils.arrayPropertyToSet(modelResource, OWL.imports));
        namespaces.addAll(MapperUtils.arrayPropertyToSet(modelResource, DCTerms.requires));
        namespaces.add(modelResource.getURI());

        var refNamespace = DataModelURI.Factory.fromURI(resourceURI).getGraphURI();

        // reference already added
        if (namespaces.contains(refNamespace)) {
            resource.addProperty(property, ResourceFactory.createResource(resourceURI));
            return;
        }

        Property referenceProperty;
        if (refNamespace.startsWith(Constants.DATA_MODEL_NAMESPACE)) {
            if (MapperUtils.isLibrary(modelResource)) {
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

    public static Resource addCommonResourceInfo(Model model, DataModelURI uri, BaseDTO dto) {
        var modelResource = model.getResource(uri.getModelURI());
        var languages = MapperUtils.arrayPropertyToSet(modelResource, DCTerms.language);
        var status = MapperUtils.propertyToString(modelResource, SuomiMeta.publicationStatus);

        var resource = model.createResource(uri.getResourceURI())
                .addProperty(SuomiMeta.publicationStatus, ResourceFactory.createResource(status))
                .addProperty(RDFS.isDefinedBy, modelResource)
                .addProperty(DCTerms.identifier, ResourceFactory.createTypedLiteral(dto.getIdentifier(), XSDDatatype.XSDNCName));
        MapperUtils.addLocalizedProperty(languages, dto.getLabel(), resource, RDFS.label);
        MapperUtils.addLocalizedProperty(languages, dto.getNote(), resource, RDFS.comment);
        MapperUtils.addOptionalStringProperty(resource, SKOS.editorialNote, dto.getEditorialNote());
        MapperUtils.addOptionalUriProperty(resource, DCTerms.subject, dto.getSubject());
        return resource;
    }

    public static void updateCommonResourceInfo(Resource resource, Resource modelResource, BaseDTO dto) {
        var languages = MapperUtils.arrayPropertyToSet(modelResource, DCTerms.language);

        MapperUtils.updateLocalizedProperty(languages, dto.getLabel(), resource, RDFS.label);
        MapperUtils.updateLocalizedProperty(languages, dto.getNote(), resource, RDFS.comment);
        MapperUtils.updateStringProperty(resource, SKOS.editorialNote, dto.getEditorialNote());
        MapperUtils.updateUriProperty(resource, DCTerms.subject, dto.getSubject());
    }

    public static void addCommonResourceDtoInfo(ResourceInfoBaseDTO dto, Resource resource, Resource modelResource, Model orgModel, boolean hasRightToModel) {
        var uriDTO = uriToURIDTO(resource.getURI(), resource.getModel());
        if (uriDTO == null) {
            return;
        }

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
        var u = DataModelURI.Factory.fromURI(uri);

        Map<String, String> label = null;
        var resource = model.getResource(uri);
        if (resource.hasProperty(RDFS.label)) {
            label = MapperUtils.localizedPropertyToMap(resource, RDFS.label);
        }

        // if resource is part of the model, add version information to URI (if not draft version)
        if (resource.listProperties().hasNext()
            && model.getResource(u.getModelURI()).hasProperty(OWL2.versionInfo)) {
            u = DataModelURI.Factory.createResourceURI(u.getModelId(),
                    u.getResourceId(),
                    MapperUtils.propertyToString(model.getResource(u.getModelURI()), OWL2.versionInfo));
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
     *
     * @param resource resource
     * @param model    model with prefix mapping
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
     *
     * @param model  model to be copied
     * @param user   copier user
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
                    var newSubject = DataModelURI.Factory.createResourceURI(newURI.getModelId(), subject.getLocalName()).getResourceURI();
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
