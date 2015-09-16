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
          
     if (session.getAttribute("displayName")!=null){
      
      response.setHeader("Refresh", "1;url=/?login=true&user="+session.getAttribute("mail"));
         
      LoginSession loginSession = new LoginSession(session);
      UserManager.checkUser(loginSession);
      
      %>
      <h1>Hei <%=session.getAttribute("displayName")%>!</h1>
      <%} else {
      
      response.setHeader("Refresh", "1;url=/?login=false");
      
      %>
      <h1>Kirjautuminen epäonnistui</h1>
      <%}%>
    </body>
</html>
