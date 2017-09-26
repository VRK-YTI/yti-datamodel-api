FROM tomcat:9-alpine

COPY target/api.war /usr/local/tomcat/webapps/api.war
