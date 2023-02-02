package fi.vm.yti.datamodel.api.v2.mapper;

import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class MapperUtils {

    /**
     * Get UUID from urn
     * Will return null if urn cannot be parsed
     * @param urn URN string formatted as urn:uuid:{uuid}
     * @return UUID
     */
    public static UUID getUUID(String urn) {
        try {
            return UUID.fromString(urn.replace("urn:uuid:", ""));
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Localized property to Map of (language, value)
     * @param resource Resource to get property from
     * @param property Property type
     * @return Map of (language, value)
     */
    public static Map<String, String> localizedPropertyToMap(Resource resource, Property property){
        var map = new HashMap<String, String>();
        resource.listProperties(property).forEach(prop -> {
            var lang = prop.getLanguage();
            var value = prop.getString();
            map.put(lang, value);
        });
        return map;
    }
}
