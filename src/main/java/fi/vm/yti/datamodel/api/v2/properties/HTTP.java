package fi.vm.yti.datamodel.api.v2.properties;

import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.ResourceFactory;

public class HTTP {

    private HTTP() {
        // property class
    }
    public static final Property API_PATH =
            ResourceFactory.createProperty("http://www.w3.org/2011/http#absolutePath");
}
