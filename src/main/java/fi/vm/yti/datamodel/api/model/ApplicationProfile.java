package fi.vm.yti.datamodel.api.model;

import fi.vm.yti.datamodel.api.utils.LDHelper;
import fi.vm.yti.datamodel.api.utils.ModelManager;
import org.apache.jena.iri.IRI;
import org.apache.jena.query.ParameterizedSparqlString;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.rdf.model.RDFList;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.vocabulary.DCTerms;

import java.util.List;
import java.util.UUID;
import java.util.logging.Logger;

/**
 * Created by malonen on 22.11.2017.
 */
public class ApplicationProfile extends AbstractModel {

    private static final Logger logger = Logger.getLogger(ApplicationProfile.class.getName());

    public ApplicationProfile(IRI profileId) {
        super(profileId);
    }

    public ApplicationProfile(String jsonld) {
        super(ModelManager.createJenaModelFromJSONLDString(jsonld));
    }

    public ApplicationProfile(String prefix, IRI namespace, String label, String lang, String allowedLang, List<String> serviceList, List<UUID> orgList) {

        this.modelOrganizations = orgList;

        ParameterizedSparqlString pss = new ParameterizedSparqlString();
        pss.setNsPrefixes(LDHelper.PREFIX_MAP);

        if(namespace.toString().endsWith("/")) {
            pss.setNsPrefix(prefix, namespace.toString());
        } else {
            pss.setNsPrefix(prefix, namespace.toString()+"#");
        }

        //TODO: Return list of recommended skos schemes?
        String queryString = "CONSTRUCT  { "
                + "?modelIRI a owl:Ontology . "
                + "?modelIRI a dcap:DCAP . "
                + "?modelIRI rdfs:label ?mlabel . "
                + "?modelIRI owl:versionInfo ?draft . "
                + "?modelIRI dcterms:created ?creation . "
                + "?modelIRI dcterms:modified ?creation . "
                //+ "?modelIRI dcterms:language "+allowedLang+" . "
                + "?modelIRI dcap:preferredXMLNamespaceName ?namespace . "
                + "?modelIRI dcap:preferredXMLNamespacePrefix ?prefix . " +
                "?modelIRI dcterms:references <http://uri.suomi.fi/terminology/jhs/terminological-vocabulary-1> . " +
                "<http://uri.suomi.fi/terminology/jhs/terminological-vocabulary-1> a skos:ConceptScheme . " +
                "<http://uri.suomi.fi/terminology/jhs/terminological-vocabulary-1> dcterms:title 'JHSMETA'@fi . " +
                "<http://uri.suomi.fi/terminology/jhs/terminological-vocabulary-1> termed:graph  '0043fa54-18b2-4f31-80cf-32eeb0bbb297' . " +
                "<http://uri.suomi.fi/terminology/jhs/terminological-vocabulary-1> termed:id '61bec1e5-70b4-34fc-acfb-ab70428fb6f8' . " +
                "<http://uri.suomi.fi/terminology/jhs/terminological-vocabulary-1> termed:type   'TerminologicalVocabulary' . "+
        "<http://uri.suomi.fi/terminology/jhs/terminological-vocabulary-1> termed:uri 'http://uri.suomi.fi/terminology/jhs/terminological-vocabulary-1' . "
                + "?modelIRI dcterms:isPartOf ?group . "
                + "?group dcterms:identifier ?code . "
                + "?group rdfs:label ?groupLabel . "
                + "?modelIRI dcterms:contributor ?org . "
                + "?org skos:prefLabel ?orgLabel . "
                + "?org a foaf:Organization . "
                + "} WHERE { "
                + "BIND(now() as ?creation) "
                + "GRAPH <urn:yti:servicecategories> { "
                + "?group at:op-code ?code . "
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

        QueryExecution qexec = QueryExecutionFactory.sparqlService(services.getCoreSparqlAddress(), pss.toString());
        this.graph = qexec.execConstruct();
        RDFList langRDFList = LDHelper.addStringListToModel(this.graph, allowedLang);
        this.graph.add(ResourceFactory.createResource(namespace.toString()), DCTerms.language, langRDFList);


    }

}
