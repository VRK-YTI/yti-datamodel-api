/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package fi.vm.yti.datamodel.api.endpoint.concepts;

import fi.vm.yti.datamodel.api.config.EndpointServices;
import fi.vm.yti.datamodel.api.config.LoginSession;
import fi.vm.yti.datamodel.api.utils.IDManager;
import fi.vm.yti.datamodel.api.utils.JerseyResponseManager;
import fi.vm.yti.datamodel.api.utils.JerseyJsonLDClient;
import fi.vm.yti.datamodel.api.utils.LDHelper;
import fi.vm.yti.datamodel.api.utils.ModelManager;
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
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.SKOS;
import org.apache.jena.vocabulary.SKOSXL;

/**
 * REST Web Service
 *
 * @author malonen
 */
@Path("conceptSuggestion")
@Api(tags = {"Concept"}, description = "Create concept suggestions")
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
                @ApiParam(value = "Label", required = true) @QueryParam("label") String label,
		        @ApiParam(value = "Comment", required = true) @QueryParam("comment") String comment,
                @ApiParam(value = "Initial language", required = true, allowableValues="fi,en") @QueryParam("lang") String lang,
                @Context HttpServletRequest request) {

                HttpSession session = request.getSession();

                if(session==null) return JerseyResponseManager.unauthorized();

                LoginSession login = new LoginSession(session);

                if(!login.isLoggedIn()) return JerseyResponseManager.unauthorized();
                
                if(modelID!=null && !modelID.equals("undefined")) {
                    if(IDManager.isInvalid(modelID)) {
                        return JerseyResponseManager.invalidIRI();
                 }
                }

                UUID conceptUUID = UUID.randomUUID();
                UUID termUUID = UUID.randomUUID();

                Model model = ModelFactory.createDefaultModel();

                Property statusProp = model.createProperty("https://www.w3.org/2003/06/sw-vocab-status/ns#term_status");
                Resource concept = model.createResource("urn:uuid:"+conceptUUID);
                Resource term = model.createResource("urn:uuid:"+termUUID);
                Literal prefLabel = ResourceFactory.createLangLiteral(label, lang);
                Literal definition = ResourceFactory.createLangLiteral(comment, lang);
                term.addLiteral(SKOSXL.literalForm, prefLabel);
                term.addProperty(RDF.type,SKOSXL.Label);
                concept.addLiteral(SKOS.definition, definition);
                concept.addProperty(SKOSXL.prefLabel,term);
                concept.addProperty(RDF.type, SKOS.Concept);
                concept.addLiteral(statusProp, "SUGGESTION");
                Property idProp = model.createProperty(LDHelper.getNamespaceWithPrefix("termed")+"id");
                concept.addProperty(idProp, conceptUUID.toString());
                
                String modelString = ModelManager.writeModelToString(model);
                
                JerseyJsonLDClient.saveConceptSuggestion(modelString,graphUUID);

                return JerseyResponseManager.successUuid(conceptUUID);

        }
  
}
