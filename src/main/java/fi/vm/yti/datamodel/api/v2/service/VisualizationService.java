package fi.vm.yti.datamodel.api.v2.service;

import fi.vm.yti.datamodel.api.v2.dto.*;
import fi.vm.yti.datamodel.api.v2.endpoint.error.ResourceNotFoundException;
import fi.vm.yti.datamodel.api.v2.mapper.MapperUtils;
import fi.vm.yti.datamodel.api.v2.mapper.VisualizationMapper;
import org.apache.jena.rdf.model.*;
import org.apache.jena.vocabulary.DCTerms;
import org.apache.jena.vocabulary.OWL;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;
import org.springframework.stereotype.Service;
import org.topbraid.shacl.vocabulary.SH;

import java.util.*;

@Service
public class VisualizationService {

    private final JenaService jenaService;

    public VisualizationService(JenaService jenaService) {
        this.jenaService = jenaService;
    }

    public Set<VisualizationClassDTO> getVisualizationData(String prefix) {
        var graph = ModelConstants.SUOMI_FI_NAMESPACE + prefix;
        var model = jenaService.getDataModel(graph);
        Model positions;
        try {
            positions = jenaService.getDataModel(ModelConstants.MODEL_POSITIONS_NAMESPACE + prefix);
        } catch(ResourceNotFoundException e) {
            positions = ModelFactory.createDefaultModel();
        }

        var modelResource = model.getResource(graph);
        var classURIs = getClassURIs(model, graph);
        var namespaces = getNamespaces(model, graph);
        var languages = MapperUtils.arrayPropertyToSet(modelResource, DCTerms.language);

        var result = new HashSet<VisualizationClassDTO>();

        while (classURIs.hasNext()) {
            String classURI = classURIs.next().getSubject().getURI();
            var classResource = model.getResource(classURI);

            var classDTO = VisualizationMapper.mapClass(classURI, model, namespaces);

            var attributesAndAssociations = getAttributesAndAssociations(model, classResource, modelResource);
            var externalPropertyURIs = new HashSet<String>();
            for (var resource : attributesAndAssociations) {
                if (!model.contains(resource, null)) {
                    externalPropertyURIs.add(resource.getURI());
                } else {
                    VisualizationMapper.mapResource(classDTO, resource, model, namespaces);
                }
            }

            if (!externalPropertyURIs.isEmpty()) {
                var externalResources = jenaService.findResources(externalPropertyURIs);
                externalPropertyURIs.forEach(uri -> VisualizationMapper
                        .mapResource(classDTO, externalResources.getResource(uri), model, namespaces));
            }

            // add dummy classes for external classes
            addExternalClasses(classDTO, languages, result);

            result.add(classDTO);
        }
        VisualizationMapper.mapPositionsDataToDTOs(positions, prefix, result);
        return result;
    }

    public void savePositionData(String modelPrefix, List<PositionDataDTO> positions) {
        String positionGraphURI = ModelConstants.MODEL_POSITIONS_NAMESPACE + modelPrefix;

        if (jenaService.doesDataModelExist(positionGraphURI)) {
            jenaService.deleteDataModel(positionGraphURI);
        }

        var positionModel = VisualizationMapper.mapPositionDataToModel(positionGraphURI, positions);
        jenaService.putDataModelToCore(positionGraphURI, positionModel);
    }

    private static void addExternalClasses(VisualizationClassDTO classDTO, Set<String> languages, HashSet<VisualizationClassDTO> result) {
        classDTO.getParentClasses().forEach(parent -> {
            if (parent.contains(":")) {
                result.add(VisualizationMapper.mapExternalClass(parent, languages));
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
                        namespaces.put(uri, uri.replace(ModelConstants.SUOMI_FI_NAMESPACE, ""));
                    } else if (!uri.startsWith(ModelConstants.TERMINOLOGY_NAMESPACE)
                            && !uri.startsWith(ModelConstants.CODELIST_NAMESPACE)) {
                        namespaces.put(uri, model.getResource(uri).getProperty(DCAP.preferredXMLNamespacePrefix).getString());
                    }
                }));
        return namespaces;
    }

    private static List<Resource> getAttributesAndAssociations(
            Model model, Resource classResource, Resource modelResource) {
        if (MapperUtils.isLibrary(modelResource)) {
            return model.listSubjectsWithProperty(RDFS.domain, classResource).toList();
        } else if (MapperUtils.isApplicationProfile(modelResource)) {
            return classResource.listProperties(SH.property)
                    .mapWith(Statement::getResource)
                    .toList();
        }
        return List.of();
    }
}
