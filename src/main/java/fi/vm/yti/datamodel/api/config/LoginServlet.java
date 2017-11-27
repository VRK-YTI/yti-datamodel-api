/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package fi.vm.yti.datamodel.api.config;

import fi.vm.yti.datamodel.api.model.Role;
import fi.vm.yti.datamodel.api.model.YtiUser;
import fi.vm.yti.datamodel.api.utils.UserManager;
import org.apache.http.ssl.SSLContexts;
import org.apache.http.ssl.TrustStrategy;

import javax.net.ssl.SSLContext;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.net.URLDecoder;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import static java.util.Collections.emptyMap;

/**
 *
 * @author malonen
 */
@WebServlet(name = "LoginServlet", urlPatterns = {"/LoginServlet"})
public class LoginServlet extends HttpServlet {

    private static String SHIBBOLETH_PROVIDER_ATTRIBUTE = "Shib-Identity-Provider";
    private static String SHIBBOLETH_MAIL_ATTRIBUTE = "mail";
    private static String SHIBBOLETH_GIVENNAME_ATTRIBUTE = "givenname";
    private static String SHIBBOLETH_SURNAME_ATTRIBUTE = "surname";

    private static final Logger logger = Logger.getLogger(LoginServlet.class.getName());

    /**
     * Processes requests for both HTTP <code>GET</code> and <code>POST</code>
     * methods.
     *
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    protected void processRequest(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        logger.info("Prosessing login");
        HttpSession session = request.getSession();
        boolean debug = ApplicationProperties.getDebugMode();

        logger.info("Session id:"+session.getId());

        if (debug) {
            request.setAttribute(SHIBBOLETH_PROVIDER_ATTRIBUTE, "fake");
            request.setAttribute(SHIBBOLETH_MAIL_ATTRIBUTE, ApplicationProperties.getDebugUserEmail());
            request.setAttribute(SHIBBOLETH_GIVENNAME_ATTRIBUTE, ApplicationProperties.getDebugUserFirstname());
            request.setAttribute(SHIBBOLETH_SURNAME_ATTRIBUTE, ApplicationProperties.getDebugUserLastname());
        }

        if (isLoggedIn(request)) {
                initializeUser(session, getAuthenticatedUser(new ShibbolethAuthenticationDetails(request)));
        } else {
                Logger.getLogger(LoginServlet.class.getName()).log(Level.INFO, "NOT LOGGED IN");
        }

        String target = getParameterAsString(request, "target");

        if(target!=null) {
            String decodedTarget = URLDecoder.decode(target, "ISO-8859-1");

            if(!debug && decodedTarget!=null && decodedTarget.startsWith(ApplicationProperties.getDefaultDomain()))
                target = decodedTarget;
        }
        if(target!=null && (debug || target.startsWith(ApplicationProperties.getDefaultDomain()))) {
            response.sendRedirect(target);
        } else {
            response.sendRedirect(resolveFrontendAddress(debug));
        }

    }

    private static String resolveFrontendAddress(boolean debug) {
        return debug ? ApplicationProperties.getDebugAdress() : "/";
    }

    private static void initializeUser(HttpSession session, YtiUser authenticatedUser) {
        session.setAttribute("authenticatedUser", authenticatedUser);
    }

    private static YtiUser getAuthenticatedUser(ShibbolethAuthenticationDetails authenticationDetails) {

        String url = ApplicationProperties.getDefaultGroupManagementAPI() + "user";

        logger.info("Fetching user from URL: " + url);

        Response response = ClientBuilder.newBuilder()
                .sslContext(naiveSSLContext())
                .build().target(url)
                .queryParam("email", authenticationDetails.getEmail())
                .request(MediaType.APPLICATION_JSON)
                .get();

        User user = response.readEntity(User.class);

        Map<UUID, Set<Role>> rolesInOrganizations = new HashMap<>();

        for (Organization organization : user.organization) {

            Set<Role> roles = organization.role.stream()
                    .filter(LoginServlet::isRoleMappableToEnum)
                    .map(Role::valueOf)
                    .collect(Collectors.toSet());

            rolesInOrganizations.put(organization.uuid, roles);
        }

        YtiUser ytiUser = new YtiUser(user.email, user.firstName, user.lastName, user.superuser, user.newlyCreated, rolesInOrganizations);

        logger.info("User fetched: " + ytiUser);

        return ytiUser;
    }

    private static boolean isRoleMappableToEnum(String roleString) {

        boolean contains = Role.contains(roleString);

        if (!contains) {
            logger.warning("Cannot map role (" + roleString + ")" + " to role enum");
        }

        return contains;
    }

    private static SSLContext naiveSSLContext() {

        TrustStrategy naivelyAcceptingTrustStrategy = (X509Certificate[] chain, String authType) -> true;

        try {
            return SSLContexts.custom()
                    .loadTrustMaterial(null, naivelyAcceptingTrustStrategy)
                    .build();
        } catch (NoSuchAlgorithmException | KeyManagementException | KeyStoreException e) {
            throw new RuntimeException(e);
        }
    }

    private static YtiUser createDebugUser(String email) {
        return new YtiUser(email, "Testi", "Testaaja", true, false, emptyMap());
    }

    private static boolean isLoggedIn(HttpServletRequest request) {
        return request.getAttribute(SHIBBOLETH_MAIL_ATTRIBUTE) != null;
    }

    private static String getParameterAsString(HttpServletRequest request, String parameterName) {
        Object attribute = request.getParameter(parameterName) ;
        return attribute != null ? attribute.toString() : null;
    }

    // <editor-fold defaultstate="collapsed" desc="HttpServlet methods. Click on the + sign on the left to edit the code.">
    /**
     * Handles the HTTP <code>GET</code> method.
     *
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        processRequest(request, response);
    }

    /**
     * Handles the HTTP <code>POST</code> method.
     *
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        processRequest(request, response);
    }

    /**
     * Returns a short description of the servlet.
     *
     * @return a String containing servlet description
     */
    @Override
    public String getServletInfo() {
        return "Short description";
    }// </editor-fold>

}

class User {

    public String email;
    public String firstName;
    public String lastName;
    public boolean superuser;
    public boolean newlyCreated;
    public List<Organization> organization;
}

class Organization {

    public UUID uuid;
    public List<String> role;
}
