/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.csc.fi.ioapi.api.concepts;

import com.csc.fi.ioapi.config.EndpointServices;
import com.csc.fi.ioapi.config.LoginSession;
import com.csc.fi.ioapi.utils.IDManager;
import com.csc.fi.ioapi.utils.JerseyResponseManager;
import com.csc.fi.ioapi.utils.JerseyJsonLDClient;
import com.csc.fi.ioapi.utils.LDHelper;
import com.csc.fi.ioapi.utils.ModelManager;
import java.util.logging.Logger;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import org.apache.jena.rdf.model.ResourceFactory;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import java.util.UUID;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import javax.ws.rs.PUT;
import javax.ws.rs.core.Response;
import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.NodeIterator;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;
import org.apache.jena.vocabulary.SKOS;

/**
 * REST Web Service
 *
 * @author malonen
 */
@Path("conceptSuggestion")
@Api(value = "/conceptSuggestion", description = "Edit resources")
public class ConceptSuggestion {

  @Context ServletContext context;
  private EndpointServices services = new EndpointServices();
  private static final Logger logger = Logger.getLogger(ConceptSuggestion.class.getName());
  
    	@PUT
	@ApiOperation(value = "Create concept suggestion", notes = "Create new concept suggestion")
	@ApiResponses(value = { @ApiResponse(code = 200, message = "New concept is created"),
			@ApiResponse(code = 400, message = "Invalid ID supplied"),
			@ApiResponse(code = 403, message = "Invalid IRI in parameter"),
                        @ApiResponse(code = 401, message = "User is not logged in"),
			@ApiResponse(code = 404, message = "Service not found") })
	public Response newConceptSuggestion(
                @ApiParam(value = "Model ID") @QueryParam("modelID") String modelID,
                @ApiParam(value = "GRAPH UUID", required = true) @QueryParam("graphUUID") String graphUUID,
                @ApiParam(value = "Scheme URI", required = true) @QueryParam("schemeID") String schemeID,
                @ApiParam(value = "Label", required = true) @QueryParam("label") String label,
		@ApiParam(value = "Comment", required = true) @QueryParam("comment") String comment,
                @ApiParam(value = "Initial language", required = true, allowableValues="fi,en") @QueryParam("lang") String lang,
                @ApiParam(value = "Broader concept ID") @QueryParam("topConceptID") String topConceptID,
                @Context HttpServletRequest request) {

                HttpSession session = request.getSession();

                if(session==null) return JerseyResponseManager.unauthorized();

                LoginSession login = new LoginSession(session);

                if(!login.isLoggedIn()) return JerseyResponseManager.unauthorized();
                
                if(IDManager.isInvalid(schemeID)) {
                    return JerseyResponseManager.invalidIRI();
                }
                
                if(modelID!=null && !modelID.equals("undefined")) {
                    if(IDManager.isInvalid(modelID)) {
                        return JerseyResponseManager.invalidIRI();
                 }
                }
                
                if(topConceptID!=null && !topConceptID.equals("undefined")) {
                    if(IDManager.isInvalid(topConceptID)) {
                        return JerseyResponseManager.invalidIRI();
                 }
                }
                
                UUID conceptUUID = UUID.randomUUID();
                
                // TODO: TEST CONCEPT SAVING.

                Model model = ModelFactory.createDefaultModel();
                Resource concept = model.createResource("urn:uuid:"+conceptUUID);
                Resource inScheme = model.createResource(schemeID);
                Literal prefLabel = ResourceFactory.createLangLiteral(label, lang);
                Literal definition = ResourceFactory.createLangLiteral(comment, lang);
                concept.addLiteral(SKOS.prefLabel, prefLabel);
                concept.addLiteral(SKOS.definition, definition);
                concept.addProperty(SKOS.inScheme,inScheme);
                concept.addProperty(RDF.type, SKOS.Concept);
                
                Model schemeModel = JerseyJsonLDClient.getSchemeAsModelFromTermedAPI(schemeID);
                Property graphProp = schemeModel.createProperty(LDHelper.getNamespaceWithPrefix("termed")+"graph");
               
                NodeIterator nodeit = schemeModel.listObjectsOfProperty(graphProp);
                
                while(nodeit.hasNext()) {
                    RDFNode node = nodeit.next();
                    concept.addProperty(graphProp, node);
                }
                
                if(schemeModel!=null) {
                    model.add(schemeModel);
                }
                
                String modelString = ModelManager.writeModelToString(model);
                System.out.println(modelString);
                
                JerseyJsonLDClient.saveConceptSuggestion(modelString,graphUUID);

                return JerseyResponseManager.okModel(model); 
                
              //  return JerseyResponseManager.okUUID(conceptUUID);

        }
  
}
