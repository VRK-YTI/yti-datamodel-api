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
@Path("codeListCreator")
@Api(tags = { "Codes" }, description = "Create reusable code list that is not resolved")
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
    @ApiOperation(value = "Create new code list", notes = "Creates new code list")
    @ApiResponses(value = { @ApiResponse(code = 200, message = "New list is created"),
        @ApiResponse(code = 400, message = "Invalid ID supplied"),
        @ApiResponse(code = 403, message = "Invalid IRI in parameter"),
        @ApiResponse(code = 404, message = "Service not found"),
        @ApiResponse(code = 401, message = "No right to create new") })
    public Response newValueScheme(
        @ApiParam(value = "Codelist uri", required = true) @QueryParam("uri") String uri,
        @ApiParam(value = "Codelist name", required = true) @QueryParam("label") String label,
        @ApiParam(value = "Codelist description", required = true) @QueryParam("description") String comment,
        @ApiParam(value = "Initial language", required = true, allowableValues = "fi,en") @QueryParam("lang") String lang) {

        IRI codeListIRI = null;

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
