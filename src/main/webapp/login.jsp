<%-- 
    Document   : login
    Created on : Sep 9, 2015, 3:18:47 PM
    Author     : malonen
--%>
<%@page import="com.csc.fi.ioapi.config.LoginSession"%>
<%@page import="com.csc.fi.ioapi.utils.UserManager"%>
<%@page import="java.util.Arrays"%>
<%@page import="java.util.Date"%>
<%@page import="java.util.Enumeration"%>
<%@page contentType="text/html" pageEncoding="UTF-8"%>
<!DOCTYPE html>
<html>
    <head>
        <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
        <title>Welcome</title>
    </head>
    <body>
      <% 
          
      //response.setHeader("Refresh", "2;url=/");
    
     if (session.getAttribute("displayName")!=null){
         
      LoginSession loginSession = new LoginSession(session);
      UserManager.checkUser(loginSession);
      
      /*
      if (request.isSecure()) { // it is HTTPS
            response.sendRedirect(response.encodeRedirectURL("http://"+request.getServerName()));
        }
      */
      
      %>
      <h1>Hei <%=session.getAttribute("displayName")%>!</h1>
      <%} else {%>
      <h1>Kirjautuminen epäonnistui</h1>
      <%}%>
    </body>
</html>
