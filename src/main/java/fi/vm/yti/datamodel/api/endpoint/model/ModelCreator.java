/*
 * Licensed under the European Union Public Licence (EUPL) V.1.1
 */
package fi.vm.yti.datamodel.api.endpoint.model;

import fi.vm.yti.datamodel.api.config.ApplicationProperties;
import fi.vm.yti.datamodel.api.model.DataModel;
import fi.vm.yti.datamodel.api.model.ServiceCategory;
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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component
@Path("v1/modelCreator")
@Tag(name = "Model" )
public class ModelCreator {

    private static final Logger logger = LoggerFactory.getLogger(ModelCreator.class.getName());

    private final JerseyResponseManager jerseyResponseManager;
    private final GraphManager graphManager;
    private final RHPOrganizationManager rhpOrganizationManager;
    private final JerseyClient jerseyClient;
    private final IDManager idManager;
    private final EndpointServices endpointServices;
    private final ApplicationProperties applicationProperties;

    @Autowired
    ModelCreator(JerseyResponseManager jerseyResponseManager,
                 GraphManager graphManager,
                 RHPOrganizationManager rhpOrganizationManager,
                 JerseyClient jerseyClient,
                 IDManager idManager,
                 EndpointServices endpointServices,
                 ApplicationProperties applicationProperties) {

        this.jerseyResponseManager = jerseyResponseManager;
        this.graphManager = graphManager;
        this.rhpOrganizationManager = rhpOrganizationManager;
        this.jerseyClient = jerseyClient;
        this.idManager = idManager;
        this.endpointServices = endpointServices;
        this.applicationProperties = applicationProperties;
    }

    @GET
    @Produces("application/ld+json")
    @Operation(description = "Create new model")
    @ApiResponses(value = { @ApiResponse(responseCode = "200", description = "New class is created"),
        @ApiResponse(responseCode = "400", description = "Invalid ID supplied"),
        @ApiResponse(responseCode = "403", description = "Invalid IRI in parameter"),
        @ApiResponse(responseCode = "404", description = "Service not found"),
        @ApiResponse(responseCode = "401", description = "No right to create new") })
    public Response newModel(
        @Parameter(description = "Redirection service", required = false) @QueryParam("redirect") String redirect,
        @Parameter(description = "Model prefix", required = true) @QueryParam("prefix") String prefix,
        @Parameter(description = "Model label", required = true) @QueryParam("label") String label,
        @Parameter(description = "Organization UUIDs", required = true) @QueryParam("orgList") String orgString,
        @Parameter(description = "Service URIs", required = true) @QueryParam("serviceList") String servicesString,
        @Parameter(description = "Label language", required = true, schema = @Schema(allowableValues = "fi,en")) @QueryParam("lang") String lang,
        @Parameter(description = "Allowed languages as space list: 'en sv pl'. Default 'fi en'") @QueryParam("langList") String allowedLang) {

        List<String> serviceList = Arrays.asList(servicesString.split(" "));

        String[] orgs = orgString.split(" ");
        List<UUID> orgList = new ArrayList<>();

        for (int i = 0; i < orgs.length; i++) {
            orgList.add(UUID.fromString(orgs[i]));
        }

        if (!ServiceCategory.containsAll(serviceList)) {
            logger.info("no services");
            return jerseyResponseManager.invalidParameter();
        }

        if (allowedLang == null || allowedLang.equals("undefined") || allowedLang.length() == 0) {
            allowedLang = "fi";
        }

        prefix = LDHelper.modelName(prefix);

        if (LDHelper.isReservedWord(prefix) || graphManager.isExistingPrefix(prefix)) {
            return jerseyResponseManager.usedIRI();
        }

        if (!rhpOrganizationManager.isExistingOrganization(orgList)) {
            logger.info("no org");
            return jerseyResponseManager.invalidParameter();
        }

        String namespace = applicationProperties.getDefaultNamespace() + prefix;

        IRI namespaceIRI;

        try {
            if (redirect != null && !redirect.equals("undefined")) {
                if (redirect.endsWith("/")) {
                    namespaceIRI = idManager.constructIRI(redirect);
                } else if (redirect.endsWith("#")) {
                    redirect = redirect.substring(0, redirect.length() - 1);
                    namespaceIRI = idManager.constructIRI(redirect);
                } else {
                    namespaceIRI = idManager.constructIRI(redirect);
                }
            } else {
                namespaceIRI = idManager.constructIRI(namespace);
            }
        } catch (IRIException e) {
            logger.warn("INVALID: " + namespace);
            return jerseyResponseManager.invalidIRI();
        } catch (NullPointerException e) {
            return jerseyResponseManager.invalidParameter();
        }

        try {
            DataModel newModel = new DataModel(prefix, namespaceIRI, label, lang, allowedLang, serviceList, orgList, graphManager, endpointServices);
            return jerseyClient.constructResponseFromGraph(newModel.asGraph());
        } catch (IllegalArgumentException ex) {
            return jerseyResponseManager.invalidParameter();
        }
    }
}
