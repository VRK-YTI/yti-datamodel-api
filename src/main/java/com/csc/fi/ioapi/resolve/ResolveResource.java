/*
 * Licensed under the European Union Public Licence (EUPL) V.1.1 
 */
package com.csc.fi.ioapi.resolve;

import com.csc.fi.ioapi.api.profile.ProfileCreator;
import com.csc.fi.ioapi.config.ApplicationProperties;
import com.csc.fi.ioapi.utils.ErrorMessage;
import com.csc.fi.ioapi.utils.GraphManager;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import org.apache.jena.iri.IRI;
import org.apache.jena.iri.IRIException;
import org.apache.jena.iri.IRIFactory;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFLanguages;

/**
 *
 * @author malonen
 */
public class ResolveResource extends HttpServlet {

    private static final Logger logger = Logger.getLogger(ResolveResource.class.getName());
   
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

        String accept = request.getHeader(HttpHeaders.ACCEPT);
        String acceptLang = request.getHeader(HttpHeaders.ACCEPT_LANGUAGE);
        
        String ifModifiedSince = request.getHeader(HttpHeaders.IF_MODIFIED_SINCE);
        Date modifiedSince = null;
        Date modified = null;
        SimpleDateFormat format = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz");
        
        if(ifModifiedSince!=null) {
            try {
                modifiedSince = format.parse(ifModifiedSince);
            } catch (ParseException ex) {
                Logger.getLogger(ResolveResource.class.getName()).log(Level.SEVERE, null, ex);
            } 
        }
        
        Lang rdfLang = RDFLanguages.contentTypeToLang(accept);
    
        String requestURI = request.getRequestURI();
        String modelID = requestURI.substring(requestURI.lastIndexOf("/") + 1, requestURI.length());
        String graphName = GraphManager.getServiceGraphNameWithPrefix(modelID);
        
         if(modifiedSince!=null) {
                modified = GraphManager.lastModified(graphName);
                if(modified!=null) {
                    if(modifiedSince.after(modified)) {
                        response.setHeader("Last-Modified", format.format(modified));
                        response.sendError(HttpServletResponse.SC_NOT_MODIFIED);
                        
                        return;
                    }
                }
        }
     
        if(graphName==null) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
        } else {
        
                if(rdfLang!=null || accept.equals("application/schema+json")) {
                    String dis = "/rest/exportModel?graph="+graphName+"&content-type="+accept+"&lang="+acceptLang;
                    logger.info("Redirecting to export: "+dis);
                    RequestDispatcher dispatcher = getServletContext().getRequestDispatcher(dis);
                    dispatcher.forward(request,response);
                } else {
                    logger.info("Redirecting to root");
                    response.sendRedirect("/#/model?urn="+graphName);
                }    
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
