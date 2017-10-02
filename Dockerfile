FROM tomcat:9-alpine

COPY target/yti-datamodel-api.war /usr/local/tomcat/webapps/yti-datamodel-api.war
