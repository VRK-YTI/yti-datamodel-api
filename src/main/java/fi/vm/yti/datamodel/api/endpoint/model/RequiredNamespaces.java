/*
 * Licensed under the European Union Public Licence (EUPL) V.1.1
 */
package fi.vm.yti.datamodel.api.endpoint.model;

import fi.vm.yti.datamodel.api.service.EndpointServices;
import fi.vm.yti.datamodel.api.service.JerseyClient;
import fi.vm.yti.datamodel.api.utils.LDHelper;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;

import org.apache.jena.query.ParameterizedSparqlString;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;

@Component
@Path("v1/listNamespaces")
@Api(tags = { "Model" }, description = "Get list of available namespaces")
public class RequiredNamespaces {

    private final EndpointServices endpointServices;
    private final JerseyClient jerseyClient;

    @Autowired
    RequiredNamespaces(EndpointServices endpointServices,
                       JerseyClient jerseyClient) {

        this.endpointServices = endpointServices;
        this.jerseyClient = jerseyClient;
    }

    @GET
    @Produces("application/ld+json")
    @ApiOperation(value = "Get available namespaces from service", notes = "Local model namespaces and technical namespaces")
    @ApiResponses(value = {
        @ApiResponse(code = 400, message = "Invalid"),
        @ApiResponse(code = 404, message = "Service not found"),
        @ApiResponse(code = 500, message = "Internal server error")
    })
    public Response json() {

        ParameterizedSparqlString pss = new ParameterizedSparqlString();

        pss.setNsPrefixes(LDHelper.PREFIX_MAP);
        /* IF ID is null or default and no group available */
        String queryString = "CONSTRUCT { "
            + "?g a ?type . "
            + "?g rdfs:label ?label . "
            + "?g dcap:preferredXMLNamespaceName ?namespace . "
            + "?g dcap:preferredXMLNamespacePrefix ?prefix . "
            + "} "
            + "WHERE { {"
            + "GRAPH ?g { "
            + "?g a ?type . "
            + "?g rdfs:label ?label . "
            + "?g dcap:preferredXMLNamespaceName ?namespace . "
            + "?g dcap:preferredXMLNamespacePrefix ?prefix . } } UNION {"
            + "GRAPH <urn:csc:iow:namespaces> {"
            + "?g a ?type . "
            + "?g rdfs:label ?label . "
            + "?g dcap:preferredXMLNamespaceName ?namespace . "
            + "?g dcap:preferredXMLNamespacePrefix ?prefix . }"
            + "}}";

        pss.setCommandText(queryString);

        return jerseyClient.constructGraphFromService(pss.toString(), endpointServices.getCoreSparqlAddress());
    }
}
