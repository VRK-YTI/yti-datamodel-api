package fi.vm.yti.datamodel.api.config;

import io.swagger.jaxrs.config.BeanConfig;
import io.swagger.jaxrs.listing.ApiListingResource;
import org.glassfish.jersey.media.multipart.MultiPartFeature;
import org.glassfish.jersey.message.GZipEncoder;
import org.glassfish.jersey.server.ResourceConfig;
import org.reflections.Reflections;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;

import javax.ws.rs.ApplicationPath;
import javax.ws.rs.Path;
import javax.ws.rs.container.ContainerResponseFilter;

@Configuration
@ApplicationPath("/api/rest")
public class JerseyConfig extends ResourceConfig {

    @Autowired
    public JerseyConfig() {

        // https://github.com/spring-projects/spring-boot/issues/1468
        // FIXME packages("fi.vm.yti.datamodel.api.endpoint"); should work but it doesn't
        myPackages("fi.vm.yti.datamodel.api.endpoint");

        register(MultiPartFeature.class);
        register(ApiListingResource.class);
        register(GZipEncoder.class);

        register((ContainerResponseFilter) (req, resp) -> {
            resp.getHeaders().add("Cache-Control", "no-cache, no-store, must-revalidate");
            resp.getHeaders().add("Pragma", "no-cache");
            resp.getHeaders().add("Expires", "0");
        });

        BeanConfig beanConfig = new BeanConfig();
        beanConfig.setBasePath("/api/rest");
        beanConfig.setResourcePackage("fi.vm.yti.datamodel.api.endpoint");
        beanConfig.setPrettyPrint(true);
        beanConfig.setScan(true);
    }

    private void myPackages(String... packages) {
        for (String pack : packages) {
            new Reflections(pack).getTypesAnnotatedWith(Path.class).forEach(this::register);
        }
    }
}
