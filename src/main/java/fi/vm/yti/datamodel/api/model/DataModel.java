package fi.vm.yti.datamodel.api.model;

import fi.vm.yti.datamodel.api.utils.LDHelper;
import fi.vm.yti.datamodel.api.utils.ModelManager;
import org.apache.jena.iri.IRI;
import org.apache.jena.query.ParameterizedSparqlString;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.rdf.model.*;

import java.util.List;
import java.util.UUID;
import java.util.logging.Logger;

/**
 * Created by malonen on 14.11.2017.
 */
public class DataModel extends AbstractModel {

    private static final Logger logger = Logger.getLogger(DataModel.class.getName());

    public DataModel(IRI graphId) throws IllegalArgumentException {
        super(graphId);
    }
ey
    public DataModel(String jsonld) throws IllegalArgumentException {
        super(ModelManager.createModelFromString(jsonld));
    }

    public DataModel(String prefix, IRI namespace, String label, String lang, String allowedLang, List<String> serviceList, List<UUID> orgList) {

        this.modelOrganizations = orgList;
        logger.info("Creating new datamodel with SPARQL CONSTRUCT");

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
                + "?modelIRI a dcap:MetadataVocabulary . "
                + "?modelIRI rdfs:label ?mlabel . "
                + "?modelIRI owl:versionInfo ?draft . "
                + "?modelIRI dcterms:created ?creation . "
                + "?modelIRI dcterms:modified ?creation . "
                + "?modelIRI dcterms:language "+allowedLang+" . "
                + "?modelIRI dcap:preferredXMLNamespaceName ?namespace . "
                + "?modelIRI dcap:preferredXMLNamespacePrefix ?prefix . "
                + "?modelIRI dcterms:isPartOf ?group . "
                + "?group at:op-code ?code . "
                + "?modelIRI dcterms:contributor ?org . "
                + "} WHERE { "
                + "BIND(now() as ?creation) "
                + "GRAPH <urn:yti:servicecategories> { "
                + "?group at:op-code ?code . "
                + "VALUES ?code { "+LDHelper.concatStringList(serviceList, " ", "'")+"}"
                + "}"
                + "GRAPH <urn:yti:organizations> {"
                + "?org skos:prefLabel ?orgLabel ."
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
        pss.setLiteral("draft", "Unstable");
        pss.setLiteral("mlabel", ResourceFactory.createLangLiteral(label, lang));
        pss.setLiteral("defLang", lang);

        logger.info(pss.toString());

        QueryExecution qexec = QueryExecutionFactory.sparqlService(services.getCoreSparqlAddress(), pss.toString());
        this.graph = qexec.execConstruct();

    }



}
