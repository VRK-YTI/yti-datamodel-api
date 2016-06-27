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
import com.csc.fi.ioapi.utils.ErrorMessage;
import com.csc.fi.ioapi.utils.GraphManager;
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
@Path("profileCreator")
@Api(value = "/profileCreator", description = "Construct new profile template")
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
                    return Response.status(403).entity(ErrorMessage.INVALIDPARAMETER).build();
                
                String[] languages = allowedLang.split(" ");
                String builtLang = "(";
                
                for(String s: languages) {
                   if(s.length()>2 || !LDHelper.isAlphaString(s)) {
                       return Response.status(403).entity(ErrorMessage.INVALIDPARAMETER).build();
                   } 
                   builtLang = builtLang.concat(" '"+s+"'");
                }
                
                builtLang = builtLang.concat(" )");
                allowedLang = builtLang;
                
            }
                
            prefix = LDHelper.modelName(prefix);
            String namespace = ApplicationProperties.getDefaultNamespace()+prefix;
            
            IRI groupIRI,namespaceIRI,namespaceSKOSIRI;
            
            try {
                    IRIFactory iri = IRIFactory.semanticWebImplementation();
                    namespaceIRI = iri.construct(namespace);
                    namespaceSKOSIRI = iri.construct(namespace+"/skos#");
                    groupIRI = iri.construct(group);
            } catch (IRIException e) {
                    logger.log(Level.WARNING, "ID is invalid IRI!");
                    return Response.status(403).entity(ErrorMessage.INVALIDIRI).build();
            }

            
            if(GraphManager.isExistingPrefix(prefix)) {
                return Response.status(405).entity(ErrorMessage.USEDIRI).build();
            }
            
            ParameterizedSparqlString pss = new ParameterizedSparqlString();
            pss.setNsPrefixes(LDHelper.PREFIX_MAP);
            pss.setNsPrefix(prefix, namespace+"#");
            
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
                    + "?modelIRI dcterms:references ?localSKOSNamespace . "
                    + "?localSKOSNamespace a skos:Collection ; "
                    + " dcterms:identifier ?prefix ; "
                    + " rdfs:label ?profileLabelSKOS . "
                    + "?modelIRI dcterms:references ?jhsScheme . "
                    + "?jhsScheme a skos:ConceptScheme ; "
                    + " dcterms:identifier 'jhsmeta' ; "
                    + " rdfs:label 'JHSMeta - Julkishallinnon määrittelevä sanasto'@fi . "
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
            pss.setIri("jhsScheme", "http://jhsmeta.fi/skos/");
            pss.setLiteral("profileLabelSKOS", ResourceFactory.createLangLiteral("Sisäinen käsitteistö", lang));
            pss.setLiteral("namespace", namespace+"#");
            pss.setLiteral("prefix", prefix);
            pss.setIri("modelIRI", namespaceIRI);
            pss.setIri("group", groupIRI);
            pss.setLiteral("draft", "Unstable");
            pss.setLiteral("profileLabel", ResourceFactory.createLangLiteral(label, lang));
            pss.setLiteral("defLang", lang);

            
            return JerseyFusekiClient.constructGraphFromService(pss.toString(), services.getCoreSparqlAddress());
            
    }   
 
}
