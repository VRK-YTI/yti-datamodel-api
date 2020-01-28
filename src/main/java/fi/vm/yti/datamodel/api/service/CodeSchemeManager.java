/*
 * Licensed under the European Union Public Licence (EUPL) V.1.1
 */
package fi.vm.yti.datamodel.api.service;

import fi.vm.yti.datamodel.api.config.ApplicationProperties;
import fi.vm.yti.datamodel.api.utils.LDHelper;

import org.apache.jena.datatypes.xsd.XSDDatatype;
import org.apache.jena.datatypes.xsd.XSDDateTime;
import org.apache.jena.query.*;
import org.apache.jena.rdf.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;

@Service
public class CodeSchemeManager {

    private static final Logger logger = LoggerFactory.getLogger(CodeSchemeManager.class.getName());
    private final EndpointServices endpointServices;
    private final JenaClient jenaClient;
    private final ApplicationProperties properties;

    @Autowired
    CodeSchemeManager(EndpointServices endpointServices,
                      JenaClient jenaClient,
                      ApplicationProperties properties) {

        this.endpointServices = endpointServices;
        this.jenaClient = jenaClient;
        this.properties = properties;
    }

    public void deleteCodeSchemeGraph(String graph) {
        jenaClient.deleteModelFromScheme(graph);
    }

    public Model getSchemeGraph(String graph) {
        return jenaClient.getModelFromSchemes(graph);
    }

    /**
     * Returns date when the model was last modified from the Export graph
     *
     * @return Returns date
     */
    public Date lastModified(String scheme) {

        ParameterizedSparqlString pss = new ParameterizedSparqlString();
        String selectResources =
            "SELECT ?date WHERE { "
                + "GRAPH ?graph { " +
                " ?codeScheme a iow:FCodeScheme . "
                + "?codeScheme dcterms:modified ?date . " +
                "}}";

        pss.setNsPrefixes(LDHelper.PREFIX_MAP);
        pss.setCommandText(selectResources);
        pss.setIri("codeScheme", scheme);

        ResultSet results = jenaClient.selectQuery(endpointServices.getSchemesSparqlAddress(), pss.asQuery());

        Date modified = null;

        while (results.hasNext()) {
            QuerySolution soln = results.nextSolution();
            if (soln.contains("date")) {

                Literal liteDate = soln.getLiteral("date");
                modified = ((XSDDateTime) XSDDatatype.XSDdateTime.parse(liteDate.getString())).asCalendar().getTime();
            }
        }

        return modified;
    }

}
