package fi.vm.yti.datamodel.api.config;

import java.util.HashMap;
import java.util.Map;


import jakarta.json.Json;
import jakarta.json.JsonWriterFactory;
import jakarta.json.stream.JsonGenerator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class JsonConfig {

    @Bean
    JsonWriterFactory jsonWriterFactory() {
        Map<String, Object> config = new HashMap<>();
        config.put(JsonGenerator.PRETTY_PRINTING, true);
        return Json.createWriterFactory(config);
    }

    /* JSON serialization and deserialization configuration under RestConfig */
}
