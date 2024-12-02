package fi.vm.yti.datamodel.api.v2;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.info.License;
import io.swagger.v3.oas.annotations.servers.Server;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(scanBasePackages = "fi.vm.yti")
@EnableScheduling
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
        ),
        servers = {
                @Server(
                        description = "Datamodel API",
                        url = "/datamodel-api")
        }
)
public class Application {

    public static void main(String[] args) {
        System.setProperty("jdk.httpclient.redirects.retrylimit", "15");
        SpringApplication.run(Application.class, args);
    }
}
