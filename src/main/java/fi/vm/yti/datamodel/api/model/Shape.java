package fi.vm.yti.datamodel.api.model;

import fi.vm.yti.datamodel.api.service.*;
import fi.vm.yti.datamodel.api.utils.*;
import org.apache.jena.iri.IRI;
import org.apache.jena.query.ParameterizedSparqlString;
import org.apache.jena.util.SplitIRI;

import java.util.UUID;
import org.slf4j.Logger;import org.slf4j.LoggerFactory;

public class Shape extends AbstractShape {

    private static final Logger logger = LoggerFactory.getLogger(Shape.class.getName());

    public Shape(IRI shapeId,
                 GraphManager graphManager) {
        super(shapeId, graphManager);
    }

    public Shape(String jsonld,
                 GraphManager graphManager,
                 ModelManager modelManager) {
        super(modelManager.createJenaModelFromJSONLDString(jsonld), graphManager);
    }

    public Shape(IRI classIRI,
                 IRI shapeIRI,
                 IRI profileIRI,
                 GraphManager graphManager,
                 EndpointServices endpointServices) {

        logger.info("Creating shape from "+classIRI.toString()+" to "+shapeIRI.toString());

        ParameterizedSparqlString pss = new ParameterizedSparqlString();
        pss.setNsPrefixes(LDHelper.PREFIX_MAP);

        String queryString;
        String service;

        /* Create Shape from Class */
        if(graphManager.isExistingServiceGraph(SplitIRI.namespace(classIRI.toString()))) {

            service = "core";
            queryString = "CONSTRUCT  { "
                    + "?shapeIRI owl:versionInfo ?draft . "
                    + "?shapeIRI dcterms:modified ?modified . "
                    + "?shapeIRI dcterms:created ?creation . "
                    + "?shapeIRI sh:targetClass ?classIRI . "
                    + "?shapeIRI a sh:NodeShape . "
                    + "?shapeIRI rdfs:isDefinedBy ?model . "
                    + "?shapeIRI sh:name ?label . "
                    + "?shapeIRI sh:description ?comment . "
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
                    + "?classIRI sh:name ?label . "
                    + "OPTIONAL { ?classIRI sh:description ?comment . } "
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
            service = "imports";
            logger.info("Using ext query:");
            queryString = QueryLibrary.externalShapeQuery;
        }

        pss.setCommandText(queryString);
        pss.setIri("classIRI", classIRI);
        pss.setIri("model", profileIRI);
        pss.setIri("modelService", endpointServices.getLocalhostCoreSparqlAddress());
        pss.setLiteral("draft", "DRAFT");
        pss.setIri("shapeIRI",shapeIRI);

        this.graph = graphManager.constructModelFromService(pss.toString(), service);

    }

}
