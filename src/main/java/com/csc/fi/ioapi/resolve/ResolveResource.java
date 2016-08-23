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
import java.util.logging.Logger;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
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

		
                logger.info(request.getContextPath());
		logger.info(request.getContentType());
		logger.info(request.getHeader("Accept"));
        

        String accept = request.getHeader("Accept");
        
        Lang rdfLang = RDFLanguages.contentTypeToLang(accept);
        
        String requestURI = request.getRequestURI();
        String modelID = requestURI.substring(requestURI.lastIndexOf("/") + 1, requestURI.length());
        String modelURL = ApplicationProperties.getDefaultDomain()+"ns/"+modelID;
       
        logger.info(modelURL);
        
        IRIFactory iriFactory = IRIFactory.iriImplementation();
            IRI modelIRI;
            try {
                modelIRI = iriFactory.construct(modelURL);
            }
            catch (IRIException e) {
                response.sendError(HttpServletResponse.SC_NOT_FOUND);
            }  
        
        if(GraphManager.isExistingServiceGraph(modelURL)) {
            
          //  String nextJSP = "/rest/class";
          //  RequestDispatcher dispatcher = getServletContext().getRequestDispatcher(nextJSP);
          //  dispatcher.forward(request,response);

           if(rdfLang==null) {
                response.sendRedirect("/");
            } else {
              String dis = "/rest/exportModel?graph="+modelURL+"&content-type="+accept;
              RequestDispatcher dispatcher = getServletContext().getRequestDispatcher(dis);
              dispatcher.forward(request,response);
            }
          
           /*
            response.setContentType("text/html;charset=UTF-8");    
                try (PrintWriter out = response.getWriter()) {
                    out.println("<!DOCTYPE html>");
                    out.println("<html>");
                    out.println("<head>");
                    out.println("<title>Servlet ResolveResource</title>");            
                    out.println("</head>");
                    out.println("<body>");

                    out.println("<h1>"+modelID+"</h1>");
                    out.println("<h1>"+request.getRequestURI() + "</h1>");
                    out.println("<h1>"+request.getQueryString() + "</h1>");
                    out.println("<h1>"+request.getHeader("Accept") + "</h1>");
                    if(rdfLang==null) out.println("<h1>NOT RDF</h1>");
                    else out.println("<h1>"+rdfLang.getLabel()+"</h1>");
                    out.println("</body>");
                    out.println("</html>");
                }

            */
            
        } else response.sendError(HttpServletResponse.SC_NOT_FOUND);
        /*
        if(rdfLang==null) {
            response.sendRedirect("/");
        } else {
            
        }*/
        
  

      
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
