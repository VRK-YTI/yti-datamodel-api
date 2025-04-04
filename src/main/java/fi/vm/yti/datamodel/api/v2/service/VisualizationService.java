package fi.vm.yti.datamodel.api.v2.service;

import fi.vm.yti.common.Constants;
import fi.vm.yti.common.properties.SuomiMeta;
import fi.vm.yti.common.service.AuditService;
import fi.vm.yti.common.util.MapperUtils;
import fi.vm.yti.datamodel.api.v2.security.DataModelAuthorizationManager;
import fi.vm.yti.datamodel.api.v2.dto.ModelConstants;
import fi.vm.yti.datamodel.api.v2.dto.visualization.*;
import fi.vm.yti.common.exception.ResourceNotFoundException;
import fi.vm.yti.datamodel.api.v2.mapper.VisualizationMapper;
import fi.vm.yti.datamodel.api.v2.properties.DCAP;
import fi.vm.yti.datamodel.api.v2.repository.CoreRepository;
import fi.vm.yti.datamodel.api.v2.utils.DataModelURI;
import fi.vm.yti.datamodel.api.v2.utils.DataModelUtils;
import fi.vm.yti.security.AuthenticatedUserProvider;
import org.apache.jena.rdf.model.*;
import org.apache.jena.util.ResourceUtils;
import org.apache.jena.vocabulary.DCTerms;
import org.apache.jena.vocabulary.OWL;
import org.apache.jena.vocabulary.RDF;
import org.springframework.stereotype.Service;
import org.topbraid.shacl.vocabulary.SH;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static fi.vm.yti.security.AuthorizationException.check;

@Service
public class VisualizationService {

    private final AuditService auditService = new AuditService("VISUALIZATION");

    private final ResourceService resourceService;
    private final CoreRepository coreRepository;
    private final DataModelAuthorizationManager authorizationManager;
    private final AuthenticatedUserProvider userProvider;
    private final SearchIndexService searchIndexService;

    public VisualizationService(ResourceService resourceService,
                                CoreRepository coreRepository,
                                DataModelAuthorizationManager authorizationManager,
                                AuthenticatedUserProvider userProvider,
                                SearchIndexService searchIndexService) {
        this.resourceService = resourceService;
        this.coreRepository = coreRepository;
        this.authorizationManager = authorizationManager;
        this.userProvider = userProvider;
        this.searchIndexService = searchIndexService;
    }

    public VisualizationResultDTO getVisualizationData(String prefix, String version) {

        var dataModelURI = DataModelURI.createModelURI(prefix, version);
        var graph = dataModelURI.getModelURI();

        var model = coreRepository.fetch(dataModelURI.getGraphURI());
        var positions = getPositions(prefix, version);

        var modelResource = model.getResource(graph);
        var classURIs = getClassURIs(model, graph);
        var namespaces = getNamespaces(model, graph);
        var languages = MapperUtils.arrayPropertyToSet(modelResource, DCTerms.language);

        DataModelUtils.addPrefixesToModel(modelResource.getURI(), model);
        var nodes = new HashSet<VisualizationNodeDTO>();

        // sort resource by label for now, since there are no custom sorting
        Function<VisualizationItemDTO, String> labelComparator = item ->
                item.getLabel().containsKey("fi")
                        ? item.getLabel().get("fi")
                        : item.getLabel().getOrDefault("en", "");

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

                var ext = externalResources.stream()
                        .map(e -> MapperUtils.propertyToString(e, OWL.onProperty))
                        .collect(Collectors.toSet());

                var extResult = searchIndexService.findResourcesByURI(ext, null).getResponseObjects();

                externalResources.forEach(restrictionResource -> {
                    var onProperty = MapperUtils.propertyToString(restrictionResource, OWL.onProperty);
                    var externalResource = extResult.stream()
                            .filter(e -> onProperty != null && onProperty.equals(e.getId()))
                            .findFirst();

                    externalResource.ifPresent(indexResource ->
                            VisualizationMapper.mapLibraryIndexResource(
                                    classDTO,
                                    restrictionResource,
                                    indexResource,
                                    namespaces));
                });
            } else {
                VisualizationMapper.mapNodeShapeResources(classDTO, classResource, model, externalResources, namespaces);
                externalResources.forEach(ext -> {
                    // search each external resource separately to avoid creating a slow query
                    var externalResource = resourceService.findResources(Set.of(ext.getURI()), namespaces.keySet());
                    var resourceURI = DataModelUtils.removeVersionFromURI(ext.getURI());
                    VisualizationMapper.mapProfileResource(
                            classDTO,
                            externalResource.getResource(resourceURI),
                            ext.getURI(),
                            model,
                            namespaces);
                });
            }
            classDTO.getAttributes().sort(Comparator.comparing(labelComparator));
            classDTO.getAssociations().sort(Comparator.comparing(labelComparator));

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
        try {
            return coreRepository.fetch(getPositionGraphURI(prefix, version));
        } catch (ResourceNotFoundException e) {
            return ModelFactory.createDefaultModel();
        }
    }

    public void savePositionData(String prefix, List<PositionDataDTO> positions) {
        savePositionData(prefix, positions, null);
    }

    public void savePositionData(String prefix, List<PositionDataDTO> positions, String version) {
        var uri = DataModelURI.createModelURI(prefix, version);
        var positionBaseURI = ModelConstants.MODEL_POSITIONS_NAMESPACE + prefix + Constants.RESOURCE_SEPARATOR;
        var positionGraphURI = getPositionGraphURI(prefix, version);

        var dataModel = coreRepository.fetch(uri.getGraphURI());
        check(authorizationManager.hasRightToModel(prefix, dataModel));

        var positionModel = VisualizationMapper.mapPositionDataToModel(positionBaseURI, positions);
        coreRepository.put(positionGraphURI, positionModel);
        auditService.log(AuditService.ActionType.SAVE, uri.getGraphURI(), userProvider.getUser());
    }

    public void addNewResourceDefaultPosition(String prefix, String identifier) {
        var positionGraphURI = getPositionGraphURI(prefix, null);
        var positions = getPositions(prefix, null);

        var yMin = positions.listStatements().toList().stream()
                .map(s -> MapperUtils.getLiteral(s.getSubject(), SuomiMeta.posY, Double.class))
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
        var draftPositionUri = getPositionGraphURI(prefix, null);
        try{
            var positionModel = coreRepository.fetch(draftPositionUri);
            coreRepository.put(getPositionGraphURI(prefix, version), positionModel);
        }catch (ResourceNotFoundException e){
            //if no positions found, do nothing
        }
    }

    public void copyVisualization(String oldPrefix, String version, String newPrefix) {
        var positions = getPositions(oldPrefix, version);
        var positionsCopy = ModelFactory.createDefaultModel().add(positions);
        var positionGraphURI = getPositionGraphURI(newPrefix, null);

        positionsCopy.listSubjects()
                .filterDrop(RDFNode::isAnon)
                .forEach(subject -> {
                    var newSubject = positionGraphURI + MapperUtils.propertyToString(subject, DCTerms.identifier);
                    ResourceUtils.renameResource(subject, newSubject);
                });

        coreRepository.put(positionGraphURI, positionsCopy);
    }

    private static void addExternalClasses(VisualizationClassDTO classDTO, Set<String> languages, HashSet<VisualizationNodeDTO> result) {
        classDTO.getReferences().forEach(ref -> {
            if (ref.getIdentifier().contains(":")) {
                result.add(VisualizationMapper.mapExternalClass(ref.getIdentifier(), languages));
            } else if (ref.getReferenceTarget().contains(":")) {
                result.add(VisualizationMapper.mapExternalClass(ref.getReferenceTarget(), languages));
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
                    var uri = DataModelURI.fromURI(ns.getObject().toString());
                    if (uri.isDataModelURI()) {
                        namespaces.put(uri.getGraphURI(), uri.getModelId());
                    } else if (!uri.isTerminologyURI() && !uri.isCodeListURI()) {
                        namespaces.put(uri.getNamespace(), MapperUtils.propertyToString(model.getResource(uri.getNamespace()), DCAP.preferredXMLNamespacePrefix));
                    }
                }));
        return namespaces;
    }

    private String getPositionGraphURI(String prefix, String version) {
        var positionURI = ModelConstants.MODEL_POSITIONS_NAMESPACE + prefix;
        if (version != null) {
            positionURI += "/" + version;
        }
        positionURI += "/";
        return positionURI;
    }
}
