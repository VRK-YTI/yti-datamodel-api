/*
 * Licensed under the European Union Public Licence (EUPL) V.1.1 
 */
package fi.vm.yti.datamodel.api.endpoint.profile;

import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.ServletContext;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;

import fi.vm.yti.datamodel.api.model.Shape;
import fi.vm.yti.datamodel.api.utils.*;
import fi.vm.yti.datamodel.api.utils.JerseyClient;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;

import javax.servlet.http.HttpServletRequest;
import org.apache.jena.iri.IRI;
import org.apache.jena.iri.IRIException;
import org.apache.jena.util.SplitIRI;

/**
 * Root resource (exposed at "classCreator" path)
 */
@Path("shapeCreator")
@Api(tags = {"Profile"}, description = "Construct new Shape template")
public class ShapeCreator {

    @Context ServletContext context;
    private static final Logger logger = Logger.getLogger(ShapeCreator.class.getName());
    
    @GET
    @Produces("application/ld+json")
    @ApiOperation(value = "Create new class", notes = "Create new")
    @ApiResponses(value = { @ApiResponse(code = 200, message = "New class is created"),
                    @ApiResponse(code = 400, message = "Invalid ID supplied"),
                    @ApiResponse(code = 403, message = "Invalid IRI in parameter"),
                    @ApiResponse(code = 404, message = "Service not found") })
    public Response newClass(
            @ApiParam(value = "Profile ID", required = true) @QueryParam("profileID") String profileID,
            @ApiParam(value = "Class ID", required = true) @QueryParam("classID") String classID,
            @ApiParam(value = "Language", required = true, allowableValues="fi,en") @QueryParam("lang") String lang,
            @Context HttpServletRequest request) {

            IRI classIRI,profileIRI,shapeIRI;
            try {
                    classIRI = IDManager.constructIRI(classID);
                    profileIRI = IDManager.constructIRI(profileID);
                    if(profileID.endsWith("/") || profileID.endsWith("#")) {
                       shapeIRI = IDManager.constructIRI(profileIRI+SplitIRI.localname(classID)); 
                    } else {
                        shapeIRI = IDManager.constructIRI(profileIRI+"#"+SplitIRI.localname(classID));
                    }
                    
            } catch (IRIException e) {
                    logger.log(Level.WARNING, "ID is invalid IRI!");
                    return JerseyResponseManager.invalidIRI();
            }

           Shape newShape = new Shape(classIRI,shapeIRI,profileIRI);

           return JerseyClient.constructResponseFromGraph(newShape.asGraph());
    }   
 
}
