package fi.vm.yti.datamodel.api.v2.migration.task;

import fi.vm.yti.common.Constants;
import fi.vm.yti.common.util.MapperUtils;
import fi.vm.yti.datamodel.api.v2.dto.ModelConstants;
import fi.vm.yti.datamodel.api.v2.properties.DCAP;
import fi.vm.yti.datamodel.api.v2.repository.CoreRepository;
import fi.vm.yti.datamodel.api.v2.utils.DataModelURI;
import fi.vm.yti.migration.MigrationTask;
import org.apache.jena.rdf.model.*;
import org.apache.jena.vocabulary.DCTerms;
import org.apache.jena.vocabulary.OWL2;
import org.apache.jena.vocabulary.RDFS;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.util.HashSet;
import java.util.List;

@SuppressWarnings("java:S101")
@Component
public class V10_RenameURIs implements MigrationTask {

    private static final Logger LOG = LoggerFactory.getLogger(V10_RenameURIs.class);

    private final CoreRepository coreRepository;

    static final String OLD_NS = "http://uri.suomi.fi/datamodel/ns/";

    static final String OLD_POSITION_NS = "http://uri.suomi.fi/datamodel/positions/";

    static final List<Property> GRAPH_PROPERTIES = List.of(
            RDFS.isDefinedBy,
            OWL2.versionIRI,
            OWL2.priorVersion,
            OWL2.imports,
            DCTerms.requires,
            DCAP.preferredXMLNamespace
    );

    public V10_RenameURIs(CoreRepository coreRepository) {
        this.coreRepository = coreRepository;
    }

    @Override
    public void migrate() {
        var graphNameQuery = """
                SELECT ?g {
                    FILTER strstarts(str(?g), "http://uri.suomi.fi/datamodel/")
                    GRAPH ?g {  }
                }
                """;
        var oldGraphs = new HashSet<String>();
        coreRepository.querySelect(graphNameQuery, (var row) -> oldGraphs.add(row.get("g").toString()));

        oldGraphs.forEach(oldGraph -> {
            var oldData = coreRepository.fetch(oldGraph);
            String modelURI;
            if (oldGraph.matches("(.*)\\d+.\\d+.\\d+$")) {
                modelURI = oldGraph.substring(0, oldGraph.lastIndexOf("/"));
            } else {
                modelURI = oldGraph;
            }

            String newGraphURI;
            String prefix;
            String version = null;

            if (oldGraph.startsWith(OLD_NS)) {
                // data models
                var modelResource = oldData.getResource(modelURI);
                prefix = MapperUtils.getLiteral(modelResource, DCAP.preferredXMLNamespacePrefix, String.class);
                version = MapperUtils.propertyToString(modelResource, OWL2.versionInfo);

                newGraphURI = DataModelURI.Factory.createModelURI(prefix, version).getGraphURI();
            } else if (oldGraph.startsWith(OLD_POSITION_NS)) {
                // positions
                try {
                    var uri = new URI(oldGraph);
                    var path = uri.getPath().split("/");
                    prefix = path[3];
                    if (path.length == 5) {
                        version = path[4];
                    }
                    newGraphURI = ModelConstants.MODEL_POSITIONS_NAMESPACE + prefix;

                    if (version != null) {
                        newGraphURI += "/" + version;
                    }
                    newGraphURI += "/";
                } catch (Exception e) {
                    LOG.warn("Invalid graph name {}", oldGraph);
                    return;
                }
            } else {
                LOG.warn("Invalid graph name {}", oldGraph);
                return;
            }

            var newData = ModelFactory.createDefaultModel();

            var iterator = oldData.listStatements();
            while (iterator.hasNext()) {
                var s = iterator.next();

                var subj = getNewSubject(modelURI, s.getSubject(), prefix);
                var pred = getNewPredicate(s.getPredicate());
                var obj = getNewObject(s);

                newData.add(ResourceFactory.createStatement(subj, pred, obj));
            }

            LOG.info("PUT new data: {}", newGraphURI);
            LOG.info("DELETE old data: {}", oldGraph);
            coreRepository.put(newGraphURI, newData);
            coreRepository.delete(oldGraph);
        });
    }

    private static RDFNode getNewObject(Statement s) {
        String newObject = null;
        var obj = s.getObject().toString();
        if (obj.startsWith(OLD_NS)) {
            newObject = obj.replace(OLD_NS, Constants.DATA_MODEL_NAMESPACE);

            // add trailing slash if object is another graph
            if (GRAPH_PROPERTIES.contains(s.getPredicate())) {
                newObject += "/";
            }
        }
        return newObject != null ? ResourceFactory.createResource(newObject) : s.getObject();
    }

    private static Property getNewPredicate(Property p) {
        String newPred = null;
        if (p.toString().startsWith(OLD_NS)) {
            newPred = p.toString().replace(OLD_NS, Constants.DATA_MODEL_NAMESPACE);
        }
        return newPred != null ? ResourceFactory.createProperty(newPred) : p;
    }

    private static Resource getNewSubject(String modelURI, Resource res, String prefix) {
        String newSubject = null;
        var subj = res.toString();

        if (subj.equals(modelURI)) {
            newSubject = DataModelURI.Factory.createModelURI(prefix, null).getModelURI();
        } else if (subj.startsWith(OLD_NS)) {
            newSubject = subj.replace(OLD_NS, Constants.DATA_MODEL_NAMESPACE);
        } else if (subj.startsWith(OLD_POSITION_NS)) {
            newSubject = subj.replace(OLD_POSITION_NS, ModelConstants.MODEL_POSITIONS_NAMESPACE);
        }
        return newSubject != null ? ResourceFactory.createResource(newSubject) : res;
    }
}
