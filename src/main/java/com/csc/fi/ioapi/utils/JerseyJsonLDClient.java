/*
 * Licensed under the European Union Public Licence (EUPL) V.1.1 
 */
package com.csc.fi.ioapi.utils;
import com.csc.fi.ioapi.api.concepts.ConceptSearch;
import com.csc.fi.ioapi.api.genericapi.ExportModel;
import com.csc.fi.ioapi.config.ApplicationProperties;
import com.csc.fi.ioapi.config.EndpointServices;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.jsonldjava.core.JsonLdError;
import com.github.jsonldjava.core.JsonLdOptions;
import com.github.jsonldjava.core.JsonLdProcessor;
import com.github.jsonldjava.utils.JsonUtils;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.RDFReader;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.net.ssl.SSLContext;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.Response.StatusType;
import org.apache.jena.atlas.json.JSON;
import org.apache.jena.atlas.json.JsonObject;
import org.apache.jena.atlas.json.JsonValue;
import org.apache.jena.atlas.web.ContentType;
import org.apache.jena.query.DatasetAccessor;
import org.apache.jena.query.DatasetAccessorFactory;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.riot.RDFLanguages;
import org.apache.jena.riot.RiotException;
import org.glassfish.jersey.client.authentication.HttpAuthenticationFeature;
import org.glassfish.jersey.uri.UriComponent;
import org.glassfish.jersey.client.ClientProperties;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
/**
 * 
 * @author malonen
 */
public class JerseyJsonLDClient {
    
    static final private Logger logger = Logger.getLogger(JerseyJsonLDClient.class.getName());
    static final private EndpointServices services = new EndpointServices();

    /**
     * Reads boolean from any url or returns false
     * @param url Url as string
     * @return boolean
     */
    public static Boolean readBooleanFromURL(String url) {
        try {
            
            Client client = ClientBuilder.newClient();
            WebTarget target = client.target(url);
            Response response = target.request("application/json").get();
            
            if (response.getStatusInfo().getFamily() != Response.Status.Family.SUCCESSFUL) {
                logger.info("Failed to read boolean from: "+url+" "+response.getStatus());
                return Boolean.FALSE;
            }

            DataInputStream dis = new DataInputStream(response.readEntity(InputStream.class));
            return new Boolean(dis.readBoolean());
        
        } catch(Exception ex) {
            logger.info("Failed in reading boolean from URL ... returning false");
            return Boolean.FALSE;
        }
    }

    /**
     * Returns Export graph as Jersey Response
     * @param graph ID of the graph
     * @param raw If true returns content as text
     * @param lang Language of the required graph
     * @param ctype Required content type
     * @return Response
     */
    public static Response getExportGraph(String graph, boolean raw, String lang, String ctype) {
        
        try {
            
            ContentType contentType = ContentType.create(ctype);
            
            Lang rdfLang = RDFLanguages.contentTypeToLang(contentType);
            
            if(rdfLang==null) {
                logger.info("Unknown RDF type: "+ctype);
                return JerseyResponseManager.notFound();
            }

            DatasetAccessor accessor = DatasetAccessorFactory.createHTTP(services.getCoreReadAddress());
            Model model = accessor.getModel(graph);

            OutputStream out = new ByteArrayOutputStream();
            
            Response response = JerseyJsonLDClient.getGraphResponseFromService(graph+"#ExportGraph", services.getCoreReadAddress(),ctype);
          
            if (response.getStatusInfo().getFamily() != Response.Status.Family.SUCCESSFUL) {
                return JerseyResponseManager.unexpected();
            }

            /* TODO: Remove builders */
            ResponseBuilder rb;
            RDFDataMgr.write(out, model, rdfLang);

            if (rdfLang.equals(Lang.JSONLD)) {

                Map<String, Object> jsonModel = null;
                try {
                    jsonModel = (Map<String, Object>) JsonUtils.fromString(out.toString());
                } catch (IOException ex) {
                    Logger.getLogger(ExportModel.class.getName()).log(Level.SEVERE, null, ex);
                    return JerseyResponseManager.unexpected();
                }

                Map<String, Object> frame = new HashMap<String, Object>();
                //Map<String,Object> frame = (HashMap<String,Object>) LDHelper.getExportContext();

                Map<String, Object> context = (Map<String, Object>) jsonModel.get("@context");

                context.putAll(LDHelper.CONTEXT_MAP);

                frame.put("@context", context);
                frame.put("@type", "owl:Ontology");

                Object data;

                try {                 
                    data = JsonUtils.fromInputStream(response.readEntity(InputStream.class));
                    
                    rb = Response.status(response.getStatus());

                    try {
                        JsonLdOptions options = new JsonLdOptions();
                        Object framed = JsonLdProcessor.frame(data, frame, options);
                        
                        ObjectMapper mapper = new ObjectMapper();
 
                        rb.entity(mapper.writerWithDefaultPrettyPrinter().writeValueAsString(framed));
                        
                    } catch (NullPointerException ex) {
                        logger.log(Level.WARNING, null, "DEFAULT GRAPH IS NULL!");
                        return rb.entity(JsonUtils.toString(data)).build();
                    } catch (JsonLdError ex) {
                        logger.log(Level.SEVERE, null, ex);
                        return JerseyResponseManager.serverError();
                    }

                } catch (IOException ex) {
                    logger.log(Level.SEVERE, null, ex);
                    return JerseyResponseManager.serverError();
                }

            } else {
                 rb = Response.status(response.getStatus());
                 rb.entity(response.readEntity(InputStream.class));
            }

            if(!raw) {
                rb.type(contentType.getContentType());
            } else {
                rb.type("text/plain");
            }
            
            return rb.build();

        } catch (Exception ex) {
            logger.log(Level.WARNING, "Expect the unexpected!", ex);
            return JerseyResponseManager.serverError();
        }
        
    }

    @Deprecated
    public static Response getSearchResultFromFinto(String vocid, String term, String lang) {
        
          
            Client client = ClientBuilder.newClient();
            String service = services.getConceptSearchAPI();
            WebTarget target = client.target(service).queryParam("lang", lang).queryParam("query", term);
                       
            if(vocid!=null && !vocid.equals("undefined")) 
               target = target.queryParam("vocab", vocid);
            
            Response response = target.request("application/ld+json").get();
            
            if (response.getStatusInfo().getFamily() != Response.Status.Family.SUCCESSFUL) {
               Logger.getLogger(ConceptSearch.class.getName()).log(Level.INFO, response.getStatus()+" from CONCEPT SERVICE");
               return JerseyResponseManager.unexpected(response.getStatus());
            }
            
            ResponseBuilder rb = Response.status(response.getStatus()); 
            rb.entity(response.readEntity(InputStream.class));
       
           return rb.build();
        
    }
    
    
    
    /**
     * Returns JENA model from JSONLD Response
     * @param response  Response object
     * @return          Jena model parsed from Reponse entity or empty model
     */
    public static Model getJSONLDResponseAsJenaModel(Response response) {
         Model model = ModelFactory.createDefaultModel();
         
         try {
         RDFReader reader = model.getReader(Lang.JSONLD.getName());
         /* TODO: Base uri input? */
         reader.read(model, (InputStream)response.getEntity(), "http://example.org/");
         } catch(RiotException ex) {
             logger.info(ex.getMessage());
             return model;
         }
         
         return model;
        
    }
    
    /**
     * Returns Jena model from the resource graph
     * @param resourceURI ID of the resource
     * @return Model
     */
    public static Model getResourceAsJenaModel(String resourceURI) {
                
         Client client = ClientBuilder.newClient();
         WebTarget target = client.target(resourceURI);
         Response response = target.request("application/rdf+xml").get();
         Model model = ModelFactory.createDefaultModel();
         
         try {
            RDFReader reader = model.getReader(Lang.RDFXML.getName());
            reader.read(model, response.readEntity(InputStream.class), resourceURI);
         } catch(RiotException ex) {
             return model;
         }
         
         return model;
        
    }
    
    /**
     * Returns Context from the graph
     * @param id ID of the graph
     * @param service ID of the service
     * @return JSON-LD Context object
     */
    public static JsonValue getGraphContextFromService(String id, String service) {
         
         Client client = ClientBuilder.newClient();
         WebTarget target = client.target(service).queryParam("graph", id);
         Response response = target.request("application/ld+json").get();


        JsonObject json = JSON.parse(response.readEntity(InputStream.class));
        JsonValue jsonContext = json.get("@context");
        
        return jsonContext;
        
       
    }
    
    /**
     * Returns Jersey response from the Fuseki service
     * @param id Id of the graph
     * @param service Id of the service
     * @return Response
     */
    public static Response getGraphResponseFromService(String id, String service) {
         
         Client client = ClientBuilder.newClient();
         WebTarget target = client.target(service).queryParam("graph", id);
         Response response = target.request("application/ld+json").get();
         
        if (response.getStatusInfo().getFamily() != Response.Status.Family.SUCCESSFUL) {
           logger.log(Level.INFO, response.getStatus()+" from SERVICE "+service+" and GRAPH "+id);
           return JerseyResponseManager.notFound();
        } else {
            ResponseBuilder rb = Response.status(response.getStatus()); 
            rb.entity(response.readEntity(InputStream.class));
            return rb.build();
        }
    }
    
    /**
     * Returns Jersey response from Fuseki service
     * @param id Id of the graph
     * @param service Id of the service
     * @param ctype Requested content-type
     * @return Response
     */
    public static Response getGraphResponseFromService(String id, String service, String ctype) {
                   
         Client client = ClientBuilder.newClient();
         WebTarget target = client.target(service).queryParam("graph", id);
         return target.request(ctype).get();
         
    }

    /**
     * Returns Jersey response from Fuseki service
     * @param id Id of the graph
     * @param service Id of the service
     * @param contentType Requested content-type
     * @param raw boolean that states if Response is needed as raw text
     * @return Response
     */
    public static Response getGraphResponseFromService(String id, String service, ContentType contentType, boolean raw) {
        try {
            
         Client client = ClientBuilder.newClient();
         WebTarget target = client.target(service).queryParam("graph", id);
         Response response = target.request(contentType.getContentType()).get();

        if (response.getStatusInfo().getFamily() != Response.Status.Family.SUCCESSFUL) {
           logger.log(Level.INFO, response.getStatus()+" from SERVICE "+service+" and GRAPH "+id);
           return JerseyResponseManager.notFound();
        }
        
        ResponseBuilder rb = Response.status(response.getStatus()); 
        rb.entity(response.readEntity(InputStream.class));
        
        if(!raw) {
            try {
                rb.type(contentType.getContentType());
            } catch(IllegalArgumentException ex) {
                 rb.type("text/plain;charset=utf-8");
            }
        } else {
            rb.type("text/plain;charset=utf-8");
        }

       return rb.build();
        } catch(Exception ex) {
          logger.log(Level.WARNING, "Expect the unexpected!", ex);
          return JerseyResponseManager.unexpected();
        }
    }
    
    
     /**
     * Saves new concept suggestion to termed
     * @param body JSON-ld as body
      * @param scheme Scheme / namespace of the vocabulary
     * @return Response
     */
    public static Response saveConceptSuggestion(String body, String scheme) {

        if(scheme.startsWith("urn:uuid:"))
            scheme = scheme.substring(scheme.indexOf("urn:uuid:"));
        
        String url = ApplicationProperties.getDefaultTermAPI()+"graphs/"+scheme+"/nodes";
        
        try {
            Client client = IgnoreSSLClient(); // ClientBuilder.newClient();
            HttpAuthenticationFeature feature = TermedAuthentication.getTermedAuth();
            client.register(feature);

            WebTarget target = client.target(url).queryParam("stream", true).queryParam("batch",true);
            Response response = target.request().post(Entity.entity(body, "application/ld+json"));

            logger.info("TERMED CALL: "+target.getUri().toString());
            
            if (response.getStatusInfo().getFamily() != Response.Status.Family.SUCCESSFUL) {
               logger.log(Level.INFO, response.getStatus()+" from URL: "+url);
               return JerseyResponseManager.notFound();
            }
            
            /* TODO: FIXME: Remove sleep once termed responds faster
                TODO: Check if fixed?
            
            try {
                Thread.sleep(5000);
            } catch (InterruptedException ex) {
                Logger.getLogger(JerseyJsonLDClient.class.getName()).log(Level.SEVERE, null, ex);
            } */

            ResponseBuilder rb = Response.status(response.getStatus()); 
            return rb.build();
            
        } catch(Exception ex) {
          logger.log(Level.WARNING, "Expect the unexpected!", ex);
          return JerseyResponseManager.notAcceptable();
        }
    }
    
     /**
     * Returns concepts from Termed api
     * @param query query string
      *              * @param schemeURI ID of the scheme
     * @return Response
     */
    public static Response searchConceptFromTermedAPI(String query, String schemeURI) {
        
        String url = ApplicationProperties.getDefaultTermAPI()+"ext";

        //FIXME: This isnt really working ... Returns TERMS instead of Concepts. Tried also with node-trees api but there is no RDF support. Options, create JENA model from JSON API or use elastic search index in frontend?

        /*
        if(graphCode!=null && !graphCode.isEmpty() && !graphCode.equals("undefined")) {
            url = url+"/"+graphCode;
        }*/
        
        try {
            
            Client client = IgnoreSSLClient(); //ClientBuilder.newClient();
            HttpAuthenticationFeature feature = TermedAuthentication.getTermedAuth();
            client.register(feature);

            WebTarget target = client.target(url).queryParam("select.referrers","prefLabelXl").queryParam("recurse.referrers.prefLabelXl","2").queryParam("where.properties.prefLabel", query).queryParam("max", "-1");
            
           if(schemeURI!=null && !schemeURI.isEmpty() && !schemeURI.equals("undefined")) {
                target = target.queryParam("graphId",schemeURI);
            }
            
            Response response = target.request("application/ld+json").get();

            logger.info("TERMED CALL: "+target.getUri().toString());
            
            if (response.getStatusInfo().getFamily() != Response.Status.Family.SUCCESSFUL) {
               logger.log(Level.INFO, response.getStatus()+" from URL: "+url);
               return JerseyResponseManager.notFound();
            }

            ResponseBuilder rb = Response.status(response.getStatus()); 
            rb.entity(response.readEntity(InputStream.class));

           return rb.build();
        } catch(Exception ex) {
          logger.log(Level.WARNING, "Expect the unexpected!", ex);
          return JerseyResponseManager.notAcceptable();
        }
    }
    
    
    
    /**
     * Returns available schemes
     * @return Response
     */
    public static Response getSchemesFromTermedAPI() {
        String url = ApplicationProperties.getDefaultTermAPI()+"ext";
        try {
            Client client = IgnoreSSLClient(); // ClientBuilder.newClient();
            HttpAuthenticationFeature feature = TermedAuthentication.getTermedAuth();
            client.register(feature);

            WebTarget target = client.target(url).queryParam("typeId", "TerminologicalVocabulary").queryParam("max", "-1");
            Response response = target.request("application/ld+json").get();

            logger.info("TERMED CALL: "+target.getUri().toString());
            
            if (response.getStatusInfo().getFamily() != Response.Status.Family.SUCCESSFUL) {
               logger.log(Level.INFO, response.getStatus()+" from URL: "+url);
               return JerseyResponseManager.notFound();
            }

            ResponseBuilder rb = Response.status(response.getStatus()); 
            rb.entity(response.readEntity(InputStream.class));

           return rb.build();
        } catch(Exception ex) {
          logger.log(Level.WARNING, "Expect the unexpected!", ex);
          return JerseyResponseManager.notAcceptable();
        }
    }
    
    /**
     * Returns Jena model of the terminology
     * @param uri
     * @return Model
     */
    public static Model getSchemeAsModelFromTermedAPI(String uri) {
        String url = ApplicationProperties.getDefaultTermAPI()+"ext";
        try {
            Client client = IgnoreSSLClient(); //ClientBuilder.newClient();
            HttpAuthenticationFeature feature = TermedAuthentication.getTermedAuth();
            client.register(feature);

            WebTarget target = client.target(url).queryParam("typeId", "TerminologicalVocabulary").queryParam("uri",uri).queryParam("max", "-1");
            Response response = target.request("application/rdf+xml").get();

            logger.info("TERMED CALL: "+target.getUri().toString());
            
            if (response.getStatusInfo().getFamily() != Response.Status.Family.SUCCESSFUL) {
               logger.log(Level.INFO, response.getStatus()+" from URL: "+url);
               return null;
            }

            Model schemeModel = ModelFactory.createDefaultModel();
            schemeModel.read(response.readEntity(InputStream.class), uri);

           return schemeModel;
           
        } catch(Exception ex) {
          logger.log(Level.WARNING, "Expect the unexpected!", ex);
          return null;
        }
    }


    /**
     * Returns concept as Jena model
     * @param resourceURI
     * @return Model
     */
    public static Model getConceptAsJenaModel(String resourceURI) {
         
        String url = ApplicationProperties.getDefaultTermAPI()+"ext";

        try {

             Client client = IgnoreSSLClient(); //ClientBuilder.newClient();
             HttpAuthenticationFeature feature = TermedAuthentication.getTermedAuth();
             client.register(feature);

             WebTarget target = client.target(url).queryParam("typeId", "Concept").queryParam("uri",resourceURI).queryParam("max", "-1");

             Response response = target.request("text/turtle").get();


             if (response.getStatusInfo().getFamily() != Response.Status.Family.SUCCESSFUL) {
                logger.info("FAIL: "+target.getUri().toString());
                return null;
             }

            logger.info(target.getUri().toString());

             Model model = ModelFactory.createDefaultModel();

             try {
             RDFReader reader = model.getReader(Lang.TURTLE.getName());
             reader.read(model, response.readEntity(InputStream.class), resourceURI);
             } catch(RiotException ex) {
                 return model;
             }

             return model;

        } catch(Exception ex) {
            logger.log(Level.WARNING, "Expect the unexpected!", ex);
            return null;
        }
    }
    
    /**
     * Returns concept as Jersey Response
     * @param uri uri of the concept
     * @param schemeUUID of the vocabulary
     * @return Response
     */
    public static Response getConceptFromTermedAPI(String uri, String schemeUUID) {
        
        String url = ApplicationProperties.getDefaultTermAPI()+"ext";
        
        /*
         if(graphCode!=null && !graphCode.isEmpty() && !graphCode.equals("undefined")) {
            url = url+"/"+graphCode;
        }*/
        
        try {
            Client client = IgnoreSSLClient(); //ClientBuilder.newClient();
            HttpAuthenticationFeature feature = TermedAuthentication.getTermedAuth();
            client.register(feature);

            WebTarget target = client.target(url).queryParam("typeId", "Concept").queryParam("max", "-1");
            
            if(uri!=null && !uri.isEmpty() && !uri.equals("undefined")) {
                logger.info(uri);
                target = target.queryParam("uri", uri);
            }
            
            if(schemeUUID!=null && !schemeUUID.isEmpty() && !schemeUUID.equals("undefined")) {
                target = target.queryParam("where.references.inScheme",schemeUUID);
            }
            
            Response response = target.request("application/ld+json").property(ClientProperties.FOLLOW_REDIRECTS, Boolean.TRUE).get();

            logger.info("TERMED CALL: "+target.getUri().toString());
            
            if (response.getStatusInfo().getFamily() != Response.Status.Family.SUCCESSFUL) {
               logger.log(Level.INFO, response.getStatus()+" from URL: "+url);
               logger.info("Location: "+response.getLocation().toString());
               return JerseyResponseManager.notFound();
            }

          //  logger.info(response.readEntity(String.class));
            ResponseBuilder rb = Response.status(response.getStatus());
            rb.entity(response.readEntity(InputStream.class));

           return rb.build();
        } catch(Exception ex) {
          logger.log(Level.WARNING, "Expect the unexpected!", ex);
          return JerseyResponseManager.notAcceptable();
        }
    }

    /**
     * This hack is needed to support self signed sertificates used in development
     * @return Client
     * @throws Exception
     */
    public static Client IgnoreSSLClient() throws Exception {
        SSLContext sslcontext = SSLContext.getInstance("TLS");
        sslcontext.init(null, new TrustManager[]{new X509TrustManager() {
            public void checkClientTrusted(X509Certificate[] arg0, String arg1) throws CertificateException {}
            public void checkServerTrusted(X509Certificate[] arg0, String arg1) throws CertificateException {}
            public X509Certificate[] getAcceptedIssuers() { return new X509Certificate[0]; }

        }}, new java.security.SecureRandom());
        return ClientBuilder.newBuilder().sslContext(sslcontext).hostnameVerifier((s1, s2) -> true).build();
    }
    
    
    /**
     * Creates new graph to the service
     * @param graph Id of the graph
     * @param body Body as JSON-LD object
     * @param service service
     * @return HTTP StatusType
     */
    public static StatusType putGraphToTheService(String graph, String body, String service) {
        
         Client client = ClientBuilder.newClient();
         WebTarget target = client.target(service).queryParam("graph", graph);
         Response response = target.request().put(Entity.entity(body, "application/ld+json"));
         
         return response.getStatusInfo();
         
    }
    
    
     /**
     * Returns true if graph is updated
     * @param graph ID of te graph
     * @param body Body as JSON-LD object
     * @param service ID of the service
     * @return boolean
     */
    public static boolean graphIsUpdatedToTheService(String graph, String body, String service) {
         Client client = ClientBuilder.newClient();
         WebTarget target = client.target(service).queryParam("graph", graph);
         Response response = target.request().put(Entity.entity(body, "application/ld+json"));
         
        if (response.getStatusInfo().getFamily() != Response.Status.Family.SUCCESSFUL) {
               logger.log(Level.WARNING, "Unexpected: Model update failed: "+graph);
               return false;
        } else return true;
        
    }
    
    
    /**
     * Updates graph
     * @param graph ID of the graph
     * @param body Body as JSON-LD object
     * @param service ID of the service
     * @return HTTP StatusType
     */
    public static StatusType postGraphToTheService(String graph, String body, String service) {
         Client client = ClientBuilder.newClient();
         WebTarget target = client.target(service).queryParam("graph", graph);
         Response response = target.request().post(Entity.entity(body, "application/ld+json"));
         
         return response.getStatusInfo();
    }
    
    
    
    /**
     * Construct query to the service using Jerseys
     * @param query Construct query
     * @param service ID of the service
     * @return Response
     */
    public static Response constructGraphFromServiceDirect(String query, String service) {
                   
            Client client = ClientBuilder.newClient();
            WebTarget target = client.target(service)
                                          .queryParam("query", UriComponent.encode(query,UriComponent.Type.QUERY));
            
            Response response = target.request("application/ld+json").get();
            
             if (response.getStatusInfo().getFamily() != Response.Status.Family.SUCCESSFUL) {
               return JerseyResponseManager.unexpected(response.getStatus());
            } else {
                ResponseBuilder rb = Response.status(response.getStatus()); 
                rb.entity(response.readEntity(InputStream.class));
                return rb.build();
             }
          
    }

    /**
     * Construct from combined model of Concept and Model
     * @param conceptID ID of concept
     * @param modelID ID of model
     * @param query Construct SPARQL query
     * @return Responses
     */
    public static Response constructFromTermedAndCore(String conceptID, String modelID, Query query) {
        
            Model conceptModel = JerseyJsonLDClient.getConceptAsJenaModel(conceptID);
            
            if(conceptModel==null) return JerseyResponseManager.notFound();
            
            DatasetAccessor testAcc = DatasetAccessorFactory.createHTTP(services.getCoreReadAddress());
            conceptModel.add(testAcc.getModel(modelID));
            
            QueryExecution qexec = QueryExecutionFactory.create(query,conceptModel);
            Model resultModel = qexec.execConstruct();
            
            qexec.close();
            
            return JerseyResponseManager.okModel(resultModel);
        
    }
    
    
    public static Response constructGraphFromService(String query, String service) {
        
            QueryExecution qexec = QueryExecutionFactory.sparqlService(service, query);
            Model constructModel = qexec.execConstruct();
        
            if(constructModel.size()<=0) {
                ResponseBuilder rb = Response.ok().type("application/ld+json");
                rb.entity(ModelManager.writeModelToString(constructModel));
                return rb.build();
            }

            ResponseBuilder rb = Response.ok();
            rb.entity(ModelManager.writeModelToString(constructModel));
            return rb.build();
          
    }

    /**
     * Constructs existing graph or returns NOT FOUND
     * @param query Construct query
     * @param service ID of the service
     * @return Response
     */
    public static Response constructNotEmptyGraphFromService(String query, String service) {
        
            QueryExecution qexec = QueryExecutionFactory.sparqlService(service, query);
            Model constructModel = qexec.execConstruct();
        
            if(constructModel.size()<=0) {
                return JerseyResponseManager.notFound();
            }

            ResponseBuilder rb = Response.ok();
            rb.entity(ModelManager.writeModelToString(constructModel));
            return rb.build();
          
    }
    
    /**
     * Constructs graph from one service and adds it to another
     * @param query Construct query
     * @param fromService ID of the original service
     * @param toService ID of the new service
     * @param toGraph ID of the graph
     * @return HTTP StatusType
     */
    public static StatusType constructGraphFromServiceToService(String query, String fromService, String toService, String toGraph) {
        
        Client client = ClientBuilder.newClient();
        WebTarget target = client.target(fromService).queryParam("query", UriComponent.encode(query,UriComponent.Type.QUERY));
        Response response = target.request("application/ld+json").get();
        
        if (response.getStatusInfo().getFamily() != Response.Status.Family.SUCCESSFUL) {
           return response.getStatusInfo();
        } else {
            return putGraphToTheService(toGraph, response.readEntity(String.class), toService);
        }
        
    }
    
    /**
     * Deletes Graph from service
     * @param graph ID of the graph
     * @param service ID of the service
     * @return Response
     */
    public static Response deleteGraphFromService(String graph, String service) {
        
        try {

            Client client = ClientBuilder.newClient();
            WebTarget target = client.target(service)
                                           .queryParam("graph", graph);

            Response response = target.request("application/ld+json").delete();

             if (response.getStatusInfo().getFamily() != Response.Status.Family.SUCCESSFUL) {
                logger.log(Level.WARNING, "Database connection error: "+graph+" was not deleted from "+service+"! Status "+response.getStatus());
                return JerseyResponseManager.unexpected();
             }
             
             return JerseyResponseManager.okNoContent();

       } catch(Exception ex) {
            logger.log(Level.WARNING, "Expect the unexpected!", ex);
            return JerseyResponseManager.unexpected();
       }
        
        
    }
    
}
