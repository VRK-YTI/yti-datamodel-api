/*
 * Licensed under the European Union Public Licence (EUPL) V.1.1
 */
package fi.vm.yti.datamodel.api.endpoint.profile;

import fi.vm.yti.datamodel.api.config.ApplicationProperties;
import fi.vm.yti.datamodel.api.model.ApplicationProfile;
import fi.vm.yti.datamodel.api.model.ServiceCategory;
import fi.vm.yti.datamodel.api.service.*;
import fi.vm.yti.datamodel.api.utils.*;
import io.swagger.annotations.*;
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
import org.slf4j.Logger;import org.slf4j.LoggerFactory;

@Component
@Path("profileCreator")
@Api(tags = {"Profile"}, description = "Construct new profile template")
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
    @ApiOperation(value = "Create new profile", notes = "Create new profile")
    @ApiResponses(value = { @ApiResponse(code = 200, message = "New profile is created"),
            @ApiResponse(code = 400, message = "Invalid ID supplied"),
            @ApiResponse(code = 403, message = "Invalid IRI in parameter"),
            @ApiResponse(code = 404, message = "Service not found"),
            @ApiResponse(code = 401, message = "No right to create new")})
    public Response newProfile(
            @ApiParam(value = "Redirection service", required = false) @QueryParam("redirect") String redirect,
            @ApiParam(value = "Model prefix", required = true) @QueryParam("prefix") String prefix,
            @ApiParam(value = "Model label", required = true) @QueryParam("label") String label,
            @ApiParam(value = "Organization UUIDs", required = true)
            @QueryParam("orgList") String orgString,
            @ApiParam(value = "Service URIs", required = true)
            @QueryParam("serviceList") String servicesString,
            @ApiParam(value = "Label language", required = true, allowableValues="fi,en") @QueryParam("lang") String lang,
            @ApiParam(value = "Allowed languages as space list: 'en sv pl'. Default 'fi en'") @QueryParam("langList") String allowedLang) {

        List<String> serviceList = Arrays.asList(servicesString.split(" "));

        String[] orgs = orgString.split(" ");
        List<UUID> orgList = new ArrayList<>();

        for(int i = 0; i<orgs.length; i++) {
            orgList.add(UUID.fromString(orgs[i]));
        }

        if(!ServiceCategory.containsAll(serviceList)) {
            return jerseyResponseManager.invalidParameter();
        }

        if(allowedLang==null || allowedLang.equals("undefined") || allowedLang.length()==0) {
            allowedLang = "fi";
        }

        prefix = LDHelper.modelName(prefix);

        if (graphManager.isExistingPrefix(prefix)) {
            return jerseyResponseManager.usedIRI();
        }

        if (!rhpOrganizationManager.isExistingOrganization(orgList)) {
            return jerseyResponseManager.invalidParameter();
        }

        String namespace = defaultNamespace + prefix;

        IRI namespaceIRI;

        try {
            if(redirect!=null && !redirect.equals("undefined")) {
                if(redirect.endsWith("/")) {
                    namespaceIRI = idManager.constructIRI(redirect);
                } else if(redirect.endsWith("#")){
                    redirect=redirect.substring(0, redirect.length()-1);
                    namespaceIRI = idManager.constructIRI(redirect);
                } else {
                    namespaceIRI = idManager.constructIRI(redirect);
                }
            } else {
                namespaceIRI = idManager.constructIRI(namespace);
            }
        } catch (IRIException e) {
            logger.warn("INVALID: "+namespace);
            return jerseyResponseManager.invalidIRI();
        } catch (NullPointerException e) {
            return jerseyResponseManager.invalidParameter();
        }

        ApplicationProfile newModel = new ApplicationProfile(prefix, namespaceIRI, label, lang, allowedLang, serviceList, orgList, graphManager, endpointServices);

        return jerseyClient.constructResponseFromGraph(newModel.asGraph());
    }
}
