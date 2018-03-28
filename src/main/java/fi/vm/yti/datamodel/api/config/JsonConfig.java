package fi.vm.yti.datamodel.api.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.json.Json;
import javax.json.JsonWriterFactory;
import javax.json.stream.JsonGenerator;
import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;

@Configuration
public class JsonConfig {

    @Bean
    JsonWriterFactory jsonWriterFactory() {
        Map<String, Object> config = new HashMap<>();
        config.put(JsonGenerator.PRETTY_PRINTING, true);
        return Json.createWriterFactory(config);
    }

    @Bean
    ObjectMapper objectMapper() {
        return new ObjectMapper();
    }
}
