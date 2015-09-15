<%-- 
    Document   : login
    Created on : Sep 9, 2015, 3:18:47 PM
    Author     : malonen
--%>
<%@page import="java.util.Date"%>
<%@page import="java.util.Enumeration"%>
<%@page contentType="text/html" pageEncoding="UTF-8"%>
<!DOCTYPE html>
<html>
    <head>
        <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
        <title>Login test page</title>
    </head>
    <body>
      <h1>HTTP Request Headers Received</h1>
      <table border="1" cellpadding="4" cellspacing="0">
      <%
         Enumeration eNames = request.getHeaderNames();
         while (eNames.hasMoreElements()) {
            String name = (String) eNames.nextElement();
            String value = normalize(request.getHeader(name));
      %>
         <tr><td><%= name %></td><td><%= value %></td></tr>
      <%
         }
      %>
      </table>
      
      <%
         Enumeration aNames = request.getAttributeNames();   
      if(aNames.hasMoreElements()) {
      %>
      <h1>HTTP Request attributes</h1>
      <table border="1" cellpadding="4" cellspacing="0">
      <%
         while (aNames.hasMoreElements()) {
            String name = (String) aNames.nextElement();
            String value = request.getAttribute(name).toString();
      %>
         <tr><td><%= name %></td><td><%= value %></td></tr>
      <%
         } }
      %>
      </table>
      
      
      <h1>Session variables</h1>
      
      <table border="1" cellpadding="4" cellspacing="0">
      <% 
         
       if(session.getAttribute("creationTime")==null) {
             session.setAttribute("creationTime", (new Date(session.getCreationTime())).toString());
       }
       
         Enumeration sNames = session.getAttributeNames();
         while(sNames.hasMoreElements()){
            String name= (String) sNames.nextElement();
            String value = (String) session.getAttribute(name);%>      
      <tr><td><%= name %></td><td><%= value %></td></tr>
      <% }%>
      </table>
      
<%!
   private String normalize(String value)
   {
      StringBuffer sb = new StringBuffer();
      for (int i = 0; i < value.length(); i++) {
         char c = value.charAt(i);
         sb.append(c);
         if (c == ';')
            sb.append("<br>");
      }
      return sb.toString();
   }
%>
    </body>
</html>
