package fi.vm.yti.datamodel.api.config;

import io.swagger.v3.jaxrs2.integration.resources.OpenApiResource;
import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.info.License;

import org.glassfish.jersey.message.DeflateEncoder;
import org.glassfish.jersey.message.GZipEncoder;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.server.filter.EncodingFilter;
import org.reflections.Reflections;
import org.reflections.scanners.SubTypesScanner;
import org.reflections.scanners.TypeAnnotationsScanner;
import org.reflections.util.ClasspathHelper;
import org.reflections.util.ConfigurationBuilder;
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
            name = "YTI Datamodel Service by the Digital and Population Data Services Agency",
            url = "https://yhteentoimiva.suomi.fi/",
            email = "yhteentoimivuus@dvv.fi"
        ),
        license = @License(
            name = "EUPL-1.2",
            url = "https://opensource.org/licenses/EUPL-1.1"
        )
    )
)
@ApplicationPath("/api")
public class JerseyConfig extends ResourceConfig {

    @Autowired
    public JerseyConfig() {

        // https://github.com/spring-projects/spring-boot/issues/1468
        // FIXME packages("fi.vm.yti.datamodel.api.endpoint"); should work but it doesn't
        myPackages("fi.vm.yti.datamodel.api.endpoint");

        register(OpenApiResource.class);
        register(EncodingFilter.class);
        register(GZipEncoder.class);
        register(DeflateEncoder.class);
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

    private void myPackages(String... packages) {
        for (String pack : packages) {
            Reflections reflections = new Reflections(new ConfigurationBuilder()
                    .setUrls(ClasspathHelper.forPackage(pack))
                    .setScanners(new SubTypesScanner(), new TypeAnnotationsScanner()));
            reflections.getTypesAnnotatedWith(Path.class).forEach(this::register);
        }
    }

}
