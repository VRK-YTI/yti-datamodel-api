package fi.vm.yti.datamodel.api.utils;

import fi.vm.yti.datamodel.api.config.EndpointServices;
import org.apache.jena.query.ParameterizedSparqlString;
import org.apache.jena.rdf.model.Model;

import java.util.logging.Logger;

/**
 * Created by malonen on 15.12.2017.
 */
public class SearchManager {

    static EndpointServices services = new EndpointServices();
    static final private Logger logger = Logger.getLogger(SearchManager.class.getName());

    public static Model search(String graph, String search, String lang) {

        /* TODO: ADD TEXTDATASET ONCE NAMESPACE BUG IS RESOLVED */
        // FIXME: Not working in long run. Needs Lucene indexing or ElasticSearch.
        // if(!search.endsWith("~")||!search.endsWith("*")) search = search+"*";

        String queryString =
                "CONSTRUCT {"
                        + "?resource rdf:type ?type ."
                        + "?resource rdfs:label ?label ."
                        + "?resource rdfs:comment ?comment ."
                        + "?resource rdfs:isDefinedBy ?super . "
                        + "?resource dcap:preferredXMLNamespaceName ?resnamespace . "
                        + "?resource dcap:preferredXMLNamespacePrefix ?resprefix . "
                        + "?super rdfs:label ?superLabel . "
                        + "?super dcap:preferredXMLNamespaceName ?namespace . "
                        + "?super dcap:preferredXMLNamespacePrefix ?prefix . "
                        + "} WHERE { "
                        + (graph==null?"":"GRAPH <"+graph+"#HasPartGraph> { <"+graph+"> dcterms:hasPart ?graph . } ")
                        + "GRAPH ?graph {"
                        + "?resource ?p ?literal . "
                        + "FILTER contains(lcase(?literal),lcase(?search)) "
                        + "?resource rdf:type ?type . "
                        + "OPTIONAL {"
                        + "?resource dcap:preferredXMLNamespaceName ?resnamespace . "
                        + "?resource dcap:preferredXMLNamespacePrefix ?resprefix . "
                        + "}"
                        + "OPTIONAL {"
                        + "?resource rdfs:isDefinedBy ?super . "
                        + "GRAPH ?super { ?super rdfs:label ?superLabel . "
                        + "?super dcap:preferredXMLNamespaceName ?namespace . "
                        + "?super dcap:preferredXMLNamespacePrefix ?prefix . "
                        + "}}"
                        //+ "UNION"
                        // + "{?resource sh:predicate ?predicate . ?super sh:property ?resource . ?super rdfs:label ?superLabel . BIND(sh:Constraint as ?type)}"
                        + "?resource rdfs:label ?label . "
                        //+ "?resource text:query '"+search+"' . "
                        + "OPTIONAL{?resource rdfs:comment ?comment .}"
                        + (lang==null||lang.equals("undefined")?"":"FILTER langMatches(lang(?label),'"+lang+"')")
                        + "}}";

        ParameterizedSparqlString pss = new ParameterizedSparqlString();
        pss.setNsPrefixes(LDHelper.PREFIX_MAP);
        pss.setLiteral("search", search);
        pss.setCommandText(queryString);

        logger.info(pss.toString());

        return GraphManager.constructModelFromCoreGraph(pss.toString());

    }

}
