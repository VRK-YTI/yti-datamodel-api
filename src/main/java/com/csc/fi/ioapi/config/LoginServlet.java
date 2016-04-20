/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.csc.fi.ioapi.config;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import com.csc.fi.ioapi.api.usermanagement.UserDefinition;
import com.csc.fi.ioapi.utils.UserManager;
import java.net.URLDecoder;

/**
 *
 * @author malonen
 */
@WebServlet(name = "LoginServlet", urlPatterns = {"/LoginServlet"})
public class LoginServlet extends HttpServlet {

    private static String SHIBBOLETH_PROVIDER_ATTRIBUTE = "Shib-Identity-Provider";
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
        boolean debug = ApplicationProperties.getDebugMode();

        if (debug) {
            initializeUser(session, createDebugUser());
        } else {
            if (isLoggedIn(request)) {
                initializeUser(session, createUser(request));
            } else {
                Logger.getLogger(LoginServlet.class.getName()).log(Level.INFO, "NOT LOGGED IN");   
            }
        }
        
        String target = getParameterAsString(request, "target");
        
        String decodedTarget = URLDecoder.decode(target, "ISO-8859-1");
        
        if(decodedTarget!=null && decodedTarget.startsWith(ApplicationProperties.getDefaultDomain()))
            target = decodedTarget;
        
        if(target!=null && (debug || target.startsWith(ApplicationProperties.getDefaultDomain())))
            response.sendRedirect(target);
        else 
            response.sendRedirect(resolveFrontendAddress(debug));
            
    }

    private static String resolveFrontendAddress(boolean debug) {
        return debug ? ApplicationProperties.getDebugAdress() : "/";
    }
    
    private static void initializeUser(HttpSession session, UserDefinition userDefinition) {
        session.setAttribute("displayName", userDefinition.getDisplayName());
        session.setAttribute("group", userDefinition.getGroup());
        session.setAttribute("mail", userDefinition.getMail());
        UserManager.checkUser(new LoginSession(session));
    }

    private static UserDefinition createUser(HttpServletRequest request) {
        String prov = getAttributeAsString(request, SHIBBOLETH_PROVIDER_ATTRIBUTE);
        String displayName = getAttributeAsString(request, "displayName");
        String group = getAttributeAsString(request, "group");
        String mail = getAttributeAsString(request, "mail");
        String sn = getAttributeAsString(request, "sn");
        String uid = getAttributeAsString(request, "uid");

        Logger.getLogger(LoginServlet.class.getName()).log(Level.INFO, displayName + " logged in from " + prov);
        Logger.getLogger(LoginServlet.class.getName()).log(Level.INFO, mail+" groups: "+group);
        
        return new UserDefinition(displayName, group, mail);
    }

    private static UserDefinition createDebugUser() {
        return new UserDefinition("Testi Testaaja", ApplicationProperties.getDebugGroups(), "testi@example.org");
    }

    private static boolean isLoggedIn(HttpServletRequest request) {
        return request.getAttribute(SHIBBOLETH_PROVIDER_ATTRIBUTE) != null;
    }
    
    private static String getAttributeAsString(HttpServletRequest request, String attributeName) {
        Object attribute = request.getAttribute(attributeName);
        return attribute != null ? attribute.toString() : null;
    }
    
    private static String getParameterAsString(HttpServletRequest request, String attributeName) {
        Object attribute = request.getParameter(attributeName) ;
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
