package fi.vm.yti.datamodel.api.v2.service;

import fi.vm.yti.datamodel.api.security.AuthorizationManager;
import fi.vm.yti.datamodel.api.v2.dto.*;
import fi.vm.yti.datamodel.api.v2.endpoint.error.MappingError;
import fi.vm.yti.datamodel.api.v2.endpoint.error.ResourceNotFoundException;
import fi.vm.yti.datamodel.api.v2.mapper.MapperUtils;
import fi.vm.yti.datamodel.api.v2.mapper.ResourceMapper;
import fi.vm.yti.datamodel.api.v2.opensearch.index.OpenSearchIndexer;
import fi.vm.yti.datamodel.api.v2.repository.CoreRepository;
import fi.vm.yti.datamodel.api.v2.repository.ImportsRepository;
import fi.vm.yti.datamodel.api.v2.utils.DataModelURI;
import fi.vm.yti.datamodel.api.v2.utils.DataModelUtils;
import fi.vm.yti.security.AuthenticatedUserProvider;
import org.apache.jena.arq.querybuilder.AskBuilder;
import org.apache.jena.arq.querybuilder.ExprFactory;
import org.apache.jena.arq.querybuilder.SelectBuilder;
import org.apache.jena.arq.querybuilder.WhereBuilder;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.vocabulary.OWL;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.topbraid.shacl.vocabulary.SH;

import javax.annotation.Nonnull;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static fi.vm.yti.security.AuthorizationException.check;

@Service
public class ResourceService extends BaseResourceService {

    private static final AuditService AUDIT_SERVICE = new AuditService("RESOURCE");
    private final CoreRepository coreRepository;
    private final ImportsRepository importsRepository;
    private final AuthorizationManager authorizationManager;
    private final GroupManagementService groupManagementService;
    private final TerminologyService terminologyService;
    private final CodeListService codeListService;
    private final AuthenticatedUserProvider userProvider;

    @Autowired
    public ResourceService(CoreRepository coreRepository,
                           ImportsRepository importsRepository,
                           AuthorizationManager authorizationManager,
                           GroupManagementService groupManagementService,
                           TerminologyService terminologyService,
                           CodeListService codeListService,
                           AuthenticatedUserProvider userProvider,
                           OpenSearchIndexer openSearchIndexer){
        super(coreRepository, importsRepository, authorizationManager, openSearchIndexer, AUDIT_SERVICE, userProvider);
        this.coreRepository = coreRepository;
        this.importsRepository = importsRepository;
        this.authorizationManager = authorizationManager;
        this.groupManagementService = groupManagementService;
        this.terminologyService = terminologyService;
        this.codeListService = codeListService;
        this.userProvider = userProvider;
    }

    public ResourceInfoBaseDTO get(String prefix, String version, String identifier) {
        var uri = DataModelURI.createResourceURI(prefix, identifier, version);
        var modelUri = uri.getModelURI();
        var versionUri = uri.getGraphURI();

        if(!coreRepository.resourceExistsInGraph(versionUri, uri.getResourceURI())){
            throw new ResourceNotFoundException(identifier);
        }

        var model = coreRepository.fetch(versionUri);
        var orgModel = coreRepository.getOrganizations();
        var hasRightToModel = authorizationManager.hasRightToModel(prefix, model);
        var modelResource = model.getResource(modelUri);
        var includedNamespaces = DataModelUtils.getInternalReferenceModels(versionUri, modelResource);

        ResourceInfoBaseDTO dto;
        if (MapperUtils.isLibrary(modelResource)) {
            dto = ResourceMapper.mapToResourceInfoDTO(model, uri, orgModel, hasRightToModel, groupManagementService.mapUser());
        } else if (MapperUtils.isApplicationProfile(modelResource)) {
            dto = ResourceMapper.mapToPropertyShapeInfoDTO(model, uri, orgModel, hasRightToModel, groupManagementService.mapUser());
        } else {
            throw new MappingError("Invalid model");
        }

        MapperUtils.addLabelsToURIs(dto, mapUriLabels(includedNamespaces));
        terminologyService.mapConcept().accept(dto);
        return dto;
    }

    public ExternalResourceDTO getExternal(String uri){
        var namespace = NodeFactory.createURI(uri).getNameSpace();
        var model = importsRepository.fetch(namespace);
        var resource = model.getResource(uri);
        if (!model.contains(resource, null, (RDFNode) null)) {
            throw new ResourceNotFoundException(uri);
        }
        return ResourceMapper.mapToExternalResource(resource);
    }

    public URI create(String prefix, BaseDTO dto, @Nonnull ResourceType resourceType, boolean applicationProfile) throws URISyntaxException {
        var dataModelURI = DataModelURI.createResourceURI(prefix, dto.getIdentifier());
        var graphUri = dataModelURI.getGraphURI();
        var resourceUri = dataModelURI.getResourceURI();
        if (coreRepository.resourceExistsInGraph(graphUri, resourceUri, false)){
            throw new MappingError("Already exists");
        }
        var model = coreRepository.fetch(graphUri);
        check(authorizationManager.hasRightToModel(prefix, model));
        checkDataModelType(model.getResource(dataModelURI.getModelURI()), dto);

        terminologyService.resolveConcept(dto.getSubject());

        if(applicationProfile && resourceType.equals(ResourceType.ATTRIBUTE)) {
            codeListService.resolveCodelistScheme(((AttributeRestriction) dto).getCodeLists());
        }

        if(applicationProfile){
            resourceUri = ResourceMapper.mapToPropertyShapeResource(dataModelURI, model, (PropertyShapeDTO) dto, resourceType, userProvider.getUser());
        }else {
            resourceUri = ResourceMapper.mapToResource(dataModelURI, model, (ResourceDTO) dto, resourceType, userProvider.getUser());
        }

        saveResource(model, graphUri, resourceUri, false);
        AUDIT_SERVICE.log(AuditService.ActionType.CREATE, resourceUri, userProvider.getUser());
        return new URI(resourceUri);
    }

    public void update(String prefix, String identifier, BaseDTO dto) {
        var dataModelURI = DataModelURI.createResourceURI(prefix, identifier);
        var graphUri = dataModelURI.getGraphURI();
        var resourceUri = dataModelURI.getResourceURI();

        if(!coreRepository.resourceExistsInGraph(graphUri, resourceUri)){
            throw new ResourceNotFoundException(identifier);
        }

        var model = coreRepository.fetch(graphUri);
        check(authorizationManager.hasRightToModel(prefix, model));
        checkDataModelType(model.getResource(dataModelURI.getModelURI()), dto);

        if(dto instanceof AttributeRestriction attributeRestriction) {
            codeListService.resolveCodelistScheme(attributeRestriction.getCodeLists());
        }

        if (dto instanceof ResourceDTO resourceDTO) {
            checkCyclicalReferences(resourceDTO.getEquivalentResource(), OWL.equivalentProperty, resourceUri);
            checkCyclicalReferences(resourceDTO.getSubResourceOf(), RDFS.subPropertyOf, resourceUri);
            ResourceMapper.mapToUpdateResource(dataModelURI, model, resourceDTO, userProvider.getUser());
        } else if (dto instanceof PropertyShapeDTO propertyShapeDTO) {
            ResourceMapper.mapToUpdatePropertyShape(dataModelURI, model, propertyShapeDTO, userProvider.getUser());
        } else {
            throw new MappingError("Invalid content type for mapping resource");
        }
        terminologyService.resolveConcept(dto.getSubject());

        saveResource(model, graphUri, resourceUri, true);
        AUDIT_SERVICE.log(AuditService.ActionType.UPDATE, resourceUri, userProvider.getUser());
    }

    public URI copyPropertyShape(String prefix, String propertyShapeIdentifier, String targetPrefix, String newIdentifier) throws URISyntaxException {
        var target = DataModelURI.createResourceURI(targetPrefix, newIdentifier);
        var targetGraph = target.getModelURI();
        var targetModel = coreRepository.fetch(targetGraph);
        var targetResource = target.getResourceURI();

        // determine version which graph the resource will be copied from
        var namespaces = DataModelUtils
                .getInternalReferenceModels(targetGraph, targetModel.getResource(targetGraph)).stream()
                .filter(ns -> ns.startsWith(ModelConstants.SUOMI_FI_NAMESPACE + prefix))
                .findFirst();

        DataModelURI source;
        if (namespaces.isPresent()) {
            var version = DataModelURI.fromURI(namespaces.get()).getVersion();
            source = DataModelURI.createResourceURI(prefix, propertyShapeIdentifier, version);
        } else {
            source = DataModelURI.createResourceURI(prefix, propertyShapeIdentifier);
        }

        var sourceGraphUri = source.getGraphURI();

        if (!coreRepository.resourceExistsInGraph(sourceGraphUri, source.getResourceURI())) {
            throw new ResourceNotFoundException(propertyShapeIdentifier);
        }
        if (coreRepository.resourceExistsInGraph(targetGraph, targetResource, false)) {
            throw new MappingError("Identifier in use");
        }

        var sourceModel = coreRepository.fetch(sourceGraphUri);

        if (!MapperUtils.isApplicationProfile(sourceModel.getResource(source.getModelURI())) || !MapperUtils.isApplicationProfile(targetModel.getResource(targetGraph))){
            throw new MappingError("Both data models have to be application profiles");
        }
        check(authorizationManager.hasRightToModel(targetPrefix, targetModel));

        ResourceMapper.mapToCopyToLocalPropertyShape(sourceModel, source, targetModel, target, userProvider.getUser());

        saveResource(targetModel, targetGraph, targetResource, false);
        AUDIT_SERVICE.log(AuditService.ActionType.CREATE, targetResource, userProvider.getUser());
        return new URI(targetResource);
    }

    private void checkDataModelType(Resource modelResource, BaseDTO dto) {
        if (dto instanceof PropertyShapeDTO && MapperUtils.isLibrary(modelResource)) {
            throw new MappingError("Cannot add property shape to ontology");
        } else if (dto instanceof ResourceDTO && MapperUtils.isApplicationProfile(modelResource)) {
            throw new MappingError("Cannot add resource to application profile");
        }
    }

    /**
     * Check if resource uri is one of given types
     * @param resourceUri Resource uri
     * @param types List of types to check
     * @return true if resource is one of types
     */
    public boolean checkIfResourceIsOneOfTypes(String resourceUri, List<Resource> types) {
        var dataModelURI = DataModelURI.fromURI(resourceUri);

        var uri = NodeFactory.createURI(resourceUri);
        var typeVar = "?type";
        var askBuilder = new AskBuilder()
                .addValueVar(typeVar, types.toArray());

        if (!dataModelURI.isDataModelURI()) {
            askBuilder.addWhere(uri, RDF.type, typeVar);
            return importsRepository.queryAsk(askBuilder.build());
        } else {
            askBuilder.addWhere(NodeFactory.createURI(dataModelURI.getResourceURI()), RDF.type, typeVar);
            askBuilder.from(dataModelURI.getGraphURI());
            return coreRepository.queryAsk(askBuilder.build());
        }
    }

    public boolean checkActiveStatus(String prefix, String uri, String version) {
        var dataModelURI = DataModelURI.createModelURI(prefix, version);

        var askBuilder = new AskBuilder()
                .addGraph(NodeFactory.createURI(dataModelURI.getGraphURI()),
                        NodeFactory.createURI(uri), SH.deactivated, "?o");

        return !coreRepository.queryAsk(askBuilder.build());
    }

     public Set<String> findNodeShapeExternalProperties(String graphURI, Resource resourceType) {
         var externalResources = new HashSet<String>();
         if (resourceType == null) {
             return externalResources;
         }

         ExprFactory exprFactory = new ExprFactory();
         var propertyVar = "?property";
         var select = new SelectBuilder()
                 .addVar(propertyVar)
                 .addWhere(new WhereBuilder()
                         .addGraph(NodeFactory.createURI(graphURI), new WhereBuilder()
                                 .addWhere("?subj", SH.property, propertyVar))
                         .addFilter(exprFactory
                                 .not(exprFactory.strstarts(exprFactory.str(propertyVar), graphURI))))
                 .addWhere(new WhereBuilder()
                         .addWhere(propertyVar, "a", resourceType));
         coreRepository.querySelect(select.build(), (var row) -> externalResources.add(row.get("property").toString()));
         return externalResources;
     }
}
