package fi.vm.yti.datamodel.api.model;

import fi.vm.yti.datamodel.api.config.EndpointServices;
import fi.vm.yti.datamodel.api.utils.*;
import org.apache.jena.iri.IRI;
import org.apache.jena.query.ParameterizedSparqlString;
import org.apache.jena.util.SplitIRI;

import java.util.UUID;
import java.util.logging.Logger;

/**
 * Created by malonen on 22.11.2017.
 */
public class Shape extends AbstractShape {

    private static final Logger logger = Logger.getLogger(Shape.class.getName());
    private EndpointServices services = new EndpointServices();

    public Shape(IRI shapeId) {
        super(shapeId);
    }

    public Shape(String jsonld) {
        super(ModelManager.createJenaModelFromJSONLDString(jsonld));
    }

    public Shape(IRI classIRI, IRI shapeIRI, IRI profileIRI) {

        logger.info("Creating shape from "+classIRI.toString()+" to "+shapeIRI.toString());

        ParameterizedSparqlString pss = new ParameterizedSparqlString();
        pss.setNsPrefixes(LDHelper.PREFIX_MAP);

        String queryString;
        String service;

        /* Create Shape from Class */
        if(GraphManager.isExistingServiceGraph(SplitIRI.namespace(classIRI.toString()))) {

            service = services.getCoreSparqlAddress();
            queryString = "CONSTRUCT  { "
                    + "?shapeIRI owl:versionInfo ?draft . "
                    + "?shapeIRI dcterms:modified ?modified . "
                    + "?shapeIRI dcterms:created ?creation . "
                    + "?shapeIRI sh:scopeClass ?classIRI . "
                    + "?shapeIRI a rdfs:Class . "
                    + "?shapeIRI a sh:Shape . "
                    + "?shapeIRI rdfs:isDefinedBy ?model . "
                    + "?shapeIRI rdfs:label ?label . "
                    + "?shapeIRI rdfs:comment ?comment . "
                    + "?shapeIRI dcterms:subject ?concept . "
                    + "?concept skos:prefLabel ?prefLabel . "
                    + "?concept skos:definition ?definition . "
                    + "?concept skos:inScheme ?scheme . "
                    + "?scheme dcterms:title ?title . "
                    + "?scheme termed:id ?schemeId . "
                    + "?scheme termed:graph ?termedGraph . "
                    + "?concept termed:graph ?termedGraph . "
                    + "?termedGraph termed:id ?termedGraphId . "
                    + "?shapeIRI sh:property ?shapeuuid . "
                    + "?shapeuuid ?p ?o . "
                    + "} WHERE { "
                    + "BIND(now() as ?creation) "
                    + "BIND(now() as ?modified) "
                    + "GRAPH ?classIRI { "
                    + "?classIRI a rdfs:Class . "
                    + "?classIRI rdfs:label ?label . "
                    + "OPTIONAL { ?classIRI rdfs:comment ?comment . } "
                    + "OPTIONAL {{ "
                    + "?classIRI dcterms:subject ?concept . "
                    + "?concept skos:prefLabel ?prefLabel . "
                    + "?concept skos:definition ?definition . "
                    + "} UNION { "
                    + "?classIRI dcterms:subject ?concept . "
                    + "?concept skos:prefLabel ?prefLabel . "
                    + "?concept skos:definition ?definition . "
                    + "?concept skos:inScheme ?collection . "
                    + "?collection dcterms:title ?title . "
                    + "}}"
                    + "OPTIONAL {"
                    + "?classIRI sh:property ?property .  "
                    /* Todo: Issue 472 */
                    + "BIND(IRI(CONCAT(STR(?property),?shapePropertyID)) as ?shapeuuid)"
                    + "?property ?p ?o . "
                    + "}} "
                    + "}";

            pss.setLiteral("shapePropertyID", "-"+ UUID.randomUUID().toString());


        } else {
            /* Create Shape from external IMPORT */
            service = services.getImportsSparqlAddress();
            logger.info("Using ext query:");
            queryString = QueryLibrary.externalShapeQuery;
        }

        pss.setCommandText(queryString);
        pss.setIri("classIRI", classIRI);
        pss.setIri("model", profileIRI);
        pss.setIri("modelService",services.getLocalhostCoreSparqlAddress());
        pss.setLiteral("draft", "Unstable");
        pss.setIri("shapeIRI",shapeIRI);

        logger.info(pss.toString());

        this.graph = GraphManager.constructModelFromGraph(pss.toString(), service);

    }

}
