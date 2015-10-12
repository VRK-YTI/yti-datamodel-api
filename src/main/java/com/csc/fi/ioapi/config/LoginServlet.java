/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.csc.fi.ioapi.config;

import com.csc.fi.ioapi.utils.UserManager;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

/**
 *
 * @author malonen
 */
@WebServlet(name = "LoginServlet", urlPatterns = {"/LoginServlet"})
public class LoginServlet extends HttpServlet {

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
        
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpSession session = httpRequest.getSession();
        HttpServletResponse httpResponse = (HttpServletResponse) response;
          
        Object prov = request.getAttribute("Shib-Identity-Provider"); 
        Object displayName = request.getAttribute("displayName"); 
        Object group = request.getAttribute("group"); 
        Object mail = request.getAttribute("mail");
        Object sn = request.getAttribute("sn"); 
        Object uid = request.getAttribute("uid");
        
        boolean debug = ApplicationProperties.getDebugMode();
          
        if(prov!=null || debug) {
            if(debug) {
                Logger.getLogger(LoginServlet.class.getName()).log(Level.INFO, "Logged in DEBUG MODE: "+debug);
                session.setAttribute("displayName","Testi Testaaja");
                session.setAttribute("group",ApplicationProperties.getGroupDomain()+"SUPER_ADMINS");
                session.setAttribute("mail","testi@example.org");
            } else { 
                Logger.getLogger(LoginServlet.class.getName()).log(Level.INFO, displayName.toString()+ " logged in from "+prov.toString());
                Logger.getLogger(LoginServlet.class.getName()).log(Level.INFO, "Groups: "+group.toString());
                session.setAttribute("displayName",displayName.toString());
                session.setAttribute("group",group.toString());
                session.setAttribute("mail",mail.toString());
            }
            
            // &user="+session.getAttribute("mail")
            httpResponse.sendRedirect("/");
            LoginSession loginSession = new LoginSession(session);
            UserManager.checkUser(loginSession);

        } else {
            Logger.getLogger(LoginServlet.class.getName()).log(Level.INFO, "NOT LOGGED IN");
            httpResponse.sendRedirect("/");
        }
        
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
