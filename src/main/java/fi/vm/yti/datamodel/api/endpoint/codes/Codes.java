/*
 * Licensed under the European Union Public Licence (EUPL) V.1.1
 */
package fi.vm.yti.datamodel.api.endpoint.codes;

import fi.vm.yti.datamodel.api.config.ApplicationProperties;
import fi.vm.yti.datamodel.api.model.SuomiCodeServer;
import fi.vm.yti.datamodel.api.service.*;
import fi.vm.yti.datamodel.api.model.OPHCodeServer;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.ws.rs.*;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;

@Component
@Path("v1/codeValues")
@Tag(name = "Codes")
public class Codes {

    private final EndpointServices endpointServices;
    private final JerseyResponseManager jerseyResponseManager;
    private final ApplicationProperties applicationProperties;
    private final CodeSchemeManager codeSchemeManager;

    @Autowired
    Codes(EndpointServices endpointServices,
          JerseyResponseManager jerseyResponseManager,
          ApplicationProperties applicationProperties,
          CodeSchemeManager codeSchemeManager) {
        this.endpointServices = endpointServices;
        this.jerseyResponseManager = jerseyResponseManager;
        this.applicationProperties = applicationProperties;
        this.codeSchemeManager = codeSchemeManager;
    }

    @GET
    @Produces("application/ld+json")
    @Operation(description = "Get code values with id")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "codes"),
        @ApiResponse(responseCode = "406", description = "code not defined"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public Response getCodes(
        @Parameter(description = "uri", required = true)
        @QueryParam("uri") String uri,
        @Parameter(description = "forced update")
        @QueryParam("force") boolean force) {
        if (uri.startsWith("http://uri.suomi.fi")) {
            SuomiCodeServer codeServer = new SuomiCodeServer("https://koodistot.suomi.fi", applicationProperties.getDefaultSuomiCodeServerAPI(), endpointServices, codeSchemeManager);
            codeServer.updateCodes(uri, force);
        } else if (uri.startsWith("https://virkailija.opintopolku.fi")) {
            OPHCodeServer codeServer = new OPHCodeServer("https://virkailija.opintopolku.fi/koodisto-service/rest/json/", endpointServices);
            if (!codeServer.containsCodeList(uri)) {
                codeServer.updateCodes(uri);
            }
        } else {
            return jerseyResponseManager.invalidParameter();
        }

        Model codeModel = codeSchemeManager.getSchemeGraph(uri);

        // If codeValues are empty for example are codes are DRAFT but scheme is VALID
        if (codeModel == null) {
            codeModel = ModelFactory.createDefaultModel();
        }

        return jerseyResponseManager.okModel(codeModel);

    }

    @PUT
    @Operation(description = "Get code values with id", tags = { "Codes" })
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "codes"),
        @ApiResponse(responseCode = "406", description = "code not defined"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public Response updateCodes(
        @Parameter(description = "uri", required = true)
        @QueryParam("uri") String uri) {

        ResponseBuilder rb;

        if (uri.startsWith("http://uri.suomi.fi")) {
            SuomiCodeServer codeServer = new SuomiCodeServer("https://koodistot.suomi.fi", applicationProperties.getDefaultSuomiCodeServerAPI(), endpointServices, codeSchemeManager);
        } else if (uri.startsWith("https://virkailija.opintopolku.fi")) {
            OPHCodeServer codeServer = new OPHCodeServer("https://virkailija.opintopolku.fi/koodisto-service/rest/json/", endpointServices);
            codeServer.updateCodes(uri);
        } else {
            return jerseyResponseManager.invalidParameter();
        }

        return jerseyResponseManager.ok();
    }
}
