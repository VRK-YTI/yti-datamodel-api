/*
 * Licensed under the European Union Public Licence (EUPL) V.1.1
 */
package fi.vm.yti.datamodel.api.endpoint.codes;

import fi.vm.yti.datamodel.api.config.ApplicationProperties;
import fi.vm.yti.datamodel.api.model.SuomiCodeServer;
import fi.vm.yti.datamodel.api.service.*;
import fi.vm.yti.datamodel.api.model.OPHCodeServer;
import io.swagger.annotations.*;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.ws.rs.*;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;

@Component
@Path("codeValues")
@Api(tags = { "Codes" }, description = "Get codevalues with ID")
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
    @ApiOperation(value = "Get code values with id", notes = "codes will be loaded to TEMP database when used the first time")
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "codes"),
        @ApiResponse(code = 406, message = "code not defined"),
        @ApiResponse(code = 500, message = "Internal server error")
    })
    public Response code(
        @ApiParam(value = "uri", required = true)
        @QueryParam("uri") String uri) {

        if (uri.startsWith("http://uri.suomi.fi")) {
            SuomiCodeServer codeServer = new SuomiCodeServer("https://koodistot.suomi.fi", applicationProperties.getDefaultSuomiCodeServerAPI(), endpointServices, codeSchemeManager);
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
