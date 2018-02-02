package fi.vm.yti.datamodel.api.model;

import fi.vm.yti.datamodel.api.utils.GraphManager;
import fi.vm.yti.datamodel.api.utils.LDHelper;
import fi.vm.yti.datamodel.api.utils.ModelManager;
import fi.vm.yti.datamodel.api.utils.TermedTerminologyManager;
import org.apache.jena.iri.IRI;
import org.apache.jena.query.ParameterizedSparqlString;
import org.apache.jena.rdf.model.ResourceFactory;
import java.util.logging.Logger;


/**
 * Created by malonen on 29.11.2017.
 */

public class ReusablePredicate extends AbstractPredicate {

    private static final Logger logger = Logger.getLogger(ReusablePredicate.class.getName());

    public ReusablePredicate(IRI predicateId) throws IllegalArgumentException {
        super(predicateId);
    }

    public ReusablePredicate(String jsonld) throws IllegalArgumentException {
        super(ModelManager.createJenaModelFromJSONLDString(jsonld));
    }

    public ReusablePredicate(IRI conceptIRI, IRI modelIRI, String predicateLabel, String lang, IRI typeIRI) {
        ParameterizedSparqlString pss = new ParameterizedSparqlString();
        pss.setNsPrefixes(LDHelper.PREFIX_MAP);

        String queryString = "CONSTRUCT  { "
                + "?predicateIRI owl:versionInfo ?draft . "
                + "?predicateIRI dcterms:created ?creation . "
                + "?predicateIRI dcterms:modified ?modified . "
                + "?predicateIRI a ?type .  "
                + "?predicateIRI rdfs:isDefinedBy ?model . "
                + "?model rdfs:label ?modelLabel . "
                + "?model a ?modelType . "
                + "?predicateIRI rdfs:label ?predicateLabel . "
                + "?predicateIRI rdfs:comment ?comment . "
                + "?predicateIRI dcterms:subject ?concept . "
                + "?concept a skos:Concept . "
                + "?concept termed:id ?conceptId . "
                + "?concept termed:graph ?graphId . "
                + "?concept skos:prefLabel ?label . "
                + "?concept skos:definition ?comment . "
                + "?concept skos:inScheme ?scheme . "
                + "?scheme a skos:ConceptScheme . "
                + "?scheme dcterms:title ?title . "
                + "} "
                + "WHERE { "
                + "BIND(now() as ?creation) "
                + "BIND(now() as ?modified) "
                + "?model a ?modelType . "
                + "?model rdfs:label ?modelLabel . "
                + "?concept a skos:Concept . "
                + "?concept termed:id ?conceptId . "
                + "?concept termed:graph ?graphId . "
                + "?concept skos:prefLabel ?label . "
                + "?concept skos:inScheme ?scheme . "
                + "?scheme dcterms:title ?title . "
                + "OPTIONAL {"
                + "?concept skos:definition ?comment . } "
                + "}";

        pss.setCommandText(queryString);

        if(conceptIRI.toString().startsWith("urn:uuid:"))
            pss.setLiteral("conceptId", conceptIRI.toString().replaceFirst("urn:uuid:",""));
        else
            pss.setIri("concept", conceptIRI);

        pss.setIri("model", modelIRI);
        pss.setIri("type", typeIRI);
        pss.setLiteral("draft", "DRAFT");
        pss.setLiteral("predicateLabel", ResourceFactory.createLangLiteral(predicateLabel, lang));
        String predicateName = LDHelper.propertyName(predicateLabel);
        pss.setIri("predicateIRI",LDHelper.resourceIRI(modelIRI.toString(), predicateName));

        this.graph = TermedTerminologyManager.constructCleanedModelFromTermedAPIAndCore(conceptIRI.toString(),modelIRI.toString(),pss.asQuery());

    }

    public ReusablePredicate(IRI modelIRI, String predicateLabel, String lang, IRI typeIRI) {
        ParameterizedSparqlString pss = new ParameterizedSparqlString();
        pss.setNsPrefixes(LDHelper.PREFIX_MAP);

        String queryString = "CONSTRUCT  { "
                + "?predicateIRI owl:versionInfo ?draft . "
                + "?predicateIRI dcterms:created ?creation . "
                + "?predicateIRI dcterms:modified ?modified . "
                + "?predicateIRI a ?type .  "
                + "?predicateIRI rdfs:isDefinedBy ?model . "
                + "?model rdfs:label ?modelLabel . "
                + "?model a ?modelType . "
                + "?predicateIRI rdfs:label ?predicateLabel . "
                + "} "
                + "WHERE { "
                + "?model a ?modelType . "
                + "?model rdfs:label ?modelLabel . "
                + "BIND(now() as ?creation) "
                + "BIND(now() as ?modified) "
                + "}";

        pss.setCommandText(queryString);
        pss.setIri("model", modelIRI);
        pss.setIri("type", typeIRI);
        pss.setLiteral("draft", "DRAFT");
        pss.setLiteral("predicateLabel", ResourceFactory.createLangLiteral(predicateLabel, lang));
        String predicateName = LDHelper.propertyName(predicateLabel);
        pss.setIri("predicateIRI",LDHelper.resourceIRI(modelIRI.toString(), predicateName));

        this.graph = GraphManager.constructModelFromCoreGraph(pss.toString());

    }


}

