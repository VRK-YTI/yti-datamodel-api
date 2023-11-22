package fi.vm.yti.datamodel.api.v2.service;

import fi.vm.yti.datamodel.api.security.AuthorizationManager;
import fi.vm.yti.datamodel.api.v2.dto.ModelConstants;
import fi.vm.yti.datamodel.api.v2.dto.visualization.*;
import fi.vm.yti.datamodel.api.v2.endpoint.error.ResourceNotFoundException;
import fi.vm.yti.datamodel.api.v2.mapper.MapperUtils;
import fi.vm.yti.datamodel.api.v2.mapper.VisualizationMapper;
import fi.vm.yti.datamodel.api.v2.properties.DCAP;
import fi.vm.yti.datamodel.api.v2.properties.Iow;
import fi.vm.yti.datamodel.api.v2.repository.CoreRepository;
import fi.vm.yti.datamodel.api.v2.utils.DataModelUtils;
import org.apache.jena.rdf.model.*;
import org.apache.jena.vocabulary.DCTerms;
import org.apache.jena.vocabulary.OWL;
import org.apache.jena.vocabulary.RDF;
import org.springframework.stereotype.Service;
import org.topbraid.shacl.vocabulary.SH;

import java.util.*;
import java.util.stream.Collectors;

import static fi.vm.yti.security.AuthorizationException.check;

@Service
public class VisualizationService {

    private final ResourceService resourceService;
    private final CoreRepository coreRepository;
    private final AuthorizationManager authorizationManager;
    public VisualizationService(ResourceService resourceService,
                                CoreRepository coreRepository,
                                AuthorizationManager authorizationManager) {
        this.resourceService = resourceService;
        this.coreRepository = coreRepository;
        this.authorizationManager = authorizationManager;
    }

    public VisualizationResultDTO getVisualizationData(String prefix, String version) {
        var graph = ModelConstants.SUOMI_FI_NAMESPACE + prefix;
        var graphUri = graph;
        if (version != null) {
            graphUri += ModelConstants.RESOURCE_SEPARATOR + version;
        }
        var model = coreRepository.fetch(graphUri);
        var positions = getPositions(prefix, version);

        var modelResource = model.getResource(graph);
        var classURIs = getClassURIs(model, graph);
        var namespaces = getNamespaces(model, graph);
        var languages = MapperUtils.arrayPropertyToSet(modelResource, DCTerms.language);

        DataModelUtils.addPrefixesToModel(modelResource.getURI(), model);
        var nodes = new HashSet<VisualizationNodeDTO>();

        while (classURIs.hasNext()) {
            var subject = classURIs.next().getSubject();
            if (subject.isAnon()) {
                continue;
            }
            var classResource = model.getResource(subject.getURI());
            var externalResources = new HashSet<Resource>();

            var classDTO = VisualizationMapper.mapClass(subject.getURI(), model, namespaces);

            if (MapperUtils.isLibrary(modelResource)) {
                VisualizationMapper.mapLibraryClassResources(classDTO, model, classResource, externalResources, namespaces);
                VisualizationMapper.mapAssociationsWithDomain(classDTO, model, classResource, namespaces);
                externalResources.forEach(restrictionResource -> {
                    var onProperty = MapperUtils.propertyToString(restrictionResource, OWL.onProperty);
                    var externalResource = resourceService.findResource(onProperty, namespaces.keySet());
                    if (externalResource != null) {
                        VisualizationMapper.mapLibraryResource(classDTO, restrictionResource, externalResource, namespaces);
                    }
                });
            } else {
                VisualizationMapper.mapNodeShapeResources(classDTO, classResource, model, externalResources, namespaces);
                var propertyURIs = externalResources.stream().map(Resource::getURI).collect(Collectors.toSet());
                var externalResourceResult = resourceService.findResources(propertyURIs, namespaces.keySet());
                externalResources.forEach(ext -> {
                    var resourceURI = DataModelUtils.removeVersionFromURI(ext.getURI());
                    VisualizationMapper
                            .mapProfileResource(classDTO, externalResourceResult.getResource(resourceURI), ext.getURI(), model, namespaces);
                    }
                );
            }

            // add dummy classes for external classes
            addExternalClasses(classDTO, languages, nodes);

            nodes.add(classDTO);
        }
        nodes.addAll(VisualizationMapper.mapAttributesWithDomain(model));

        var hiddenNodes = VisualizationMapper.mapPositionsDataToDTOsAndCreateHiddenNodes(positions, prefix, nodes);

        var visualizationResult = new VisualizationResultDTO();

        visualizationResult.setNodes(nodes);
        visualizationResult.setHiddenNodes(hiddenNodes);

        return visualizationResult;
    }

    public Model getPositions(String prefix, String version) {
        var positionUri = ModelConstants.MODEL_POSITIONS_NAMESPACE + prefix;
        if (version != null) {
            positionUri += ModelConstants.RESOURCE_SEPARATOR + version;
        }

        try {
            return coreRepository.fetch(positionUri);
        } catch (ResourceNotFoundException e) {
            return ModelFactory.createDefaultModel();
        }
    }

    public void savePositionData(String prefix, List<PositionDataDTO> positions) {
        savePositionData(prefix, positions, null);
    }

    public void savePositionData(String prefix, List<PositionDataDTO> positions, String version) {
        var modelURI = ModelConstants.SUOMI_FI_NAMESPACE + prefix;
        var positionBaseURI = ModelConstants.MODEL_POSITIONS_NAMESPACE + prefix;
        var positionGraphURI = positionBaseURI;

        if (version != null) {
            modelURI += ModelConstants.RESOURCE_SEPARATOR + version;
            positionGraphURI += ModelConstants.RESOURCE_SEPARATOR + version;
        }

        var dataModel = coreRepository.fetch(modelURI);
        check(authorizationManager.hasRightToModel(prefix, dataModel));

        var positionModel = VisualizationMapper.mapPositionDataToModel(positionBaseURI, positions);
        coreRepository.put(positionGraphURI, positionModel);
    }

    public void addNewResourceDefaultPosition(String prefix, String identifier) {
        var positionGraphURI = ModelConstants.MODEL_POSITIONS_NAMESPACE + prefix;
        var positions = getPositions(prefix, null);

        var yMin = positions.listStatements().toList().stream()
                .map(s -> MapperUtils.getLiteral(s.getSubject(), Iow.posY, Double.class))
                .filter(Objects::nonNull)
                .min(Double::compare)
                .orElse(0.0);

        var newPosition = new PositionDataDTO();
        newPosition.setIdentifier(identifier);
        newPosition.setX(0.0);
        newPosition.setY(yMin - 50.0);

        VisualizationMapper.mapPosition(positions, positionGraphURI, newPosition);

        coreRepository.put(positionGraphURI, positions);
    }

    /**
     * Save current DRAFT version of positions to versioned graph
     * @param prefix data model prefix
     * @param version version of the model
     */
    public void saveVersionedPositions(String prefix, String version) {
        var positionUri = ModelConstants.MODEL_POSITIONS_NAMESPACE + prefix;
        try{
            var positionModel = coreRepository.fetch(positionUri);
            coreRepository.put(positionUri + ModelConstants.RESOURCE_SEPARATOR + version, positionModel);
        }catch (ResourceNotFoundException e){
            //if no positions found, do nothing
        }
    }

    private static void addExternalClasses(VisualizationClassDTO classDTO, Set<String> languages, HashSet<VisualizationNodeDTO> result) {
        classDTO.getReferences().forEach(parent -> {
            if (parent.getIdentifier().contains(":")) {
                result.add(VisualizationMapper.mapExternalClass(parent.getIdentifier(), languages));
            }
        });
        classDTO.getAssociations().forEach(association -> {
            var target = association.getReferenceTarget();
            if (target != null && target.contains(":")) {
                result.add(VisualizationMapper.mapExternalClass(target, languages));
            }
        });
    }

    private static StmtIterator getClassURIs(Model model, String graph) {
        var classType = MapperUtils.isLibrary(model.getResource(graph))
                ? OWL.Class
                : SH.NodeShape;
        return model.listStatements(
                new SimpleSelector(null, RDF.type, classType));
    }

    /**
     * Map external and internal namespaces and prefixes
     *
     * @param model model
     * @param graph graph
     */
    private static HashMap<String, String> getNamespaces(Model model, String graph) {
        var namespaces = new HashMap<String, String>();
        var modelResource = model.getResource(graph);
        Arrays.asList(OWL.imports, DCTerms.requires)
                .forEach(prop -> modelResource.listProperties(prop).toList().forEach(ns -> {
                    var uri = ns.getObject().toString();
                    if (uri.startsWith(ModelConstants.SUOMI_FI_NAMESPACE)) {
                        namespaces.put(uri, MapperUtils.getModelIdFromNamespace(uri));
                    } else if (!uri.contains(ModelConstants.SUOMI_FI_DOMAIN)) {
                        namespaces.put(uri, MapperUtils.propertyToString(model.getResource(uri), DCAP.preferredXMLNamespacePrefix));
                    }
                }));
        return namespaces;
    }
}
