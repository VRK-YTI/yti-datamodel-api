package fi.vm.yti.datamodel.api.service;

import org.apache.jena.iri.IRI;
import org.apache.jena.query.ParameterizedSparqlString;
import org.apache.jena.rdf.model.Model;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import fi.vm.yti.datamodel.api.utils.LDHelper;
import fi.vm.yti.datamodel.api.utils.QueryLibrary;

@Service
public class ExternalGraphManager {

    private final EndpointServices endpointServices;
    private final JenaClient jenaClient;

    @Autowired
    ExternalGraphManager(EndpointServices endpointServices,
                         JenaClient jenaClient) {
        this.endpointServices = endpointServices;
        this.jenaClient = jenaClient;
    }

    public Model getListOfExternalClasses(String model) {
        /* If no id is provided create a list of classes */
        ParameterizedSparqlString pss = new ParameterizedSparqlString();
        pss.setNsPrefixes(LDHelper.PREFIX_MAP);

        String queryString = "CONSTRUCT { "
            + "?class rdfs:isDefinedBy ?externalModel . "
            + "?externalModel rdfs:label ?externalModelLabel . "
            + "?externalModel a dcterms:Standard . "
            + "?class sh:name ?label . "
            + "?class sh:description ?comment . "
            + "?class a rdfs:Class . "
            + "?class dcterms:modified ?modified . "
            + "} WHERE { "
            + "SERVICE ?modelService { "
            + "GRAPH ?library { "
            + "?library dcterms:requires ?externalModel . "
            + "?externalModel rdfs:label ?externalModelLabel . "
            + "}}"
            + "GRAPH ?externalModel { "
            + "?class a ?type . "
            + "FILTER(!isBlank(?class)) "
            + "VALUES ?type { rdfs:Class owl:Class sh:NodeShape sh:Shape } "
            /* GET LABEL */
            + "OPTIONAL{{ ?class ?labelPred ?labelStr . "
            + "VALUES ?labelPred { rdfs:label sh:name dc:title dcterms:title }"
            + "FILTER(LANG(?labelStr) = '') BIND(STRLANG(STR(?labelStr),'en') as ?label) }"
            + "UNION"
            + "{ ?class ?labelPred ?label . "
            + "VALUES ?labelPred { rdfs:label sh:name dc:title dcterms:title }"
            + " FILTER(LANG(?label)!='') }"
            /* GET COMMENT */
            + "{ ?class ?commentPred ?commentStr . "
            + "VALUES ?commentPred { rdfs:comment skos:definition dcterms:description dc:description prov:definition sh:description }"
            + "FILTER(LANG(?commentStr) = '') BIND(STRLANG(STR(?commentStr),'en') as ?comment) }"
            + "UNION"
            + "{ ?class ?commentPred ?comment . "
            + "VALUES ?commentPred { rdfs:comment skos:definition dcterms:description dc:description prov:definition sh:description }"
            + " FILTER(LANG(?comment)!='') }"
            + "}}"
            + "}";

        pss.setIri("library", model);
        pss.setIri("modelService", endpointServices.getLocalhostCoreSparqlAddress());
        pss.setCommandText(queryString);

        return jenaClient.constructFromExt(pss.toString());

    }

    public Model getExternalClass(IRI id,
                                  String model) {

        ParameterizedSparqlString pss = new ParameterizedSparqlString();

        pss.setNsPrefixes(LDHelper.PREFIX_MAP);

        /* TODO: FIX dublin core etc. rdf:Property properties */

        String queryString = QueryLibrary.externalClassQuery;

        pss.setIri("model", model);
        pss.setIri("modelService", endpointServices.getLocalhostCoreSparqlAddress());
        pss.setCommandText(queryString);
        pss.setIri("classIRI", id);
        pss.setLiteral("draft", "VALID");

        if (!model.equals("undefined")) {
            pss.setIri("library", model);
        }

        return jenaClient.constructFromExt(pss.toString());

    }

    public Model getListOfExternalPredicates(String model) {

        /* If no id is provided create a list of classes */
        ParameterizedSparqlString pss = new ParameterizedSparqlString();
        pss.setNsPrefixes(LDHelper.PREFIX_MAP);

        String queryString = "CONSTRUCT { "
            + "?externalModel rdfs:label ?externalModelLabel . "
            + "?predicate rdfs:isDefinedBy ?externalModel . "
            + "?externalModel a dcterms:Standard . "
            + "?predicate rdfs:label ?label . "
            + "?predicate owl:versionInfo ?draft . "
            + "?predicate a ?type . "
            + "?predicate dcterms:modified ?modified . "
            + "?predicate rdfs:isDefinedBy ?source . "
            + "?source rdfs:label ?sourceLabel . "
            + "} WHERE { "
            + "SERVICE ?modelService { "
            + "GRAPH ?library { "
            + "?library dcterms:requires ?externalModel . "
            + "?externalModel rdfs:label ?externalModelLabel . "
            + "}}"
            + "GRAPH ?externalModel { "
            /* IF Predicate type is known */
            + "{"
            + "?predicate a owl:DatatypeProperty . "
            + "FILTER NOT EXISTS { ?predicate a owl:ObjectProperty }"
            + "BIND(owl:DatatypeProperty as ?type) "
            + "} UNION {"
            + "?predicate a owl:ObjectProperty . "
            + "FILTER NOT EXISTS { ?predicate a owl:DatatypeProperty }"
            + "BIND(owl:ObjectProperty as ?type) "
            + "} UNION {"
            /* Treat owl:AnnotationProperty as DatatypeProperty */
            + "?predicate a owl:AnnotationProperty. "
            + "?predicate rdfs:label ?atLeastSomeLabel . "
            + "FILTER NOT EXISTS { ?predicate a owl:DatatypeProperty }"
            + "BIND(owl:DatatypeProperty as ?type) "
            + "} UNION {"
            /* IF Predicate Type is rdf:Property and range is rdfs:Literal = DatatypeProperty */
            + "?predicate a rdf:Property . "
            + "?predicate rdfs:range rdfs:Literal ."
            + "FILTER NOT EXISTS { ?predicate a ?multiType . VALUES ?multiType { owl:DatatypeProperty owl:ObjectProperty } }"
            + "BIND(owl:DatatypeProperty as ?type) "
            + "} UNION {"
            /* IF Predicate Type is rdf:Property and range is rdfs:Resource then property is object property */
            + "?predicate a rdf:Property . "
            + "?predicate rdfs:range rdfs:Resource ."
            + "FILTER NOT EXISTS { ?predicate a ?multiType . VALUES ?multiType { owl:DatatypeProperty owl:ObjectProperty } }"
            + "BIND(owl:ObjectProperty as ?type) "
            + "}UNION {"
            /* IF Predicate Type is rdf:Property and range is resource that is class or thing */
            + "?predicate a rdf:Property . "
            + "FILTER NOT EXISTS { ?predicate a ?multiType . VALUES ?multiType { owl:DatatypeProperty owl:ObjectProperty } }"
            + "?predicate rdfs:range ?rangeClass . "
            + "FILTER(?rangeClass!=rdfs:Literal)"
            + "?rangeClass a ?rangeClassType . "
            + "VALUES ?rangeClassType { skos:Concept owl:Thing rdfs:Class }"
            + "BIND(owl:ObjectProperty as ?type) "
            + "} UNION {"
            /* IF Predicate type cannot be guessed */
            + "?predicate a rdf:Property . "
            + "FILTER NOT EXISTS { ?predicate a ?multiType . VALUES ?multiType { owl:DatatypeProperty owl:ObjectProperty } }"
            + "FILTER NOT EXISTS { ?predicate rdfs:range rdfs:Literal . }"
            + "FILTER NOT EXISTS { ?predicate rdfs:range rdfs:Resource . }"
            + "FILTER NOT EXISTS { ?predicate rdfs:range ?rangeClass . ?rangeClass a ?rangeClassType . }"
            + "BIND(rdf:Property as ?type)"
            + "} "
            + "FILTER(STRSTARTS(STR(?predicate), STR(?externalModel)))"
            /* GET LABEL */
            + "{ ?predicate ?labelPred ?labelStr . "
            + "VALUES ?labelPred { rdfs:label sh:name dc:title dcterms:title }"
            + "FILTER(LANG(?labelStr) = '') BIND(STRLANG(STR(?labelStr),'en') as ?label) }"
            + "UNION"
            + "{ ?predicate ?labelPred ?label . "
            + "VALUES ?labelPred { rdfs:label sh:name dc:title dcterms:title }"
            + " FILTER(LANG(?label)!='') }"
            /* GET COMMENT */
            + "{ ?predicate ?commentPred ?commentStr . "
            + "VALUES ?commentPred { rdfs:comment skos:definition dcterms:description dc:description prov:definition sh:description }"
            + "FILTER(LANG(?commentStr) = '') BIND(STRLANG(STR(?commentStr),'en') as ?comment) }"
            + "UNION"
            + "{ ?predicate ?commentPred ?comment . "
            + "VALUES ?commentPred { rdfs:comment skos:definition dcterms:description dc:description prov:definition sh:description }"
            + " FILTER(LANG(?comment)!='') }"
            + "} "
            + "}";

        pss.setIri("library", model);
        pss.setIri("modelService", endpointServices.getLocalhostCoreSparqlAddress());
        pss.setLiteral("draft", "VALID");

        pss.setCommandText(queryString);

        return jenaClient.constructFromExt(pss.toString());
    }

    public Model getExternalPredicate(IRI idIRI,
                                      String model) {

        ParameterizedSparqlString pss = new ParameterizedSparqlString();

        pss.setNsPrefixes(LDHelper.PREFIX_MAP);

        String queryString = "CONSTRUCT { "
            + "?externalModel rdfs:label ?externalModelLabel . "
            + "?predicate rdfs:isDefinedBy ?externalModel . "
            + "?externalModel a dcterms:Standard . "
            + "?predicate rdfs:label ?label . "
            + "?predicate rdfs:comment ?comment . "
            + "?predicate a ?type . "
            + "?predicate dcterms:modified ?modified . "
            + "?predicate rdfs:range ?range . "
            + "?predicate rdfs:domain ?domain . "
            + "} WHERE { "
            + "SERVICE ?modelService { "
            + "GRAPH ?library { "
            + "?library dcterms:requires ?externalModel . "
            + "?externalModel rdfs:label ?externalModelLabel . "
            + "}}"
            + "GRAPH ?externalModel { "
            + "{"
            + "?predicate a owl:DatatypeProperty . "
            + "FILTER NOT EXISTS { ?predicate a owl:ObjectProperty }"
            + "BIND(owl:DatatypeProperty as ?type) "
            + "} UNION {"
            + "?predicate a owl:ObjectProperty . "
            + "FILTER NOT EXISTS { ?predicate a owl:DatatypeProperty }"
            + "BIND(owl:ObjectProperty as ?type) "
            + "} UNION {"
            /* Treat owl:AnnotationProperty as DatatypeProperty */
            + "?predicate a owl:AnnotationProperty. "
            + "?predicate rdfs:label ?atLeastSomeLabel . "
            + "FILTER NOT EXISTS { ?predicate a owl:DatatypeProperty }"
            + "BIND(owl:DatatypeProperty as ?type) "
            + "} UNION {"
            /* IF Predicate Type is rdf:Property and range is rdfs:Literal = DatatypeProperty */
            + "?predicate a rdf:Property . "
            + "?predicate rdfs:range rdfs:Literal ."
            + "BIND(owl:DatatypeProperty as ?type) "
            + "FILTER NOT EXISTS { ?predicate a ?multiType . VALUES ?multiType { owl:DatatypeProperty owl:ObjectProperty owl:AnnotationProperty } }"
            + "} UNION {"
            /* IF Predicate Type is rdf:Property and range is rdfs:Resource then property is object property */
            + "?predicate a rdf:Property . "
            + "?predicate rdfs:range rdfs:Resource ."
            + "BIND(owl:ObjectProperty as ?type) "
            + "FILTER NOT EXISTS { ?predicate a ?multiType . VALUES ?multiType { owl:DatatypeProperty owl:ObjectProperty owl:AnnotationProperty  } }"
            + "} UNION {"
            /* IF Predicate Type is rdf:Property and range is resource that is class or thing */
            + "?predicate a rdf:Property . "
            + "FILTER NOT EXISTS { ?predicate a ?multiType . VALUES ?multiType { owl:DatatypeProperty owl:ObjectProperty owl:AnnotationProperty } }"
            + "?predicate rdfs:range ?rangeClass . "
            + "FILTER(?rangeClass!=rdfs:Literal)"
            + "?rangeClass a ?rangeClassType . "
            + "VALUES ?rangeClassType { skos:Concept owl:Thing rdfs:Class }"
            + "BIND(owl:ObjectProperty as ?type) "
            + "} UNION {"
            /* IF Predicate type cannot be guessed */
            + "?predicate a rdf:Property . "
            + "FILTER NOT EXISTS { ?predicate a ?multiType . VALUES ?multiType { owl:DatatypeProperty owl:ObjectProperty owl:AnnotationProperty } }"
            + "FILTER NOT EXISTS { ?predicate rdfs:range rdfs:Literal . }"
            + "FILTER NOT EXISTS { ?predicate rdfs:range rdfs:Resource . }"
            + "FILTER NOT EXISTS { ?predicate rdfs:range ?rangeClass . ?rangeClass a ?rangeClassType . }"
            + "BIND(rdf:Property as ?type)"
            + "} "
            + "OPTIONAL { ?predicate rdfs:range ?range . FILTER (!isBlank(?range)) }"
            /* TODO: Support only direct range no OWL unions */
            //   + "OPTIONAL { ?predicate rdfs:domain ?domain .  }"
            + "OPTIONAL { ?predicate rdfs:label ?labelStr . FILTER(LANG(?labelStr) = '') BIND(STRLANG(STR(?labelStr),'en') as ?label) }"
            + "OPTIONAL { ?predicate rdfs:label ?label . FILTER(LANG(?label)!='') }"
            + "VALUES ?predicateCommentPred { rdfs:comment skos:definition dcterms:description dc:description }"

            /* Predicate comments - if lang unknown create english tag */
            + "OPTIONAL { ?predicate ?predicateCommentPred ?propertyCommentStr . FILTER(LANG(?propertyCommentStr) = '') "
            + "BIND(STRLANG(STR(?propertyCommentStr),'en') as ?comment) }"

            + "OPTIONAL { ?predicate ?predicateCommentPred ?propertyCommentToStr . FILTER(LANG(?propertyCommentToStr)!='') "
            + "BIND(STR(?propertyCommentToStr) as ?comment) }"

            + "} "
            + "}";

        pss.setCommandText(queryString);

        pss.setIri("predicate", idIRI);
        pss.setIri("modelService", endpointServices.getLocalhostCoreSparqlAddress());

        if (model != null && !model.equals("undefined")) {
            pss.setIri("library", model);
        }

        return jenaClient.constructFromExt(pss.toString());

    }

}
