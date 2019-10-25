/*
 * Licensed under the European Union Public Licence (EUPL) V.1.1
 */
package fi.vm.yti.datamodel.api.endpoint.profile;

import fi.vm.yti.datamodel.api.config.ApplicationProperties;
import fi.vm.yti.datamodel.api.model.ApplicationProfile;
import fi.vm.yti.datamodel.api.model.ServiceCategory;
import fi.vm.yti.datamodel.api.service.*;
import fi.vm.yti.datamodel.api.utils.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

import org.apache.jena.iri.IRI;
import org.apache.jena.iri.IRIException;
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
@Path("v1/profileCreator")
@Tag(name = "Profile" )
public class ProfileCreator {

    private static final Logger logger = LoggerFactory.getLogger(ProfileCreator.class.getName());
    private final String defaultNamespace;
    private final JerseyResponseManager jerseyResponseManager;
    private final GraphManager graphManager;
    private final RHPOrganizationManager rhpOrganizationManager;
    private final IDManager idManager;
    private final JerseyClient jerseyClient;
    private final EndpointServices endpointServices;

    ProfileCreator(ApplicationProperties properties,
                   JerseyResponseManager jerseyResponseManager,
                   GraphManager graphManager,
                   RHPOrganizationManager rhpOrganizationManager,
                   IDManager idManager,
                   JerseyClient jerseyClient,
                   EndpointServices endpointServices) {

        this.defaultNamespace = properties.getDefaultNamespace();
        this.jerseyResponseManager = jerseyResponseManager;
        this.graphManager = graphManager;
        this.rhpOrganizationManager = rhpOrganizationManager;
        this.idManager = idManager;
        this.jerseyClient = jerseyClient;
        this.endpointServices = endpointServices;
    }

    @GET
    @Produces("application/ld+json")
    @Operation(description = "Create new profile")
    @ApiResponses(value = { @ApiResponse(responseCode = "200", description = "New profile is created"),
        @ApiResponse(responseCode = "400", description = "Invalid ID supplied"),
        @ApiResponse(responseCode = "403", description = "Invalid IRI in parameter"),
        @ApiResponse(responseCode = "404", description = "Service not found"),
        @ApiResponse(responseCode = "401", description = "No right to create new") })
    public Response newProfile(
        @Parameter(description = "Redirection service", required = false) @QueryParam("redirect") String redirect,
        @Parameter(description = "Model prefix", required = true) @QueryParam("prefix") String prefix,
        @Parameter(description = "Model label", required = true) @QueryParam("label") String label,
        @Parameter(description = "Organization UUIDs", required = true)
        @QueryParam("orgList") String orgString,
        @Parameter(description = "Service URIs", required = true)
        @QueryParam("serviceList") String servicesString,
        @Parameter(description = "Label language", required = true, schema = @Schema(allowableValues = "fi,en")) @QueryParam("lang") String lang,
        @Parameter(description = "Allowed languages as space list: 'en sv pl'. Default 'fi en'") @QueryParam("langList") String allowedLang) {

        List<String> serviceList = Arrays.asList(servicesString.split(" "));

        String[] orgs = orgString.split(" ");
        List<UUID> orgList = new ArrayList<>();

        for (int i = 0; i < orgs.length; i++) {
            orgList.add(UUID.fromString(orgs[i]));
        }

        if (!ServiceCategory.containsAll(serviceList)) {
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
            return jerseyResponseManager.invalidParameter();
        }

        String namespace = defaultNamespace + prefix;

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

        ApplicationProfile newModel = new ApplicationProfile(prefix, namespaceIRI, label, lang, allowedLang, serviceList, orgList, graphManager, endpointServices);

        return jerseyClient.constructResponseFromGraph(newModel.asGraph());
    }
}
