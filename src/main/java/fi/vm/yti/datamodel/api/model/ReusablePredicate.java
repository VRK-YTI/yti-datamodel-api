package fi.vm.yti.datamodel.api.model;

import fi.vm.yti.datamodel.api.service.*;
import fi.vm.yti.datamodel.api.utils.LDHelper;

import org.apache.jena.iri.IRI;
import org.apache.jena.query.ParameterizedSparqlString;
import org.apache.jena.rdf.model.*;
import org.apache.jena.util.ResourceUtils;
import org.apache.jena.vocabulary.DCTerms;
import org.apache.jena.vocabulary.OWL;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.topbraid.shacl.vocabulary.SH;

import java.util.Iterator;
import java.util.List;
import java.util.UUID;

public class ReusablePredicate extends AbstractPredicate {

    private static final Logger logger = LoggerFactory.getLogger(ReusablePredicate.class.getName());

    public ReusablePredicate(IRI predicateId,
                             GraphManager graphManager) throws IllegalArgumentException {
        super(predicateId, graphManager);
    }

    public ReusablePredicate(Model model,
                             GraphManager graphManager) throws IllegalArgumentException {
        super(model, graphManager);
    }

    public ReusablePredicate(IRI conceptIRI,
                             IRI modelIRI,
                             String predicateLabel,
                             String lang,
                             IRI typeIRI,
                             GraphManager graphManager,
                             TermedTerminologyManager termedTerminologyManager) {

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
            + "?scheme skos:prefLabel ?title . "
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
            + "?scheme skos:prefLabel ?title . "
            + "OPTIONAL {"
            + "?concept skos:definition ?comment . } "
            + "}";

        pss.setCommandText(queryString);

        if (conceptIRI.toString().startsWith("urn:uuid:"))
            pss.setLiteral("conceptId", conceptIRI.toString().replaceFirst("urn:uuid:", ""));
        else
            pss.setIri("concept", conceptIRI);

        pss.setIri("model", modelIRI);
        pss.setIri("type", typeIRI);
        pss.setLiteral("draft", "DRAFT");
        pss.setLiteral("predicateLabel", ResourceFactory.createLangLiteral(predicateLabel, lang));
        String predicateName = LDHelper.propertyName(predicateLabel);
        pss.setIri("predicateIRI", LDHelper.resourceIRI(modelIRI.toString(), predicateName));

        this.graph = termedTerminologyManager.constructCleanedModelFromTermedAPIAndCore(conceptIRI.toString(), modelIRI.toString(), pss.asQuery());

    }

    /**
     * Creates superpredicate from exiting predicate
     *
     * @param oldPredicateIRI IRI of the existing class
     * @param newModelIRI     Model to create the superclass
     * @param graphManager    Graphservice
     */
    public ReusablePredicate(IRI oldPredicateIRI,
                             IRI newModelIRI,
                             Property relatedProperty,
                             GraphManager graphManager) {

        this.graph = graphManager.getCoreGraph(oldPredicateIRI);

        if (this.graph.size() < 1) {
            throw new IllegalArgumentException("No existing predicate found");
        }

        if (!(this.graph.contains(ResourceFactory.createResource(oldPredicateIRI.toString()), RDF.type, OWL.DatatypeProperty) || this.graph.contains(ResourceFactory.createResource(oldPredicateIRI.toString()), RDF.type, OWL.ObjectProperty))) {
            throw new IllegalArgumentException("Expected predicate type");
        }

        Resource relatedPredicate = this.graph.getResource(oldPredicateIRI.toString());
        String superPredicateIRI = newModelIRI + "#" + relatedPredicate.getLocalName();
        logger.debug("Creating new superPredicate: " + superPredicateIRI);
        ResourceUtils.renameResource(relatedPredicate, superPredicateIRI);

        relatedPredicate = this.graph.getResource(superPredicateIRI);
        relatedPredicate.removeAll(OWL.versionInfo);
        relatedPredicate.removeAll(RDFS.range);
        relatedPredicate.removeAll(RDFS.domain);
        relatedPredicate.addLiteral(OWL.versionInfo, "DRAFT");

        Resource oldModel = relatedPredicate.getPropertyResourceValue(RDFS.isDefinedBy);
        oldModel.removeProperties();
        relatedPredicate.removeAll(RDFS.isDefinedBy);
        relatedPredicate.addProperty(RDFS.isDefinedBy, ResourceFactory.createResource(newModelIRI.toString()));
        relatedPredicate.addProperty(relatedProperty, superPredicateIRI);

        LDHelper.rewriteLiteral(this.graph, relatedPredicate, DCTerms.created, LDHelper.getDateTimeLiteral());
        LDHelper.rewriteLiteral(this.graph, relatedPredicate, DCTerms.modified, LDHelper.getDateTimeLiteral());
        relatedPredicate.removeAll(DCTerms.identifier);

        this.graph.add(graphManager.getModelInfo(newModelIRI));

    }

    public ReusablePredicate(IRI modelIRI,
                             String predicateLabel,
                             String lang,
                             IRI typeIRI,
                             GraphManager graphManager) {

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
            + "WHERE { GRAPH ?model {"
            + "?model a ?modelType . "
            + "?model rdfs:label ?modelLabel . } "
            + "BIND(now() as ?creation) "
            + "BIND(now() as ?modified) "
            + "}";

        pss.setCommandText(queryString);
        pss.setIri("model", modelIRI);
        pss.setIri("type", typeIRI);
        pss.setLiteral("draft", "DRAFT");
        pss.setLiteral("predicateLabel", ResourceFactory.createLangLiteral(predicateLabel, lang));
        String predicateName = LDHelper.propertyName(predicateLabel);
        pss.setIri("predicateIRI", LDHelper.resourceIRI(modelIRI.toString(), predicateName));

        this.graph = graphManager.constructModelFromCoreGraph(pss.toString());

    }

}

