package fi.vm.yti.datamodel.api.migration.task;

import fi.vm.yti.datamodel.api.v2.dto.ModelConstants;
import fi.vm.yti.datamodel.api.v2.dto.visualization.*;
import fi.vm.yti.datamodel.api.v2.mapper.MapperUtils;
import fi.vm.yti.datamodel.api.v2.mapper.VisualizationMapper;
import fi.vm.yti.datamodel.api.v2.properties.SuomiMeta;
import fi.vm.yti.datamodel.api.v2.repository.CoreRepository;
import fi.vm.yti.datamodel.api.v2.service.VisualizationService;
import fi.vm.yti.datamodel.api.v2.utils.DataModelURI;
import fi.vm.yti.migration.MigrationTask;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.vocabulary.DCTerms;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.*;

@SuppressWarnings("java:S101")
@Component
public class V15_VisualizationAddOrigin implements MigrationTask {

    private static final Logger LOG = LoggerFactory.getLogger(V15_VisualizationAddOrigin.class);
    private final CoreRepository repository;
    private final VisualizationService visualizationService;

    public V15_VisualizationAddOrigin(CoreRepository repository,
                                      VisualizationService visualizationService) {
        this.repository = repository;
        this.visualizationService = visualizationService;
    }

    @Override
    public void migrate() {
        var graphs = new ArrayList<String>();
        String graphsQuery = """
                select * where { graph ?g {}
                   filter strstarts(str(?g), "https://iri.suomi.fi/model/")
                }
                """;
        repository.querySelect(graphsQuery, row -> graphs.add(row.get("g").toString()));

        for (var graph : graphs) {
            LOG.info("Migrating visualization for {}", graph);

            var dataModelURI = DataModelURI.fromURI(graph);
            VisualizationResultDTO visuData;
            Model positions;
            try {
                visuData = visualizationService.getVisualizationData(dataModelURI.getModelId(), dataModelURI.getVersion());
                positions = repository.fetch(getPositionGraphURI(dataModelURI.getModelId(), dataModelURI.getVersion()));
            } catch (Exception e) {
                LOG.info("No visualization data for graph {}", dataModelURI.getModelURI());
                continue;
            }

            // backup old data
            var backUpGraph = getPositionGraphURI(dataModelURI.getModelId(), dataModelURI.getVersion())
                    .replace("iri.suomi.fi/model-positions/", "iri.suomi.fi/model-positions-backup/");
            repository.put(backUpGraph, positions);

            var hiddenNodes = mapPathAndCreateHiddenNodes(positions, visuData.getNodes());

            visuData.setHiddenNodes(hiddenNodes);

            // construct visualization payload using old implementation
            var positionData = new ArrayList<PositionDataDTO>();
            visuData.getNodes().forEach(n -> {
                var position = getPositionData(n.getIdentifier(), n.getPosition());
                var targets = new HashSet<PositionDataDTO.ReferenceTarget>();

                if (n instanceof VisualizationClassDTO classDTO) {
                    classDTO.getAssociations().forEach(r -> {
                        if (r.getReferenceTarget() != null && r.getReferenceTarget().startsWith(ModelConstants.CORNER_PREFIX)) {
                            targets.add(new PositionDataDTO.ReferenceTarget(r.getReferenceTarget(), r.getIdentifier()));
                        }
                    });
                }
                n.getReferences().forEach(r -> {
                    if (r.getReferenceTarget() != null && r.getReferenceTarget().startsWith(ModelConstants.CORNER_PREFIX)) {
                        targets.add(new PositionDataDTO.ReferenceTarget(r.getReferenceTarget(), r.getIdentifier()));
                    }
                });
                position.setReferenceTargets(targets);

                positionData.add(position);
            });

            visuData.getHiddenNodes().forEach(n -> {
                var position = getPositionData(n.getIdentifier(), n.getPosition());
                position.setReferenceTargets(Set.of(new PositionDataDTO.ReferenceTarget(n.getReferenceTarget(), n.getOrigin())));
                positionData.add(position);
            });

            var model = VisualizationMapper.mapPositionDataToModel(
                    getPositionGraphURI(dataModelURI.getModelId(), dataModelURI.getVersion()),  positionData);
            model.setNsPrefixes(ModelConstants.PREFIXES);

            var targetGraph = getPositionGraphURI(dataModelURI.getModelId(), dataModelURI.getVersion());

            repository.put(targetGraph, model);
        }
    }

    private static PositionDataDTO getPositionData(String identifier, PositionDTO position) {
        var positionDTO = new PositionDataDTO();
        positionDTO.setIdentifier(identifier);
        positionDTO.setX(roundPosition(position.getX()));
        positionDTO.setY(roundPosition(position.getY()));

        return positionDTO;
    }
    public static Set<VisualizationHiddenNodeDTO> mapPathAndCreateHiddenNodes(
            Model positions, Set<VisualizationNodeDTO> classes) {
        var hiddenElements = new HashSet<VisualizationHiddenNodeDTO>();

        var handledElements = new HashSet<String>();

        classes.forEach(dto -> {

            // check if there are hidden nodes between parent relations
            if (dto.getReferences() != null && !dto.getReferences().isEmpty()) {
                dto.getReferences().forEach(reference -> {
                    var path = handlePath(positions, dto.getIdentifier(), reference, hiddenElements, handledElements);
                    reference.setReferenceTarget(path.get(0));
                });
            }

            if (dto instanceof VisualizationClassDTO classDTO) {
                // check if there are hidden nodes between association relations
                classDTO.getAssociations().forEach(reference -> {
                    var target = reference.getReferenceTarget();
                    if (target != null) {
                        var path = handlePath(positions, dto.getIdentifier(), reference, hiddenElements, handledElements);
                        reference.setReferenceTarget(path.get(0));
                    }
                });
            }
        });
        return hiddenElements;
    }

    private static List<String> handlePath(Model positions, String sourceIdentifier, VisualizationReferenceDTO reference,
                                           Set<VisualizationHiddenNodeDTO> hiddenElements, Set<String> handledElements) {

        var positionResources = positions.listSubjectsWithProperty(SuomiMeta.referenceTarget, reference.getReferenceTarget()).toList();
        for (Resource positionResource : positionResources) {
            var path = new LinkedList<String>();
            while (positionResource.hasProperty(SuomiMeta.referenceTarget)) {
                var identifier = MapperUtils.propertyToString(positionResource, DCTerms.identifier);

                if (shouldSkip(positions, sourceIdentifier, handledElements, identifier)) {
                    break;
                } else if (sourceIdentifier.equals(identifier)) {
                    // reached the source node
                    if (path.isEmpty()) {
                        path.add(reference.getReferenceTarget());
                    }
                    return path;
                }

                handledElements.add(positionResource.getLocalName());

                path.addFirst(positionResource.getLocalName());
                hiddenElements.add(mapHiddenNode(positionResource, reference, sourceIdentifier));

                var references = positions.listSubjectsWithProperty(SuomiMeta.referenceTarget, positionResource.getLocalName()).toList();

                // no more references found, return path
                // should not happen as each hidden node should have references
                // and otherwise should have returned earlier
                if (references.isEmpty()) {
                    return path;
                }

                // assume, that there is only one reference for hidden nodes
                positionResource = references.get(0);
            }
        }
        // no hidden nodes between source and target
        return List.of(reference.getReferenceTarget());
    }

    private static boolean shouldSkip(Model positions, String sourceIdentifier, Set<String> handledElements, String identifier) {
        if (identifier == null) {
            return true;
        } else if (!identifier.startsWith(ModelConstants.CORNER_PREFIX) && !identifier.equals(sourceIdentifier)) {
            // if there is an element in the path, that is not a hidden node and not the same as source node,
            // this is the wrong path. Jump to the next resource
            return true;
        } else if (handledElements.contains(identifier)) {
            // already handled
            return true;
        } else if (identifier.startsWith(ModelConstants.CORNER_PREFIX)) {
            // find starting point of hidden node, it should equal to sourceIdentifier
            var i = identifier;
            while (i.startsWith(ModelConstants.CORNER_PREFIX)) {
                var s = positions.listSubjectsWithProperty(SuomiMeta.referenceTarget, i).next();
                i = s.getLocalName();
            }
            return !i.equals(sourceIdentifier);
        }
        return false;
    }

    private static VisualizationHiddenNodeDTO mapHiddenNode(Resource position, VisualizationReferenceDTO reference, String sourceIdentifier) {
        var dto = new VisualizationHiddenNodeDTO();

        dto.setIdentifier(MapperUtils.propertyToString(position, DCTerms.identifier));
        dto.setPosition(getPositionFromResource(position));
        dto.setReferenceTarget(MapperUtils.propertyToString(position, SuomiMeta.referenceTarget));
        dto.setReferenceType(reference.getReferenceType());

        if (reference.getReferenceType().equals(VisualizationReferenceType.ASSOCIATION)) {
            dto.setOrigin(reference.getIdentifier());
        } else {
            dto.setOrigin(sourceIdentifier);
        }

        return dto;
    }

    private static PositionDTO getPositionFromResource(Resource positionResource) {
        var x = MapperUtils.getLiteral(positionResource, SuomiMeta.posX, Double.class);
        var y = MapperUtils.getLiteral(positionResource, SuomiMeta.posY, Double.class);
        return new PositionDTO(roundPosition(x), roundPosition(y));
    }

    private static Double roundPosition(Double p) {
        if (p != null) {
            return (double) (Math.round(p / 10) * 10);
        }
        return 0.0;
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
