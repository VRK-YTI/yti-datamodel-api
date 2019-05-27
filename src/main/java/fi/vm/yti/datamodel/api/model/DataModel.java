package fi.vm.yti.datamodel.api.model;

import fi.vm.yti.datamodel.api.service.*;
import fi.vm.yti.datamodel.api.utils.LDHelper;
import org.apache.jena.iri.IRI;
import org.apache.jena.query.ParameterizedSparqlString;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.rdf.model.*;
import org.apache.jena.vocabulary.DCTerms;
import org.topbraid.shacl.vocabulary.SH;

import java.util.List;
import java.util.UUID;

public class DataModel extends AbstractModel {

    public DataModel(IRI graphId,
                     GraphManager graphManager) throws IllegalArgumentException {
        super(graphId, graphManager);
    }

    public DataModel(Model model,
                     GraphManager graphManager,
                     RHPOrganizationManager rhpOrganizationManager) throws IllegalArgumentException {
        super(model, graphManager, rhpOrganizationManager);
    }

    public DataModel(String prefix,
                     IRI namespace,
                     String label,
                     String lang,
                     String allowedLang,
                     List<String> serviceList,
                     List<UUID> orgList,
                     GraphManager graphManager,
                     EndpointServices endpointServices) {
        super(graphManager);

        this.modelOrganizations = orgList;

        ParameterizedSparqlString pss = new ParameterizedSparqlString();
        pss.setNsPrefixes(LDHelper.PREFIX_MAP);

        if(namespace.toString().endsWith("/")) {
            pss.setNsPrefix(prefix, namespace.toString());
        } else {
            pss.setNsPrefix(prefix, namespace.toString()+"#");
        }

        String queryString = "CONSTRUCT  { "
                + "?modelIRI a owl:Ontology . "
                + "?modelIRI a dcap:MetadataVocabulary . "
                + "?modelIRI rdfs:label ?mlabel . "
                + "?modelIRI owl:versionInfo ?draft . "
                + "?modelIRI dcterms:created ?creation . "
                + "?modelIRI dcterms:modified ?creation . "
                //+ "?modelIRI dcterms:language "+allowedLang+" . "
                + "?modelIRI dcap:preferredXMLNamespaceName ?namespace . "
                + "?modelIRI dcap:preferredXMLNamespacePrefix ?prefix . "
               // "?modelIRI dcterms:references <http://uri.suomi.fi/terminology/jhs/terminological-vocabulary-1> . " +
               // "<http://uri.suomi.fi/terminology/jhs/terminological-vocabulary-1> a skos:ConceptScheme . " +
               // "<http://uri.suomi.fi/terminology/jhs/terminological-vocabulary-1> dcterms:title 'Julkisen hallinnon yhteinen sanasto'@fi . " +
               // "<http://uri.suomi.fi/terminology/jhs/terminological-vocabulary-1> termed:graph  '0043fa54-18b2-4f31-80cf-32eeb0bbb297' . " +
               // "<http://uri.suomi.fi/terminology/jhs/terminological-vocabulary-1> termed:id '61bec1e5-70b4-34fc-acfb-ab70428fb6f8' . " +
               // "<http://uri.suomi.fi/terminology/jhs/terminological-vocabulary-1> termed:type   'TerminologicalVocabulary' . " +
               // "<http://uri.suomi.fi/terminology/jhs/terminological-vocabulary-1> termed:uri 'http://uri.suomi.fi/terminology/jhs/terminological-vocabulary-1' . "
                + "?modelIRI dcterms:isPartOf ?group . "
                + "?group dcterms:identifier ?code . "
                + "?group rdfs:label ?groupLabel . "
                + "?modelIRI dcterms:contributor ?org . "
                + "?org skos:prefLabel ?orgLabel . "
                + "?org a foaf:Organization . "
                + "} WHERE { "
                + "BIND(now() as ?creation) "
                + "GRAPH <urn:yti:servicecategories> { "
                + "?group skos:notation ?code . "
                + "?group skos:prefLabel ?groupLabel . "
                + "FILTER(LANGMATCHES(LANG(?groupLabel), '"+lang+"'))"
                + "VALUES ?code { "+LDHelper.concatStringList(serviceList, " ", "'")+"}"
                + "}"
                + "GRAPH <urn:yti:organizations> {"
                + "?org a ?orgType . "
                + "?org skos:prefLabel ?orgLabel . "
                + "VALUES ?org { "+LDHelper.concatWithReplace(orgList," ", "<urn:uuid:@this>")+" }"
                + "}"
                + "}";

        pss.setCommandText(queryString);

        if(namespace.toString().endsWith("/")) {
            pss.setLiteral("namespace", namespace.toString());
        } else {
            pss.setLiteral("namespace", namespace.toString()+"#");
        }

        pss.setLiteral("prefix", prefix);
        pss.setIri("modelIRI", namespace);
        pss.setLiteral("draft", "DRAFT");
        pss.setLiteral("mlabel", ResourceFactory.createLangLiteral(label, lang));
        pss.setLiteral("defLang", lang);

        try(QueryExecution qexec = QueryExecutionFactory.sparqlService(endpointServices.getCoreSparqlAddress(), pss.toString())) {
            this.graph = qexec.execConstruct();
        }
        RDFList langRDFList = LDHelper.addStringListToModel(this.graph, allowedLang);
        this.graph.add(ResourceFactory.createResource(namespace.toString()), DCTerms.language, langRDFList);

    }

    public String getPrefix() {
        return this.asGraph().getRequiredProperty(ResourceFactory.createResource(this.getId()),LDHelper.curieToProperty("dcap:preferredXMLNamespacePrefix")).getString();
    }

    public String getNamespace() {
        return this.asGraph().getRequiredProperty(ResourceFactory.createResource(this.getId()),LDHelper.curieToProperty("dcap:preferredXMLNamespaceName")).getString();
    }
}
