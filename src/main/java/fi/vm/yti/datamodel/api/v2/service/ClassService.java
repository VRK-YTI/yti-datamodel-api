package fi.vm.yti.datamodel.api.v2.service;

import fi.vm.yti.datamodel.api.security.AuthorizationManager;
import fi.vm.yti.datamodel.api.v2.dto.*;
import fi.vm.yti.datamodel.api.v2.endpoint.error.MappingError;
import fi.vm.yti.datamodel.api.v2.endpoint.error.ResourceNotFoundException;
import fi.vm.yti.datamodel.api.v2.mapper.ClassMapper;
import fi.vm.yti.datamodel.api.v2.mapper.MapperUtils;
import fi.vm.yti.datamodel.api.v2.mapper.ResourceMapper;
import fi.vm.yti.datamodel.api.v2.opensearch.dto.ResourceSearchRequest;
import fi.vm.yti.datamodel.api.v2.opensearch.index.IndexResourceInfo;
import fi.vm.yti.datamodel.api.v2.opensearch.index.OpenSearchIndexer;
import fi.vm.yti.datamodel.api.v2.repository.CoreRepository;
import fi.vm.yti.datamodel.api.v2.repository.ImportsRepository;
import fi.vm.yti.datamodel.api.v2.utils.DataModelUtils;
import fi.vm.yti.security.AuthenticatedUserProvider;
import org.apache.jena.arq.querybuilder.AskBuilder;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.rdf.model.SimpleSelector;
import org.apache.jena.vocabulary.OWL;
import org.apache.jena.vocabulary.RDFS;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.topbraid.shacl.vocabulary.SH;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static fi.vm.yti.security.AuthorizationException.check;

@Service
public class ClassService {

    private final Logger logger = LoggerFactory.getLogger(ClassService.class);

    private final ResourceService resourceService;
    private final CoreRepository coreRepository;
    private final ImportsRepository importsRepository;

    private final AuthorizationManager authorizationManager;
    private final AuthenticatedUserProvider userProvider;
    private final TerminologyService terminologyService;
    private final GroupManagementService groupManagementService;
    private final OpenSearchIndexer openSearchIndexer;
    private final SearchIndexService searchIndexService;

    @Autowired
    public ClassService(ResourceService resourceService,
                        CoreRepository coreRepository,
                        ImportsRepository importsRepository,
                        AuthorizationManager authorizationManager,
                        AuthenticatedUserProvider userProvider,
                        TerminologyService terminologyService,
                        GroupManagementService groupManagementService,
                        OpenSearchIndexer openSearchIndexer,
                        SearchIndexService searchIndexService) {
        this.resourceService = resourceService;
        this.coreRepository = coreRepository;
        this.importsRepository = importsRepository;
        this.authorizationManager = authorizationManager;
        this.userProvider = userProvider;
        this.terminologyService = terminologyService;
        this.groupManagementService = groupManagementService;
        this.openSearchIndexer = openSearchIndexer;
        this.searchIndexService = searchIndexService;
    }

    public ResourceInfoBaseDTO get(String prefix, String version, String classIdentifier) {
        var modelURI = ModelConstants.SUOMI_FI_NAMESPACE + prefix;

        var versionUri = modelURI;
        if(version != null){
            versionUri += ModelConstants.RESOURCE_SEPARATOR + version;
        }

        var classURI = modelURI + ModelConstants.RESOURCE_SEPARATOR + classIdentifier;
        if(!coreRepository.resourceExistsInGraph(versionUri , classURI)){
            throw new ResourceNotFoundException(classURI);
        }
        var model = coreRepository.fetch(versionUri);
        var hasRightToModel = authorizationManager.hasRightToModel(prefix, model);

        var orgModel = coreRepository.getOrganizations();
        var userMapper = hasRightToModel ? groupManagementService.mapUser() : null;

        ResourceInfoBaseDTO dto;
        var modelResource = model.getResource(modelURI);
        var includedNamespaces = DataModelUtils.getInternalReferenceModels(versionUri, modelResource);

        if (MapperUtils.isLibrary(modelResource)) {
            dto = ClassMapper.mapToClassDTO(model, modelURI, classIdentifier, orgModel,
                    hasRightToModel, userMapper);

            var classResource = model.getResource(classURI);
            var restrictions = ClassMapper.getClassRestrictionList(model, classResource)
                    .stream()
                    .filter(r -> r.hasProperty(OWL.onProperty))
                    .map(restriction -> {
                        var restrictionDTO = new SimpleResourceDTO();

                        restrictionDTO.setUri(MapperUtils.propertyToString(restriction, OWL.onProperty));
                        restrictionDTO.setRange(MapperUtils.uriToURIDTO(
                                MapperUtils.propertyToString(restriction, OWL.someValuesFrom), model));
                        return restrictionDTO;
                    }).collect(Collectors.toSet());

            var restrictionURIs = restrictions.stream()
                    .map(SimpleResourceDTO::getUri)
                    .collect(Collectors.toSet());
            var findResourcesModel = resourceService.findResources(restrictionURIs, includedNamespaces);

            ClassMapper.addClassResourcesToDTO(findResourcesModel, restrictions, (ClassInfoDTO) dto, terminologyService.mapConceptToResource());
        } else {
            dto = ClassMapper.mapToNodeShapeDTO(model, modelURI, classIdentifier, orgModel,
                    hasRightToModel, userMapper);
            var existingProperties = getTargetNodeProperties(MapperUtils.propertyToString(model.getResource(classURI), SH.node), includedNamespaces);
            var nodeShapeResources = coreRepository.queryConstruct(ClassMapper.getNodeShapeResourcesQuery(classURI));
            ClassMapper.addNodeShapeResourcesToDTO(model, nodeShapeResources, (NodeShapeInfoDTO) dto, existingProperties);
        }

        terminologyService.mapConcept().accept(dto);
        MapperUtils.addLabelsToURIs(dto, resourceService.mapUriLabels(includedNamespaces));
        return dto;
    }

    public ExternalClassDTO getExternal(String uri) {
        var namespace = NodeFactory.createURI(uri).getNameSpace();
        var model = importsRepository.fetch(namespace);

        var dto = ClassMapper.mapExternalClassToDTO(model, uri);
        var resources = importsRepository.queryConstruct(ClassMapper.getClassResourcesQuery(uri, true));
        ClassMapper.addExternalClassResourcesToDTO(resources, dto);

        return dto;
    }

    public List<IndexResourceInfo> getNodeShapes(String targetClass) throws IOException {
        var request = new ResourceSearchRequest();
        request.setStatus(Set.of(Status.VALID, Status.SUGGESTED));
        request.setTargetClass(targetClass);
        return searchIndexService
                .searchInternalResourcesWithInfo(request, userProvider.getUser())
                .getResponseObjects();
    }

    public URI create(String prefix, BaseDTO dto, boolean applicationProfile) throws URISyntaxException {
        var modelUri = ModelConstants.SUOMI_FI_NAMESPACE + prefix;
        var classUri = modelUri + ModelConstants.RESOURCE_SEPARATOR + dto.getIdentifier();
        if(coreRepository.resourceExistsInGraph(modelUri, classUri)){
            throw new MappingError("Class already exists");
        }
        var model = coreRepository.fetch(modelUri);
        var modelResource = model.getResource(modelUri);
        check(authorizationManager.hasRightToModel(prefix, model));
        checkDataModelType(modelResource, dto);
        terminologyService.resolveConcept(dto.getSubject());

        var includedNamespaces = DataModelUtils.getInternalReferenceModels(modelUri, modelResource);

        if(applicationProfile) {
            ClassMapper.createNodeShapeAndMapToModel(modelUri, model, (NodeShapeDTO) dto, userProvider.getUser());
            addNodeShapeProperties(model, classUri, (NodeShapeDTO) dto, includedNamespaces);
        }else {
            ClassMapper.createOntologyClassAndMapToModel(modelUri, model, (ClassDTO) dto, userProvider.getUser());
        }

        coreRepository.put(modelUri, model);
        openSearchIndexer.createResourceToIndex(ResourceMapper.mapToIndexResource(model, classUri));
        return new URI(classUri);
    }

    public void addNodeShapeProperties(Model model, String classUri, NodeShapeDTO dto, Set<String> includedNamespaces) {
        var allProperties = new HashSet<String>();
        // Node shape based on an existing node shape (sh:node)
        allProperties.addAll(getTargetNodeProperties(dto.getTargetNode(), includedNamespaces));
        // User defined properties from target class reference
        allProperties.addAll(getNodeShapeTargetClassProperties(dto, model, classUri));

        ClassMapper.mapNodeShapeProperties(model, classUri, allProperties);
    }

    public void checkDataModelType(Resource modelResource, BaseDTO dto) {
        if (dto instanceof NodeShapeDTO && MapperUtils.isLibrary(modelResource)) {
            throw new MappingError("Cannot add node shape to ontology");
        } else if (dto instanceof ClassDTO && MapperUtils.isApplicationProfile(modelResource)) {
            throw new MappingError("Cannot add ontology class to application profile");
        }
    }

    public void update(String prefix, String classIdentifier, BaseDTO dto) {
        logger.info("Updating class {}", classIdentifier);

        var graph = ModelConstants.SUOMI_FI_NAMESPACE + prefix;
        var classURI = graph + ModelConstants.RESOURCE_SEPARATOR + classIdentifier;
        if(!coreRepository.resourceExistsInGraph(graph, classURI)){
            throw new ResourceNotFoundException(classIdentifier);
        }
        var model = coreRepository.fetch(graph);
        checkDataModelType(model.getResource(graph), dto);
        check(authorizationManager.hasRightToModel(prefix, model));
        terminologyService.resolveConcept(dto.getSubject());

        var classResource = model.getResource(classURI);
        if (MapperUtils.isLibrary(model.getResource(graph))) {
            var classDTO = (ClassDTO) dto;
            resourceService.checkCyclicalReferences(classDTO.getEquivalentClass(), OWL.equivalentClass, classURI);
            resourceService.checkCyclicalReferences(classDTO.getSubClassOf(), RDFS.subClassOf, classURI);
            resourceService.checkCyclicalReferences(classDTO.getDisjointWith(), OWL.disjointWith, classURI);
            ClassMapper.mapToUpdateOntologyClass(model, graph, classResource, classDTO, userProvider.getUser());
        } else {
            var nodeShape = (NodeShapeDTO) dto;

            if(nodeShape.getTargetNode() != null && nodeShape.getTargetNode().equals(classResource.getURI())) {
                throw new MappingError("Target node is a self reference");
            }
            resourceService.checkCyclicalReference(nodeShape.getTargetClass(), SH.targetClass, classURI);
            resourceService.checkCyclicalReference(nodeShape.getTargetNode(), SH.targetNode, classURI);

            var oldNode = MapperUtils.propertyToString(classResource, SH.node);
            var oldTarget = MapperUtils.propertyToString(classResource, SH.targetClass);
            var includedNamespaces = DataModelUtils.getInternalReferenceModels(graph, model.getResource(graph));

            var nodeShapeProperties = classResource.listProperties(SH.property)
                    .mapWith((var stmt) -> stmt.getResource().getURI())
                    .toSet();

            if (oldNode == null && nodeShape.getTargetNode() != null) {
                // add new sh:node, add properties from sh:node reference
                nodeShapeProperties.addAll(getTargetNodeProperties(nodeShape.getTargetNode(), includedNamespaces));
            } else if (oldNode != null && nodeShape.getTargetNode() != null && !oldNode.equals(nodeShape.getTargetNode())) {
                // replace sh:node, remove properties inherited from old sh:node and add properties from new sh:node reference
                var oldProperties = getTargetNodeProperties(oldNode, includedNamespaces);
                nodeShapeProperties.removeAll(oldProperties);
                nodeShapeProperties.addAll(getTargetNodeProperties(nodeShape.getTargetNode(), includedNamespaces));
            } else if (oldNode != null && nodeShape.getTargetNode() == null) {
                // remove sh:node, remove properties old sh:node reference
                var oldProperties = getTargetNodeProperties(oldNode, includedNamespaces);
                nodeShapeProperties.removeAll(oldProperties);
            }

            // add properties from new target class and create placeholders
            if (nodeShape.getTargetClass() != null && !nodeShape.getTargetClass().equals(oldTarget)) {
                nodeShapeProperties.addAll(getNodeShapeTargetClassProperties(nodeShape, model, classURI));
            }

            ClassMapper.mapToUpdateNodeShape(model, graph, classResource, (NodeShapeDTO) dto,
                    nodeShapeProperties, userProvider.getUser());
        }
        coreRepository.put(graph, model);

        var indexClass = ResourceMapper.mapToIndexResource(model, classURI);
        openSearchIndexer.updateResourceToIndex(indexClass);
    }

    /**
     * Delete a Class or Node shape
     * @param prefix Model prefix
     * @param identifier Resource identifier
     */
    public void delete(String prefix, String identifier) {
        var modelURI = ModelConstants.SUOMI_FI_NAMESPACE + prefix;
        var classURI = modelURI + ModelConstants.RESOURCE_SEPARATOR + identifier;
        if(!coreRepository.resourceExistsInGraph(modelURI , classURI)){
            throw new ResourceNotFoundException(classURI);
        }
        var model = coreRepository.fetch(modelURI);
        check(authorizationManager.hasRightToModel(prefix, model));
        coreRepository.deleteResource(classURI);
        openSearchIndexer.deleteResourceFromIndex(classURI);
    }

    private Set<String> getTargetNodeProperties(String targetNode, Set<String> graphsIncluded) {
        var propertyShapes = new HashSet<String>();
        var handledNodeShapes = new HashSet<String>();

        // collect recursively all property shape uris from target node
        while (targetNode != null) {
            var targetNodeURI = DataModelUtils.removeVersionFromURI(targetNode);
            if (handledNodeShapes.contains(targetNode)) {
                throw new MappingError("Circular dependency, cannot add sh:node reference");
            }
            handledNodeShapes.add(targetNode);

            var nodeModel = resourceService.findResources(Set.of(targetNode), graphsIncluded);
            var nodeResource = nodeModel.getResource(targetNodeURI);

            propertyShapes.addAll(nodeResource.listProperties(SH.property)
                    .mapWith((var stmt) -> stmt.getResource().getURI())
                    .toList());

            if (!nodeResource.hasProperty(SH.node)) {
                break;
            }
            targetNode = nodeResource.getProperty(SH.node).getObject().toString();
        }
        return propertyShapes;
    }

    private Set<String> getNodeShapeTargetClassProperties(NodeShapeDTO nodeShapeDTO, Model model, String classURI) {
        // skip creating new resource if there is already resource with sh:path reference to the property
        var existingProperties = nodeShapeDTO.getProperties().stream()
                .map(p -> {
                    var iter = model.listStatements(new SimpleSelector(null, SH.path, ResourceFactory.createResource(p)));
                    return iter.hasNext()
                            ? iter.next().getSubject().getURI()
                            : null;
                })
                .filter(Objects::nonNull)
                .toList();

        var modelURI = DataModelUtils.removeTrailingSlash(NodeFactory.createURI(classURI).getNameSpace());
        var included = DataModelUtils.getInternalReferenceModels(modelURI, model.getResource(modelURI));
        var newProperties = nodeShapeDTO.getProperties().stream()
                .filter(p -> !existingProperties.contains(p))
                .collect(Collectors.toSet());
        var newPropertiesModel = resourceService.findResources(newProperties, included);

        Predicate<String> checkFreeIdentifier =
                (var uri) -> coreRepository.resourceExistsInGraph(model.getResource(classURI).getNameSpace(), uri);

        // create new property shape resources to the model
        var createdProperties = ClassMapper.mapPlaceholderPropertyShapes(model, classURI, newPropertiesModel,
                userProvider.getUser(), checkFreeIdentifier);

        var allProperties = new HashSet<String>();
        allProperties.addAll(existingProperties);
        allProperties.addAll(createdProperties);

        // index new resources
        openSearchIndexer.bulkInsert(OpenSearchIndexer.OPEN_SEARCH_INDEX_RESOURCE,
                createdProperties.stream()
                        .map(p -> ResourceMapper.mapToIndexResource(model, p))
                        .toList());

        return allProperties;
    }

    public boolean exists(String prefix, String identifier) {
        // identifiers e.g. corner-abcd1234 are reserved for visualization
        if (identifier.startsWith("corner-")) {
            return true;
        }
        var graphUri = ModelConstants.SUOMI_FI_NAMESPACE + prefix;
        return coreRepository.resourceExistsInGraph(graphUri, graphUri + ModelConstants.RESOURCE_SEPARATOR + identifier);
    }

    public void handlePropertyShapeReference(String prefix, String nodeShapeIdentifier, String uri, boolean delete) {
        var modelURI = ModelConstants.SUOMI_FI_NAMESPACE + prefix;
        var model = coreRepository.fetch(modelURI);
        var classURI = modelURI + ModelConstants.RESOURCE_SEPARATOR + nodeShapeIdentifier;
        check(authorizationManager.hasRightToModel(prefix, model));

        var includedNamespaces = DataModelUtils.getInternalReferenceModels(modelURI, model.getResource(modelURI));
        var classResource = model.getResource(classURI);
        var existingProperties = getTargetNodeProperties(MapperUtils.propertyToString(classResource, SH.node), includedNamespaces);
        if(delete) {
            ClassMapper.mapRemoveNodeShapeProperty(model, classResource, uri, existingProperties);
        }else {
            ClassMapper.mapAppendNodeShapeProperty(classResource, uri, existingProperties);
        }
        coreRepository.put(modelURI, model);
    }

    public void handleUpdateClassRestrictionReference(String prefix, String classIdentifier, String restrictionURI, String currentTarget, String newTarget) {
        var modelURI = ModelConstants.SUOMI_FI_NAMESPACE + prefix;
        var model = coreRepository.fetch(modelURI);
        var classURI = modelURI + ModelConstants.RESOURCE_SEPARATOR + classIdentifier;
        if (!coreRepository.resourceExistsInGraph(modelURI, classURI)) {
            throw new ResourceNotFoundException(classURI);
        }
        check(authorizationManager.hasRightToModel(prefix, model));

        var classResource = model.getResource(classURI);
        var modelResource = model.getResource(modelURI);

        var includedNamespaces = DataModelUtils.getInternalReferenceModels(modelURI, modelResource);
        var result = resourceService.findResources(Set.of(restrictionURI), includedNamespaces);
        var restrictionResource = result.getResource(restrictionURI);

        if (!restrictionResource.listProperties().hasNext()) {
            throw new ResourceNotFoundException(restrictionURI);
        }

        if (newTarget == null) {
            ClassMapper.mapRemoveClassRestrictionProperty(model, classResource, restrictionResource, currentTarget);
        } else {
            ResourceType resourceType;
            if (MapperUtils.hasType(restrictionResource, OWL.ObjectProperty)) {
                var restrictionTargetResult = resourceService.findResources(Set.of(newTarget), includedNamespaces);
                var newTargetResource = restrictionTargetResult.getResource(DataModelUtils.removeVersionFromURI(newTarget));
                if (!newTargetResource.listProperties().hasNext()) {
                    throw new ResourceNotFoundException(newTarget);
                }
                resourceType = ResourceType.ASSOCIATION;
            } else if (MapperUtils.hasType(restrictionResource, OWL.DatatypeProperty)) {
                if (!ModelConstants.SUPPORTED_DATA_TYPES.contains(newTarget)) {
                    throw new MappingError("Unsupported data type");
                }
                resourceType = ResourceType.ATTRIBUTE;
            } else {
                throw new MappingError("Unsupported restriction type");
            }

            ClassMapper.mapUpdateClassRestrictionProperty(model, classResource, restrictionURI, currentTarget, newTarget, resourceType);
        }
        coreRepository.put(modelURI, model);
    }

    public void handleAddClassRestrictionReference(String prefix, String classIdentifier, String uri) {
        var modelURI = ModelConstants.SUOMI_FI_NAMESPACE + prefix;
        var model = coreRepository.fetch(modelURI);
        var classURI = modelURI + ModelConstants.RESOURCE_SEPARATOR + classIdentifier;

        if (!coreRepository.resourceExistsInGraph(modelURI, classURI)) {
            throw new ResourceNotFoundException(classURI);
        }

        check(authorizationManager.hasRightToModel(prefix, model));

        var classResource = model.getResource(classURI);
        var modelResource = model.getResource(modelURI);
        var includedNamespaces = DataModelUtils.getInternalReferenceModels(modelURI, modelResource);
        var findResourceModel = resourceService.findResources(Set.of(uri), includedNamespaces);
        var propertyResource = findResourceModel.getResource(uri);
        if (findResourceModel.size() == 0) {
            throw new ResourceNotFoundException(uri);
        }

        ClassMapper.mapClassRestrictionProperty(model, classResource, propertyResource);
        coreRepository.put(modelURI, model);
    }

    public void togglePropertyShape(String prefix, String propertyUri) {
        var modelURI = ModelConstants.SUOMI_FI_NAMESPACE + prefix;

        var query = new AskBuilder()
                .addGraph(NodeFactory.createURI(modelURI), "?s", SH.property, NodeFactory.createURI(propertyUri))
                .build();

        var externalExists = coreRepository.queryAsk(query);

        if(!coreRepository.resourceExistsInGraph(modelURI, propertyUri) && !externalExists){
            throw new ResourceNotFoundException(propertyUri);
        }
        var model = coreRepository.fetch(modelURI);
        check(authorizationManager.hasRightToModel(prefix, model));
        ClassMapper.toggleAndMapDeactivatedProperty(model, propertyUri, externalExists);
        coreRepository.put(modelURI, model);
    }

    public URI renameResource(String prefix, String oldIdentifier, String newIdentifier) throws URISyntaxException {
        return resourceService.renameResource(prefix, oldIdentifier, newIdentifier);
    }
}
