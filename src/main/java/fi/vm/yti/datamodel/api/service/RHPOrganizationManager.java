package fi.vm.yti.datamodel.api.service;

import java.io.InputStream;
import java.util.*;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;

import jakarta.json.*;
import org.apache.jena.query.ParameterizedSparqlString;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.rdf.model.*;
import org.apache.jena.sparql.vocabulary.FOAF;
import org.apache.jena.vocabulary.DCTerms;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.SKOS;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import fi.vm.yti.datamodel.api.config.ApplicationProperties;
import fi.vm.yti.datamodel.api.utils.LDHelper;

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
        String service = properties.getDefaultGroupManagementAPI() + "organizations?onlyValid=true";
        logger.debug("Getting organizations from: "+service);
        Client client = clientFactory.create();
        WebTarget target = client.target(service);
        Invocation.Builder builder = target.request("application/json");
        Response resp = builder.get();
        return clientFactory.create().target(service).request("application/json").get();
    }

    public UUID getParentOrganizationId(String childOrganizationId) {
        String service = properties.getPrivateGroupManagementAPI() + "parentorganization?childOrganizationId=" + childOrganizationId;
        Response response = clientFactory.create().target(service).request("application/json").get();

        if (response.getStatus() == Response.Status.OK.getStatusCode()) {
            JsonReader reader = Json.createReader(response.readEntity(InputStream.class));
            JsonObject jsonObject = reader.readObject();
            String uuid = jsonObject.getString("uuid");

            return UUID.fromString(uuid);
        }
        return null;
    }

    public List<UUID> getOrganizationIdsWithParent(String[] orgs) {
        Set<UUID> uniqOrgList = new HashSet<>();

        for (int i = 0; i < orgs.length; i++) {
            uniqOrgList.add(UUID.fromString(orgs[i]));

            logger.info("Finding parent organization for {}", orgs[i]);
            UUID parentId = getParentOrganizationId(orgs[i]);

            if (parentId != null) {
                logger.info("Add parent organization {} as a contributor", parentId.toString());
                uniqOrgList.add(parentId);
            }
        }
        return new ArrayList<>(uniqOrgList);
    }

    public List<String> getChildOrganizations(String parentId) {
        String service = properties.getPrivateGroupManagementAPI() + "childorganizations?parentId=" + parentId;
        Response response = clientFactory.create().target(service).request("application/json").get();

        List<String> result = new ArrayList<>();
        if (response.getStatus() == Response.Status.OK.getStatusCode()) {
            JsonReader reader = Json.createReader(response.readEntity(InputStream.class));
            JsonArray jsonValues = reader.readArray();

            for (int i = 0; i < jsonValues.size(); i++) {
                JsonObject jsonObject = jsonValues.getJsonObject(i);
                result.add(jsonObject.getString("uuid"));
            }

        } else {
            logger.warn("Error fetching child organizations {}", response.getStatus());
        }
        return result;
    }

    public Model getOrganizationModelFromRHP() {

        Model model = ModelFactory.createDefaultModel();
        model.setNsPrefix("dcterms", "http://purl.org/dc/terms/");
        model.setNsPrefix("iow", "http://uri.suomi.fi/datamodel/ns/iow#");
        model.setNsPrefix("skos", "http://www.w3.org/2004/02/skos/core#");
        model.setNsPrefix("foaf", "http://xmlns.com/foaf/0.1/");

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

                String preflabel_fi = prefLabel.containsKey("fi") && prefLabel.get("fi").getValueType() != JsonValue.ValueType.NULL ? prefLabel.getString("fi") : null;
                String preflabel_en = prefLabel.containsKey("en") && prefLabel.get("en").getValueType() != JsonValue.ValueType.NULL ? prefLabel.getString("en") : null;
                String preflabel_sv = prefLabel.containsKey("sv") && prefLabel.get("sv").getValueType() != JsonValue.ValueType.NULL ? prefLabel.getString("sv") : null;
                String description_fi = description.containsKey("fi") && description.get("fi").getValueType() != JsonValue.ValueType.NULL ? description.getString("fi") : null;
                String description_en = description.containsKey("en") && description.get("en").getValueType() != JsonValue.ValueType.NULL ? description.getString("en") : null;
                String description_sv = description.containsKey("sv") && description.get("sv").getValueType() != JsonValue.ValueType.NULL ? description.getString("sv") : null;
                String url = org.containsKey("url") && org.get("url").getValueType() != JsonValue.ValueType.NULL ? org.getString("url") : null;

                String parentId = org.containsKey("parentId") && org.get("parentId").getValueType() != JsonValue.ValueType.NULL ? org.getString("parentId") : null;

                Resource res = model.createResource("urn:uuid:" + uuid);
                res.addProperty(RDF.type, FOAF.Organization);

                if (preflabel_fi != null && preflabel_fi.length() > 1)
                    res.addLiteral(SKOS.prefLabel, ResourceFactory.createLangLiteral(preflabel_fi, "fi"));

                if (preflabel_en != null && preflabel_en.length() > 1)
                    res.addLiteral(SKOS.prefLabel, ResourceFactory.createLangLiteral(preflabel_en, "en"));

                if (preflabel_sv != null && preflabel_sv.length() > 1)
                    res.addLiteral(SKOS.prefLabel, ResourceFactory.createLangLiteral(preflabel_sv, "sv"));

                if (description_fi != null && description_fi.length() > 1)
                    res.addLiteral(DCTerms.description, ResourceFactory.createLangLiteral(description_fi, "fi"));

                if (description_en != null && description_en.length() > 1)
                    res.addLiteral(DCTerms.description, ResourceFactory.createLangLiteral(description_en, "en"));

                if (description_sv != null && description_sv.length() > 1)
                    res.addLiteral(DCTerms.description, ResourceFactory.createLangLiteral(description_sv, "sv"));

                if (url != null && url.length() > 1)
                    res.addLiteral(FOAF.homepage, url);

                Property parentOrganizationProperty = ResourceFactory.createProperty(
                        LDHelper.PREFIX_MAP.get("iow") + "parentOrganization");
                if (parentId != null) {
                    res.addProperty(parentOrganizationProperty,
                            ResourceFactory.createResource(String.format("urn:uuid:%s", parentId)));
                } else {
                    // for json framing set empty value for main organizations
                    res.addLiteral(parentOrganizationProperty, "");
                }
                /*
                Expected format:
                     [
                      {
                        "uuid": "d9c76d52-03d3-4480-8c2c-b66e6d9c57f2",
                        "prefLabel": {
                          "en": "Digital and Population Data Services Agency",
                          "fi": "Digi- ja väestötietovirasto",
                          "sv": ""
                        },
                        "description": {
                          "en": "",
                          "fi": "",
                          "sv": ""
                        },
                        "url": "http://dvv.fi/"
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

        } else {
            logger.debug("Error getting organizations from RHP: " + response.getStatus());
            return null;
        }

        return model;
    }

    public void initOrganizationsFromRHP() {
        Model graph = getOrganizationModelFromRHP();
        if (graph != null) {
            graphManager.putToGraph(graph, "urn:yti:organizations");
        } else {
            logger.debug("No organizations initialized in group management!");
        }
    }

    public void initTestOrganizations() {
        Model model = ModelFactory.createDefaultModel();
        model.setNsPrefix("dcterms", "http://purl.org/dc/terms/");
        model.setNsPrefix("iow", "http://uri.suomi.fi/datamodel/ns/iow#");
        model.setNsPrefix("skos", "http://www.w3.org/2004/02/skos/core#");
        model.setNsPrefix("foaf", "http://xmlns.com/foaf/0.1/");
        Resource res = model.createResource("urn:uuid:7d3a3c00-5a6b-489b-a3ed-63bb58c26a63");
        res.addProperty(RDF.type, FOAF.Organization);
        res.addLiteral(SKOS.prefLabel, ResourceFactory.createLangLiteral("Test organization", "en"));
        res.addLiteral(DCTerms.description, ResourceFactory.createLangLiteral("This organization is for testing only", "en"));
        res.addLiteral(FOAF.homepage, "http://example.org");
        graphManager.addToGraph(model, "urn:yti:organizations");
    }

    public Model getOrganizationModel() {
        return graphManager.getCoreGraph("urn:yti:organizations");
    }

    public boolean isExistingOrganization(List<UUID> orgList) {

        ParameterizedSparqlString pss = new ParameterizedSparqlString();
        String sparqlOrgList = LDHelper.concatUUIDWithReplace(orgList, " ", "<urn:uuid:@this> a foaf:Organization . ");
        String queryString = " ASK { GRAPH <urn:yti:organizations> { " + sparqlOrgList + " } }";
        pss.setNsPrefixes(LDHelper.PREFIX_MAP);
        pss.setCommandText(queryString);
        Query query = pss.asQuery();

        try (QueryExecution qexec = QueryExecution.service(endpointServices.getCoreSparqlAddress(), query)) {
            boolean b = qexec.execAsk();
            logger.info("EXISTS " + sparqlOrgList + ":" + b);
            return b;
        } catch (Exception ex) {
            return false;
        }
    }

}
