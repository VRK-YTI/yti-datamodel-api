/*
 * Licensed under the European Union Public Licence (EUPL) V.1.1
 */
package fi.vm.yti.datamodel.api.endpoint.codes;

import fi.vm.yti.datamodel.api.model.OPHCodeServer;
import fi.vm.yti.datamodel.api.model.SuomiCodeServer;
import fi.vm.yti.datamodel.api.service.EndpointServices;
import fi.vm.yti.datamodel.api.service.JerseyClient;
import io.swagger.annotations.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;

@Component
@Path("codeList")
@Api(tags = {"Codes"}, description = "Get list of codes from code sercer")
public class CodeList {

    private final JerseyClient jerseyClient;
    private final EndpointServices endpointServices;

    @Autowired
    CodeList(JerseyClient jerseyClient,
             EndpointServices endpointServices) {
        this.jerseyClient = jerseyClient;
        this.endpointServices = endpointServices;
    }

    @GET
    @Produces("application/ld+json")
    @ApiOperation(value = "Get list of codelists from code server", notes = "Groups and codeLists")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Codelists"),
            @ApiResponse(code = 406, message = "Term not defined"),
            @ApiResponse(code = 500, message = "Internal server error")
    })
    public Response codeList(
            @ApiParam(value = "Codeserver uri", required = true) @QueryParam("uri") String uri) {

        if(uri.startsWith("https://koodistot.suomi.fi")) {
            SuomiCodeServer suomiCodeServer = new SuomiCodeServer("https://koodistot.suomi.fi","https://koodistot-dev.suomi.fi/codelist-api/api/v1/", endpointServices);
            suomiCodeServer.updateCodelistsFromServer();
        } else {
            OPHCodeServer codeServer = new OPHCodeServer("https://virkailija.opintopolku.fi/koodisto-service/rest/json/", endpointServices);
            codeServer.updateCodelistsFromServer();
        }

        return jerseyClient.getGraphResponseFromService(uri, endpointServices.getSchemesReadAddress());
    }
}
