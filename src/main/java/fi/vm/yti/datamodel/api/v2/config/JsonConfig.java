package fi.vm.yti.datamodel.api.v2.config;

import jakarta.json.Json;
import jakarta.json.JsonWriterFactory;
import jakarta.json.stream.JsonGenerator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class JsonConfig {

    @Bean
    JsonWriterFactory jsonWriterFactory() {
        Map<String, Object> config = new HashMap<>();
        config.put(JsonGenerator.PRETTY_PRINTING, true);
        return Json.createWriterFactory(config);
    }
}
