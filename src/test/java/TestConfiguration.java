import fi.vm.yti.datamodel.api.config.JsonConfig;
import fi.vm.yti.datamodel.api.config.PropertiesConfig;
import org.apache.http.ssl.SSLContexts;
import org.apache.http.ssl.TrustStrategy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import javax.net.ssl.SSLContext;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;

@Configuration
@ActiveProfiles("test")
@ComponentScan(basePackages = {
        "fi.vm.yti.datamodel.api.service",
        "fi.vm.yti.datamodel.api.security"
})
@Import({ PropertiesConfig.class, JsonConfig.class } )
public class TestConfiguration {

    @Bean
    SSLContext sslContext() {

        TrustStrategy naivelyAcceptingTrustStrategy = (X509Certificate[] chain, String authType) -> true;

        try {
            return SSLContexts.custom()
                    .loadTrustMaterial(null, naivelyAcceptingTrustStrategy)
                    .build();

        } catch (NoSuchAlgorithmException | KeyManagementException | KeyStoreException e) {
            throw new RuntimeException(e);
        }
    }
}
