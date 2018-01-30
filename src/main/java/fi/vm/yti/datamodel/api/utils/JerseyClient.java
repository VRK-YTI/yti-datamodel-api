/*
 * Licensed under the European Union Public Licence (EUPL) V.1.1 
 */
package fi.vm.yti.datamodel.api.utils;
import fi.vm.yti.datamodel.api.config.EndpointServices;
import fi.vm.yti.datamodel.api.endpoint.genericapi.ExportModel;
import fi.vm.yti.datamodel.api.config.ApplicationProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.jsonldjava.core.JsonLdError;
import com.github.jsonldjava.core.JsonLdOptions;
import com.github.jsonldjava.core.JsonLdProcessor;
import com.github.jsonldjava.utils.JsonUtils;
import jersey.repackaged.com.google.common.collect.ImmutableMap;
import org.apache.jena.query.*;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.RDFReader;

import java.io.*;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.net.ssl.SSLContext;
import javax.ws.rs.client.*;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.Response.StatusType;

import org.apache.jena.atlas.web.ContentType;
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
public class JerseyClient {
    
    static final private Logger logger = Logger.getLogger(JerseyClient.class.getName());
    static EndpointServices services = new EndpointServices();


    public static Response getResponseFromURL(String url, String accept) {
        Client client = ClientBuilder.newClient();
        WebTarget target = client.target(url);
        Invocation.Builder requestBuilder = target.request();
        if(accept!=null) requestBuilder.accept(accept);
        Response response = requestBuilder.get();
        return response;
    }

    /**
     * Returns Jersey response from Fuseki service
     * @param id Id of the graph
     * @param service Id of the service
     * @param ctype Requested content-type
     * @return Response
     */
    public static Response getResponseFromService(String id, String service, String ctype) {

        Client client = ClientBuilder.newClient();
        WebTarget target = client.target(service).queryParam("graph", id);
        return target.request(ctype).get();

    }

    /* FIXME: Generic function for all responses?
    public static Response getResponseFromURL(String url, String accept, Map<String, String> queryParams) {
        Client client = ClientBuilder.newClient();
        WebTarget target = client.target(url);
        if(queryParams!=null) {
            for(String key : queryParams.keySet()) {
                target = target.queryParam(key, queryParams.get(key));
            }
        }
        Invocation.Builder requestBuilder = target.request();
        if(accept!=null) requestBuilder = requestBuilder.accept(accept);
        Response response = requestBuilder.get();
        return response;
    }
    */

    /**
     * Reads boolean from any url or returns false
     * @param url Url as string
     * @return boolean
     */
    public static Boolean readBooleanFromURL(String url) {
        try {

            Response response = getResponseFromURL(url, "application/json");

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

            Model model = JenaClient.getModelFromCore(graph);

            OutputStream out = new ByteArrayOutputStream();
            
            Response response = getResponseFromService(graph+"#ExportGraph", services.getCoreReadAddress(), ctype);
          
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
        
          /*
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
        */
          return null;
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
           reader.read(model, (InputStream)response.getEntity(), "urn:yti:resource");
         } catch(RiotException ex) {
             logger.info(ex.getMessage());
             return model;
         }
         
         return model;
        
    }


    /**
     * Returns Jena model from the service
     * @param service ID of the resource
     * @return Model
     */
    public static Response getGraphsAsResponse(String service, String ctype) {

        Response response = getResponseFromURL(services.getEndpoint()+"/"+service+"/", ctype);

        logger.info(ctype+" from "+services.getEndpoint()+"/"+service+"/ response: "+ response.getStatus());

        PushbackInputStream input = new PushbackInputStream(response.readEntity(InputStream.class));

        try {
            int test;
            test = input.read();
            if(test == -1) {
                logger.info(service+" is empty?");
                return Response.noContent().build();
            } else {
                input.unread(test);
                return Response.ok(input).header("Content-type",ctype).build();
            }
        } catch(IOException ex) {
            logger.info(ex.getMessage());
            return Response.noContent().build();
        }


    }

    /**
     * Returns Jena model from the service
     * @param serviceURL ID of the resource
     * @return Model
     */
    public static Response getExternalGraphsAsResponse(String serviceURL) {

        Response response = getResponseFromURL(serviceURL, "application/ld+json");

        logger.info(serviceURL+" response: "+ response.getStatus());

        return response;

    }

    /**
     * Returns JENA model from external JSONLD Response
     * @param serviceURL  Response object
     * @return          Jena model parsed from Reponse entity or empty model
     */
    public static Dataset getExternalJSONLDDatasets(String serviceURL) {
        Response response = getExternalGraphsAsResponse(serviceURL);
        Dataset dataset = DatasetFactory.create();
        try {
            RDFDataMgr.read(dataset, response.readEntity(InputStream.class), Lang.JSONLD);
        } catch(Exception ex) {
            logger.info(ex.getMessage());
            return dataset;
        }
        return dataset;
    }


    /**
     * Returns Jena model from the resource graph
     * @param resourceURI ID of the resource
     * @return Model
     */
    public static Model getResourceAsJenaModel(String resourceURI) {

         Response response = getResponseFromURL(resourceURI, Lang.JSONLD.getHeaderString());

         Model model = ModelFactory.createDefaultModel();
         
         try {
            RDFReader reader = model.getReader(Lang.JSONLD.getHeaderString());
            reader.read(model, response.readEntity(InputStream.class), resourceURI);
         } catch(RiotException ex) {
             return model;
         }
         
         return model;
        
    }

    /**
     * Returns JSON-LD Jersey response from the Fuseki service
     * @param id Id of the graph
     * @param service Id of the service
     * @return Response
     */
    public static Response getGraphResponseFromService(String id, String service) {
        return getGraphResponseFromService(id, service, null);
    }

    /**
     * Returns Jersey response from the Fuseki service
     * @param id Id of the graph
     * @param service Id of the service
     * @return Response
     */
    public static Response getGraphResponseFromService(String id, String service, String ctype) {

        if(ctype==null) ctype = "application/ld+json";

        Client client = ClientBuilder.newClient();
        WebTarget target = client.target(service).queryParam("graph", id);
        Response response = target.request(ctype).get();

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
     * @param contentType Requested content-type
     * @param raw boolean that states if Response is needed as raw text
     * @return Response
     */
    public static Response getGraphResponseFromService(String id, String service, String contentType, boolean raw) {
        try {

            Client client = ClientBuilder.newClient();
            WebTarget target = client.target(service).queryParam("graph", id);
            Response response = target.request(contentType).get();

            if (response.getStatusInfo().getFamily() != Response.Status.Family.SUCCESSFUL) {
                logger.log(Level.INFO, response.getStatus()+" from SERVICE "+service+" and GRAPH "+id);
                return JerseyResponseManager.notFound();
            }

            ResponseBuilder rb = Response.status(response.getStatus());
            rb.entity(response.readEntity(InputStream.class));

            if(!raw) {
                try {
                    rb.type(contentType);
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

            ResponseBuilder rb = Response.status(response.getStatus()); 
            return rb.build();
            
        } catch(Exception ex) {
          logger.log(Level.WARNING, "Expect the unexpected!", ex);
          return JerseyResponseManager.notAcceptable();
        }
    }

    public static Model searchConceptFromTermedAPIAsModel(String query, String schemeURI, String conceptURI, String graphId) {

        String url = ApplicationProperties.getDefaultTermAPI()+"node-trees";

        try {

            Client client = IgnoreSSLClient(); //ClientBuilder.newClient();
            HttpAuthenticationFeature feature = TermedAuthentication.getTermedAuth();
            client.register(feature);

            WebTarget target = client.target(url)
                    .queryParam("select", "uri,id,references.prefLabelXl:2,properties.prefLabel,properties.definition")
                    .queryParam("where", "typeId:Concept")
                    .queryParam("max", "-1");

            if (graphId != null) {
                target = target.queryParam("where", "graphId:" + graphId);
            }

            if (conceptURI == null) {
                target = target.queryParam("where", "references.prefLabelXl.properties.prefLabel:" + query);
            } else {
                if (IDManager.isValidUrl(conceptURI)) {
                    target = target.queryParam("where", "uri:" + conceptURI);
                } else {
                    target = target.queryParam("where", "id:" + conceptURI);
                }

            }

            if (schemeURI != null && IDManager.isValidUrl(schemeURI)) {
                target = target.queryParam("where", "graph.uri:" + schemeURI);
            }

            Response response = target.request("application/ld+json").get();

            Model conceptModel = JerseyClient.getJSONLDResponseAsJenaModel(response);
            conceptModel.add(TermedTerminologyManager.getSchemesAsModelFromTermedAPI());

            QueryExecution qexec = QueryExecutionFactory.create(QueryLibrary.skosXlToSkos, conceptModel);

            Model simpleSkos = qexec.execConstruct();
            simpleSkos = TermedTerminologyManager.cleanModelDefinitions(simpleSkos);
            simpleSkos.setNsPrefixes(LDHelper.PREFIX_MAP);


            logger.info("TERMED CALL: " + target.getUri().toString());

            if (response.getStatusInfo().getFamily() != Response.Status.Family.SUCCESSFUL) {
                logger.log(Level.INFO, response.getStatus() + " from URL: " + url);
                return null;
            }

            return simpleSkos;
        } catch(Exception ex) {
                 logger.warning(ex.getMessage());
                return null;
            }

        }

    
     /**
     * Returns concepts from Termed api
     * @param query query string
      *              * @param schemeURI ID of the scheme
     * @return Response
     */
    public static Response searchConceptFromTermedAPI(String query, String schemeURI, String conceptURI, String graphId) {
        
            Model simpleSkos = searchConceptFromTermedAPIAsModel(query, schemeURI, conceptURI, graphId);
            
            if (simpleSkos == null) {
               return JerseyResponseManager.notFound();
            }

              ResponseBuilder rb = Response.status(Response.Status.ACCEPTED);
              rb.entity(ModelManager.writeModelToJSONLDString(simpleSkos));

           return rb.build();

    }

    /**
     * Returns available schemes
     * @return Response
     */
    @Deprecated
    public static Response getSchemesFromTermedAPI() {
        String url = ApplicationProperties.getDefaultTermAPI()+"node-trees";
        try {
            Client client = IgnoreSSLClient(); // ClientBuilder.newClient();
            HttpAuthenticationFeature feature = TermedAuthentication.getTermedAuth();
            client.register(feature);

            WebTarget target = client.target(url)
                    .queryParam("select","uri,id,properties.prefLabel,properties.description")
                    .queryParam("where", "typeId:TerminologicalVocabulary")
                    .queryParam("max", "-1");
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
     * Returns concept as Jersey Response
     * @param uri uri of the concept
     * @return Response
     */
    public static Response getConceptFromTermedAPI(String uri) {
        
        String url = ApplicationProperties.getDefaultTermAPI()+"node-trees";

        try {
            Client client = IgnoreSSLClient(); //ClientBuilder.newClient();
            HttpAuthenticationFeature feature = TermedAuthentication.getTermedAuth();
            client.register(feature);

            WebTarget target = client.target(url)
                    .queryParam("select", "id,uri,properties.prefLabel")
                    .queryParam("where","typeId:Concept")
                    .queryParam("where","uri:"+uri)
                    .queryParam("max", "-1");
            

            Response response = target.request("application/ld+json").property(ClientProperties.FOLLOW_REDIRECTS, Boolean.TRUE).get();

            logger.info("TERMED CALL: "+target.getUri().toString());

            if (response.getStatusInfo().getFamily() != Response.Status.Family.SUCCESSFUL) {
               logger.log(Level.INFO, response.getStatus()+" from URL: "+url);
               logger.info("Location: "+response.getLocation().toString());
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
         WebTarget target = client.target(service).queryParam("graph", UriComponent.encode(graph,UriComponent.Type.QUERY));
         Response response = target.request().put(Entity.entity(body, "application/ld+json"));
         client.close();

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
         WebTarget target = client.target(service).queryParam("graph", UriComponent.encode(graph,UriComponent.Type.QUERY));
         Response response = target.request().put(Entity.entity(body, "application/ld+json"));
         client.close();

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
         WebTarget target = client.target(service).queryParam("graph", UriComponent.encode(graph,UriComponent.Type.QUERY));
         Response response = target.request().post(Entity.entity(body, "application/ld+json"));
         client.close();
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


    public static Response constructNonEmptyGraphFromService(String query, String service) {

        Model constructModel = JenaClient.constructFromService(query, service);

        if(constructModel.size()<=0) {
            return JerseyResponseManager.notFound();
        }

        ResponseBuilder rb = Response.ok();
        rb.entity(ModelManager.writeModelToJSONLDString(constructModel));
        return rb.build();

    }

    
    public static Response constructGraphFromService(String query, String service) {
        
            Model constructModel = JenaClient.constructFromService(query, service);
        
            if(constructModel.size()<=0) {
                ResponseBuilder rb = Response.ok().type("application/ld+json");
                rb.entity(ModelManager.writeModelToJSONLDString(constructModel));
                return rb.build();
            }

            ResponseBuilder rb = Response.ok();
            rb.entity(ModelManager.writeModelToJSONLDString(constructModel));
            return rb.build();
          
    }

    /**
     * Constructs Jersey response from Jena model or returns error
     * @param graph Jena model
     * @return Response
     */

    public static Response constructResponseFromGraph(Model graph) {

        if(graph==null ||graph.size()<=0) {
            return JerseyResponseManager.error();
        }

        ResponseBuilder rb = Response.ok();
        rb.entity(ModelManager.writeModelToJSONLDString(graph));
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
        client.close();

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
            WebTarget target = client.target(service).queryParam("graph", UriComponent.encode(graph,UriComponent.Type.QUERY));

            Response response = target.request("application/ld+json").delete();

            client.close();

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
