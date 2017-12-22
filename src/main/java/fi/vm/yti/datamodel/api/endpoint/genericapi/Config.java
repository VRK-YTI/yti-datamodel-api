/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package fi.vm.yti.datamodel.api.endpoint.genericapi;

import fi.vm.yti.datamodel.api.utils.JerseyResponseManager;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.Context;
import javax.ws.rs.Path;
import javax.ws.rs.GET;
import javax.ws.rs.core.Response;

/**
 * REST Web Service
 *
 * @author malonen
 */
@Path("config")
@Api(tags = {"Admin"}, description = "Get API config")
public class Config {

    @Context ServletContext context;
    @GET
    @ApiOperation(value = "Returns API config", notes = "Returns API config")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "OK")
    })
    public Response json(@Context HttpServletRequest request) {

        return JerseyResponseManager.config();

    }

}
