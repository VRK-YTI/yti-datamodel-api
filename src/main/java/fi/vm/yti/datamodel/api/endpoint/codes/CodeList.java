/*
 * Licensed under the European Union Public Licence (EUPL) V.1.1
 */
package fi.vm.yti.datamodel.api.endpoint.codes;

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
   /*
        ParameterizedSparqlString pss = new ParameterizedSparqlString();
        pss.setNsPrefixes(LDHelper.PREFIX_MAP);
        
        String queryString = "CONSTRUCT { "
                + "?codeGroup dcterms:hasPart ?codeList . "
                + "?codeGroup dcterms:title ?groupTitle . "
                + "?codeGroup a ?groupType . "
                + "?codeList dcterms:identifier ?id . "
                + "?codeList dcterms:title ?title . "
                + "?codeList dcterms:creator ?creator . "
                + "?codeList a ?codeType . "
                + "} WHERE { "
                + "GRAPH ?codeServer {"
                + "?codeGroup dcterms:hasPart ?codeList . "
                + "?codeGroup dcterms:title ?groupTitle . "
                + "?codeGroup a ?groupType . "
                + "?codeList dcterms:identifier ?id . "
                + "?codeList dcterms:title ?title . "
                + "OPTIONAL {?codeList dcterms:creator ?creator . }"
                + "?codeList a ?codeType . "
                + "}}"; 
        
        pss.setIri("codeServer", uri);
        pss.setCommandText(queryString);

        return JerseyFusekiClient.constructGraphFromService(pss.toString(), services.getSchemesSparqlAddress()); */

        /* Use graph protocol instead for now ... */
        return jerseyClient.getGraphResponseFromService(uri, endpointServices.getSchemesReadAddress());
    }
}
