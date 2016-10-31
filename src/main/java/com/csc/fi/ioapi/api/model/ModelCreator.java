/*
 * Licensed under the European Union Public Licence (EUPL) V.1.1 
 */
package com.csc.fi.ioapi.api.model;

import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.ServletContext;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import com.csc.fi.ioapi.config.ApplicationProperties;
import com.csc.fi.ioapi.config.EndpointServices;
import com.csc.fi.ioapi.utils.ErrorMessage;
import com.csc.fi.ioapi.utils.GraphManager;
import com.csc.fi.ioapi.utils.JerseyResponseManager;
import com.csc.fi.ioapi.utils.JerseyFusekiClient;
import com.csc.fi.ioapi.utils.LDHelper;
import org.apache.jena.query.ParameterizedSparqlString;
import org.apache.jena.rdf.model.ResourceFactory;
import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;
import com.wordnik.swagger.annotations.ApiParam;
import com.wordnik.swagger.annotations.ApiResponse;
import com.wordnik.swagger.annotations.ApiResponses;
import org.apache.jena.iri.IRI;
import org.apache.jena.iri.IRIException;
import org.apache.jena.iri.IRIFactory;
 
/**
 * Root resource (exposed at "modelCreator" path)
 */
@Path("modelCreator")
@Api(value = "/modelCreator", description = "Construct new model template")
public class ModelCreator {

    @Context ServletContext context;
    EndpointServices services = new EndpointServices();
     private static final Logger logger = Logger.getLogger(ModelCreator.class.getName());
    
    @GET
    @Produces("application/ld+json")
    @ApiOperation(value = "Create new model", notes = "Create new model")
    @ApiResponses(value = { @ApiResponse(code = 200, message = "New class is created"),
                    @ApiResponse(code = 400, message = "Invalid ID supplied"),
                    @ApiResponse(code = 403, message = "Invalid IRI in parameter"),
                    @ApiResponse(code = 404, message = "Service not found"),
                    @ApiResponse(code = 401, message = "No right to create new")})
    public Response newModel(
            @ApiParam(value = "Redirection service", required = false) @QueryParam("redirect") String redirect,
            @ApiParam(value = "Model prefix", required = true) @QueryParam("prefix") String prefix,
            @ApiParam(value = "Model label", required = true) @QueryParam("label") String label,
            @ApiParam(value = "Group ID", required = true) @QueryParam("group") String group,
            @ApiParam(value = "Label language", required = true, allowableValues="fi,en") @QueryParam("lang") String lang,
            @ApiParam(value = "Allowed languages as space list: 'en sv pl'. Default 'fi en'") @QueryParam("langList") String allowedLang) {

            if(allowedLang==null || allowedLang.equals("undefined") || allowedLang.length()<2) {
                allowedLang = "('fi' 'en')"; }
            else if(allowedLang.length()==2 && LDHelper.isAlphaString(lang)) {
                 allowedLang="('"+allowedLang+"')";
            }
            else {
                if(!allowedLang.contains(" "))
                    return JerseyResponseManager.invalidParameter();
                
                String[] languages = allowedLang.split(" ");
                String builtLang = "(";
                
                for(String s: languages) {
                   if(s.length()>2 || !LDHelper.isAlphaString(s)) {
                       return JerseyResponseManager.invalidParameter();
                   } 
                   builtLang = builtLang.concat(" '"+s+"'");
                }
                
                builtLang = builtLang.concat(" )");
                allowedLang = builtLang;
                
            }
                      
            prefix = LDHelper.modelName(prefix);
            String namespace = ApplicationProperties.getDefaultNamespace()+prefix;
            
            IRI groupIRI, namespaceIRI, namespaceSKOSIRI;
            IRI redirectIRI = null;
            
            try {
                    IRIFactory iri = IRIFactory.iriImplementation();
                    namespaceIRI = iri.construct(namespace);
                    namespaceSKOSIRI = iri.construct(namespace+"/skos#");
                    groupIRI = iri.construct(group);
                    if(redirect!=null && !redirect.equals("undefined")) {
                        if(redirect.endsWith("/")) {
                            redirectIRI = iri.construct(redirect);
                        } else if(redirect.endsWith("#")){
                            redirect=redirect.substring(0, redirect.length()-1);
                            redirectIRI = iri.construct(redirect);
                        } else {
                            redirectIRI = iri.construct(redirect);
                        }
                    }
            } catch (IRIException e) {
                    logger.warning("INVALID: "+namespace);
                    return JerseyResponseManager.invalidIRI();
            } catch (NullPointerException e) {
                    return JerseyResponseManager.invalidParameter();
            }
            
        if(GraphManager.isExistingPrefix(prefix)) {
            return JerseyResponseManager.usedIRI();
        }

            ParameterizedSparqlString pss = new ParameterizedSparqlString();
            pss.setNsPrefixes(LDHelper.PREFIX_MAP);
            
            if(redirectIRI!=null) {
                if(redirect.endsWith("/")) {
                    pss.setNsPrefix(prefix, redirect);
                } else pss.setNsPrefix(prefix, redirect+"#");
            } else pss.setNsPrefix(prefix, namespace+"#");
           
            
            String queryString = "CONSTRUCT  { "
                    + "?modelIRI a owl:Ontology . "
                    + "?modelIRI a dcap:MetadataVocabulary . "
                    + "?modelIRI rdfs:label ?mlabel . "
                    + "?modelIRI owl:versionInfo ?draft . "
                    + "?modelIRI dcterms:created ?creation . "
                    + "?modelIRI dcterms:modified ?creation . "
                    + "?modelIRI dcterms:language "+allowedLang+" . "
                    + "?modelIRI dcap:preferredXMLNamespaceName ?namespace . "
                    + "?modelIRI dcap:preferredXMLNamespacePrefix ?prefix . "
                    + "?modelIRI dcterms:isPartOf ?group . "
                    + "?group rdfs:label ?groupLabel . "
                    + "?modelIRI dcterms:references ?localSKOSNamespace . "
                    + "?localSKOSNamespace a skos:Collection ; "
                    + " dcterms:identifier ?prefix ; "
                    + " dcterms:title ?profileLabelSKOS . "
                    + "?modelIRI dcterms:references ?jhsScheme . "
                    + "?jhsScheme a skos:ConceptScheme ; "
                    + " dcterms:identifier 'jhsmeta' ; "
                    + " dcterms:title 'JHSMeta - Julkishallinnon määrittelevä sanasto'@fi . "
                    + "} WHERE { "
                    + "BIND(now() as ?creation) "
                    + "GRAPH <urn:csc:groups> { "
                    + "?group a foaf:Group . "
                    + "?group rdfs:label ?groupLabel . "
                    + "FILTER(lang(?groupLabel) = ?defLang)"
                    + "}"
                    + "}";

            pss.setCommandText(queryString);
            pss.setIri("localSKOSNamespace", namespaceSKOSIRI);
            pss.setLiteral("profileLabelSKOS", ResourceFactory.createLangLiteral("Sisäinen käsitteistö", lang));
            if(redirectIRI!=null) {
                if(redirect.endsWith("/")) {
                    pss.setLiteral("namespace", redirect);
                } else pss.setLiteral("namespace", redirect+"#");
            } else {
                pss.setLiteral("namespace", namespace+"#");
            }
            pss.setLiteral("prefix", prefix);
            pss.setIri("jhsScheme", "http://jhsmeta.fi/skos/");
            if(redirectIRI!=null) {
                pss.setIri("modelIRI",redirectIRI);
            } else pss.setIri("modelIRI", namespaceIRI);
            pss.setIri("group", groupIRI);
            pss.setLiteral("draft", "Unstable");
            pss.setLiteral("mlabel", ResourceFactory.createLangLiteral(label, lang));
            pss.setLiteral("defLang", lang);

            
            return JerseyFusekiClient.constructGraphFromService(pss.toString(), services.getCoreSparqlAddress());
            
    }   
 
}
