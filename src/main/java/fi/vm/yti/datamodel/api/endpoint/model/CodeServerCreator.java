/*
 * Licensed under the European Union Public Licence (EUPL) V.1.1
 */
package fi.vm.yti.datamodel.api.endpoint.model;

import fi.vm.yti.datamodel.api.service.EndpointServices;
import fi.vm.yti.datamodel.api.service.IDManager;
import fi.vm.yti.datamodel.api.service.JerseyClient;
import fi.vm.yti.datamodel.api.service.JerseyResponseManager;
import fi.vm.yti.datamodel.api.utils.LDHelper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

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
@Path("v1/coreServerCreator")
@Tag(name = "Codes" )
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
    @Operation(description = "Create new code server object")
    @ApiResponses(value = { @ApiResponse(responseCode = "200", description = "New class is created"),
        @ApiResponse(responseCode = "400", description = "Invalid ID supplied"),
        @ApiResponse(responseCode = "403", description = "Invalid IRI in parameter"),
        @ApiResponse(responseCode = "404", description = "Service not found"),
        @ApiResponse(responseCode = "401", description = "No right to create new") })
    public Response createCodeServer(
        @Parameter(description = "Code server api uri", required = true) @QueryParam("uri") String uri,
        @Parameter(description = "Code server name", required = true) @QueryParam("label") String label,
        @Parameter(description = "Code server description", required = true) @QueryParam("description") String comment,
        @Parameter(description = "Initial language", required = true, schema = @Schema(allowableValues = "fi,en")) @QueryParam("lang") String lang,
        @Parameter(description = "Update codelists", schema = @Schema(defaultValue = "false")) @QueryParam("update") boolean force) {

        if (uri != null && !uri.equals("undefined")) {

            if ((!uri.startsWith("https://koodistot.suomi.fi") || !uri.startsWith("https://virkailija.opintopolku.fi")) && !idManager.isInvalid(uri)) {
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
        pss.setIri("iri", uri);

        pss.setCommandText(queryString);

        return jerseyClient.constructGraphFromService(pss.toString(), endpointServices.getTempConceptReadSparqlAddress());

    }
}
