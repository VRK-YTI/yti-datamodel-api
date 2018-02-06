/*
 * Licensed under the European Union Public Licence (EUPL) V.1.1 
 */
package fi.vm.yti.datamodel.api.endpoint.profile;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.logging.Logger;
import javax.servlet.ServletContext;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;

import fi.vm.yti.datamodel.api.model.ApplicationProfile;
import fi.vm.yti.datamodel.api.model.ServiceCategory;
import fi.vm.yti.datamodel.api.utils.*;
import fi.vm.yti.datamodel.api.config.ApplicationProperties;
import fi.vm.yti.datamodel.api.config.EndpointServices;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import org.apache.jena.iri.IRI;
import org.apache.jena.iri.IRIException;
 
/**
 * Root resource (exposed at "modelCreator" path)
 */
@Path("profileCreator")
@Api(tags = {"Profile"}, description = "Construct new profile template")
public class ProfileCreator {

    @Context ServletContext context;
    EndpointServices services = new EndpointServices();
     private static final Logger logger = Logger.getLogger(ProfileCreator.class.getName());
    
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
                return JerseyResponseManager.invalidParameter();
            }

            if(allowedLang==null || allowedLang.equals("undefined") || allowedLang.length()==0) {
                allowedLang = "fi";
            }

            prefix = LDHelper.modelName(prefix);

            if(GraphManager.isExistingPrefix(prefix)) {
                return JerseyResponseManager.usedIRI();
            }

            if(!RHPOrganizationManager.isExistingOrganization(orgList)) {
                return JerseyResponseManager.invalidParameter();
            }

            String namespace = ApplicationProperties.getDefaultNamespace()+prefix;

            IRI namespaceIRI;

            try {
                if(redirect!=null && !redirect.equals("undefined")) {
                    if(redirect.endsWith("/")) {
                        namespaceIRI = IDManager.constructIRI(redirect);
                    } else if(redirect.endsWith("#")){
                        redirect=redirect.substring(0, redirect.length()-1);
                        namespaceIRI = IDManager.constructIRI(redirect);
                    } else {
                        namespaceIRI = IDManager.constructIRI(redirect);
                    }
                } else {
                    namespaceIRI = IDManager.constructIRI(namespace);
                }
            } catch (IRIException e) {
                logger.warning("INVALID: "+namespace);
                return JerseyResponseManager.invalidIRI();
            } catch (NullPointerException e) {
                return JerseyResponseManager.invalidParameter();
            }

            ApplicationProfile newModel = new ApplicationProfile(prefix, namespaceIRI, label, lang, allowedLang, serviceList, orgList);

            return JerseyClient.constructResponseFromGraph(newModel.asGraph());
    }   
 
}
