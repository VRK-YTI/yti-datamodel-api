package fi.vm.yti.datamodel.api.index;

import fi.vm.yti.datamodel.api.service.JenaClient;
import fi.vm.yti.datamodel.api.utils.LDHelper;
import org.apache.jena.query.ParameterizedSparqlString;
import org.apache.jena.query.Query;
import org.apache.jena.rdf.model.Model;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.inject.Singleton;
import javax.ws.rs.NotFoundException;

@Singleton
@Service
public final class FrameManager {

    private static final Logger logger = LoggerFactory.getLogger(FrameManager.class);

    private final JenaClient jenaClient;

    @Autowired
    public FrameManager(final JenaClient jenaClient) {
        this.jenaClient = jenaClient;
    }

    /**
     * Creates export graph by joining all the resources to one graph
     *
     * @param graph model IRI that is used to create export graph
     */
    public Model constructExportGraph(String graph) {

        String queryString = "CONSTRUCT { "
            + "?model <http://purl.org/dc/terms/hasPart> ?resource . "
            + "?rs ?rp ?ro . "
            + " } WHERE {"
            + " GRAPH ?model {"
            + "?model a owl:Ontology . "
            + "} OPTIONAL {"
            + "GRAPH ?modelHasPartGraph { "
            + " ?model <http://purl.org/dc/terms/hasPart> ?resource . "
            + " } GRAPH ?resource { "
            + "?rs ?rp ?ro . "
            + "}"
            + "}}";

        ParameterizedSparqlString pss = new ParameterizedSparqlString();
        pss.setCommandText(queryString);
        pss.setNsPrefixes(LDHelper.PREFIX_MAP);
        pss.setIri("model", graph);
        pss.setIri("modelHasPartGraph", graph + "#HasPartGraph");

        Query query = pss.asQuery();
        Model exportModel = jenaClient.getModelFromCore(graph);

        if (exportModel != null) {
            Model exportModelConstruct = jenaClient.constructFromService(query.toString(), jenaClient.getEndpointServices().getCoreSparqlAddress());
            exportModel.add(exportModelConstruct);
            return exportModel;
        }
        throw new NotFoundException("Could not found graph " + graph);
    }
}
