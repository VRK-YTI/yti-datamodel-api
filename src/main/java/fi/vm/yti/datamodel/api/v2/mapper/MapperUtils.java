package fi.vm.yti.datamodel.api.v2.mapper;

import fi.vm.yti.datamodel.api.v2.endpoint.error.MappingError;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.shared.JenaException;

import java.util.*;

public class MapperUtils {

    private MapperUtils(){
        //Util class so we need to hide constructor
    }

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

    /**
     * Add localized property to Jena model
     * @param data Map of (language, value)
     * @param resource Resource to add to
     * @param property Property to add
     * @param model Jena model to add to
     */
    public static void addLocalizedProperty(Set<String> languages,
                                            Map<String, String> data,
                                            Resource resource,
                                            Property property,
                                            Model model) {
        if (data == null || languages == null || languages.isEmpty()) {
            return;
        }

        data.forEach((lang, value) -> {
            if(!languages.contains(lang)){
                throw new MappingError("Model missing language for localized property {" + lang + "}");
            }
            resource.addProperty(property, model.createLiteral(value, lang));
        });
    }

    /**
     * Convert array property to list of strings
     * @param resource Resource to get property from
     * @param property Property type
     * @return List of property values
     */
    public static List<String> arrayPropertyToList(Resource resource, Property property){
        var list = new ArrayList<String>();
        //return empty list if property is not found
        if(!resource.hasProperty(property)){
            return list;
        }
        try{
            resource.getProperty(property)
                    .getList()
                    .asJavaList()
                    .forEach(node -> list.add(node.toString()));
        }catch(JenaException ex){
            //if item could not be gotten as list it means it is multiple statements of the property
            resource.listProperties(property)
                    .forEach(val -> list.add(val.getObject().toString()));
        }
        return list;
    }

    /**
     * Convert array property to set of strings
     * @param resource Resource to get property from
     * @param property Property type
     * @return Set of property values, empty if property is not found
     */
    public static Set<String> arrayPropertyToSet(Resource resource, Property property){
        var list = new HashSet<String>();
        //return empty list if property is not found
        if(!resource.hasProperty(property)){
            return list;
        }
        try{
            resource.getProperty(property)
                    .getList()
                    .asJavaList()
                    .forEach(node -> list.add(node.toString()));
        }catch(JenaException ex){
            //if item could not be gotten as list it means it is multiple statements of the property
            resource.listProperties(property)
                    .forEach(val -> list.add(val.getObject().toString()));
        }
        return list;
    }

    /**
     * Convert property to String, with null checks to ensure no NullPointerException
     * @param resource Resource to get property from
     * @param property Property
     * @return String if property is found, null if not
     */
    public static String propertyToString(Resource resource, Property property){
        var prop = resource.getProperty(property);
        //null check for property
        if(prop == null){
            return null;
        }
        var object = prop.getObject();
        //null check for object
        return object == null ? null : object.toString();
    }
}
