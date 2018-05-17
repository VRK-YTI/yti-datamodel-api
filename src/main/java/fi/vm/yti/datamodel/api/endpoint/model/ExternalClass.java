/*
 * Licensed under the European Union Public Licence (EUPL) V.1.1
 */
package fi.vm.yti.datamodel.api.endpoint.model;

import fi.vm.yti.datamodel.api.service.EndpointServices;
import fi.vm.yti.datamodel.api.service.IDManager;
import fi.vm.yti.datamodel.api.service.JerseyClient;
import fi.vm.yti.datamodel.api.service.JerseyResponseManager;
import fi.vm.yti.datamodel.api.utils.LDHelper;
import fi.vm.yti.datamodel.api.utils.QueryLibrary;
import io.swagger.annotations.*;
import org.apache.jena.iri.IRI;
import org.apache.jena.iri.IRIException;
import org.apache.jena.query.ParameterizedSparqlString;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;
import org.slf4j.Logger;import org.slf4j.LoggerFactory;

@Component
@Path("externalClass")
@Api(tags = {"Class"}, description = "External class operations")
public class ExternalClass {

    private static final Logger logger = LoggerFactory.getLogger(ExternalClass.class.getName());
    private final JerseyResponseManager jerseyResponseManager;
    private final IDManager idManager;
    private final EndpointServices endpointServices;
    private final JerseyClient jerseyClient;

    @Autowired
    ExternalClass(JerseyResponseManager jerseyResponseManager,
                  IDManager idManager,
                  EndpointServices endpointServices,
                  JerseyClient jerseyClient) {

        this.jerseyResponseManager = jerseyResponseManager;
        this.idManager = idManager;
        this.endpointServices = endpointServices;
        this.jerseyClient = jerseyClient;
    }

    @GET
    @Produces("application/ld+json")
    @ApiOperation(value = "Get external class from requires", notes = "Get class in JSON-LD")
    @ApiResponses(value = {
            @ApiResponse(code = 404, message = "No such resource"),
            @ApiResponse(code = 400, message = "Invalid model supplied"),
            @ApiResponse(code = 404, message = "Service not found"),
            @ApiResponse(code = 500, message = "Internal server error")
    })
    public Response json(
            @ApiParam(value = "Class id")
            @QueryParam("id") String id,
            @ApiParam(value = "Model id")
            @QueryParam("model") String model) {

        IRI idIRI;

        /* Check that Model URI is valid */
        if(!idManager.isValidUrl(model)) {
            return jerseyResponseManager.invalidIRI();
        }

        if(id==null || id.equals("undefined") || id.equals("default")) {

            /* If no id is provided create a list of classes */
            ParameterizedSparqlString pss = new ParameterizedSparqlString();
            pss.setNsPrefixes(LDHelper.PREFIX_MAP);

            String queryString = "CONSTRUCT { "
                    + "?externalModel rdfs:label ?externalModelLabel . "
                    + "?class rdfs:isDefinedBy ?externalModel . "
                    + "?class sh:name ?label . "
                    + "?class sh:description ?comment . "
                    + "?class a rdfs:Class . "
                    + "?class dcterms:modified ?modified . "
                    + "} WHERE { "
                    + "SERVICE ?modelService { "
                    + "GRAPH ?library { "
                    + "?library dcterms:requires ?externalModel . "
                    + "?externalModel rdfs:label ?externalModelLabel . "
                    + "}}"
                    + "GRAPH ?externalModel { "
                    + "?class a ?type . "
                    + "VALUES ?type { rdfs:Class owl:Class sh:NodeShape sh:Shape } "
                    /* GET LABEL */
                    + "{ ?class ?labelPred ?labelStr . "
                    + "VALUES ?labelPred { rdfs:label sh:name dc:title dcterms:title }"                    
                    + "FILTER(LANG(?labelStr) = '') BIND(STRLANG(STR(?labelStr),'en') as ?label) }"
                    + "UNION"
                    + "{ ?class ?labelPred ?label . "
                    + "VALUES ?commentPred { rdfs:label sh:name dc:title dcterms:title }"
                    + " FILTER(LANG(?comment)!='') }"
                    /* GET COMMENT */
                    + "{ ?class ?commentPred ?commentStr . "
                    + "VALUES ?commentPred { rdfs:comment skos:definition dcterms:description dc:description prov:definition sh:description }"
                    + "FILTER(LANG(?commentStr) = '') BIND(STRLANG(STR(?commentStr),'en') as ?comment) }"
                    + "UNION"
                    + "{ ?class ?commentPred ?comment . "
                    + "VALUES ?commentPred { rdfs:comment skos:definition dcterms:description dc:description prov:definition sh:description }"
                    + " FILTER(LANG(?comment)!='') }"
                    + "} "

                    + "}";


            pss.setIri("library", model);
            pss.setIri("modelService", endpointServices.getLocalhostCoreSparqlAddress());


            pss.setCommandText(queryString);


            return jerseyClient.constructGraphFromService(pss.toString(), endpointServices.getImportsSparqlAddress());

        } else {

            try {
                idIRI = idManager.constructIRI(id);
            }
            catch (IRIException e) {
                return jerseyResponseManager.invalidIRI();
            }

            String sparqlService = endpointServices.getImportsSparqlAddress();

            ParameterizedSparqlString pss = new ParameterizedSparqlString();

            pss.setNsPrefixes(LDHelper.PREFIX_MAP);

            /* TODO: FIX dublin core etc. rdf:Property properties */

            logger.info("Using ext query:");

            String queryString = QueryLibrary.externalClassQuery;


            logger.info(queryString);

            pss.setIri("library", model);
            pss.setIri("modelService", endpointServices.getLocalhostCoreSparqlAddress());
            pss.setCommandText(queryString);
            pss.setIri("classIRI", idIRI);


            if(!model.equals("undefined")) {
                pss.setIri("library", model);
            }
            return jerseyClient.constructGraphFromService(pss.toString(), sparqlService);

        }
    }
}
