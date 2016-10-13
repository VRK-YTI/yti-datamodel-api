/*
 * Licensed under the European Union Public Licence (EUPL) V.1.1 
 */
package com.csc.fi.ioapi.api.model;

import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.ServletContext;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import com.csc.fi.ioapi.config.EndpointServices;
import com.csc.fi.ioapi.config.LoginSession;
import com.csc.fi.ioapi.utils.ErrorMessage;
import com.csc.fi.ioapi.utils.GraphManager;
import com.csc.fi.ioapi.utils.JerseyFusekiClient;
import com.csc.fi.ioapi.utils.ServiceDescriptionManager;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.NodeIterator;
import org.apache.jena.rdf.model.ResIterator;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.vocabulary.DCTerms;
import org.apache.jena.vocabulary.RDF;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.uri.UriComponent;
import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;
import com.wordnik.swagger.annotations.ApiParam;
import com.wordnik.swagger.annotations.ApiResponse;
import com.wordnik.swagger.annotations.ApiResponses;
import java.io.DataInputStream;
import java.util.HashMap;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import org.apache.jena.iri.IRI;
import org.apache.jena.iri.IRIException;
import org.apache.jena.iri.IRIFactory;
import org.apache.jena.util.ResourceUtils;
import org.apache.jena.web.DatasetAdapter;
import org.apache.jena.web.DatasetGraphAccessorHTTP;
 
/**
 * Root resource (exposed at "myresource" path)
 */
@Path("renameNamespace")
@Api(value = "/renameNamespace", description = "HAZARD operation")
public class RenameNamespace {
  
    @Context ServletContext context;
    EndpointServices services = new EndpointServices();
    private static final Logger logger = Logger.getLogger(RenameNamespace.class.getName());
   
  @PUT
  @ApiOperation(value = "Updates graph in service and writes service description to default", notes = "PUT Body should be json-ld")
  @ApiResponses(value = {
      @ApiResponse(code = 204, message = "Graph is saved"),
      @ApiResponse(code = 400, message = "Invalid graph supplied"),
      @ApiResponse(code = 401, message = "Unauthorized"),
      @ApiResponse(code = 405, message = "Update not allowed"),
      @ApiResponse(code = 403, message = "Illegal graph parameter"),
      @ApiResponse(code = 404, message = "Service not found"),
      @ApiResponse(code = 500, message = "Bad data?") 
  })
  public Response postJson(
                @ApiParam(value = "Model ID") 
                @QueryParam("modelID") 
                String modelID,
                @ApiParam(value = "New model ID") 
                @QueryParam("newModelID") 
                String newModelID,
                @Context HttpServletRequest request) {
      
      
       if(modelID==null || modelID.equals("undefined")) {
            return Response.status(403).entity(ErrorMessage.INVALIDIRI).build();
       } 
       
       if(newModelID==null || newModelID.equals("undefined")) {
            return Response.status(403).entity(ErrorMessage.INVALIDIRI).build();
       } 
       
        HttpSession session = request.getSession();
        
        if(session==null) return Response.status(403).entity(ErrorMessage.UNAUTHORIZED).build();
        
        LoginSession login = new LoginSession(session);
        
        if(!(login.isLoggedIn() && login.isSuperAdmin())) {
            return Response.status(403).entity(ErrorMessage.UNAUTHORIZED).build();
        }
        
        if(!GraphManager.isExistingGraph(modelID)) {
            return Response.status(403).entity(ErrorMessage.INVALIDIRI).build();
        }
        
        Model oldModel = GraphManager.getCoreGraph(modelID);
        
        Resource modelResource = oldModel.getResource(modelID);
        
        ResourceUtils.renameResource(modelResource,newModelID);
        
        oldModel.write(System.out);

       return Response.status(200).entity("{}").build();

  }
  
  
  /*
  
  
  PREFIX schema: <http://schema.org/>
PREFIX dcap: <http://purl.org/ws-mmi-dc/terms/>
PREFIX dcam: <http://purl.org/dc/dcam/>
PREFIX owl: <http://www.w3.org/2002/07/owl#>
PREFIX xsd: <http://www.w3.org/2001/XMLSchema#>
PREFIX skos: <http://www.w3.org/2004/02/skos/core#>
PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>
PREFIX iow: <http://iow.csc.fi/ns/iow#>
PREFIX kmr: <http://iow.csc.fi/ns/kmr#>
PREFIX sd: <http://www.w3.org/ns/sparql-service-description#>
PREFIX sh: <http://www.w3.org/ns/shacl#>
PREFIX dcterms: <http://purl.org/dc/terms/>
PREFIX text: <http://jena.apache.org/text#>
PREFIX rak: <http://iow.csc.fi/ns/rak#>
PREFIX prov: <http://www.w3.org/ns/prov#>
PREFIX foaf: <http://xmlns.com/foaf/0.1/>
PREFIX corevoc: <http://joinup.ec.europa.eu/site/core_vocabularies/registry/corevoc/>
PREFIX void: <http://rdfs.org/ns/void#>
PREFIX adms: <http://www.w3.org/ns/adms#>
PREFIX afn: <http://jena.hpl.hp.com/ARQ/function#>
PREFIX vtj: <http://iow.csc.fi/ns/vtj#>
PREFIX jhs: <http://iow.csc.fi/ns/jhs#>
PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>
PREFIX ts: <http://www.w3.org/2003/06/sw-vocab-status/ns#>
PREFIX dc: <http://purl.org/dc/elements/1.1/>

INSERT { 
 GRAPH ?graph {
    ?any skos:inScheme <http://jhsmeta.fi/skos/>. 
  }
} WHERE {
  GRAPH ?graph {
    ?graph dcterms:subject ?any .
    FILTER NOT EXISTS { ?any skos:inScheme ?scheme . }
    FILTER(regex(str(?any), 'http://jhsmeta.fi/skos/' ) )
  }
}





PREFIX schema: <http://schema.org/>
PREFIX dcap: <http://purl.org/ws-mmi-dc/terms/>
PREFIX dcam: <http://purl.org/dc/dcam/>
PREFIX owl: <http://www.w3.org/2002/07/owl#>
PREFIX xsd: <http://www.w3.org/2001/XMLSchema#>
PREFIX skos: <http://www.w3.org/2004/02/skos/core#>
PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>
PREFIX iow: <http://iow.csc.fi/ns/iow#>
PREFIX kmr: <http://iow.csc.fi/ns/kmr#>
PREFIX sd: <http://www.w3.org/ns/sparql-service-description#>
PREFIX sh: <http://www.w3.org/ns/shacl#>
PREFIX dcterms: <http://purl.org/dc/terms/>
PREFIX text: <http://jena.apache.org/text#>
PREFIX rak: <http://iow.csc.fi/ns/rak#>
PREFIX prov: <http://www.w3.org/ns/prov#>
PREFIX foaf: <http://xmlns.com/foaf/0.1/>
PREFIX corevoc: <http://joinup.ec.europa.eu/site/core_vocabularies/registry/corevoc/>
PREFIX void: <http://rdfs.org/ns/void#>
PREFIX adms: <http://www.w3.org/ns/adms#>
PREFIX afn: <http://jena.hpl.hp.com/ARQ/function#>
PREFIX vtj: <http://iow.csc.fi/ns/vtj#>
PREFIX jhs: <http://iow.csc.fi/ns/jhs#>
PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>
PREFIX ts: <http://www.w3.org/2003/06/sw-vocab-status/ns#>
PREFIX dc: <http://purl.org/dc/elements/1.1/>

INSERT {
  GRAPH ?graph {
    ?any skos:inScheme ?skos .
  }
}
WHERE {
  GRAPH ?graph {
    ?graph rdfs:isDefinedBy ?library .
    ?graph dcterms:subject ?any .
    FILTER NOT EXISTS { ?any skos:inScheme ?scheme . }
    BIND(IRI(CONCAT(STR(?library),'/skos#')) as ?skos)
  }

}






PREFIX schema: <http://schema.org/>
PREFIX dcap: <http://purl.org/ws-mmi-dc/terms/>
PREFIX dcam: <http://purl.org/dc/dcam/>
PREFIX owl: <http://www.w3.org/2002/07/owl#>
PREFIX xsd: <http://www.w3.org/2001/XMLSchema#>
PREFIX skos: <http://www.w3.org/2004/02/skos/core#>
PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>
PREFIX iow: <http://iow.csc.fi/ns/iow#>
PREFIX kmr: <http://iow.csc.fi/ns/kmr#>
PREFIX sd: <http://www.w3.org/ns/sparql-service-description#>
PREFIX sh: <http://www.w3.org/ns/shacl#>
PREFIX dcterms: <http://purl.org/dc/terms/>
PREFIX text: <http://jena.apache.org/text#>
PREFIX rak: <http://iow.csc.fi/ns/rak#>
PREFIX prov: <http://www.w3.org/ns/prov#>
PREFIX foaf: <http://xmlns.com/foaf/0.1/>
PREFIX corevoc: <http://joinup.ec.europa.eu/site/core_vocabularies/registry/corevoc/>
PREFIX void: <http://rdfs.org/ns/void#>
PREFIX adms: <http://www.w3.org/ns/adms#>
PREFIX afn: <http://jena.hpl.hp.com/ARQ/function#>
PREFIX vtj: <http://iow.csc.fi/ns/vtj#>
PREFIX jhs: <http://iow.csc.fi/ns/jhs#>
PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>
PREFIX ts: <http://www.w3.org/2003/06/sw-vocab-status/ns#>
PREFIX dc: <http://purl.org/dc/elements/1.1/>

DELETE {
  GRAPH ?any {
  ?graph dcterms:references <http://iow.csc.fi/ap/oiliu/skos#> .
   <http://iow.csc.fi/ap/oiliu/skos#> ?p ?o .
   }
}
INSERT {
    GRAPH ?any {
  ?graph dcterms:references <http://iow.csc.fi/ns/oiliu/skos#> .
    <http://iow.csc.fi/ns/oiliu/skos#> ?p ?o .
   }
}
WHERE {
  GRAPH ?any {
    ?graph dcterms:references <http://iow.csc.fi/ap/oiliu/skos#> .
    <http://iow.csc.fi/ap/oiliu/skos#> ?p ?o .
  }
}













PREFIX schema: <http://schema.org/>
PREFIX dcap: <http://purl.org/ws-mmi-dc/terms/>
PREFIX dcam: <http://purl.org/dc/dcam/>
PREFIX owl: <http://www.w3.org/2002/07/owl#>
PREFIX xsd: <http://www.w3.org/2001/XMLSchema#>
PREFIX skos: <http://www.w3.org/2004/02/skos/core#>
PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>
PREFIX iow: <http://iow.csc.fi/ns/iow#>
PREFIX kmr: <http://iow.csc.fi/ns/kmr#>
PREFIX sd: <http://www.w3.org/ns/sparql-service-description#>
PREFIX sh: <http://www.w3.org/ns/shacl#>
PREFIX dcterms: <http://purl.org/dc/terms/>
PREFIX text: <http://jena.apache.org/text#>
PREFIX rak: <http://iow.csc.fi/ns/rak#>
PREFIX prov: <http://www.w3.org/ns/prov#>
PREFIX foaf: <http://xmlns.com/foaf/0.1/>
PREFIX corevoc: <http://joinup.ec.europa.eu/site/core_vocabularies/registry/corevoc/>
PREFIX void: <http://rdfs.org/ns/void#>
PREFIX adms: <http://www.w3.org/ns/adms#>
PREFIX afn: <http://jena.hpl.hp.com/ARQ/function#>
PREFIX vtj: <http://iow.csc.fi/ns/vtj#>
PREFIX jhs: <http://iow.csc.fi/ns/jhs#>
PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>
PREFIX ts: <http://www.w3.org/2003/06/sw-vocab-status/ns#>
PREFIX dc: <http://purl.org/dc/elements/1.1/>

DELETE {
  GRAPH ?graph {
   ?graph ?p ?o .
  }
}
INSERT {
  GRAPH ?graph {
   ?graph ?p ?o .
  }
} WHERE { GRAPH ?graph {
   ?graph ?p ?o .
   FILTER(STRSTARTS(STR(?graph), "http://iow.csc.fi/ap/oiliu#"))
   BIND(STRAFTER(STR(?graph), "#") AS ?localName)
   BIND(IRI(CONCAT("http://iow.csc.fi/ns/oiliu#",?localName)) as ?newIRI)
  }










PREFIX schema: <http://schema.org/>
PREFIX dcap: <http://purl.org/ws-mmi-dc/terms/>
PREFIX dcam: <http://purl.org/dc/dcam/>
PREFIX owl: <http://www.w3.org/2002/07/owl#>
PREFIX xsd: <http://www.w3.org/2001/XMLSchema#>
PREFIX skos: <http://www.w3.org/2004/02/skos/core#>
PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>
PREFIX iow: <http://iow.csc.fi/ns/iow#>
PREFIX kmr: <http://iow.csc.fi/ns/kmr#>
PREFIX sd: <http://www.w3.org/ns/sparql-service-description#>
PREFIX sh: <http://www.w3.org/ns/shacl#>
PREFIX dcterms: <http://purl.org/dc/terms/>
PREFIX text: <http://jena.apache.org/text#>
PREFIX rak: <http://iow.csc.fi/ns/rak#>
PREFIX prov: <http://www.w3.org/ns/prov#>
PREFIX foaf: <http://xmlns.com/foaf/0.1/>
PREFIX corevoc: <http://joinup.ec.europa.eu/site/core_vocabularies/registry/corevoc/>
PREFIX void: <http://rdfs.org/ns/void#>
PREFIX adms: <http://www.w3.org/ns/adms#>
PREFIX afn: <http://jena.hpl.hp.com/ARQ/function#>
PREFIX vtj: <http://iow.csc.fi/ns/vtj#>
PREFIX jhs: <http://iow.csc.fi/ns/jhs#>
PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>
PREFIX ts: <http://www.w3.org/2003/06/sw-vocab-status/ns#>
PREFIX dc: <http://purl.org/dc/elements/1.1/>

DELETE {
  GRAPH ?graph {
   ?graph ?p ?o .
  }
}
INSERT {
  GRAPH ?graph {
   ?graph ?p ?newIRI .
  }
} WHERE { GRAPH ?graph {
   ?graph ?p ?o .
   FILTER(STRSTARTS(STR(?o), "http://iow.csc.fi/ap/oiliu#"))
   BIND(STRAFTER(STR(?o), "#") AS ?localName)
   BIND(IRI(CONCAT("http://iow.csc.fi/ns/oiliu#",?localName)) as ?newIRI)
  }
}








PREFIX schema: <http://schema.org/>
PREFIX dcap: <http://purl.org/ws-mmi-dc/terms/>
PREFIX dcam: <http://purl.org/dc/dcam/>
PREFIX owl: <http://www.w3.org/2002/07/owl#>
PREFIX xsd: <http://www.w3.org/2001/XMLSchema#>
PREFIX skos: <http://www.w3.org/2004/02/skos/core#>
PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>
PREFIX iow: <http://iow.csc.fi/ns/iow#>
PREFIX kmr: <http://iow.csc.fi/ns/kmr#>
PREFIX sd: <http://www.w3.org/ns/sparql-service-description#>
PREFIX sh: <http://www.w3.org/ns/shacl#>
PREFIX dcterms: <http://purl.org/dc/terms/>
PREFIX text: <http://jena.apache.org/text#>
PREFIX rak: <http://iow.csc.fi/ns/rak#>
PREFIX prov: <http://www.w3.org/ns/prov#>
PREFIX foaf: <http://xmlns.com/foaf/0.1/>
PREFIX corevoc: <http://joinup.ec.europa.eu/site/core_vocabularies/registry/corevoc/>
PREFIX void: <http://rdfs.org/ns/void#>
PREFIX adms: <http://www.w3.org/ns/adms#>
PREFIX afn: <http://jena.hpl.hp.com/ARQ/function#>
PREFIX vtj: <http://iow.csc.fi/ns/vtj#>
PREFIX jhs: <http://iow.csc.fi/ns/jhs#>
PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>
PREFIX ts: <http://www.w3.org/2003/06/sw-vocab-status/ns#>
PREFIX dc: <http://purl.org/dc/elements/1.1/>

DELETE {
 GRAPH ?hasPartGraph {
   ?graph dcterms:hasPart ?resource .
  }
}
INSERT {
 GRAPH ?hasPartGraph {
   ?graph dcterms:hasPart ?newIRI .
  }
}
WHERE { 
  GRAPH ?graph {
   ?graph a dcap:DCAP .
  }
  GRAPH ?hasPartGraph {
   ?graph dcterms:hasPart ?resource .
    FILTER(STRSTARTS(STR(?resource),"http://iow.csc.fi/ap/oiliu#"))
    BIND(STRAFTER(STR(?resource), "#") AS ?localName)
    BIND(IRI(CONCAT("http://iow.csc.fi/ns/oiliu#",?localName)) as ?newIRI)
  }
}













PREFIX schema: <http://schema.org/>
PREFIX dcap: <http://purl.org/ws-mmi-dc/terms/>
PREFIX dcam: <http://purl.org/dc/dcam/>
PREFIX owl: <http://www.w3.org/2002/07/owl#>
PREFIX xsd: <http://www.w3.org/2001/XMLSchema#>
PREFIX skos: <http://www.w3.org/2004/02/skos/core#>
PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>
PREFIX iow: <http://iow.csc.fi/ns/iow#>
PREFIX kmr: <http://iow.csc.fi/ns/kmr#>
PREFIX sd: <http://www.w3.org/ns/sparql-service-description#>
PREFIX sh: <http://www.w3.org/ns/shacl#>
PREFIX dcterms: <http://purl.org/dc/terms/>
PREFIX text: <http://jena.apache.org/text#>
PREFIX rak: <http://iow.csc.fi/ns/rak#>
PREFIX prov: <http://www.w3.org/ns/prov#>
PREFIX foaf: <http://xmlns.com/foaf/0.1/>
PREFIX corevoc: <http://joinup.ec.europa.eu/site/core_vocabularies/registry/corevoc/>
PREFIX void: <http://rdfs.org/ns/void#>
PREFIX adms: <http://www.w3.org/ns/adms#>
PREFIX afn: <http://jena.hpl.hp.com/ARQ/function#>
PREFIX vtj: <http://iow.csc.fi/ns/vtj#>
PREFIX jhs: <http://iow.csc.fi/ns/jhs#>
PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>
PREFIX ts: <http://www.w3.org/2003/06/sw-vocab-status/ns#>
PREFIX dc: <http://purl.org/dc/elements/1.1/>

DELETE {
GRAPH ?graph {
   ?graph ?p ?o .
  }
}
INSERT {
GRAPH ?newIRI {
   ?newIRI ?p ?o .
  }
}
WHERE { 
  GRAPH ?graph {
   ?graph a dcap:DCAP .
   ?graph ?p ?o .
   FILTER(?graph=<http://iow.csc.fi/ap/oiliu>)
    BIND(IRI(STR("http://iow.csc.fi/ns/oiliu")) as ?newIRI)
  }
}







PREFIX schema: <http://schema.org/>
PREFIX dcap: <http://purl.org/ws-mmi-dc/terms/>
PREFIX dcam: <http://purl.org/dc/dcam/>
PREFIX owl: <http://www.w3.org/2002/07/owl#>
PREFIX xsd: <http://www.w3.org/2001/XMLSchema#>
PREFIX skos: <http://www.w3.org/2004/02/skos/core#>
PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>
PREFIX iow: <http://iow.csc.fi/ns/iow#>
PREFIX kmr: <http://iow.csc.fi/ns/kmr#>
PREFIX sd: <http://www.w3.org/ns/sparql-service-description#>
PREFIX sh: <http://www.w3.org/ns/shacl#>
PREFIX dcterms: <http://purl.org/dc/terms/>
PREFIX text: <http://jena.apache.org/text#>
PREFIX rak: <http://iow.csc.fi/ns/rak#>
PREFIX prov: <http://www.w3.org/ns/prov#>
PREFIX foaf: <http://xmlns.com/foaf/0.1/>
PREFIX corevoc: <http://joinup.ec.europa.eu/site/core_vocabularies/registry/corevoc/>
PREFIX void: <http://rdfs.org/ns/void#>
PREFIX adms: <http://www.w3.org/ns/adms#>
PREFIX afn: <http://jena.hpl.hp.com/ARQ/function#>
PREFIX vtj: <http://iow.csc.fi/ns/vtj#>
PREFIX jhs: <http://iow.csc.fi/ns/jhs#>
PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>
PREFIX ts: <http://www.w3.org/2003/06/sw-vocab-status/ns#>
PREFIX dc: <http://purl.org/dc/elements/1.1/>
PREFIX sg: <http://name.scigraph.com/ontologies/core/>

DELETE {
  GRAPH <urn:csc:iow:sd> {
  	?namedGraph sd:name ?graph . 
  }
}
INSERT {
  GRAPH <urn:csc:iow:sd> {
 	 ?namedGraph sd:name ?newIRI . 
  }
}
WHERE { 
  GRAPH <urn:csc:iow:sd> {
   ?graphs sd:namedGraph ?namedGraph .
   ?namedGraph sd:name ?graph . 
   FILTER(?graph=<http://iow.csc.fi/ap/oiliu>)
   BIND(IRI(STR("http://iow.csc.fi/ns/oiliu#")) as ?newIRI)
  }
}
  
  
  
  */
  

  
}
