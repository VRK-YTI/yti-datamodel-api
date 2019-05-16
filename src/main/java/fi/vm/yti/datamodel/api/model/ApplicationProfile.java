package fi.vm.yti.datamodel.api.model;

import fi.vm.yti.datamodel.api.service.*;
import fi.vm.yti.datamodel.api.utils.LDHelper;

import org.apache.jena.iri.IRI;
import org.apache.jena.query.ParameterizedSparqlString;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.RDFList;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.vocabulary.DCTerms;

import java.util.List;
import java.util.UUID;

public class ApplicationProfile extends AbstractModel {

    public ApplicationProfile(IRI profileId,
                              GraphManager graphManager) {
        super(profileId, graphManager);
    }

    public ApplicationProfile(Model model,
                              GraphManager graphManager,
                              RHPOrganizationManager rhpOrganizationManager) {
        super(model, graphManager, rhpOrganizationManager);
    }

    public ApplicationProfile(String prefix,
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

        if (namespace.toString().endsWith("/")) {
            pss.setNsPrefix(prefix, namespace.toString());
        } else {
            pss.setNsPrefix(prefix, namespace.toString() + "#");
        }

        //TODO: Return list of recommended skos schemes?
        String queryString = "CONSTRUCT  { "
            + "?modelIRI a owl:Ontology . "
            + "?modelIRI a dcap:DCAP . "
            + "?modelIRI rdfs:label ?mlabel . "
            + "?modelIRI owl:versionInfo ?draft . "
            + "?modelIRI dcterms:created ?creation . "
            + "?modelIRI dcterms:modified ?creation . "
            + "?modelIRI dcap:preferredXMLNamespaceName ?namespace . "
            + "?modelIRI dcap:preferredXMLNamespacePrefix ?prefix . "
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
            + "FILTER(LANGMATCHES(LANG(?groupLabel), '" + lang + "'))"
            + "VALUES ?code { " + LDHelper.concatStringList(serviceList, " ", "'") + "}"
            + "}"
            + "GRAPH <urn:yti:organizations> {"
            + "?org a ?orgType . "
            + "?org skos:prefLabel ?orgLabel . "
            + "VALUES ?org { " + LDHelper.concatWithReplace(orgList, " ", "<urn:uuid:@this>") + " }"
            + "}"
            + "}";

        pss.setCommandText(queryString);

        if (namespace.toString().endsWith("/")) {
            pss.setLiteral("namespace", namespace.toString());
        } else {
            pss.setLiteral("namespace", namespace.toString() + "#");
        }

        pss.setLiteral("prefix", prefix);
        pss.setIri("modelIRI", namespace);
        pss.setLiteral("draft", "DRAFT");
        pss.setLiteral("mlabel", ResourceFactory.createLangLiteral(label, lang));
        pss.setLiteral("defLang", lang);

        try (QueryExecution qexec = QueryExecutionFactory.sparqlService(endpointServices.getCoreSparqlAddress(), pss.toString())) {
            this.graph = qexec.execConstruct();
        }

        RDFList langRDFList = LDHelper.addStringListToModel(this.graph, allowedLang);
        this.graph.add(ResourceFactory.createResource(namespace.toString()), DCTerms.language, langRDFList);

    }

    public String getType() {
        return "dcap:DCAP";
    }

}
