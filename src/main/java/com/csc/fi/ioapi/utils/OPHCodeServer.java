/*
 * Licensed under the European Union Public Licence (EUPL) V.1.1 
 */
package com.csc.fi.ioapi.utils;

import com.csc.fi.ioapi.config.EndpointServices;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Iterator;
import java.util.logging.Logger;
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

/**
 *
 * @author malonen
 */
public class OPHCodeServer {
    
    EndpointServices services = new EndpointServices();
    static final private Logger logger = Logger.getLogger(OPHCodeServer.class.getName());
    public boolean status;
    
    public OPHCodeServer() {
        this.status = false;
    }
    
    public OPHCodeServer(String uri, boolean force) {
        DatasetGraphAccessorHTTP accessor = new DatasetGraphAccessorHTTP(services.getSchemesReadWriteAddress());
        DatasetAdapter adapter = new DatasetAdapter(accessor);
       
        if(force) 
            this.status = copyCodelistsFromServer(uri);
        else 
            this.status = adapter.containsModel(uri);
   
    }
    
    
    private boolean copyCodelistsFromServer(String uri) {
        
        try {
    
            Model model = ModelFactory.createDefaultModel();
            model.setNsPrefix("dcterms", "http://purl.org/dc/terms/");
            model.setNsPrefix("iow", "http://iow.csc.fi/ns/iow#");
            
            Response.ResponseBuilder rb;
           
            
            Client client = ClientBuilder.newClient();
            WebTarget target = client.target(uri).queryParam("format","application/json");
            Response response = target.request("application/json").get();

            
            if (response.getStatusInfo().getFamily() == Response.Status.Family.SUCCESSFUL) {
                
             
                JsonReader jsonReader = Json.createReader(response.readEntity(InputStream.class));
                JsonArray codeListArray = jsonReader.readArray();
                jsonReader.close();
                
                Iterator<JsonValue> groupIterator = codeListArray.iterator();
                
                while(groupIterator.hasNext()) {

                JsonObject codeList = (JsonObject) groupIterator.next();
               
                String groupID = codeList.getJsonString("koodistoRyhmaUri").getString();
                
                JsonArray locGroupName = codeList.getJsonArray("metadata");
                    
                locGroupName = clean(locGroupName);
                
                Iterator<JsonValue> groupNameIterator = locGroupName.iterator();
                
                // UUID groupUUID = UUID.randomUUID();
                
                Resource group = model.createResource(groupID);
               
                
                    while(groupNameIterator.hasNext()) {
                        JsonObject groupName = (JsonObject) groupNameIterator.next();
                        String lang = groupName.getString("kieli").toLowerCase();
                        String label = groupName.getString("nimi");
                        JsonValue kuvausValue = groupName.get("kuvaus");
                        if(kuvausValue!=null && kuvausValue.getValueType()==ValueType.STRING) {
                            String comment = groupName.getString("kuvaus");
                            Property description = ResourceFactory.createProperty("http://purl.org/dc/terms/", "description"); 
                            group.addLiteral(description, ResourceFactory.createLangLiteral(comment,lang));
                        }
                        group.addProperty(RDF.type, ResourceFactory.createResource("http://iow.csc.fi/ns/iow#FCodeGroup"));
                        Property name = ResourceFactory.createProperty("http://purl.org/dc/terms/", "title"); 
                        group.addLiteral(name, ResourceFactory.createLangLiteral(label,lang));
                        
                    }

                JsonArray codeSchemeArr = codeList.getJsonArray("koodistos");
                
                Iterator<JsonValue> codeListIterator = codeSchemeArr.iterator();
                
                while(codeListIterator.hasNext()) {
                    
                    JsonObject codes = (JsonObject) codeListIterator.next();
                    // codes.getString("resourceUri")
                    Resource valueScheme = model.createResource(uri+codes.getString("koodistoUri")+"/koodi");
                    valueScheme.addProperty(RDF.type, ResourceFactory.createResource("http://iow.csc.fi/ns/iow#FCodeScheme"));
                    
                    Property isPartOf = ResourceFactory.createProperty("http://purl.org/dc/terms/", "isPartOf");    
                    //group.addProperty(hasPart, valueScheme);
                    valueScheme.addProperty(isPartOf, group);
                    
                    Property id = ResourceFactory.createProperty("http://purl.org/dc/terms/", "identifier");
                    Property creator = ResourceFactory.createProperty("http://purl.org/dc/terms/", "creator");
                
                    valueScheme.addLiteral(id, ResourceFactory.createPlainLiteral(codes.getString("koodistoUri")));
                    JsonValue owner = codes.get("omistaja");
                    
                    if(owner.getValueType()==ValueType.STRING)
                        valueScheme.addLiteral(creator, ResourceFactory.createPlainLiteral(codes.getString("omistaja")));
                    
                    JsonArray locArr = codes.getJsonObject("latestKoodistoVersio").getJsonArray("metadata");
                    
                    locArr = clean(locArr);
                    
                    Iterator<JsonValue> codeNameIterator = locArr.iterator();
                    
                    while(codeNameIterator.hasNext()) {
                        JsonObject codeName = (JsonObject) codeNameIterator.next();
                        
                        String lang = codeName.getString("kieli").toLowerCase();
                        String label = codeName.getString("nimi");
                        
                        JsonValue kuvausValue = codeName.get("kuvaus");
                        if(kuvausValue!=null && kuvausValue.getValueType()==ValueType.STRING) {
                               String comment = codeName.getString("kuvaus");
                               Property description = ResourceFactory.createProperty("http://purl.org/dc/terms/", "description"); 
                               valueScheme.addLiteral(description, ResourceFactory.createLangLiteral(comment,lang));
                        }
                     
                        Property name = ResourceFactory.createProperty("http://purl.org/dc/terms/", "title");
                      
                        valueScheme.addLiteral(name, ResourceFactory.createLangLiteral(label,lang));
        
                       
                        }
              
                  
                }
                
                }
                
              
                   DatasetGraphAccessorHTTP accessor = new DatasetGraphAccessorHTTP(services.getSchemesReadWriteAddress());
                   DatasetAdapter adapter = new DatasetAdapter(accessor);
                
                   adapter.putModel(uri, model);
                   
                return true;
                
            } else {
                return false;
            }
        
            } catch(Exception ex) {
                logger.warning(ex.getStackTrace().toString());
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
        model.setNsPrefix("iow", "http://iow.csc.fi/ns/iow#");
            
            Response.ResponseBuilder rb;

            Client client = ClientBuilder.newClient();
            WebTarget target = client.target(uri).queryParam("format","application/json");
            Response response = target.request("application/json").get();

            if (response.getStatusInfo().getFamily() == Response.Status.Family.SUCCESSFUL) {
                
                logger.info("STATUS OK");
                
                JsonReader jsonReader = Json.createReader(response.readEntity(InputStream.class));
                JsonArray codeListArray = jsonReader.readArray();
                jsonReader.close();
                
                Iterator<JsonValue> codeIterator = codeListArray.iterator();
                
                while(codeIterator.hasNext()) {
                    
                    JsonObject codeObj = (JsonObject) codeIterator.next();
                    Resource codeRes = model.createResource(codeObj.getString("resourceUri"));
                    codeRes.addProperty(RDF.type, ResourceFactory.createResource("http://iow.csc.fi/ns/iow#FCode"));
                    Property id = ResourceFactory.createProperty("http://purl.org/dc/terms/", "identifier"); 

                    codeRes.addLiteral(id, ResourceFactory.createPlainLiteral(codeObj.getString("koodiArvo")));
                        
                    JsonArray locArr = codeObj.getJsonArray("metadata");
                    
                    locArr = clean(locArr);
                    
                    Iterator<JsonValue> codeNameIterator = locArr.iterator();
                    
                    while(codeNameIterator.hasNext()) {
                        JsonObject codeName = (JsonObject) codeNameIterator.next();
                        
                        String lang = codeName.getString("kieli").toLowerCase();
                        String label = codeName.getString("nimi");
                        
                        Property name = ResourceFactory.createProperty("http://purl.org/dc/terms/", "title"); 
                        codeRes.addLiteral(name, ResourceFactory.createLangLiteral(label, lang));
                        
                        JsonValue kuvausValue = codeName.get("kuvaus");
                        if(kuvausValue!=null && kuvausValue.getValueType()==ValueType.STRING) {
                               String comment = codeName.getString("kuvaus");
                               Property description = ResourceFactory.createProperty("http://purl.org/dc/terms/", "description"); 
                               codeRes.addLiteral(description, ResourceFactory.createLangLiteral(comment,lang));
                        }
                        
                    }
                    
                }
                
                //   RDFDataMgr.write(System.out, model, Lang.TURTLE) ;
                   
                   DatasetGraphAccessorHTTP accessor = new DatasetGraphAccessorHTTP(services.getSchemesReadWriteAddress());
                   DatasetAdapter adapter = new DatasetAdapter(accessor);
                
                   adapter.putModel(uri, model);
                
                return true;
            } else {
                logger.info(""+response.getStatus());
                return false;
            }
            
        
    }
    
    
   private JsonArray clean(JsonArray argh) {
        
        HashMap<String,JsonObject> jsonMap = new HashMap<String,JsonObject>(); 
        Iterator arrIte = argh.iterator();
        
        while(arrIte.hasNext()) {
            JsonObject obj = (JsonObject) arrIte.next();
            String name = obj.getString("nimi");
            String lang = obj.getString("kieli").toLowerCase();
            if(!jsonMap.containsKey(name) || (jsonMap.containsKey(name) && lang.equals("fi")))
                jsonMap.put(name, obj);
        }
        
        JsonArrayBuilder arrBuild = Json.createArrayBuilder();
        Iterator<String> it = jsonMap.keySet().iterator();
        while(it.hasNext()) {
            String key = it.next();
            arrBuild.add((JsonValue)jsonMap.get(key));
        }
        
        return arrBuild.build();
    } 
    
    
}
