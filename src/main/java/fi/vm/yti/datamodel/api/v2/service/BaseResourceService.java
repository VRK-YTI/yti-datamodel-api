package fi.vm.yti.datamodel.api.v2.service;

import fi.vm.yti.common.Constants;
import fi.vm.yti.common.exception.JenaQueryException;
import fi.vm.yti.common.service.AuditService;
import fi.vm.yti.common.util.MapperUtils;
import fi.vm.yti.datamodel.api.v2.security.DataModelAuthorizationManager;
import fi.vm.yti.datamodel.api.v2.utils.DataModelMapperUtils;
import fi.vm.yti.datamodel.api.v2.dto.ModelConstants;
import fi.vm.yti.datamodel.api.v2.dto.ResourceReferenceDTO;
import fi.vm.yti.datamodel.api.v2.dto.ResourceType;
import fi.vm.yti.datamodel.api.v2.dto.UriDTO;
import fi.vm.yti.datamodel.api.v2.endpoint.error.MappingError;
import fi.vm.yti.common.exception.ResourceNotFoundException;
import fi.vm.yti.datamodel.api.v2.mapper.ResourceMapper;
import fi.vm.yti.datamodel.api.v2.repository.CoreRepository;
import fi.vm.yti.datamodel.api.v2.repository.ImportsRepository;
import fi.vm.yti.datamodel.api.v2.utils.DataModelURI;
import fi.vm.yti.datamodel.api.v2.utils.DataModelUtils;
import fi.vm.yti.datamodel.api.v2.utils.SparqlUtils;
import fi.vm.yti.security.AuthenticatedUserProvider;
import fi.vm.yti.security.Role;
import fi.vm.yti.security.YtiUser;
import org.apache.jena.arq.querybuilder.AskBuilder;
import org.apache.jena.arq.querybuilder.ConstructBuilder;
import org.apache.jena.arq.querybuilder.SelectBuilder;
import org.apache.jena.arq.querybuilder.WhereBuilder;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.query.Query;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.sparql.path.PathFactory;
import org.apache.jena.vocabulary.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.topbraid.shacl.vocabulary.SH;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static fi.vm.yti.security.AuthorizationException.check;

abstract class BaseResourceService {

    private static final Logger LOG = LoggerFactory.getLogger(BaseResourceService.class);
    private final AuditService auditService;

    private final CoreRepository coreRepository;
    private final ImportsRepository importsRepository;
    private final DataModelAuthorizationManager authorizationManager;
    private final AuthenticatedUserProvider userProvider;
    private final IndexService indexService;

    BaseResourceService(CoreRepository coreRepository,
                        ImportsRepository importsRepository,
                        DataModelAuthorizationManager authorizationManager,
                        IndexService indexService,
                        AuditService auditService,
                        AuthenticatedUserProvider userProvider) {
        this.coreRepository = coreRepository;
        this.importsRepository = importsRepository;
        this.authorizationManager = authorizationManager;
        this.indexService = indexService;
        this.auditService = auditService;
        this.userProvider = userProvider;
    }

    public void delete(String prefix, String resourceIdentifier) {
        var uri = DataModelURI.createResourceURI(prefix, resourceIdentifier);
        var graphUri = uri.getGraphURI();
        var resourceUri = uri.getResourceURI();
        if (!coreRepository.resourceExistsInGraph(graphUri, resourceUri)) {
            throw new ResourceNotFoundException(resourceUri);
        }

        var model = coreRepository.fetch(graphUri);
        check(authorizationManager.hasRightToModel(prefix, model));

        var references = getResourceReferences(resourceUri, true);

        // If the references include the resource being deleted, remove it so it won't prevent deletion
        references.forEach((type, refs) -> {
            refs.removeIf(r -> r.getResourceURI().getUri().equals(resourceUri));
        });
        references.entrySet().removeIf(entry -> entry.getValue().isEmpty());

        if (!references.isEmpty()) {
            var refList = references.keySet().stream()
                    .flatMap(r -> references.get(r).stream())
                    .map(r -> r.getResourceURI().getUri())
                    .limit(10)
                    .collect(Collectors.joining(", "));
            throw new MappingError("Cannot remove because other resources refer to it: " + refList);
        }

        coreRepository.deleteResource(uri.getResourceURI());
        indexService.deleteResourceFromIndex(resourceUri);
        auditService.log(AuditService.ActionType.DELETE, resourceUri, userProvider.getUser());
    }

    public boolean exists(String prefix, String identifier) {
        // identifiers e.g. corner-abcd1234 are reserved for visualization
        if(identifier.startsWith(ModelConstants.CORNER_PREFIX)) {
            return true;
        }
        var uri = DataModelURI.createResourceURI(prefix, identifier);
        return coreRepository.resourceExistsInGraph(uri.getModelURI(), uri.getResourceURI(), false);
    }

    public URI renameResource(String prefix, String oldIdentifier, String newIdentifier) throws URISyntaxException {
        var uri = DataModelURI.createResourceURI(prefix, oldIdentifier);
        var graphURI = uri.getGraphURI();
        var resourceURI = uri.getResourceURI();
        var newResourceURI = DataModelURI.createResourceURI(prefix, newIdentifier).getResourceURI();

        var model = coreRepository.fetch(graphURI);

        if(!coreRepository.resourceExistsInGraph(graphURI, resourceURI)) {
            throw new ResourceNotFoundException(resourceURI);
        }

        if(coreRepository.resourceExistsInGraph(graphURI, newResourceURI)) {
            throw new MappingError("Resource already exists with identifier " + newIdentifier);
        }

        check(authorizationManager.hasRightToModel(prefix, model));


        var newResource = DataModelMapperUtils.renameResource(model.getResource(resourceURI), newIdentifier);
        coreRepository.put(graphURI, model);

        // rename visualization resource as well
        var visualizationModelUri = ModelConstants.MODEL_POSITIONS_NAMESPACE + prefix;
        var visualizationResourceUri = visualizationModelUri + Constants.RESOURCE_SEPARATOR + oldIdentifier;
        if(coreRepository.resourceExistsInGraph(visualizationModelUri, visualizationResourceUri)) {
            var visualizationModel = coreRepository.fetch(visualizationModelUri);
            DataModelMapperUtils.renameResource(visualizationModel.getResource(visualizationResourceUri), newIdentifier);
            coreRepository.put(visualizationModelUri, visualizationModel);
        }


        indexService.deleteResourceFromIndex(resourceURI);
        indexService.createResourceToIndex(ResourceMapper.mapToIndexResource(model, newResourceURI));
        auditService.log(AuditService.ActionType.UPDATE, resourceURI, userProvider.getUser());
        return new URI(newResource.getURI());
    }

    public void checkCyclicalReferences(Collection<String> references, Property property, String resourceUri) {
        var path = PathFactory.pathZeroOrMoreN(PathFactory.pathLink(property.asNode()));
        var resource = NodeFactory.createURI(resourceUri);
        references.forEach(ref -> {
            var query = new AskBuilder()
                    .addWhere(NodeFactory.createURI(ref), path, resource)
                    .build();
            if(coreRepository.queryAsk(query)) {
                throw new MappingError(ref + " has cyclical reference to this class");
            }
        });
    }

    public void checkCyclicalReference(String reference, Property property, String resourceUri) {
        if(reference == null) {
            return;
        }
        var path = PathFactory.pathZeroOrMoreN(PathFactory.pathLink(property.asNode()));
        var query = new AskBuilder()
                .addWhere(NodeFactory.createURI(reference), path, NodeFactory.createURI(resourceUri))
                .build();
        if(coreRepository.queryAsk(query)) {
            throw new MappingError(reference + " has cyclical reference to this class");
        }
    }

    public Model findResources(Set<String> resourceURIs, Set<String> graphsIncluded) {
        if(resourceURIs == null || resourceURIs.isEmpty()) {
            return ModelFactory.createDefaultModel();
        }
        var coreBuilder = new ConstructBuilder()
                .addPrefixes(ModelConstants.PREFIXES);
        SparqlUtils.addConstructOptional("?graph", coreBuilder, OWL2.versionIRI, "?versionIRI");
        graphsIncluded.forEach(coreBuilder::from);

        var importsBuilder = new ConstructBuilder()
                .addPrefixes(ModelConstants.PREFIXES);

        // TODO: resource type specific list of properties?
        var predicates = List.of(RDFS.isDefinedBy, DCTerms.identifier, RDF.type, RDFS.label,
                RDFS.range, DCTerms.subject, SH.path, SH.datatype, SH.minCount, SH.maxCount, SH.property);

        var iterator = resourceURIs.iterator();
        var count = 0;
        while(iterator.hasNext()) {
            var uri = iterator.next();

            // fetch resources without version number
            if(uri.startsWith(Constants.DATA_MODEL_NAMESPACE)) {
                uri = DataModelUtils.removeVersionFromURI(uri);
            }
            var resource = ResourceFactory.createResource(uri);
            for(var pred : predicates) {
                var obj = "?" + pred.getLocalName() + count;
                if(uri.startsWith(Constants.DATA_MODEL_NAMESPACE)) {
                    coreBuilder.addConstruct(resource, pred, obj);
                    coreBuilder.addOptional(resource, pred, obj);
                } else {
                    importsBuilder.addConstruct(resource, pred, obj);
                    importsBuilder.addOptional(resource, pred, obj);
                }
            }
            count++;
        }

        var resultModel = ModelFactory.createDefaultModel();

        Query coreQuery = coreBuilder.build();
        Query importsQuery = importsBuilder.build();

        if (!coreQuery.getConstructTemplate().getTriples().isEmpty()) {
            resultModel.add(coreRepository.queryConstruct(coreQuery));
        }
        if (!importsQuery.getConstructTemplate().getTriples().isEmpty()) {
            resultModel.add(importsRepository.queryConstruct(importsQuery));
        }

        return resultModel;
    }

    public Consumer<Set<UriDTO>> mapUriLabels(Set<String> includedNamespaces) {

        return (var uriDtos) -> {
            var uris = uriDtos.stream()
                    .filter(u -> u != null && u.getLabel() == null)
                    .map(UriDTO::getUri)
                    .collect(Collectors.toSet());

            if(uris.isEmpty()) {
                return;
            }

            var model = findResources(uris, includedNamespaces);

            uriDtos.forEach(u -> {
                if(u != null && u.getLabel() == null) {
                    var res = model.getResource(u.getUri());

                    // use local name if no rdfs:label defined
                    u.setLabel(res.hasProperty(RDFS.label)
                            ? MapperUtils.localizedPropertyToMap(res, RDFS.label)
                            : Map.of("en", u.getCurie().substring(u.getCurie().lastIndexOf(":") + 1)));
                }
            });
        };
    }

    public void saveResource(Model model, String graphUri, String resourceUri, boolean update) {
        coreRepository.put(graphUri, model);
        var indexClass = ResourceMapper.mapToIndexResource(model, resourceUri);
        if(update) {
            indexService.updateResourceToIndex(indexClass);
        } else {
            indexService.createResourceToIndex(indexClass);
        }
    }

    public Map<ResourceType, List<ResourceReferenceDTO>> getResourceReferences(String uri) {
        return getResourceReferences(uri, false);
    }

    public Map<ResourceType, List<ResourceReferenceDTO>> getResourceReferences(String uri, boolean currentGraph) {
        var u = DataModelURI.fromURI(uri);
        var objects = new ArrayList<>();

        if (currentGraph) {
            objects.add(NodeFactory.createURI(u.getResourceURI()));
        } else {
            // search references for both versioned (other graphs) and non versioned (current graph) resource
            objects.add(NodeFactory.createURI(uri));
            if (u.getVersion() != null) {
                objects.add(NodeFactory.createURI(u.getResourceURI()));
            }
        }

        var user = userProvider.getUser();
        var roles = user.getOrganizations(Role.ADMIN, Role.DATA_MODEL_EDITOR).stream()
                .map(UUID::toString)
                .toList();

        var select = new SelectBuilder();
        var exp = select.getExprFactory();

        List.of("resource", "predicate", "type", "label", "contributor", "version")
                .forEach(select::addVar);

        var restrictionPath = Stream.of(
                PathFactory.pathLink(OWL.intersectionOf.asNode()),
                PathFactory.pathZeroOrMoreN(PathFactory.pathLink(RDF.rest.asNode())),
                PathFactory.pathLink(RDF.first.asNode())
        ).reduce(PathFactory::pathSeq).orElseThrow();

        var graphVar = currentGraph ? NodeFactory.createURI(u.getGraphURI()) : "?graph";
        try {
            select.addPrefixes(ModelConstants.PREFIXES)
                    .addWhereValueVar("?object", objects.toArray())
                    .addGraph(graphVar, new WhereBuilder()
                            .addWhere("?subject", "?predicate", "?object")
                            .addWhere("?subject", RDF.type, "?type")

                            // reference as a restriction (blank node) in library class
                            .addOptional(new WhereBuilder()
                                    .addWhere("?restriction", restrictionPath, "?subject")
                                    .addWhere("?classSubj", OWL.equivalentClass, "?restriction")
                                    .addWhere("?classSubj", RDFS.label, "?label")
                                    .addBind("?classSubj", "?resource")
                            )

                            // reference as an object
                            .addOptional(new WhereBuilder()
                                    .addWhere("?subject", RDFS.label, "?label")
                                    .addBind("?subject", "?resource")
                            )
                            .addWhere("?resource", RDFS.isDefinedBy, "?model")
                            .addWhere("?model", DCTerms.contributor, "?contributor")
                            .addOptional("?model", OWL.versionInfo, "?version")
                    );
        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
            throw new JenaQueryException("Error getting resource references: " + uri);
        }

        if (u.isDataModelURI() && !currentGraph) {
            select.addFilter(exp.or(
                    exp.eq(exp.str("?graph"), u.getGraphURI()),
                    exp.not(exp.strstarts(exp.str("?graph"), u.getModelURI()))
            ));
        }

        var resultModel = ModelFactory.createDefaultModel();
        resultModel.setNsPrefixes(ModelConstants.PREFIXES);

        // add subjects (versioned resource URIs) as a resource to jena model
        coreRepository.querySelect(select.build(),
                (var row) -> mapReferencesToModel(row, user, roles, resultModel));

        return ResourceMapper.mapToResourceReference(resultModel);
    }

    private static void mapReferencesToModel(QuerySolution row, YtiUser user, List<String> roles, Model resultModel) {
        String version = row.contains("version")
                ? row.get("version").toString()
                : null;
        var contributor = row.get("contributor").toString()
                .replace(Constants.URN_UUID, "");

        // skip rows without label and draft resources if user has no permissions
        if (row.get("label") == null
            || (version == null && !user.isSuperuser() && !roles.contains(contributor))
        ) {
            return;
        }

        var dataModelURI = DataModelURI.fromURI(row.get("resource").toString());
        var resource = DataModelURI.createResourceURI(dataModelURI.getModelId(), dataModelURI.getResourceId(), version);
        resultModel.getResource(resource.getResourceVersionURI())
                .addProperty(RDFS.label, row.get("label"))
                .addProperty(RDF.type, row.get("type"))
                .addProperty(DCTerms.references, row.get("predicate"));
    }
}
