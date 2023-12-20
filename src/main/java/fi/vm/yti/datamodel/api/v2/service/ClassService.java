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
import fi.vm.yti.datamodel.api.v2.utils.DataModelURI;
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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.topbraid.shacl.vocabulary.SH;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static fi.vm.yti.security.AuthorizationException.check;

@Service
public class ClassService extends BaseResourceService {

    private static final AuditService AUDIT_SERVICE = new AuditService("CLASS");
    
    private final CoreRepository coreRepository;
    private final ImportsRepository importsRepository;
    private final AuthorizationManager authorizationManager;
    private final AuthenticatedUserProvider userProvider;
    private final TerminologyService terminologyService;
    private final GroupManagementService groupManagementService;
    private final OpenSearchIndexer openSearchIndexer;
    private final SearchIndexService searchIndexService;
    private final VisualizationService visualizationService;

    @Autowired
    public ClassService(CoreRepository coreRepository,
                        ImportsRepository importsRepository,
                        AuthorizationManager authorizationManager,
                        AuthenticatedUserProvider userProvider,
                        TerminologyService terminologyService,
                        GroupManagementService groupManagementService,
                        OpenSearchIndexer openSearchIndexer,
                        SearchIndexService searchIndexService,
                        VisualizationService visualizationService) {
        super(coreRepository, importsRepository, authorizationManager, openSearchIndexer, AUDIT_SERVICE, userProvider);
        this.coreRepository = coreRepository;
        this.importsRepository = importsRepository;
        this.authorizationManager = authorizationManager;
        this.userProvider = userProvider;
        this.terminologyService = terminologyService;
        this.groupManagementService = groupManagementService;
        this.openSearchIndexer = openSearchIndexer;
        this.searchIndexService = searchIndexService;
        this.visualizationService = visualizationService;
    }

    public ResourceInfoBaseDTO get(String prefix, String version, String classIdentifier) {
        var uri = DataModelURI.createResourceURI(prefix, classIdentifier, version);
        var modelURI = uri.getModelURI();
        var classURI = uri.getResourceURI();

        if(!coreRepository.resourceExistsInGraph(uri.getGraphURI(), classURI)){
            throw new ResourceNotFoundException(classURI);
        }
        var model = coreRepository.fetch(uri.getGraphURI());
        var hasRightToModel = authorizationManager.hasRightToModel(prefix, model);

        var orgModel = coreRepository.getOrganizations();
        var userMapper = hasRightToModel ? groupManagementService.mapUser() : null;

        ResourceInfoBaseDTO dto;
        var modelResource = model.getResource(modelURI);
        var includedNamespaces = DataModelUtils.getInternalReferenceModels(uri.getGraphURI(), modelResource);

        if (MapperUtils.isLibrary(modelResource)) {
            dto = ClassMapper.mapToClassDTO(model, uri, orgModel, hasRightToModel, userMapper);

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

            var restrictionInternalURIs = restrictions.stream()
                    .filter(u -> u.getUri().startsWith(modelURI))
                    .collect(Collectors.toSet());

            var restrictionExternalURIs = restrictions.stream()
                    .map(SimpleResourceDTO::getUri)
                    .filter(u -> !u.startsWith(modelURI))
                    .collect(Collectors.toSet());

            var uriResultExternal = searchIndexService.findResourcesByURI(restrictionExternalURIs, null);

            ClassMapper.addClassResourcesToDTO(model, restrictionInternalURIs, (ClassInfoDTO) dto, terminologyService.mapConceptToResource());
            ClassMapper.addClassResourcesToDTO(uriResultExternal.getResponseObjects(), restrictions, (ClassInfoDTO) dto);
        } else {
            dto = ClassMapper.mapToNodeShapeDTO(model, uri, orgModel, hasRightToModel, userMapper);
            var existingProperties = getTargetNodeProperties(MapperUtils.propertyToString(model.getResource(classURI), SH.node), includedNamespaces);
            var nodeShapeResources = coreRepository.queryConstruct(ClassMapper.getNodeShapeResourcesQuery(classURI));
            ClassMapper.addNodeShapeResourcesToDTO(model, nodeShapeResources, (NodeShapeInfoDTO) dto, existingProperties);
        }

        terminologyService.mapConcept().accept(dto);
        MapperUtils.addLabelsToURIs(dto, mapUriLabels(includedNamespaces));
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

    public List<IndexResourceInfo> getNodeShapes(String targetClass) {
        var request = new ResourceSearchRequest();
        request.setStatus(Set.of(Status.VALID, Status.SUGGESTED));
        request.setTargetClass(targetClass);
        return searchIndexService
                .searchInternalResourcesWithInfo(request, userProvider.getUser())
                .getResponseObjects();
    }

    public URI create(String prefix, BaseDTO dto, boolean applicationProfile) throws URISyntaxException {
        var uri = DataModelURI.createResourceURI(prefix, dto.getIdentifier());
        var graphUri = uri.getGraphURI();
        var classUri = uri.getResourceURI();
        if(coreRepository.resourceExistsInGraph(graphUri, classUri, false)){
            throw new MappingError("Class already exists");
        }
        var model = coreRepository.fetch(graphUri);
        var modelResource = model.getResource(uri.getModelURI());
        check(authorizationManager.hasRightToModel(prefix, model));
        checkDataModelType(modelResource, dto);
        try {
            terminologyService.resolveConcept(dto.getSubject());
        } catch (Exception e) {
            // TODO remove try catch after migration
            dto.setSubject(null);
        }

        var includedNamespaces = DataModelUtils.getInternalReferenceModels(graphUri, modelResource);

        if(applicationProfile) {
            ClassMapper.createNodeShapeAndMapToModel(uri, model, (NodeShapeDTO) dto, userProvider.getUser());
            addNodeShapeProperties(model, classUri, (NodeShapeDTO) dto, includedNamespaces);
        }else {
            ClassMapper.createOntologyClassAndMapToModel(uri, model, (ClassDTO) dto, userProvider.getUser());
        }

        saveResource(model, graphUri, classUri, false);
        visualizationService.addNewResourceDefaultPosition(prefix, dto.getIdentifier());
        AUDIT_SERVICE.log(AuditService.ActionType.CREATE, classUri, userProvider.getUser());
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
        var uri = DataModelURI.createResourceURI(prefix, classIdentifier);
        var graph = uri.getGraphURI();
        var classURI = uri.getResourceURI();

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
            checkCyclicalReferences(classDTO.getEquivalentClass(), OWL.equivalentClass, classURI);
            checkCyclicalReferences(classDTO.getSubClassOf(), RDFS.subClassOf, classURI);
            checkCyclicalReferences(classDTO.getDisjointWith(), OWL.disjointWith, classURI);
            ClassMapper.mapToUpdateOntologyClass(model, graph, classResource, classDTO, userProvider.getUser());
        } else {
            var nodeShape = (NodeShapeDTO) dto;

            if(nodeShape.getTargetNode() != null && nodeShape.getTargetNode().equals(classResource.getURI())) {
                throw new MappingError("Target node is a self reference");
            }
            checkCyclicalReference(nodeShape.getTargetClass(), SH.targetClass, classURI);
            checkCyclicalReference(nodeShape.getTargetNode(), SH.targetNode, classURI);

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

        saveResource(model, graph, classURI, true);
        AUDIT_SERVICE.log(AuditService.ActionType.UPDATE, classURI, userProvider.getUser());
    }

    private Set<String> getTargetNodeProperties(String targetNode, Set<String> graphsIncluded) {
        var propertyShapes = new HashSet<String>();
        var handledNodeShapes = new HashSet<String>();

        // collect recursively all property shape uris from target node
        while (targetNode != null) {
            var targetNodeURI = DataModelURI.fromURI(targetNode).getResourceURI();
            if (handledNodeShapes.contains(targetNode)) {
                throw new MappingError("Circular dependency, cannot add sh:node reference");
            }
            handledNodeShapes.add(targetNode);

            var nodeModel = findResources(Set.of(targetNode), graphsIncluded);
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

        var dataModelURI = DataModelURI.fromURI(classURI);
        var included = DataModelUtils.getInternalReferenceModels(dataModelURI.getGraphURI(), model.getResource(dataModelURI.getModelURI()));
        var newProperties = nodeShapeDTO.getProperties().stream()
                .filter(p -> !existingProperties.contains(p))
                .collect(Collectors.toSet());
        var newPropertiesModel = findResources(newProperties, included);

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

    public void handlePropertyShapeReference(String prefix, String nodeShapeIdentifier, String uri, boolean delete) {
        var dataModelURI = DataModelURI.createResourceURI(prefix, nodeShapeIdentifier);
        var graphURI = dataModelURI.getGraphURI();
        var model = coreRepository.fetch(graphURI);
        var classURI = dataModelURI.getResourceURI();
        check(authorizationManager.hasRightToModel(prefix, model));

        var includedNamespaces = DataModelUtils.getInternalReferenceModels(graphURI, model.getResource(dataModelURI.getModelURI()));
        var classResource = model.getResource(classURI);
        var existingProperties = getTargetNodeProperties(MapperUtils.propertyToString(classResource, SH.node), includedNamespaces);
        if(delete) {
            ClassMapper.mapRemoveNodeShapeProperty(model, classResource, uri, existingProperties);
        }else {
            ClassMapper.mapAppendNodeShapeProperty(classResource, uri, existingProperties);
        }
        AUDIT_SERVICE.log(AuditService.ActionType.UPDATE, classURI, userProvider.getUser());
        coreRepository.put(graphURI, model);
    }

    public void handleUpdateClassRestrictionReference(String prefix, String classIdentifier, String restrictionURI, String currentTarget, String newTarget) {
        var dataModelURI = DataModelURI.createResourceURI(prefix, classIdentifier);
        var modelURI = dataModelURI.getModelURI();
        var classURI = dataModelURI.getResourceURI();
        var graphURI = dataModelURI.getGraphURI();
        var model = coreRepository.fetch(graphURI);
        if (!coreRepository.resourceExistsInGraph(graphURI, classURI)) {
            throw new ResourceNotFoundException(classURI);
        }
        check(authorizationManager.hasRightToModel(prefix, model));

        var classResource = model.getResource(classURI);
        var modelResource = model.getResource(modelURI);

        var includedNamespaces = DataModelUtils.getInternalReferenceModels(modelURI, modelResource);
        var result = findResources(Set.of(restrictionURI), includedNamespaces);
        var restrictionResource = result.getResource(restrictionURI);

        if (!restrictionResource.listProperties().hasNext()) {
            throw new ResourceNotFoundException(restrictionURI);
        }

        if (newTarget == null) {
            ClassMapper.mapRemoveClassRestrictionProperty(model, classResource, restrictionResource, currentTarget);
        } else {
            ResourceType resourceType;
            if (MapperUtils.hasType(restrictionResource, OWL.ObjectProperty)) {
                var restrictionTargetResult = findResources(Set.of(newTarget), includedNamespaces);
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
        AUDIT_SERVICE.log(AuditService.ActionType.UPDATE, classURI, userProvider.getUser());
        coreRepository.put(graphURI, model);
    }

    public void handleAddClassRestrictionReference(String prefix, String classIdentifier, String uri) {
        var dataModelURI = DataModelURI.createResourceURI(prefix, classIdentifier);
        var graphURI = dataModelURI.getGraphURI();
        var classURI = dataModelURI.getResourceURI();
        var model = coreRepository.fetch(graphURI);

        if (!coreRepository.resourceExistsInGraph(graphURI, classURI)) {
            throw new ResourceNotFoundException(classURI);
        }

        check(authorizationManager.hasRightToModel(prefix, model));

        var classResource = model.getResource(classURI);
        var modelResource = model.getResource(graphURI);
        var includedNamespaces = DataModelUtils.getInternalReferenceModels(graphURI, modelResource);
        var findResourceModel = findResources(Set.of(uri), includedNamespaces);
        var propertyResource = findResourceModel.getResource(uri);
        if (findResourceModel.isEmpty()) {
            throw new ResourceNotFoundException(uri);
        }

        ClassMapper.mapClassRestrictionProperty(model, classResource, propertyResource);
        AUDIT_SERVICE.log(AuditService.ActionType.UPDATE, classURI, userProvider.getUser());
        coreRepository.put(graphURI, model);
    }

    public void togglePropertyShape(String prefix, String propertyUri) {
        var graphURI = DataModelURI.createModelURI(prefix).getGraphURI();

        var query = new AskBuilder()
                .addGraph(NodeFactory.createURI(graphURI), "?s", SH.property, NodeFactory.createURI(propertyUri))
                .build();

        var externalExists = coreRepository.queryAsk(query);

        if(!coreRepository.resourceExistsInGraph(graphURI, propertyUri) && !externalExists){
            throw new ResourceNotFoundException(propertyUri);
        }
        var model = coreRepository.fetch(graphURI);
        check(authorizationManager.hasRightToModel(prefix, model));
        ClassMapper.toggleAndMapDeactivatedProperty(model, propertyUri, externalExists);
        AUDIT_SERVICE.log(AuditService.ActionType.UPDATE, propertyUri, userProvider.getUser());
        coreRepository.put(graphURI, model);
    }

}
