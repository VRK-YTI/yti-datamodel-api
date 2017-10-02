/*
 * Licensed under the European Union Public Licence (EUPL) V.1.1 
 */
package fi.vm.yti.datamodel.api.endpoint.model;

import java.util.logging.Logger;
import javax.servlet.ServletContext;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;

import fi.vm.yti.datamodel.api.config.EndpointServices;
import fi.vm.yti.datamodel.api.utils.IDManager;
import fi.vm.yti.datamodel.api.utils.JerseyResponseManager;
import fi.vm.yti.datamodel.api.utils.JerseyJsonLDClient;
import fi.vm.yti.datamodel.api.utils.LDHelper;
import fi.vm.yti.datamodel.api.utils.NamespaceManager;
import org.apache.jena.query.ParameterizedSparqlString;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import java.util.Map;
import org.apache.jena.iri.IRI;
import org.apache.jena.iri.IRIException;

/**
 * Root resource (exposed at "usage" path)
 */
@Path("usage")
@Api(tags = {"Resource"}, description = "Returns all known references to the given resource. Resource ID and Model ID are alternative parameters")
public class Usage {

    @Context ServletContext context;
    EndpointServices services = new EndpointServices();
    private static final Logger logger = Logger.getLogger(Usage.class.getName());
    
    @GET
    @Produces("application/ld+json")
    @ApiOperation(value = "Create new class", notes = "Create new")
    @ApiResponses(value = { @ApiResponse(code = 200, message = "New class is created"),
                    @ApiResponse(code = 400, message = "Invalid ID supplied"),
                    @ApiResponse(code = 403, message = "Invalid IRI in parameter"),
                    @ApiResponse(code = 404, message = "Service not found") })
    public Response newClass(
            @ApiParam(value = "Resource ID") @QueryParam("id") String id,
            @ApiParam(value = "Model ID") @QueryParam("model") String model,
            @ApiParam(value = "Concept ID") @QueryParam("concept") String concept) {

            IRI resourceIRI = null;
            IRI modelIRI = null;
            IRI conceptIRI = null;
            
            try {
                    if(id!=null && !id.equals("undefined")) resourceIRI = IDManager.constructIRI(id);
                    if(model!=null && !model.equals("undefined")) modelIRI = IDManager.constructIRI(model);
                    if(concept!=null && !concept.equals("undefined")) conceptIRI = IDManager.constructIRI(concept);
            } 
            catch (NullPointerException e) {
                    return JerseyResponseManager.invalidParameter();
            }
            catch (IRIException e) {
                    return JerseyResponseManager.invalidIRI();
            }

            ParameterizedSparqlString pss = new ParameterizedSparqlString();
            Map<String,String> namespaces = NamespaceManager.getCoreNamespaceMap();
            namespaces.putAll(LDHelper.PREFIX_MAP);
            pss.setNsPrefixes(namespaces);
            
            String conceptQueryString = "CONSTRUCT  { "
                    + "?concept dcterms:isReferencedBy ?resource . "
                    + "?concept skos:prefLabel ?prefLabel . "
                    + "?resource a ?type . "
                    + "?resource rdfs:label ?label . "
                    + "?resource rdfs:comment ?comment . "
                    + "?resource rdfs:isDefinedBy ?resourceModel . "
                    + "?resourceModel a ?modelType . "
                    + "?resourceModel rdfs:label ?modelLabel . "
                    + "?resourceModel dcap:preferredXMLNamespaceName ?namespace . "
                    + "?resourceModel dcap:preferredXMLNamespacePrefix ?prefix . "
                    + "} WHERE { "
                    + "GRAPH ?resource { "
                    + "?resource dcterms:subject ?concept . "
                    + "?resource a ?type . "
                    + "?resource rdfs:label ?label . "
                    + "OPTIONAL { ?resource rdfs:comment ?comment . }"
                    + "OPTIONAL {?resource rdfs:isDefinedBy ?resourceModel . }"
                    + "GRAPH ?resourceModel {"
                    + "?resourceModel a ?modelType . "
                    + "?resourceModel rdfs:label ?modelLabel . "
                    + "?resourceModel dcap:preferredXMLNamespaceName ?namespace . "
                    + "?resourceModel dcap:preferredXMLNamespacePrefix ?prefix . "
                    + "}"
                    + "}}";
            
            String queryString = "CONSTRUCT  { "
                    + "?resource a ?type . "
                    + "?resource rdfs:label ?label . "
                    + "?resource rdfs:isDefinedBy ?resourceModel . "
                    + "?resourceModel a ?modelType . "
                    + "?resourceModel rdfs:label ?modelLabel . "
                    + "?resourceModel dcap:preferredXMLNamespaceName ?namespace . "
                    + "?resourceModel dcap:preferredXMLNamespacePrefix ?prefix . "
                    + "?resource dcterms:isReferencedBy ?usage . "
                    + "?usage a ?usageType . "
                    + "?usage rdfs:label ?usageLabel . "
                    + "?usage rdfs:isDefinedBy ?usageModel . "
                    + "?usageModel a ?usageModelType . "
                    + "?usageModel rdfs:label ?usageModelLabel . "
                    + "?usageModel dcap:preferredXMLNamespaceName ?usageNamespace . "
                    + "?usageModel dcap:preferredXMLNamespacePrefix ?usagePrefix . "
                    + "} WHERE { "
                    + "GRAPH ?resource { "
                    + "?resource a ?type . "
                    + "?resource rdfs:label ?label . "
                    + "?resource rdfs:isDefinedBy ?resourceModel . }"
                    + "GRAPH ?resourceModel {"
                    + "?resourceModel a ?modelType . "
                    + "?resourceModel rdfs:label ?modelLabel . "
                    + "?resourceModel dcap:preferredXMLNamespaceName ?namespace . "
                    + "?resourceModel dcap:preferredXMLNamespacePrefix ?prefix . "
                    + "}"
                    + "GRAPH ?usage { "
                    + "?subject ?property ?resource . "
                    + "?usage a ?usageType . "
                    + "?usage rdfs:label ?usageLabel . "
                    + "OPTIONAL {?usage rdfs:isDefinedBy ?usageModel . }}"
                    + "FILTER(?usage!=?resource && ?subject!=?resource)"
                    + "GRAPH ?usageModel {"
                    + "?usageModel a ?usageModelType . "
                    + "?usageModel rdfs:label ?usageModelLabel . "
                    + "?usageModel dcap:preferredXMLNamespaceName ?usageNamespace . "
                    + "?usageModel dcap:preferredXMLNamespacePrefix ?usagePrefix . "
                    + "}"
                    + "}";
            
                String modelQueryString = "CONSTRUCT  { "
                    + "?resourceModel a ?modelType . "
                    + "?resourceModel rdfs:label ?modelLabel . "
                    + "?resourceModel dcterms:isReferencedBy ?usage . "
                    + "?resourceModel dcap:preferredXMLNamespaceName ?namespace . "
                    + "?resourceModel dcap:preferredXMLNamespacePrefix ?prefix . "
                    + "?usage a ?usageType . "
                    + "?usage rdfs:label ?usageLabel . "
                    + "?usage rdfs:isDefinedBy ?usageModel . "
                         
                    + "?usageModel a ?usageModelType . "
                    + "?usageModel rdfs:label ?usageModelLabel . "
                    + "?usageModel dcap:preferredXMLNamespaceName ?usageNamespace . "
                    + "?usageModel dcap:preferredXMLNamespacePrefix ?usagePrefix . "
         
                    + "} WHERE { "
                    + "GRAPH ?resource { "
                    + "?resource a ?type . "
                    + "?resource rdfs:label ?label . "
                    + "?resource rdfs:isDefinedBy ?resourceModel . }"
                    + "GRAPH ?resourceModel {"
                    + "?resourceModel a ?modelType . "
                    + "?resourceModel rdfs:label ?modelLabel . "
                    + "?resourceModel dcap:preferredXMLNamespaceName ?namespace . "
                    + "?resourceModel dcap:preferredXMLNamespacePrefix ?prefix . "
                    + "}"
                    + "GRAPH ?usage { "
                    + "?subject ?property ?resource . "
                    + "?usage a ?usageType . "
                    + "?usage rdfs:label ?usageLabel . "
                    + "?usage rdfs:isDefinedBy ?usageModel . "    
                    + "}"
                    + "FILTER(?usageModel!=?resourceModel && ?subject!=?resource)"
                    + "GRAPH ?usageModel {"
                    + "?usageModel a ?usageModelType . "
                    + "?usageModel rdfs:label ?usageModelLabel . "
                    + "?usageModel dcap:preferredXMLNamespaceName ?usageNamespace . "
                    + "?usageModel dcap:preferredXMLNamespacePrefix ?usagePrefix . "
                    + "}"
                    + "}";

            if(resourceIRI!=null) {
                pss.setCommandText(queryString);
                pss.setIri("resource", resourceIRI);
            } else if(modelIRI!=null) {
                pss.setCommandText(modelQueryString);
                pss.setIri("resourceModel", modelIRI);
            } else if(conceptIRI!=null) {
                pss.setCommandText(conceptQueryString);
                pss.setIri("concept", conceptIRI);
            } else return JerseyResponseManager.invalidParameter();
           
            return JerseyJsonLDClient.constructGraphFromService(pss.toString(), services.getCoreSparqlAddress());
    }   
 
}
