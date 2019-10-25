package fi.vm.yti.datamodel.api.config;

import io.swagger.v3.jaxrs2.integration.resources.OpenApiResource;
import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.info.License;
import io.swagger.v3.oas.annotations.servers.Server;

import org.glassfish.jersey.message.GZipEncoder;
import org.glassfish.jersey.server.ResourceConfig;
import org.reflections.Reflections;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;

import javax.ws.rs.ApplicationPath;
import javax.ws.rs.Path;
import javax.ws.rs.container.ContainerResponseFilter;

@Configuration
@OpenAPIDefinition(
    info = @Info(
        description = "YTI Datamodel Service",
        version = "1.0",
        title = "YTI Datamodel Service",
        termsOfService = "https://opensource.org/licenses/EUPL-1.1",
        contact = @Contact(
            name = "YTI Datamodel Service by the Population Register Center of Finland",
            url = "https://yhteentoimiva.suomi.fi/",
            email = "yhteentoimivuus@vrk.fi"
        ),
        license = @License(
            name = "EUPL-1.2",
            url = "https://opensource.org/licenses/EUPL-1.1"
        )
    ),
    servers = {
        @Server(
            description = "Datamodel service API",
            url = "/datamodel/api")
    }
)
@ApplicationPath("/datamodel/api")
public class JerseyConfig extends ResourceConfig {

    @Autowired
    public JerseyConfig() {

        packages("fi.vm.yti.datamodel.api.endpoint");
        register(OpenApiResource.class);
        register(GZipEncoder.class);
        register(JsonParseExceptionMapper.class);
        register(IllegalArgumentExceptionMapper.class);
        register((ContainerResponseFilter) (req, resp) -> {
            resp.getHeaders().add("Cache-Control", "no-cache, no-store, must-revalidate");
            resp.getHeaders().add("Pragma", "no-cache");
            resp.getHeaders().add("Expires", "0");

            resp.getHeaders().add("Access-Control-Allow-Origin", "*");
            resp.getHeaders().add("Access-Control-Allow-Headers", "content-type");
            resp.getHeaders().add("Access-Control-Allow-Methods", "GET");
        });

    }

}
