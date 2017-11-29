/*
 * Licensed under the European Union Public Licence (EUPL) V.1.1 
 */
package fi.vm.yti.datamodel.api.endpoint.model;

import java.util.logging.Logger;
import javax.servlet.ServletContext;
import javax.validation.constraints.Null;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import fi.vm.yti.datamodel.api.model.ReusableClass;
import fi.vm.yti.datamodel.api.utils.GraphManager;
import fi.vm.yti.datamodel.api.utils.JerseyJsonLDClient;
import fi.vm.yti.datamodel.api.utils.JerseyResponseManager;
import fi.vm.yti.datamodel.api.config.EndpointServices;
import fi.vm.yti.datamodel.api.utils.IDManager;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import javax.servlet.http.HttpServletRequest;
import org.apache.jena.iri.IRI;
import org.apache.jena.iri.IRIException;

/**
 * Root resource (exposed at "classCreator" path)
 */
@Path("classCreator")
@Api(tags = {"Class"}, description = "Construct new Class template")
public class ClassCreator {

    @Context ServletContext context;
    EndpointServices services = new EndpointServices();
    private static final Logger logger = Logger.getLogger(ClassCreator.class.getName());
    
    @GET
    @Produces("application/ld+json")
    @ApiOperation(value = "Create new class", notes = "Create new")
    @ApiResponses(value = { @ApiResponse(code = 200, message = "New class is created"),
                    @ApiResponse(code = 400, message = "Invalid ID supplied"),
                    @ApiResponse(code = 403, message = "Invalid IRI in parameter"),
                    @ApiResponse(code = 404, message = "Service not found") })
    public Response newClass(
            @ApiParam(value = "Model ID", required = true) @QueryParam("modelID") String modelID,
            @ApiParam(value = "Class label", required = true) @QueryParam("classLabel") String classLabel,
            @ApiParam(value = "Concept ID") @QueryParam("conceptID") String conceptID,
            @ApiParam(value = "Language", required = true, allowableValues="fi,en") @QueryParam("lang") String lang,
            @Context HttpServletRequest request) {

            IRI conceptIRI = null;
            IRI modelIRI;

            try {

                    if(conceptID!=null) {
                        logger.info("!null");
                        conceptIRI = IDManager.constructIRI(conceptID);
                    }

                    modelIRI = IDManager.constructIRI(modelID);
            } catch (IRIException e) {
                    return JerseyResponseManager.invalidIRI();
            } catch (NullPointerException e) {
                return JerseyResponseManager.invalidParameter();
            }

            if(!GraphManager.isExistingGraph(modelIRI)) {
                return JerseyResponseManager.notFound();
            }

            ReusableClass newClass;

            if(conceptIRI!=null) {
                newClass = new ReusableClass(conceptIRI, modelIRI, classLabel, lang);
            }
            else {
                newClass = new ReusableClass(modelIRI, classLabel, lang);
            }

            return JerseyJsonLDClient.constructResponseFromGraph(newClass.asGraph());
            
    }   
 
}
