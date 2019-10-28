/*
 * Licensed under the European Union Public Licence (EUPL) V.1.1
 */
package fi.vm.yti.datamodel.api.endpoint.model;

import fi.vm.yti.datamodel.api.service.EndpointServices;
import fi.vm.yti.datamodel.api.service.ExternalGraphManager;
import fi.vm.yti.datamodel.api.service.IDManager;
import fi.vm.yti.datamodel.api.service.JerseyClient;
import fi.vm.yti.datamodel.api.service.JerseyResponseManager;
import fi.vm.yti.datamodel.api.utils.LDHelper;
import fi.vm.yti.datamodel.api.utils.QueryLibrary;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component
@Path("v1/externalClass")
@Tag(name = "Class")
public class ExternalClass {

    private static final Logger logger = LoggerFactory.getLogger(ExternalClass.class.getName());
    private final JerseyResponseManager jerseyResponseManager;
    private final IDManager idManager;
    private final ExternalGraphManager externalGraphManager;
    private final JerseyClient jerseyClient;

    @Autowired
    ExternalClass(JerseyResponseManager jerseyResponseManager,
                  IDManager idManager,
                  JerseyClient jerseyClient,
                  ExternalGraphManager externalGraphManager) {

        this.jerseyClient = jerseyClient;
        this.jerseyResponseManager = jerseyResponseManager;
        this.idManager = idManager;
        this.externalGraphManager = externalGraphManager;
    }

    @GET
    @Produces("application/ld+json")
    @Operation(description = "Get external class from requires")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "404", description = "No such resource"),
        @ApiResponse(responseCode = "400", description = "Invalid model supplied"),
        @ApiResponse(responseCode = "404", description = "Service not found"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public Response getExternalClass(
        @Parameter(description = "Class id") @QueryParam("id") String id,
        @Parameter(description = "Model id", required = true) @QueryParam("model") String model) {

        IRI idIRI;

        /* Check that Model URI is valid */
        if (!idManager.isValidUrl(model)) {
            logger.debug("Invalid model uri: "+model);
            return jerseyResponseManager.invalidIRI();
        }

        if (id == null || id.equals("undefined") || id.equals("default")) {

            return jerseyClient.constructResponseFromGraph(externalGraphManager.getListOfExternalClasses(model));

        } else {
            try {
                idIRI = idManager.constructIRI(id);
            } catch (IRIException e) {
                logger.debug("Invalid class iri: "+id);
                return jerseyResponseManager.invalidIRI();
            }
            return jerseyClient.constructResponseFromGraph(externalGraphManager.getExternalClass(idIRI, model));
        }
    }
}
