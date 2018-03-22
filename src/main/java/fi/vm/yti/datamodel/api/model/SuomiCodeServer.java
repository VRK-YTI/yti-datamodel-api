/*
 * Licensed under the European Union Public Licence (EUPL) V.1.1
 */
package fi.vm.yti.datamodel.api.model;

import fi.vm.yti.datamodel.api.service.EndpointServices;
import fi.vm.yti.datamodel.api.utils.LDHelper;
import org.apache.jena.iri.IRI;
import org.apache.jena.iri.IRIFactory;
import org.apache.jena.rdf.model.*;
import org.apache.jena.riot.system.IRIResolver;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.web.DatasetAdapter;
import org.apache.jena.web.DatasetGraphAccessorHTTP;

import javax.json.*;
import javax.json.JsonValue.ValueType;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Iterator;
import java.util.logging.Logger;

public class SuomiCodeServer {

    static final private Logger logger = Logger.getLogger(SuomiCodeServer.class.getName());

    static private Property name = ResourceFactory.createProperty("http://purl.org/dc/terms/", "title");
    static private Property description = ResourceFactory.createProperty("http://purl.org/dc/terms/", "description");
    static private Property isPartOf = ResourceFactory.createProperty("http://purl.org/dc/terms/", "isPartOf");
    static private Property id = ResourceFactory.createProperty("http://purl.org/dc/terms/", "identifier");
    static private Property creator = ResourceFactory.createProperty("http://purl.org/dc/terms/", "creator");

    private final EndpointServices endpointServices;
    private String uri;
    private String url;
    private DatasetGraphAccessorHTTP accessor;
    private DatasetAdapter adapter;


    public SuomiCodeServer(EndpointServices endpointServices) {
        this.accessor = new DatasetGraphAccessorHTTP(endpointServices.getSchemesReadWriteAddress());
        this.adapter = new DatasetAdapter(accessor);
        this.endpointServices = endpointServices;
    }

    public SuomiCodeServer(String uri, String url, EndpointServices endpointServices) {
        this.accessor = new DatasetGraphAccessorHTTP(endpointServices.getSchemesReadWriteAddress());
        this.adapter = new DatasetAdapter(accessor);
        this.endpointServices = endpointServices;
        this.uri = uri;
        this.url = url;
    }

    public boolean containsCodeList(String uri) {
        return adapter.containsModel(uri);
    }

    // https://koodistot-dev.suomi.fi/codelist-api/api/v1/coderegistries/
    public void updateCodelistsFromServer() {

        Model model = ModelFactory.createDefaultModel();
        model.setNsPrefix("dcterms", "http://purl.org/dc/terms/");
        model.setNsPrefix("iow", "http://uri.suomi.fi/datamodel/ns/iow#");

        Response.ResponseBuilder rb;
        Client client = ClientBuilder.newClient();

        logger.info("Updating suomi.fi codeLists: " + url);

        WebTarget target = client.target(url + "coderegistries/").queryParam("format", "application/json");
        Response response = target.request("application/json").get();

        if (response.getStatusInfo().getFamily() == Response.Status.Family.SUCCESSFUL) {

            JsonReader jsonReader = Json.createReader(response.readEntity(InputStream.class));
            JsonObject registryListObject = jsonReader.readObject();
            jsonReader.close();

            JsonArray registryListArray = registryListObject.getJsonArray("results");
            Iterator<JsonValue> registryIterator = registryListArray.iterator();

            while (registryIterator.hasNext()) {

                JsonObject codeRegistry = (JsonObject) registryIterator.next();
                String groupID = codeRegistry.getJsonString("uri").getString();

                // FIXME: This should not happen!
                if(LDHelper.isInvalidIRI(groupID)) {
                    logger.warning("Invalid IRI: "+groupID);
                    return;
                }

                String groupUrl = codeRegistry.getJsonString("url").getString();

                Resource group = model.createResource(groupID);

                addLangLiteral(group, codeRegistry.getJsonObject("prefLabel"), name);
                addLangLiteral(group, codeRegistry.getJsonObject("definition"), description);

                group.addProperty(RDF.type, ResourceFactory.createResource("http://uri.suomi.fi/datamodel/ns/iow#FCodeGroup"));

                WebTarget schemeTarget = client.target(groupUrl + "codeschemes/").queryParam("format", "application/json");
                Response schemeResponse = schemeTarget.request("application/json").get();

                if (schemeResponse.getStatusInfo().getFamily() == Response.Status.Family.SUCCESSFUL) {

                    jsonReader = Json.createReader(schemeResponse.readEntity(InputStream.class));
                    JsonObject codeSchemeResponse = jsonReader.readObject();
                    jsonReader.close();

                    JsonArray codeSchemeArr = codeSchemeResponse.getJsonArray("results");
                    Iterator<JsonValue> codeListIterator = codeSchemeArr.iterator();

                    while (codeListIterator.hasNext()) {

                        JsonObject codeList = (JsonObject) codeListIterator.next();

                        String codeListUri = codeList.getString("uri");
                        String codeListUrl = codeList.getString("url");

                        // FIXME: This should not happen!
                        if(LDHelper.isInvalidIRI(codeListUri)) {
                            logger.warning("Invalid IRI: "+codeListUri);
                            return;
                        }

                        Resource valueScheme = model.createResource(codeListUri);
                        valueScheme.addProperty(RDF.type, ResourceFactory.createResource("http://uri.suomi.fi/datamodel/ns/iow#FCodeScheme"));

                        valueScheme.addProperty(isPartOf, group);

                        valueScheme.addLiteral(id, ResourceFactory.createPlainLiteral(codeListUri));

                        addLangLiteral(valueScheme, codeList.getJsonObject("description"), description);
                        addLangLiteral(valueScheme, codeList.getJsonObject("prefLabel"), name);

                        if(!adapter.containsModel(codeListUri)) {
                            updateCodes(codeListUrl+"codes/", codeListUri);
                        }

                    }
                }

               adapter.putModel(uri, model);

               // model.write(System.out, "text/turtle");

            }

        }

    }

    public static void addLangLiteral(Resource res, JsonObject obj, Property prop) {

        Iterator<String> langObjIterator = obj.keySet().iterator();

            while (langObjIterator.hasNext()) {
                String lang = langObjIterator.next();
                String value = obj.getString(lang);
                res.addLiteral(prop, ResourceFactory.createLangLiteral(value, lang));
            }

    }


    public void updateCodes(String url, String uri) {

        Model model = ModelFactory.createDefaultModel();
        model.setNsPrefix("dcterms", "http://purl.org/dc/terms/");
        model.setNsPrefix("iow", "http://uri.suomi.fi/datamodel/ns/iow#");

        Response.ResponseBuilder rb;

        Client client = ClientBuilder.newClient();
        WebTarget target = client.target(url).queryParam("format","application/json");
        Response response = target.request("application/json").get();

        if (response.getStatusInfo().getFamily() == Response.Status.Family.SUCCESSFUL) {

            JsonReader jsonReader = Json.createReader(response.readEntity(InputStream.class));
            JsonObject codeListResponse = jsonReader.readObject();
            jsonReader.close();

            JsonArray codeSchemeArr = codeListResponse.getJsonArray("results");
            Iterator<JsonValue> codeIterator = codeSchemeArr.iterator();

            while(codeIterator.hasNext()) {

                JsonObject codeObj = (JsonObject) codeIterator.next();
                String codeURI = codeObj.getString("uri");

                // FIXME: This should not happen!
                if(LDHelper.isInvalidIRI(codeURI)) {
                    logger.warning("Invalid IRI: "+codeURI);
                    return;
                }

                Resource codeRes = model.createResource(codeURI);

                codeRes.addProperty(RDF.type, ResourceFactory.createResource("http://uri.suomi.fi/datamodel/ns/iow#FCode"));
                codeRes.addLiteral(id, ResourceFactory.createPlainLiteral(codeObj.getString("codeValue")));

                addLangLiteral(codeRes, codeObj.getJsonObject("prefLabel"), name);
                addLangLiteral(codeRes, codeObj.getJsonObject("description"), description);

                }

           // model.write(System.out, "text/turtle") ;

            adapter.putModel(uri, model);
            logger.info("Saved: "+uri);

        } else {
            logger.info(""+response.getStatus());
        }


    }

}
