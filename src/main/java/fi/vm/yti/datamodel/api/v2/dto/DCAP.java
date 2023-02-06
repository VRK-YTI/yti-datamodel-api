package fi.vm.yti.datamodel.api.v2.dto;

import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.ResourceFactory;

public class DCAP {

    private DCAP(){
        //property class
    }

    public static final String URI ="http://purl.org/ws-mmi-dc/terms/";

    public static final Property preferredXMLNamespacePrefix = ResourceFactory.createProperty(URI, "preferredXMLNamespacePrefix");
    public static final Property preferredXMLNamespace = ResourceFactory.createProperty(URI, "preferredXMLNamespace");
}
