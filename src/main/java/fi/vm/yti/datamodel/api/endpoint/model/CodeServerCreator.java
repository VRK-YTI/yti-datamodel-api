/*
 * Licensed under the European Union Public Licence (EUPL) V.1.1
 */
package fi.vm.yti.datamodel.api.endpoint.model;

import fi.vm.yti.datamodel.api.service.EndpointServices;
import fi.vm.yti.datamodel.api.service.IDManager;
import fi.vm.yti.datamodel.api.service.JerseyClient;
import fi.vm.yti.datamodel.api.service.JerseyResponseManager;
import fi.vm.yti.datamodel.api.utils.LDHelper;
import io.swagger.annotations.*;
import org.apache.jena.query.ParameterizedSparqlString;
import org.apache.jena.rdf.model.ResourceFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;

@Component
@Path("coreServerCreator")
@Api(tags = {"Codes"}, description = "Create new reference to code server")
public class CodeServerCreator {

    private final EndpointServices endpointServices;
    private final JerseyResponseManager jerseyResponseManager;
    private final JerseyClient jerseyClient;
    private final IDManager idManager;

    @Autowired
    CodeServerCreator(EndpointServices endpointServices,
                      JerseyResponseManager jerseyResponseManager,
                      JerseyClient jerseyClient,
                      IDManager idManager) {

        this.endpointServices = endpointServices;
        this.jerseyResponseManager = jerseyResponseManager;
        this.jerseyClient = jerseyClient;
        this.idManager = idManager;
    }

    @GET
    @Produces("application/ld+json")
    @ApiOperation(value = "Create new value scheme", notes = "Creates value scheme object")
    @ApiResponses(value = { @ApiResponse(code = 200, message = "New class is created"),
            @ApiResponse(code = 400, message = "Invalid ID supplied"),
            @ApiResponse(code = 403, message = "Invalid IRI in parameter"),
            @ApiResponse(code = 404, message = "Service not found"),
            @ApiResponse(code = 401, message = "No right to create new")})
    public Response newValueScheme(
            @ApiParam(value = "Code server api uri", required = true) @QueryParam("uri") String uri,
            @ApiParam(value = "Code server name", required = true) @QueryParam("label") String label,
            @ApiParam(value = "Code server description", required = true) @QueryParam("description") String comment,
            @ApiParam(value = "Initial language", required = true, allowableValues="fi,en") @QueryParam("lang") String lang,
            @ApiParam(value = "Update codelists", defaultValue = "false") @QueryParam("update") boolean force) {

        if(uri!=null && !uri.equals("undefined")) {

            if(idManager.isInvalid(uri)) {
                return jerseyResponseManager.invalidIRI();
            }

        } else {
            return jerseyResponseManager.invalidParameter();
        }


        String queryString;
        ParameterizedSparqlString pss = new ParameterizedSparqlString();
        pss.setNsPrefixes(LDHelper.PREFIX_MAP);
        queryString = "CONSTRUCT  { "
                + "?iri a ?type . "
                + "?iri dcterms:title ?label . "
                + "?iri dcterms:description ?description . "
                + "?iri dcterms:identifier ?uuid . "
                + "} WHERE { "
                + "BIND(UUID() as ?uuid) "
                + " }";

        pss.setLiteral("label", ResourceFactory.createLangLiteral(label, lang));
        pss.setLiteral("description", ResourceFactory.createLangLiteral(comment, lang));
        pss.setIri("iri",uri);

        pss.setCommandText(queryString);

        return jerseyClient.constructGraphFromService(pss.toString(), endpointServices.getTempConceptReadSparqlAddress());

    }
}
