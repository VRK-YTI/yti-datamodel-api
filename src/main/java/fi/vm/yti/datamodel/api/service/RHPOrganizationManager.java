package fi.vm.yti.datamodel.api.service;

import fi.vm.yti.datamodel.api.config.ApplicationProperties;
import fi.vm.yti.datamodel.api.utils.LDHelper;
import org.apache.jena.query.ParameterizedSparqlString;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.sparql.vocabulary.FOAF;
import org.apache.jena.vocabulary.DCTerms;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.SKOS;
import org.springframework.stereotype.Service;

import javax.json.*;
import javax.ws.rs.core.Response;
import java.io.InputStream;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class RHPOrganizationManager {

    static final private Logger logger = LoggerFactory.getLogger(RHPOrganizationManager.class.getName());

    private final ClientFactory clientFactory;
    private final EndpointServices endpointServices;
    private final ApplicationProperties properties;
    private final GraphManager graphManager;

    RHPOrganizationManager(ClientFactory clientFactory,
                           EndpointServices endpointServices,
                           ApplicationProperties properties,
                           GraphManager graphManager) {
        this.clientFactory = clientFactory;
        this.endpointServices = endpointServices;
        this.properties = properties;
        this.graphManager = graphManager;
    }

    public Response getOrganizations() {

        String service = properties.getDefaultGroupManagementAPI() + "organizations";
        return clientFactory.create().target(service).request("application/json").get();
    }

    public Model getOrganizationModelFromRHP() {

        Model model = ModelFactory.createDefaultModel();
        model.setNsPrefix("dcterms", "http://purl.org/dc/terms/");
        model.setNsPrefix("iow", "http://uri.suomi.fi/datamodel/ns/iow#");
        model.setNsPrefix("skos","http://www.w3.org/2004/02/skos/core#");
        model.setNsPrefix("foaf","http://xmlns.com/foaf/0.1/");

        Response response = getOrganizations();

        if (response.getStatusInfo().getFamily() == Response.Status.Family.SUCCESSFUL) {
            JsonReader jsonReader = Json.createReader(response.readEntity(InputStream.class));
            JsonArray orgArray = jsonReader.readArray();
            jsonReader.close();

            Iterator<JsonValue> orgIterator = orgArray.iterator();

            while (orgIterator.hasNext()) {

                JsonObject org = (JsonObject) orgIterator.next();
                String uuid = org.getString("uuid");
                JsonObject prefLabel = org.getJsonObject("prefLabel");
                JsonObject description = org.getJsonObject("description");
                String preflabel_fi = prefLabel.getString("fi");
                String preflabel_en = prefLabel.getString("en");
                String preflabel_sv = prefLabel.getString("sv");
                String description_fi = description.getString("fi");
                String description_en = description.getString("en");
                String description_sv = description.getString("sv");
                String url = org.getString("url");

                Resource res = model.createResource("urn:uuid:"+uuid);

                res.addProperty(RDF.type,FOAF.Organization);
               // res.addProperty(DCTerms.identifier, uuid);

                if(preflabel_fi!=null && preflabel_fi.length()>1)
                    res.addLiteral(SKOS.prefLabel, ResourceFactory.createLangLiteral(preflabel_fi,"fi"));

                if(preflabel_en!=null && preflabel_en.length()>1)
                    res.addLiteral(SKOS.prefLabel, ResourceFactory.createLangLiteral(preflabel_en,"en"));

                if(preflabel_sv!=null && preflabel_sv.length()>1)
                    res.addLiteral(SKOS.prefLabel, ResourceFactory.createLangLiteral(preflabel_sv,"sv"));

                if(description_fi!=null && description_fi.length()>1)
                    res.addLiteral(DCTerms.description, ResourceFactory.createLangLiteral(description_fi,"fi"));

                if(description_en!=null && description_en.length()>1)
                    res.addLiteral(DCTerms.description, ResourceFactory.createLangLiteral(description_en,"en"));

                if(description_sv!=null && description_sv.length()>1)
                    res.addLiteral(DCTerms.description, ResourceFactory.createLangLiteral(description_sv,"sv"));

                if(url!=null && url.length()>1)
                    res.addLiteral(FOAF.homepage,url);

                /*
                Expected format:
                     [
                      {
                        "uuid": "d9c76d52-03d3-4480-8c2c-b66e6d9c57f2",
                        "prefLabel": {
                          "en": "Population Register Centre",
                          "fi": "Väestörekisterikeskus",
                          "sv": ""
                        },
                        "description": {
                          "en": "",
                          "fi": "",
                          "sv": ""
                        },
                        "url": "http://vrk.fi/etusivu"
                      },
                      {
                        "uuid": "2f9eab0b-d6f0-42ce-b3aa-9f3647618b4d",
                        "prefLabel": {
                          "en": "National Institute for Health and Welfare",
                          "fi": "Terveyden ja hyvinvoinnin laitos",
                          "sv": ""
                        },
                        "description": {
                          "en": "",
                          "fi": "",
                          "sv": ""
                        },
                        "url": "https://www.thl.fi/fi/"
                      },

                 */

            }

        } else return null;

        return model;
    }

    public void initOrganizationsFromRHP() {
        Model graph = getOrganizationModelFromRHP();
        if(graph!=null) {
            graphManager.putToGraph(graph, "urn:yti:organizations");
        }
    }

    public Model getOrganizationModel() {
        return graphManager.getCoreGraph("urn:yti:organizations");
    }

    public boolean isExistingOrganization(List<UUID> orgList) {

        ParameterizedSparqlString pss = new ParameterizedSparqlString();
        String sparqlOrgList = LDHelper.concatWithReplace(orgList," ","<urn:uuid:@this> a foaf:Organization . ");
        String queryString = " ASK { GRAPH <urn:yti:organizations> { "+sparqlOrgList+" } }";
        pss.setNsPrefixes(LDHelper.PREFIX_MAP);
        pss.setCommandText(queryString);
        Query query = pss.asQuery();

        try(QueryExecution qexec = QueryExecutionFactory.sparqlService(endpointServices.getCoreSparqlAddress(), query)) {
            boolean b = qexec.execAsk();
            logger.info("EXISTS "+sparqlOrgList+":"+b);
            return b;
        } catch (Exception ex) {
            return false;
        }
    }

}