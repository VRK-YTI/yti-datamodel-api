package fi.vm.yti.datamodel.api.config;

import org.apache.catalina.connector.Connector;
import org.apache.coyote.ajp.AjpNioProtocol;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.net.InetAddress;
import java.net.UnknownHostException;

@Configuration
public class AjpConfig {

    @Value(value = "${application.contextPath:/datamodel-api}")
    private String contextPath;

    @Bean
    public TomcatServletWebServerFactory servletContainer(@Value("${tomcat.ajp.port:}") Integer ajpPort) throws UnknownHostException {

        var tomcat = new TomcatServletWebServerFactory();

        tomcat.setContextPath(contextPath);

        if (ajpPort != null) {
            var ajpConnector = new Connector("AJP/1.3");
            ajpConnector.setPort(ajpPort);
            ajpConnector.setSecure(false);
            ajpConnector.setAllowTrace(false);
            ajpConnector.setScheme("http");
            ajpConnector.setProperty("allowedRequestAttributesPattern", ".*");

            AjpNioProtocol protocol = (AjpNioProtocol)ajpConnector.getProtocolHandler();
            protocol.setSecretRequired(false);
            protocol.setAddress(InetAddress.getByName("0.0.0.0"));

            tomcat.addAdditionalTomcatConnectors(ajpConnector);
        }

        return tomcat;
    }
}
