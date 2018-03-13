/*
 * Licensed under the European Union Public Licence (EUPL) V.1.1 
 */
package fi.vm.yti.datamodel.api.endpoint.model;

import fi.vm.yti.datamodel.api.service.EndpointServices;
import fi.vm.yti.datamodel.api.service.IDManager;
import fi.vm.yti.datamodel.api.service.JerseyClient;
import fi.vm.yti.datamodel.api.service.JerseyResponseManager;
import fi.vm.yti.datamodel.api.service.NamespaceManager;
import fi.vm.yti.datamodel.api.utils.LDHelper;
import io.swagger.annotations.*;
import org.apache.jena.iri.IRI;
import org.apache.jena.iri.IRIException;
import org.apache.jena.query.ParameterizedSparqlString;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;
import java.util.Map;

@Component
@Path("usage")
@Api(tags = {"Resource"}, description = "Returns all known references to the given resource. Resource ID and Model ID are alternative parameters")
public class Usage {

    private final IDManager idManager;
    private final JerseyResponseManager jerseyResponseManager;
    private final NamespaceManager namespaceManager;
    private final EndpointServices endpointServices;
    private final JerseyClient jerseyClient;

    @Autowired
    Usage(IDManager idManager,
          JerseyResponseManager jerseyResponseManager,
          NamespaceManager namespaceManager,
          EndpointServices endpointServices,
          JerseyClient jerseyClient) {

        this.idManager = idManager;
        this.jerseyResponseManager = jerseyResponseManager;
        this.namespaceManager = namespaceManager;
        this.endpointServices = endpointServices;
        this.jerseyClient = jerseyClient;
    }

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
                    if(id!=null && !id.equals("undefined")) resourceIRI = idManager.constructIRI(id);
                    if(model!=null && !model.equals("undefined")) modelIRI = idManager.constructIRI(model);
                    if(concept!=null && !concept.equals("undefined")) conceptIRI = idManager.constructIRI(concept);
            } 
            catch (NullPointerException e) {
                    return jerseyResponseManager.invalidParameter();
            }
            catch (IRIException e) {
                    return jerseyResponseManager.invalidIRI();
            }

            ParameterizedSparqlString pss = new ParameterizedSparqlString();
            Map<String,String> namespaces = namespaceManager.getCoreNamespaceMap();
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
            } else return jerseyResponseManager.invalidParameter();
           
            return jerseyClient.constructGraphFromService(pss.toString(), endpointServices.getCoreSparqlAddress());
    }
}
