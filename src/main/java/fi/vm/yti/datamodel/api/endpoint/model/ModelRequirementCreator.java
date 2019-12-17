/*
 * Licensed under the European Union Public Licence (EUPL) V.1.1
 */
package fi.vm.yti.datamodel.api.endpoint.model;

import java.util.Map;

import fi.vm.yti.datamodel.api.service.*;
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
import org.apache.jena.vocabulary.DCTerms;
import org.apache.jena.vocabulary.RDFS;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;

@Component
@Path("v1/modelRequirementCreator")
@Tag(name = "Model")
public class ModelRequirementCreator {

    private final EndpointServices endpointServices;
    private final IDManager idManager;
    private final JerseyResponseManager jerseyResponseManager;
    private final GraphManager graphManager;
    private final NamespaceManager namespaceManager;
    private final JerseyClient jerseyClient;

    @Autowired
    ModelRequirementCreator(EndpointServices endpointServices,
                            IDManager idManager,
                            JerseyResponseManager jerseyResponseManager,
                            GraphManager graphManager,
                            NamespaceManager namespaceManager,
                            JerseyClient jerseyClient) {
        this.endpointServices = endpointServices;
        this.idManager = idManager;
        this.jerseyResponseManager = jerseyResponseManager;
        this.graphManager = graphManager;
        this.namespaceManager = namespaceManager;
        this.jerseyClient = jerseyClient;
    }

    @GET
    @Produces("application/ld+json")
    @Operation(description = "Create namespace object. Namespace must be valid URI and end with # or /.")
    @ApiResponses(value = { @ApiResponse(responseCode = "200", description = "New class is created"),
        @ApiResponse(responseCode = "400", description = "Invalid ID supplied"),
        @ApiResponse(responseCode = "403", description = "Invalid IRI in parameter"),
        @ApiResponse(responseCode = "404", description = "Service not found"),
        @ApiResponse(responseCode = "401", description = "No right to create new") })
    public Response newRequiredModel(
        @Parameter(description = "Model namespace", required = true) @QueryParam("namespace") String namespace,
        @Parameter(description = "Model prefix", required = true) @QueryParam("prefix") String prefix,
        @Parameter(description = "Model label", required = true) @QueryParam("label") String label,
        @Parameter(description = "Initial language", required = true, schema = @Schema(allowableValues = {"fi","en"})) @QueryParam("lang") String lang) {

        if (namespace == null || namespace.isEmpty() || (namespace.startsWith("http") && !(namespace.endsWith("#") || namespace.endsWith("/")) )) return jerseyResponseManager.invalidIRI();

        IRI namespaceIRI;

        try {
            namespaceIRI = idManager.constructIRI(namespace);
        } catch (IRIException e) {
            return jerseyResponseManager.invalidIRI();
        }

        boolean isResolvedNamespace = true;
        boolean isLocalNamespace = true;

        if (!graphManager.isExistingServiceGraph(namespace)) {
            isResolvedNamespace = namespaceManager.resolveNamespace(namespace, null, false);
            isLocalNamespace = false;
        }

        String queryString;
        ParameterizedSparqlString pss = new ParameterizedSparqlString();
        pss.setNsPrefixes(LDHelper.PREFIX_MAP);
        queryString = "CONSTRUCT  { "
            + "?g a ?type . "
            + "?g rdfs:label ?label . "
            + "?g dcap:preferredXMLNamespaceName ?namespace . "
            + "?g dcap:preferredXMLNamespacePrefix ?prefix . "
            + (isLocalNamespace ? "" : "?g iow:isResolved ?resolved . ")
            + "} WHERE { }";

        String type = RDFS.Resource.getURI();

        if(LDHelper.PREFIX_MAP.containsKey(prefix)) {
            namespace = LDHelper.PREFIX_MAP.get(prefix);
            type = DCTerms.Standard.getURI();
        } else if (LDHelper.PREFIX_MAP.containsValue(namespace)) {
            final String nsFinal = namespace;
            type = DCTerms.Standard.getURI();
            prefix = LDHelper.PREFIX_MAP.entrySet().stream().filter(o->o.getValue().equals(nsFinal)).map(Map.Entry::getKey).findFirst().get();
        }

        pss.setCommandText(queryString);
        pss.setIri("g", namespace);
        pss.setLiteral("label", ResourceFactory.createLangLiteral(label, lang));
        pss.setIri("type",type);
        pss.setLiteral("namespace", namespace);
        pss.setLiteral("prefix", prefix);
        if (!isLocalNamespace) pss.setLiteral("resolved", isResolvedNamespace);

        return jerseyClient.constructNonEmptyGraphFromService(pss.toString(), endpointServices.getTempConceptReadSparqlAddress());
    }
}
