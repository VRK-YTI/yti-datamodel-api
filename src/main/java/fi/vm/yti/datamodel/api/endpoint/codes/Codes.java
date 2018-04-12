/*
 * Licensed under the European Union Public Licence (EUPL) V.1.1
 */
package fi.vm.yti.datamodel.api.endpoint.codes;

import fi.vm.yti.datamodel.api.model.SuomiCodeServer;
import fi.vm.yti.datamodel.api.service.EndpointServices;
import fi.vm.yti.datamodel.api.service.JerseyClient;
import fi.vm.yti.datamodel.api.service.JerseyResponseManager;
import fi.vm.yti.datamodel.api.model.OPHCodeServer;
import io.swagger.annotations.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.ws.rs.*;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;

@Component
@Path("codeValues")
@Api(tags = {"Codes"}, description = "Get codevalues with ID")
public class Codes {

    private final JerseyClient jerseyClient;
    private final EndpointServices endpointServices;
    private final JerseyResponseManager jerseyResponseManager;

    @Autowired
    Codes(JerseyClient jerseyClient,
          EndpointServices endpointServices,
          JerseyResponseManager jerseyResponseManager) {
        this.jerseyClient = jerseyClient;
        this.endpointServices = endpointServices;
        this.jerseyResponseManager = jerseyResponseManager;
    }

    @GET
    @Produces("application/ld+json")
    @ApiOperation(value = "Get code values with id", notes = "codes will be loaded to TEMP database when used the first time")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "codes"),
            @ApiResponse(code = 406, message = "code not defined"),
            @ApiResponse(code = 500, message = "Internal server error")
    })
    public Response code(
            @ApiParam(value = "uri", required = true)
            @QueryParam("uri") String uri) {

        if(uri.startsWith("http://uri.suomi.fi")) {
            SuomiCodeServer codeServer = new SuomiCodeServer("https://koodistot.suomi.fi","https://koodistot-dev.suomi.fi/codelist-api/api/v1/", endpointServices);
            // TODO: Update codes from suomi.fi codeservice? Related to YTI-148.
            // codeServer.updateCodes(uri);
        } else {
            OPHCodeServer codeServer = new OPHCodeServer("https://virkailija.opintopolku.fi/koodisto-service/rest/json/", endpointServices);
            if(!codeServer.containsCodeList(uri)) {
                codeServer.updateCodes(uri);
            }
        }

        return jerseyClient.getGraphResponseFromService(uri, endpointServices.getSchemesReadAddress());
    }

    @PUT
    @ApiOperation(value = "Get code values with id", notes = "Update certain code to temp database")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "codes"),
            @ApiResponse(code = 406, message = "code not defined"),
            @ApiResponse(code = 500, message = "Internal server error")
    })
    public Response updateCodes(
            @ApiParam(value = "uri", required = true)
            @QueryParam("uri") String uri) {

        ResponseBuilder rb;

        if(uri.startsWith("http://uri.suomi.fi")) {
            SuomiCodeServer codeServer = new SuomiCodeServer("https://koodistot.suomi.fi","https://koodistot-dev.suomi.fi/codelist-api/api/v1/", endpointServices);
            // TODO: Update codes from suomi.fi codeservice on fly? Related to YTI-148.
            // codeServer.updateCodes(uri);
        } else {
            OPHCodeServer codeServer = new OPHCodeServer("https://virkailija.opintopolku.fi/koodisto-service/rest/json/", endpointServices);
            codeServer.updateCodes(uri);
        }

        return jerseyResponseManager.ok();
    }
}
