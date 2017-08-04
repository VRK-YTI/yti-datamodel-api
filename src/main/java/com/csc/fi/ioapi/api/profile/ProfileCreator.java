/*
 * Licensed under the European Union Public Licence (EUPL) V.1.1 
 */
package com.csc.fi.ioapi.api.profile;

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
import com.csc.fi.ioapi.utils.JerseyResponseManager;
import com.csc.fi.ioapi.utils.GraphManager;
import com.csc.fi.ioapi.utils.IDManager;
import com.csc.fi.ioapi.utils.JerseyJsonLDClient;
import com.csc.fi.ioapi.utils.LDHelper;
import org.apache.jena.query.ParameterizedSparqlString;
import org.apache.jena.rdf.model.ResourceFactory;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import org.apache.jena.iri.IRI;
import org.apache.jena.iri.IRIException;
 
/**
 * Root resource (exposed at "modelCreator" path)
 */
@Path("profileCreator")
@Api(tags = {"Profile"}, description = "Construct new profile template")
public class ProfileCreator {

    @Context ServletContext context;
    EndpointServices services = new EndpointServices();
     private static final Logger logger = Logger.getLogger(ProfileCreator.class.getName());
    
    @GET
    @Produces("application/ld+json")
    @ApiOperation(value = "Create new profile", notes = "Create new profile")
    @ApiResponses(value = { @ApiResponse(code = 200, message = "New profile is created"),
                    @ApiResponse(code = 400, message = "Invalid ID supplied"),
                    @ApiResponse(code = 403, message = "Invalid IRI in parameter"),
                    @ApiResponse(code = 404, message = "Service not found"),
                    @ApiResponse(code = 401, message = "No right to create new")})
    public Response newPfofile(
            @ApiParam(value = "Redirection service", required = false) @QueryParam("redirect") String redirect,
            @ApiParam(value = "Profile prefix", required = true) @QueryParam("prefix") String prefix,
            @ApiParam(value = "Profile label", required = true) @QueryParam("label") String label,
            @ApiParam(value = "Group ID", required = true) @QueryParam("group") String group,
            @ApiParam(value = "Initial language", required = true, allowableValues="fi,en") @QueryParam("lang") String lang,
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
            
            IRI groupIRI,namespaceIRI,namespaceSKOSIRI;
            IRI redirectIRI = null;
            
            try {
                    namespaceIRI = IDManager.constructIRI(namespace);
                    namespaceSKOSIRI = IDManager.constructIRI(namespace+"/skos#");
                    groupIRI = IDManager.constructIRI(group);                    
                    if(redirect!=null && !redirect.equals("undefined")) {
                        if(redirect.endsWith("/")) {
                            redirectIRI = IDManager.constructIRI(redirect);
                        } else if(redirect.endsWith("#")){
                            redirect=redirect.substring(0, redirect.length()-1);
                            redirectIRI = IDManager.constructIRI(redirect);
                        } else {
                            redirectIRI = IDManager.constructIRI(redirect);
                        }
                    }
            } catch (IRIException e) {
                    logger.log(Level.WARNING, "ID is invalid IRI!");
                    return JerseyResponseManager.invalidIRI();
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
                    + "?modelIRI a dcap:DCAP . "
                    + "?modelIRI rdfs:label ?profileLabel . "
                    + "?modelIRI owl:versionInfo ?draft . "
                    + "?modelIRI dcterms:created ?creation . "
                    + "?modelIRI dcterms:modified ?creation . "
                    + "?modelIRI dcterms:language "+allowedLang+" . "
                    + "?modelIRI dcap:preferredXMLNamespaceName ?namespace . "
                    + "?modelIRI dcap:preferredXMLNamespacePrefix ?prefix . "
                    + "?modelIRI dcterms:isPartOf ?group . "
                    + "?group rdfs:label ?groupLabel . "
                    + "?group dcterms:references ?skosScheme . "
                    + "?skosScheme dcterms:title ?schemeTitle . "
                    + "?skosScheme termed:graph ?termedGraph . "
                    + "?termedGraph termed:id ?termedGraphId . "
                    + "?termedGraph termed:code ?termedGraphCode . "
                   /* + "?modelIRI dcterms:references ?localSKOSNamespace . "
                    + "?localSKOSNamespace a skos:Collection ; "
                    + " dcterms:identifier ?prefix ; "
                    + " dcterms:title ?profileLabelSKOS . "
                    + "?modelIRI dcterms:references ?jhsScheme . "
                    + "?jhsScheme a skos:ConceptScheme ; "
                    + " dcterms:identifier 'jhsmeta' ; "
                    + " dcterms:title 'JHSMeta - Julkishallinnon määrittelevä sanasto'@fi . " */
                    + "} WHERE { "
                    + "BIND(now() as ?creation) "
                    + "GRAPH <urn:csc:groups> { "
                    + "?group a foaf:Group . "
                    + "?group dcterms:references ?skosScheme . "
                    + "?skosScheme dcterms:title ?schemeTitle . "
                    + "?skosScheme termed:graph ?termedGraph . "
                    + "?termedGraph termed:id ?termedGraphId . "
                    + "?termedGraph termed:code ?termedGraphCode . "
                    + "?group rdfs:label ?groupLabel . "
                    + "FILTER(lang(?groupLabel) = ?defLang)"
                    + "}"
                    + "}";

            pss.setCommandText(queryString);
            pss.setIri("localSKOSNamespace", namespaceSKOSIRI);
            pss.setIri("jhsScheme", "http://jhsmeta.fi/skos/");
            pss.setLiteral("profileLabelSKOS", ResourceFactory.createLangLiteral("Sisäinen käsitteistö", lang));
            if(redirectIRI!=null) {
                if(redirect.endsWith("/")) {
                    pss.setLiteral("namespace", redirect);
                } else pss.setLiteral("namespace", redirect+"#");
            } else {
                pss.setLiteral("namespace", namespace+"#");
            }
            pss.setLiteral("prefix", prefix);
            if(redirectIRI!=null) {
                pss.setIri("modelIRI",redirectIRI);
            } else pss.setIri("modelIRI", namespaceIRI);
            pss.setIri("group", groupIRI);
            pss.setLiteral("draft", "Unstable");
            pss.setLiteral("profileLabel", ResourceFactory.createLangLiteral(label, lang));
            pss.setLiteral("defLang", lang);

            
            return JerseyJsonLDClient.constructNotEmptyGraphFromService(pss.toString(), services.getCoreSparqlAddress());
            
    }   
 
}
