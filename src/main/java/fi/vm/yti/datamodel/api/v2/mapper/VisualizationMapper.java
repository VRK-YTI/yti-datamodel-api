package fi.vm.yti.datamodel.api.v2.mapper;

import fi.vm.yti.datamodel.api.v2.dto.DCAP;
import fi.vm.yti.datamodel.api.v2.dto.ModelConstants;
import fi.vm.yti.datamodel.api.v2.dto.PositionDTO;
import fi.vm.yti.datamodel.api.v2.dto.VisualizationClassDTO;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.SimpleSelector;
import org.apache.jena.vocabulary.DCTerms;
import org.apache.jena.vocabulary.OWL;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class VisualizationMapper {
    private static final Logger LOG = LoggerFactory.getLogger(VisualizationMapper.class);
    private VisualizationMapper() {
    }
    public static List<VisualizationClassDTO> mapVisualizationData(String prefix, Model model, Model positions) {
        var graph = ModelConstants.SUOMI_FI_NAMESPACE + prefix;
        LOG.info("Map visualization data for {}", graph);
        var classIds = model.listStatements(
                new SimpleSelector(null, RDF.type, OWL.Class));

        HashMap<String, String> namespaces = getNamespaces(model, graph);

        var result = new ArrayList<VisualizationClassDTO>();
        while (classIds.hasNext()) {
            String classId = classIds.next().getSubject().getURI();
            var resource = model.getResource(classId);
            var classDTO = new VisualizationClassDTO();
            classDTO.setLabel(MapperUtils.localizedPropertyToMap(resource, RDFS.label));
            classDTO.setIdentifier(resource.getProperty(DCTerms.identifier).getString());
            classDTO.setParentClasses(getParentClasses(resource, namespaces));

            // TODO save and map positions
            var positionResource = positions.getResource(classId);
            if (positionResource == null) {
                classDTO.setPosition(new PositionDTO(0.0, 0.0));
            }
            result.add(classDTO);
        }

        return result;
    }

    /**
     * Map external and internal namespaces and prefixes
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
                    } else {
                        namespaces.put(uri, model.getResource(uri).getProperty(DCAP.preferredXMLNamespacePrefix).getString());
                    }
                }));
        return namespaces;
    }

    @NotNull
    private static HashSet<String> getParentClasses(Resource resource, Map<String, String> namespaceMap) {
        var parentClasses = new HashSet<String>();
        var subClassOf = resource.listProperties(RDFS.subClassOf).toList();
        subClassOf.forEach(parent -> {
            String s = parent.getObject().toString();
            // skip default parent class (owl#Thing)
            if ("http://www.w3.org/2002/07/owl#Thing".equals(s)) {
                return;
            }

            var parts = s.split("#");
            if (parts.length != 2) {
                return;
            }

            var extPrefix = namespaceMap.get(parts[0]);
            if (extPrefix != null) {
                // parent class in the external namespace or other model in Interoperability platform
                parentClasses.add(extPrefix + ":" + parts[1]);
            } else {
                // parent class in the same model
                parentClasses.add(parts[1]);
            }
        });
        return parentClasses;
    }
}
