package fi.vm.yti.datamodel.api.model;

import fi.vm.yti.datamodel.api.service.*;
import fi.vm.yti.datamodel.api.utils.LDHelper;
import org.apache.jena.iri.IRI;
import org.apache.jena.query.ParameterizedSparqlString;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ResourceFactory;

import org.slf4j.Logger;import org.slf4j.LoggerFactory;

public class ReusableClass extends AbstractClass {

    private static final Logger logger = LoggerFactory.getLogger(ReusableClass.class.getName());

    public ReusableClass(IRI classId,
                         GraphManager graphManager) throws IllegalArgumentException {
        super(classId, graphManager);
    }

    public ReusableClass(Model model,
                         GraphManager graphManager) throws IllegalArgumentException {
        super(model, graphManager);
    }

    public ReusableClass(IRI conceptIRI,
                         IRI modelIRI,
                         String classLabel,
                         String lang,
                         GraphManager graphManager,
                         TermedTerminologyManager termedTerminologyManager) {

        ParameterizedSparqlString pss = new ParameterizedSparqlString();
        pss.setNsPrefixes(LDHelper.PREFIX_MAP);

        String queryString = "CONSTRUCT  { "
                + "?classIRI owl:versionInfo ?draft . "
                + "?classIRI dcterms:modified ?modified . "
                + "?classIRI dcterms:created ?creation . "
                + "?classIRI a rdfs:Class . "
                + "?classIRI rdfs:isDefinedBy ?model . "
                + "?model rdfs:label ?modelLabel . "
                + "?model a ?modelType . "
                + "?classIRI sh:name ?classLabel . "
                + "?classIRI sh:description ?comment . "
                + "?classIRI dcterms:subject ?concept . "
                + "?concept a skos:Concept . "
                + "?concept termed:id ?conceptId . "
                + "?concept termed:graph ?graphId . "
                + "?concept skos:prefLabel ?label . "
                + "?concept skos:definition ?comment . "
                + "?concept skos:inScheme ?scheme . "
                + "?scheme a skos:ConceptScheme . "
                + "?scheme skos:prefLabel ?title . "
                + "} WHERE { "
                + "BIND(now() as ?creation) "
                + "BIND(now() as ?modified) "
                + "?model a ?modelType . "
                + "?model rdfs:label ?modelLabel . "
                + "?concept a skos:Concept . "
                + "?concept termed:id ?conceptId . "
                + "?concept termed:graph ?graphId . "
                + "?concept skos:prefLabel ?label . "
                + "?concept skos:inScheme ?scheme . "
                + "?scheme skos:prefLabel ?title . "
                + "OPTIONAL {"
                + "?concept skos:definition ?comment . } "
                + "}";

        pss.setCommandText(queryString);

        if(conceptIRI.toString().startsWith("urn:uuid:"))
             pss.setLiteral("conceptId", conceptIRI.toString().replaceFirst("urn:uuid:",""));
        else
            pss.setIri("concept", conceptIRI);

        pss.setIri("model", modelIRI);
        pss.setLiteral("draft", "DRAFT");
        pss.setLiteral("classLabel", ResourceFactory.createLangLiteral(classLabel, lang));
        String resourceName = LDHelper.resourceName(classLabel);
        pss.setIri("classIRI",LDHelper.resourceIRI(modelIRI.toString(),resourceName));

        this.graph = termedTerminologyManager.constructCleanedModelFromTermedAPIAndCore(conceptIRI.toString(),modelIRI.toString(),pss.asQuery());

    }

    public ReusableClass(IRI modelIRI,
                         String classLabel,
                         String lang,
                         GraphManager graphManager) {

        ParameterizedSparqlString pss = new ParameterizedSparqlString();
        pss.setNsPrefixes(LDHelper.PREFIX_MAP);

        String queryString = "CONSTRUCT  { "
                + "?classIRI owl:versionInfo ?draft . "
                + "?classIRI dcterms:modified ?modified . "
                + "?classIRI dcterms:created ?creation . "
                + "?classIRI a rdfs:Class . "
                + "?classIRI rdfs:isDefinedBy ?model . "
                + "?model rdfs:label ?modelLabel . "
                + "?model a ?modelType . "
                + "?classIRI sh:name ?classLabel . "
                + "?classIRI sh:description ?comment . "
                + "} WHERE { "
                + "BIND(now() as ?creation) "
                + "BIND(now() as ?modified) "
                + "?model a ?modelType . "
                + "?model rdfs:label ?modelLabel . "
                + "}";

        pss.setCommandText(queryString);
        pss.setIri("model", modelIRI);
        pss.setLiteral("draft", "DRAFT");
        pss.setLiteral("classLabel", ResourceFactory.createLangLiteral(classLabel, lang));
        String resourceName = LDHelper.resourceName(classLabel);
        pss.setIri("classIRI",LDHelper.resourceIRI(modelIRI.toString(),resourceName));

        this.graph = graphManager.constructModelFromCoreGraph(pss.toString());

    }


}
