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

import org.apache.jena.iri.IRI;
import org.apache.jena.iri.IRIException;
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
@Path("v1/codeListCreator")
@Tag(name = "Codes" )
public class CodeListCreator {

    private final IDManager idManager;
    private final JerseyResponseManager jerseyResponseManager;
    private final JerseyClient jerseyClient;
    private final EndpointServices endpointServices;

    @Autowired
    CodeListCreator(IDManager idManager,
                    JerseyResponseManager jerseyResponseManager,
                    JerseyClient jerseyClient,
                    EndpointServices endpointServices) {

        this.idManager = idManager;
        this.jerseyResponseManager = jerseyResponseManager;
        this.jerseyClient = jerseyClient;
        this.endpointServices = endpointServices;
    }

    @GET
    @Produces("application/ld+json")
    @Operation(description = "Create new code list")
    @ApiResponses(value = { @ApiResponse(responseCode = "200", description = "New list is created"),
        @ApiResponse(responseCode = "400", description = "Invalid ID supplied"),
        @ApiResponse(responseCode = "403", description = "Invalid IRI in parameter"),
        @ApiResponse(responseCode = "404", description = "Service not found"),
        @ApiResponse(responseCode = "401", description = "No right to create new") })
    public Response newCodeList(
        @Parameter(description = "Codelist uri", required = true) @QueryParam("uri") String uri,
        @Parameter(description = "Codelist name", required = true) @QueryParam("label") String label,
        @Parameter(description = "Codelist description", required = true) @QueryParam("description") String comment,
        @Parameter(description = "Initial language", required = true, schema = @Schema(allowableValues = {"fi","en"})) @QueryParam("lang") String lang) {

        IRI codeListIRI;

        if (uri != null && !uri.equals("undefined")) {
            try {
                codeListIRI = idManager.constructIRI(uri.toLowerCase());
            } catch (IRIException e) {
                return jerseyResponseManager.invalidIRI();
            }
        } else {
            return jerseyResponseManager.invalidParameter();
        }

        String queryString;
        ParameterizedSparqlString pss = new ParameterizedSparqlString();
        pss.setNsPrefixes(LDHelper.PREFIX_MAP);
        queryString = "CONSTRUCT  { "
            + "?iri a dcam:VocabularyEncodingScheme . "
            + "?iri dcterms:title ?label . "
            + "?iri dcterms:description ?description . "
            + "?iri dcterms:identifier ?uuid . "
            + "?iri dcterms:isPartOf iow:OtherCodeGroup . "
            + "iow:OtherCodeGroup dcterms:title 'Muut luokitukset'@fi . "
            + "iow:OtherCodeGroup dcterms:title 'Reference data'@en . "
            + "} WHERE { "
            + "BIND(UUID() as ?uuid) "
            + " }";

        pss.setLiteral("label", ResourceFactory.createLangLiteral(label, lang));
        pss.setLiteral("description", ResourceFactory.createLangLiteral(comment, lang));
        pss.setIri("iri", codeListIRI);

        pss.setCommandText(queryString);

        return jerseyClient.constructNonEmptyGraphFromService(pss.toString(), endpointServices.getSchemesSparqlAddress());

    }
}
