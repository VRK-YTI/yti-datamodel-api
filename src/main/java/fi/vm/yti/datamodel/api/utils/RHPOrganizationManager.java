package fi.vm.yti.datamodel.api.utils;

import fi.vm.yti.datamodel.api.config.ApplicationProperties;
import fi.vm.yti.datamodel.api.config.EndpointServices;
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

import javax.json.*;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;
import java.io.InputStream;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;
import java.util.logging.Logger;

import static fi.vm.yti.datamodel.api.utils.SSLContextFactory.naiveSSLContext;

/**
 *
 * @author malonen
 */
public class RHPOrganizationManager {

    private static EndpointServices services = new EndpointServices();
    static final private Logger logger = Logger.getLogger(RHPOrganizationManager.class.getName());

    public static Response getOrganizations() {

        Client client = ClientBuilder.newBuilder()
                .sslContext(naiveSSLContext())
                .build();

        String service = ApplicationProperties.getDefaultGroupManagementAPI() + "organizations";
        WebTarget target = client.target(service);
        return target.request("application/json").get();
    }

    public static Model getOrganizationModelFromRHP() {

        Model model = ModelFactory.createDefaultModel();
        model.setNsPrefix("dcterms", "http://purl.org/dc/terms/");
        model.setNsPrefix("iow", "http://iow.csc.fi/ns/iow#");
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

    public static void initOrganizationsFromRHP() {
        Model graph = getOrganizationModelFromRHP();
        if(graph!=null) {
            GraphManager.putToGraph(graph, "urn:yti:organizations");
            logger.info("Organizations initialized");
        }
    }

    public static Model getOrganizationModel() {
        return GraphManager.getCoreGraph("urn:yti:organizations");
    }

    public static boolean isExistingOrganization(List<UUID> orgList) {

        ParameterizedSparqlString pss = new ParameterizedSparqlString();
        String sparqlOrgList = LDHelper.concatWithReplace(orgList," ","<urn:uuid:@this> a foaf:Organization . ");
        String queryString = " ASK { GRAPH <urn:yti:organizations> { "+sparqlOrgList+" } }";
        pss.setNsPrefixes(LDHelper.PREFIX_MAP);
        pss.setCommandText(queryString);
        Query query = pss.asQuery();

        //logger.info(pss.toString());
        QueryExecution qexec = QueryExecutionFactory.sparqlService(services.getCoreSparqlAddress(), query);

        try {
            boolean b = qexec.execAsk();
            logger.info("EXISTS "+sparqlOrgList+":"+b);
            return b;
        } catch (Exception ex) {
            return false;
        }
    }

}