/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.csc.fi.ioapi.config;

import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

/**
 *
 * @author malonen
 */
public class LoginFilter implements Filter {
    
    private static final boolean debug = true;

    // The filter configuration object we are associated with.  If
    // this value is null, this filter instance is not currently
    // configured. 
    private FilterConfig filterConfig = null;
    
    public LoginFilter() {
    }    
    

    /**
     *
     * @param request The servlet request we are processing
     * @param response The servlet response we are creating
     * @param chain The filter chain we are processing
     *
     * @exception IOException if an input/output error occurs
     * @exception ServletException if a servlet error occurs
     */
    public void doFilter(ServletRequest request, ServletResponse response,
            FilterChain chain)
            throws IOException, ServletException {
        
        /* TO BE REMOVED */
      /*  Logger.getLogger(LoginFilter.class.getName()).log(Level.INFO, "FILTERING!");
        
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpSession session = httpRequest.getSession();
        HttpServletResponse httpResponse = (HttpServletResponse) response;
        String requestURI = httpRequest.getRequestURL().toString();
        
        Object prov = request.getAttribute("Shib-Identity-Provider"); 
        Object displayName = request.getAttribute("displayName"); 
        Object group = request.getAttribute("group"); 
        Object mail = request.getAttribute("mail");
        Object sn = request.getAttribute("sn"); 
        Object uid = request.getAttribute("uid"); 
        
        if(prov!=null) {  
            Logger.getLogger(LoginFilter.class.getName()).log(Level.INFO, displayName.toString()+ " from "+prov.toString());
            session.setAttribute("displayName",displayName.toString());
            session.setAttribute("group",group.toString());
            session.setAttribute("mail",mail.toString());
            session.setAttribute("uid",uid.toString());
        }
          
       //requestURI = httpResponse.encodeRedirectURL(requestURI);
      //  requestURI = requestURI.replaceFirst("https", "http");
       // requestURI = requestURI.replaceFirst("/login", "/welcome");
        
      //  Logger.getLogger(LoginFilter.class.getName()).log(Level.INFO, httpRequest.getRequestURI());
  
        //request.getRequestDispatcher("/err").forward(httpRequest, httpResponse); 
               
        httpResponse.sendRedirect("/");
        */
                
    }

   
    /**
     * Destroy method for this filter
     */
    public void destroy() {        
    }

    /**
     * Init method for this filter
     */
    public void init(FilterConfig filterConfig) {        
        this.filterConfig = filterConfig;
    }
    
}
