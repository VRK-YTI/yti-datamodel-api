/*
 * Licensed under the European Union Public Licence (EUPL) V.1.1
 */
package fi.vm.yti.datamodel.api.model;

import java.io.InputStream;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.Iterator;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;

import jakarta.json.*;
import org.apache.jena.datatypes.xsd.XSDDatatype;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.rdfconnection.RDFConnection;
import org.apache.jena.vocabulary.RDF;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fi.vm.yti.datamodel.api.service.CodeSchemeManager;
import fi.vm.yti.datamodel.api.service.EndpointServices;
import fi.vm.yti.datamodel.api.utils.LDHelper;

public class SuomiCodeServer {

    private static final Logger logger = LoggerFactory.getLogger(SuomiCodeServer.class.getName());

    private static Property name = ResourceFactory.createProperty("http://purl.org/dc/terms/", "title");
    private static Property description = ResourceFactory.createProperty("http://purl.org/dc/terms/", "description");
    private static Property modified = ResourceFactory.createProperty("http://purl.org/dc/terms/", "modified");
    private static Property isPartOf = ResourceFactory.createProperty("http://purl.org/dc/terms/", "isPartOf");
    private static Property id = ResourceFactory.createProperty("http://purl.org/dc/terms/", "identifier");
    private static Property creator = ResourceFactory.createProperty("http://purl.org/dc/terms/", "creator");
    private static Property status = ResourceFactory.createProperty("http://uri.suomi.fi/datamodel/ns/iow#", "status");

    private final EndpointServices endpointServices;
    private String uri;
    private String url;
    private RDFConnection connection;
    private CodeSchemeManager codeSchemeManager;
    private final DateTimeFormatter dfmt = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");

    public SuomiCodeServer(EndpointServices endpointServices,
                           CodeSchemeManager codeSchemeManager) {
        this.connection = RDFConnection.connect(endpointServices.getSchemesReadWriteAddress());
        this.endpointServices = endpointServices;
        this.codeSchemeManager = codeSchemeManager;
    }

    public SuomiCodeServer(String uri,
                           String url,
                           EndpointServices endpointServices,
                           CodeSchemeManager codeSchemeManager) {
        this.connection = RDFConnection.connect(endpointServices.getSchemesReadWriteAddress());

        this.endpointServices = endpointServices;
        this.uri = uri;
        this.url = url;
        this.codeSchemeManager = codeSchemeManager;
    }

    public static void addLangLiteral(Resource res,
                                      JsonObject obj,
                                      Property prop) {
        Iterator<String> langObjIterator = obj.keySet().iterator();

        while (langObjIterator.hasNext()) {
            String lang = langObjIterator.next();
            String value = obj.getString(lang);
            res.addLiteral(prop, ResourceFactory.createLangLiteral(value, lang));
        }
    }

    public boolean containsCodeList(String uri) {
        return codeSchemeManager.codeSchemeExists(uri);
    }

    public void updateCodeSchemeList() {

        Model model = ModelFactory.createDefaultModel();
        model.setNsPrefix("dcterms", "http://purl.org/dc/terms/");
        model.setNsPrefix("iow", "http://uri.suomi.fi/datamodel/ns/iow#");

        Client client = ClientBuilder.newClient();

        logger.debug("Updating suomi.fi codeLists: " + url);

        WebTarget target = client.target(url + "v1/coderegistries/").queryParam("format", "application/json");
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
                if (LDHelper.isInvalidIRI(groupID)) {
                    logger.warn("Invalid IRI: " + groupID);
                    return;
                }

                String groupUrl = codeRegistry.getJsonString("url").getString();

                Resource group = model.createResource(groupID);

                JsonObject registryName = codeRegistry.getJsonObject("prefLabel");
                JsonObject registryDescription = codeRegistry.getJsonObject("description");

                if (registryName != null) {
                    addLangLiteral(group, registryName, name);
                }

                if (registryDescription != null) {
                    addLangLiteral(group, registryDescription, description);
                }

                group.addProperty(RDF.type, ResourceFactory.createResource("http://uri.suomi.fi/datamodel/ns/iow#FCodeGroup"));

                WebTarget schemeTarget = client.target(groupUrl + "/codeschemes/").queryParam("format", "application/json");
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

                        // FIXME: This should not happen!
                        if (LDHelper.isInvalidIRI(codeListUri)) {
                            logger.warn("Invalid IRI: " + codeListUri);
                            return;
                        }

                        Resource valueScheme = model.createResource(codeListUri);
                        valueScheme.addProperty(RDF.type, ResourceFactory.createResource("http://uri.suomi.fi/datamodel/ns/iow#FCodeScheme"));

                        valueScheme.addProperty(isPartOf, group);

                        valueScheme.addLiteral(id, ResourceFactory.createPlainLiteral(codeListUri));

                        JsonObject codeListDescription = codeList.getJsonObject("description");
                        JsonObject codeListName = codeList.getJsonObject("prefLabel");

                        if (codeListDescription != null) {
                            addLangLiteral(valueScheme, codeListDescription, description);
                        }

                        if (codeListName != null) {
                            addLangLiteral(valueScheme, codeListName, name);
                        }

                        valueScheme.addLiteral(status, codeList.getString("status"));

                    }
                } else {
                    logger.info("Failed to update codelists from " + schemeTarget.getUri().toString());
                }

            }

            // model.write(System.out, "text/turtle");
            connection.put(uri, model);

        } else {
            logger.warn("Connection to " + target.toString() + " failed: " + response.getStatus());
        }

    }

    public Model createModelFromCodeList(String containerUri,
                                         String codeSchemeModified) {
        Model model = ModelFactory.createDefaultModel();
        model.setNsPrefix("dcterms", "http://purl.org/dc/terms/");
        model.setNsPrefix("iow", "http://uri.suomi.fi/datamodel/ns/iow#");

        Client client = ClientBuilder.newClient();
        WebTarget target = client.target(url + "v1/integration/resources").queryParam("includeIncomplete", "true").queryParam("container", containerUri).queryParam("format", "application/json");
        Response response = target.request("application/json").get();

        if (response.getStatusInfo().getFamily() == Response.Status.Family.SUCCESSFUL) {

            Resource valueScheme = model.createResource(containerUri);
            valueScheme.addProperty(RDF.type, ResourceFactory.createResource("http://uri.suomi.fi/datamodel/ns/iow#FCodeScheme"));
            valueScheme.addLiteral(modified, ResourceFactory.createTypedLiteral(codeSchemeModified, XSDDatatype.XSDdateTime));

            JsonReader jsonReader = Json.createReader(response.readEntity(InputStream.class));
            JsonObject codeListResponse = jsonReader.readObject();
            jsonReader.close();

            JsonArray codeSchemeArr = codeListResponse.getJsonArray("results");
            Iterator<JsonValue> codeIterator = codeSchemeArr.iterator();

            while (codeIterator.hasNext()) {

                JsonObject codeObj = (JsonObject) codeIterator.next();
                String codeURI = codeObj.getString("uri");

                if (LDHelper.isInvalidIRI(codeURI)) {
                    logger.warn("Invalid IRI: " + codeURI);
                    return null;
                }

                Resource codeRes = model.createResource(codeURI);

                codeRes.addProperty(RDF.type, ResourceFactory.createResource("http://uri.suomi.fi/datamodel/ns/iow#FCode"));
                codeRes.addLiteral(id, ResourceFactory.createPlainLiteral(codeObj.getString("localName")));

                JsonObject codeName = codeObj.getJsonObject("prefLabel");
                JsonObject descriptionName = codeObj.getJsonObject("definition");

                if (codeName != null) {
                    addLangLiteral(codeRes, codeName, name);
                }

                if (descriptionName != null) {
                    addLangLiteral(codeRes, descriptionName, description);
                }

                codeRes.addLiteral(status, codeObj.getString("status"));

            }

            if (model.size() < 1) {
                logger.warn("Codes graph from " + uri + " is empty! No valid codes?");
                return null;
            } else {
                return model;
            }
        } else {
            return null;
        }
    }

    public void updateCodes(String containerUri, boolean force) {

        LocalDateTime codeSchemeModified = null;
        Model model = null;

        Client containerClient = ClientBuilder.newClient();
        WebTarget containerTarget = containerClient.target(url + "v1/integration/containers").queryParam("includeIncomplete", "true").queryParam("uri", containerUri).queryParam("format", "application/json");
        Response containerResponse = containerTarget.request("application/json").get();

        if (containerResponse.getStatusInfo().getFamily() == Response.Status.Family.SUCCESSFUL) {

            JsonReader jsonReader = Json.createReader(containerResponse.readEntity(InputStream.class));
            JsonObject codeSchemeResponse = jsonReader.readObject();
            jsonReader.close();

            try {
                JsonObject codeList = codeSchemeResponse.getJsonArray("results").getJsonObject(0);
                String schemeModifiedString = codeList.containsKey("contentModified")
                        ? codeList.getString("contentModified")
                        : codeList.getString("modified");

                if (schemeModifiedString != null) {
                    logger.info("Container last-modified: " + schemeModifiedString);
                    codeSchemeModified = LocalDateTime.parse(schemeModifiedString, dfmt);
                }

                if (!containsCodeList(containerUri)) {
                    logger.info("No scheme found. Creating scheme: " + containerUri);
                    model = createModelFromCodeList(containerUri, schemeModifiedString);
                } else {
                    Date lastModifiedDateTime = codeSchemeManager.lastModified(containerUri);
                    if (lastModifiedDateTime == null || force) {
                        logger.info("No modified found or forced update. Updating scheme: " + containerUri);
                        model = createModelFromCodeList(containerUri, schemeModifiedString);
                    } else {
                        LocalDateTime lastModifiedLocalDateTime = lastModifiedDateTime.toInstant()
                            .atZone(ZoneId.of("GMT"))
                            .toLocalDateTime();
                        if (codeSchemeModified != null && codeSchemeModified.isAfter(lastModifiedLocalDateTime)) {
                            logger.info("Updating scheme to new version: " + containerUri);
                            model = createModelFromCodeList(containerUri, schemeModifiedString);
                        } else {
                            logger.debug("Scheme already up to date: " + containerUri);
                            return;
                        }
                    }
                }
            } catch (Exception ex) {
                logger.error("Could not find codescheme: " + containerUri, ex);
                return;
            }
        } else {
            logger.warn("Could not connect to code service: " + url);
        }

        if (model == null) {
            logger.warn("Codes graph from " + containerUri + " is empty! No valid codes?");
        } else {
            connection.put(containerUri, model);
        }
    }
}
