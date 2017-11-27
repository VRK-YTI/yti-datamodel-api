/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package fi.vm.yti.datamodel.api.config;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.net.URLDecoder;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author malonen
 */
@WebServlet(name = "LoginServlet", urlPatterns = {"/LoginServlet"})
public class LoginServlet extends HttpServlet {

    private static String SHIBBOLETH_MAIL_ATTRIBUTE = "mail";

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

        HttpSession session = request.getSession();

        logger.info("Prosessing login");
        logger.info("Session id:"+session.getId());

        boolean debug = ApplicationProperties.getDebugMode();
        ShibbolethAuthenticationDetails authenticationDetails = resolveAuthenticationDetails(request);

        if (authenticationDetails != null) {
            LoginHandler.initialize(session, authenticationDetails);
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

    private static ShibbolethAuthenticationDetails resolveAuthenticationDetails(HttpServletRequest request) {

        boolean debug = ApplicationProperties.getDebugMode();

        if (debug) {
            return new ShibbolethAuthenticationDetails(ApplicationProperties.getDebugUserEmail(), ApplicationProperties.getDebugUserFirstname(), ApplicationProperties.getDebugUserLastname());
        } else if (isLoggedIn(request)) {
            return new ShibbolethAuthenticationDetails(request);
        } else {
            return null;
        }
    }

    private static String getParameterAsString(HttpServletRequest request, String parameterName) {
        Object attribute = request.getParameter(parameterName) ;
        return attribute != null ? attribute.toString() : null;
    }

    private static boolean isLoggedIn(HttpServletRequest request) {
        return request.getAttribute(SHIBBOLETH_MAIL_ATTRIBUTE) != null;
    }

    private static String resolveFrontendAddress(boolean debug) {
        return debug ? ApplicationProperties.getDebugAdress() : "/";
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
