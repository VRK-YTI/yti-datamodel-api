/*
 * Licensed under the European Union Public Licence (EUPL) V.1.1
 */
package fi.vm.yti.datamodel.api.model;

import fi.vm.yti.datamodel.api.service.EndpointServices;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Iterator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.json.JsonValue;
import javax.json.JsonValue.ValueType;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.web.DatasetAdapter;
import org.apache.jena.web.DatasetGraphAccessorHTTP;

public class OPHCodeServer {

    static final private Logger logger = LoggerFactory.getLogger(OPHCodeServer.class.getName());

    private final EndpointServices endpointServices;
    private DatasetAdapter adapter;
    private String uri;
    private Property description = ResourceFactory.createProperty("http://purl.org/dc/terms/", "description");
    private Property name = ResourceFactory.createProperty("http://purl.org/dc/terms/", "title");
    private Property isPartOf = ResourceFactory.createProperty("http://purl.org/dc/terms/", "isPartOf");
    private Property id = ResourceFactory.createProperty("http://purl.org/dc/terms/", "identifier");
    private Property creator = ResourceFactory.createProperty("http://purl.org/dc/terms/", "creator");
    static private Property statusProperty = ResourceFactory.createProperty("http://uri.suomi.fi/datamodel/ns/iow#", "status");

    private final HashMap<String, String> statusMap = new HashMap<>() {{
        put("LUONNOS", "DRAFT");
        put("HYVAKSYTTY", "VALID");
        put("PASSIIVINEN", "DEPRECATED");
    }};

    public OPHCodeServer(String uri,
                         EndpointServices endpointServices) {
        DatasetGraphAccessorHTTP accessor = new DatasetGraphAccessorHTTP(endpointServices.getSchemesReadWriteAddress());
        this.adapter = new DatasetAdapter(accessor);
        this.endpointServices = endpointServices;
        this.uri = uri;
    }

    public boolean containsCodeList(String uri) {
        return adapter.containsModel(uri);
    }

    public boolean updateCodelistsFromServer() {

        try {

            Model model = ModelFactory.createDefaultModel();
            model.setNsPrefix("dcterms", "http://purl.org/dc/terms/");
            model.setNsPrefix("iow", "http://uri.suomi.fi/datamodel/ns/iow#");

            Client client = ClientBuilder.newClient();
            logger.info("Updating OPH codeLists: " + uri);
            WebTarget target = client.target(uri).queryParam("format", "application/json");
            Response response = target.request("application/json").get();

            if (response.getStatusInfo().getFamily() == Response.Status.Family.SUCCESSFUL) {

                JsonReader jsonReader = Json.createReader(response.readEntity(InputStream.class));
                JsonArray codeListArray = jsonReader.readArray();
                jsonReader.close();

                Iterator<JsonValue> groupIterator = codeListArray.iterator();

                while (groupIterator.hasNext()) {

                    JsonObject codeList = (JsonObject) groupIterator.next();

                    String groupID = codeList.getJsonString("koodistoRyhmaUri").getString();

                    JsonArray locGroupName = codeList.getJsonArray("metadata");

                    locGroupName = clean(locGroupName);

                    Iterator<JsonValue> groupNameIterator = locGroupName.iterator();

                    Resource group = model.createResource(groupID);

                    while (groupNameIterator.hasNext()) {
                        JsonObject groupName = (JsonObject) groupNameIterator.next();
                        String lang = groupName.getString("kieli").toLowerCase();
                        String label = groupName.getString("nimi");
                        JsonValue kuvausValue = groupName.get("kuvaus");
                        if (kuvausValue != null && kuvausValue.getValueType() == ValueType.STRING) {
                            String comment = groupName.getString("kuvaus");
                            group.addLiteral(description, ResourceFactory.createLangLiteral(comment, lang));
                        }
                        group.addProperty(RDF.type, ResourceFactory.createResource("http://uri.suomi.fi/datamodel/ns/iow#FCodeGroup"));
                        group.addLiteral(name, ResourceFactory.createLangLiteral(label, lang));

                    }

                    JsonArray codeSchemeArr = codeList.getJsonArray("koodistos");

                    Iterator<JsonValue> codeListIterator = codeSchemeArr.iterator();

                    while (codeListIterator.hasNext()) {

                        JsonObject codes = (JsonObject) codeListIterator.next();

                        // codes.getString("resourceUri")
                        String koodistoUri = codes.getString("koodistoUri");
                        String schemeUri = uri + koodistoUri + "/koodi";
                        Resource valueScheme = model.createResource(schemeUri);
                        valueScheme.addProperty(RDF.type, ResourceFactory.createResource("http://uri.suomi.fi/datamodel/ns/iow#FCodeScheme"));

                        //group.addProperty(hasPart, valueScheme);
                        valueScheme.addProperty(isPartOf, group);

                        valueScheme.addLiteral(id, ResourceFactory.createPlainLiteral(koodistoUri));
                        JsonValue owner = codes.get("omistaja");

                        if (owner.getValueType() == ValueType.STRING)
                            valueScheme.addLiteral(creator, ResourceFactory.createPlainLiteral(codes.getString("omistaja")));

                        JsonObject latestKoodisto = codes.getJsonObject("latestKoodistoVersio");

                        JsonValue status = latestKoodisto.get("tila");

                        if (status.getValueType() == ValueType.STRING) {
                            String statusText = status.toString().replaceAll("\"", "");
                            if (statusMap.containsKey(statusText)) {
                                valueScheme.addLiteral(statusProperty, ResourceFactory.createPlainLiteral(statusMap.get(statusText)));
                            } else {
                                logger.warn("Could not find status from the codelist: " + koodistoUri);
                                valueScheme.addLiteral(statusProperty, ResourceFactory.createPlainLiteral("INCOMPLETE"));
                            }
                        } else {
                            logger.warn("Bad data from codelist api");
                        }

                        JsonArray locArr = latestKoodisto.getJsonArray("metadata");

                        locArr = clean(locArr);

                        Iterator<JsonValue> codeNameIterator = locArr.iterator();

                        while (codeNameIterator.hasNext()) {
                            JsonObject codeName = (JsonObject) codeNameIterator.next();

                            String lang = codeName.getString("kieli").toLowerCase();
                            String label = codeName.getString("nimi");

                            JsonValue kuvausValue = codeName.get("kuvaus");
                            if (kuvausValue != null && kuvausValue.getValueType() == ValueType.STRING) {
                                String comment = codeName.getString("kuvaus");
                                valueScheme.addLiteral(description, ResourceFactory.createLangLiteral(comment, lang));
                            }

                            valueScheme.addLiteral(name, ResourceFactory.createLangLiteral(label, lang));

                        }

                        // Copying all codelists takes too much time. Better to do this on the fly?
                     /*   if(!adapter.containsModel(schemeUri)) {
                            updateCodes(schemeUri);
                        }*/

                    }

                }

                DatasetGraphAccessorHTTP accessor = new DatasetGraphAccessorHTTP(endpointServices.getSchemesReadWriteAddress());
                DatasetAdapter adapter = new DatasetAdapter(accessor);

                adapter.putModel(uri, model);

                return true;

            } else {
                return false;
            }

        } catch (Exception ex) {
            logger.warn(ex.getMessage());
            logger.info("Not connected to the code server");
            return false;
        }
    }

    /*
    
    {
    "koodiUri": "talonrakennusalanatjarjestys_12",
    "resourceUri": "https:\/\/virkailija.opintopolku.fi\/koodisto-service\/rest\/codeelement\/talonrakennusalanatjarjestys_12",
    "version": 9,
    "versio": 2,
    "koodisto": {
      "koodistoUri": "talonrakennusalanatjarjestys",
      "organisaatioOid": "1.2.246.562.10.00000000001",
      "koodistoVersios": [
        1,
        2
      ]
    },
    "koodiArvo": "12",
    "paivitysPvm": 1453362929927,
    "voimassaAlkuPvm": "2014-09-01",
    "voimassaLoppuPvm": null,
    "tila": "HYVAKSYTTY",
    "metadata": [
      {
        "nimi": "Puuelementtien asennus",
        "kuvaus": "valinnainen",
        "lyhytNimi": "at",
        "kayttoohje": "",
        "kasite": "",
        "sisaltaaMerkityksen": "",
        "eiSisallaMerkitysta": "",
        "huomioitavaKoodi": "",
        "sisaltaaKoodiston": "",
        "kieli": "FI"
      }
    ]
  },
    
    */
    public boolean updateCodes(String uri) {

        Model model = ModelFactory.createDefaultModel();
        model.setNsPrefix("dcterms", "http://purl.org/dc/terms/");
        model.setNsPrefix("iow", "http://uri.suomi.fi/datamodel/ns/iow#");

        Response.ResponseBuilder rb;

        Client client = ClientBuilder.newClient();
        WebTarget target = client.target(uri).queryParam("format", "application/json");
        Response response = target.request("application/json").get();

        if (response.getStatusInfo().getFamily() == Response.Status.Family.SUCCESSFUL) {

            logger.info("Copying " + uri);

            JsonReader jsonReader = Json.createReader(response.readEntity(InputStream.class));
            JsonArray codeListArray = jsonReader.readArray();
            jsonReader.close();

            Iterator<JsonValue> codeIterator = codeListArray.iterator();

            while (codeIterator.hasNext()) {

                JsonObject codeObj = (JsonObject) codeIterator.next();
                Resource codeRes = model.createResource(codeObj.getString("resourceUri"));
                codeRes.addProperty(RDF.type, ResourceFactory.createResource("http://uri.suomi.fi/datamodel/ns/iow#FCode"));

                codeRes.addLiteral(id, ResourceFactory.createPlainLiteral(codeObj.getString("koodiArvo")));

                String codeStatus = codeObj.getString("tila");

                if (statusMap.containsKey(codeStatus)) {
                    codeRes.addLiteral(statusProperty, ResourceFactory.createPlainLiteral(statusMap.get(codeStatus)));
                }

                JsonArray locArr = codeObj.getJsonArray("metadata");

                locArr = clean(locArr);

                Iterator<JsonValue> codeNameIterator = locArr.iterator();

                while (codeNameIterator.hasNext()) {
                    JsonObject codeName = (JsonObject) codeNameIterator.next();

                    String lang = codeName.getString("kieli").toLowerCase();
                    String label = codeName.getString("nimi");

                    codeRes.addLiteral(name, ResourceFactory.createLangLiteral(label, lang));

                    JsonValue kuvausValue = codeName.get("kuvaus");
                    if (kuvausValue != null && kuvausValue.getValueType() == ValueType.STRING) {
                        String comment = codeName.getString("kuvaus");
                        codeRes.addLiteral(description, ResourceFactory.createLangLiteral(comment, lang));
                    }

                }

            }

            //   RDFDataMgr.write(System.out, model, Lang.TURTLE) ;

            DatasetGraphAccessorHTTP accessor = new DatasetGraphAccessorHTTP(endpointServices.getSchemesReadWriteAddress());
            DatasetAdapter adapter = new DatasetAdapter(accessor);

            adapter.putModel(uri, model);

            return true;
        } else {
            logger.info("" + response.getStatus());
            return false;
        }

    }

    private JsonArray clean(JsonArray argh) {

        HashMap<String, JsonObject> jsonMap = new HashMap<>();
        Iterator arrIte = argh.iterator();

        while (arrIte.hasNext()) {
            JsonObject obj = (JsonObject) arrIte.next();
            String name = obj.getString("nimi");
            String lang = obj.getString("kieli").toLowerCase();
            if (!jsonMap.containsKey(name) || (jsonMap.containsKey(name) && lang.equals("fi")))
                jsonMap.put(name, obj);
        }

        JsonArrayBuilder arrBuild = Json.createArrayBuilder();
        Iterator<String> it = jsonMap.keySet().iterator();
        while (it.hasNext()) {
            String key = it.next();
            arrBuild.add(jsonMap.get(key));
        }

        return arrBuild.build();
    }
}
