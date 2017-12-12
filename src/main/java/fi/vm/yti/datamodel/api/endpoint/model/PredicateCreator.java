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
import fi.vm.yti.datamodel.api.model.ReusablePredicate;
import fi.vm.yti.datamodel.api.utils.IDManager;
import fi.vm.yti.datamodel.api.utils.JerseyResponseManager;
import fi.vm.yti.datamodel.api.utils.JerseyJsonLDClient;
import fi.vm.yti.datamodel.api.utils.LDHelper;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import javax.servlet.http.HttpServletRequest;
import org.apache.jena.iri.IRI;
import org.apache.jena.iri.IRIException;

@Path("predicateCreator")
@Api(tags = {"Predicate"}, description = "Creates new RDF properties that can be based on SKOS concepts")
public class PredicateCreator {

    @Context ServletContext context;
    EndpointServices services = new EndpointServices();
     private static final Logger logger = Logger.getLogger(PredicateCreator.class.getName());
    
    @GET
    @Produces("application/ld+json")
    @ApiOperation(value = "Create new predicate", notes = "Create new predicate")
    @ApiResponses(value = { @ApiResponse(code = 200, message = "New predicate is created"),
                    @ApiResponse(code = 400, message = "Invalid ID supplied"),
                    @ApiResponse(code = 403, message = "Invalid IRI in parameter"),
                    @ApiResponse(code = 404, message = "Service not found") })

    public Response newPredicate(
            @ApiParam(value = "Model ID", required = true) @QueryParam("modelID") String modelID,
            @ApiParam(value = "Predicate label", required = true) @QueryParam("predicateLabel") String predicateLabel,
            @ApiParam(value = "Concept ID") @QueryParam("conceptID") String conceptID,
            @ApiParam(value = "Predicate type", required = true, allowableValues="owl:DatatypeProperty,owl:ObjectProperty") @QueryParam("type") String type,
            @ApiParam(value = "Language", required = true, allowableValues="fi,en") @QueryParam("lang") String lang,
            @Context HttpServletRequest request) {

        IRI conceptIRI = null;
        IRI modelIRI,typeIRI;
        try {
                String typeURI = type.replace("owl:", "http://www.w3.org/2002/07/owl#");
                if(conceptID!=null && IDManager.isValidUrl(conceptID)) {
                    conceptIRI = IDManager.constructIRI(conceptID);
                }
                modelIRI = IDManager.constructIRI(modelID);
                typeIRI = IDManager.constructIRI(typeURI);
        } 
        catch (NullPointerException e) {
                return JerseyResponseManager.invalidParameter();
        }
         catch (IRIException e) {
                return JerseyResponseManager.invalidIRI();
        }

        try {
            ReusablePredicate newPredicate;

            if (conceptIRI != null) {
                newPredicate = new ReusablePredicate(conceptIRI, modelIRI, predicateLabel, lang, typeIRI);
            } else {
                newPredicate = new ReusablePredicate(modelIRI, predicateLabel, lang, typeIRI);
            }

            return JerseyJsonLDClient.constructResponseFromGraph(newPredicate.asGraph());
        } catch(IllegalArgumentException ex) {
            logger.info(ex.toString());
            return JerseyResponseManager.error();
        }
    }   
 
}
