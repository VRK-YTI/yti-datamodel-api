package fi.vm.yti.datamodel.api.v2.service;

import fi.vm.yti.datamodel.api.security.AuthorizationManager;
import fi.vm.yti.datamodel.api.v2.dto.ModelConstants;
import fi.vm.yti.datamodel.api.v2.dto.UriDTO;
import fi.vm.yti.datamodel.api.v2.endpoint.error.MappingError;
import fi.vm.yti.datamodel.api.v2.endpoint.error.ResourceNotFoundException;
import fi.vm.yti.datamodel.api.v2.mapper.MapperUtils;
import fi.vm.yti.datamodel.api.v2.mapper.ResourceMapper;
import fi.vm.yti.datamodel.api.v2.opensearch.index.OpenSearchIndexer;
import fi.vm.yti.datamodel.api.v2.repository.CoreRepository;
import fi.vm.yti.datamodel.api.v2.repository.ImportsRepository;
import fi.vm.yti.datamodel.api.v2.utils.DataModelURI;
import fi.vm.yti.datamodel.api.v2.utils.DataModelUtils;
import fi.vm.yti.datamodel.api.v2.utils.SparqlUtils;
import fi.vm.yti.security.AuthenticatedUserProvider;
import org.apache.jena.arq.querybuilder.AskBuilder;
import org.apache.jena.arq.querybuilder.ConstructBuilder;
import org.apache.jena.arq.querybuilder.SelectBuilder;
import org.apache.jena.arq.querybuilder.WhereBuilder;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.sparql.path.PathFactory;
import org.apache.jena.vocabulary.DCTerms;
import org.apache.jena.vocabulary.OWL2;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;
import org.topbraid.shacl.vocabulary.SH;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static fi.vm.yti.security.AuthorizationException.check;

abstract class BaseResourceService {

    private final AuditService auditService;

    private final CoreRepository coreRepository;
    private final ImportsRepository importsRepository;
    private final AuthorizationManager authorizationManager;
    private final AuthenticatedUserProvider userProvider;
    private final OpenSearchIndexer openSearchIndexer;

    BaseResourceService(CoreRepository coreRepository,
                        ImportsRepository importsRepository,
                        AuthorizationManager authorizationManager,
                        OpenSearchIndexer openSearchIndexer,
                        AuditService auditService,
                        AuthenticatedUserProvider userProvider) {
        this.coreRepository = coreRepository;
        this.importsRepository = importsRepository;
        this.authorizationManager = authorizationManager;
        this.openSearchIndexer = openSearchIndexer;
        this.auditService = auditService;
        this.userProvider = userProvider;
    }


    public void delete(String prefix, String resourceIdentifier) {
        var uri = DataModelURI.createResourceURI(prefix, resourceIdentifier);
        var graphUri = uri.getGraphURI();
        var resourceUri = uri.getResourceURI();
        if(!coreRepository.resourceExistsInGraph(graphUri, resourceUri)) {
            throw new ResourceNotFoundException(resourceUri);
        }

        var selectBuilder = new SelectBuilder()
                .addPrefixes(ModelConstants.PREFIXES);
        var exprFactory = selectBuilder.getExprFactory();
        var expr = exprFactory.notexists(new WhereBuilder().addWhere("?s", DCTerms.hasPart, "?o"));
        selectBuilder.addFilter(expr);
        selectBuilder.addGraph(NodeFactory.createURI(graphUri), "?s", "?p", "?o");
        selectBuilder.addWhere("?s", "?p", NodeFactory.createURI(resourceUri));

        var references = new ArrayList<String>();
        coreRepository.querySelect(selectBuilder.build(), res -> references.add(res.get("s").toString()));
        if(!references.isEmpty()) {
            //Cannot list subjects in error message because we cannot list blank nodes
            //TODO is it possible to do?
            throw new MappingError("referenced-by-others");
        }

        var model = coreRepository.fetch(graphUri);

        check(authorizationManager.hasRightToModel(prefix, model));
        coreRepository.deleteResource(resourceUri);
        openSearchIndexer.deleteResourceFromIndex(resourceUri);
        auditService.log(AuditService.ActionType.DELETE, resourceUri, userProvider.getUser());
    }

    public boolean exists(String prefix, String identifier) {
        // identifiers e.g. corner-abcd1234 are reserved for visualization
        if(identifier.startsWith("corner-")) {
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


        var newResource = MapperUtils.renameResource(model.getResource(resourceURI), newIdentifier);
        coreRepository.put(graphURI, model);

        // rename visualization resource as well
        var visualizationModelUri = ModelConstants.MODEL_POSITIONS_NAMESPACE + prefix;
        var visualizationResourceUri = visualizationModelUri + ModelConstants.RESOURCE_SEPARATOR + oldIdentifier;
        if(coreRepository.resourceExistsInGraph(visualizationModelUri, visualizationResourceUri)) {
            var visualizationModel = coreRepository.fetch(visualizationModelUri);
            MapperUtils.renameResource(visualizationModel.getResource(visualizationResourceUri), newIdentifier);
            coreRepository.put(visualizationModelUri, visualizationModel);
        }


        openSearchIndexer.deleteResourceFromIndex(resourceURI);
        openSearchIndexer.createResourceToIndex(ResourceMapper.mapToIndexResource(model, newResourceURI));
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
            if(uri.startsWith(ModelConstants.SUOMI_FI_NAMESPACE)) {
                uri = DataModelUtils.removeVersionFromURI(uri);
            }
            var resource = ResourceFactory.createResource(uri);
            for(var pred : predicates) {
                var obj = "?" + pred.getLocalName() + count;
                if(uri.startsWith(ModelConstants.SUOMI_FI_NAMESPACE)) {
                    coreBuilder.addConstruct(resource, pred, obj);
                    coreBuilder.addOptional(resource, pred, obj);
                } else {
                    importsBuilder.addConstruct(resource, pred, obj);
                    importsBuilder.addOptional(resource, pred, obj);
                }
            }
            count++;
        }

        var resultModel = coreRepository.queryConstruct(coreBuilder.build());
        var importsModel = importsRepository.queryConstruct(importsBuilder.build());

        resultModel.add(importsModel);
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
            openSearchIndexer.updateResourceToIndex(indexClass);
        } else {
            openSearchIndexer.createResourceToIndex(indexClass);
        }
    }
    
}
