package fi.vm.yti.datamodel.api.v2.dto;

import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.ResourceFactory;

public class Iow {

    private Iow(){
        //property class
    }

    public static final String URI ="http://uri.suomi.fi/datamodel/ns/iow#";

    public static final Property contentModified = ResourceFactory.createProperty(URI, "contentModified");
    public static final Property documentation = ResourceFactory.createProperty(URI, "documentation");
    public static final Property parentOrganization = ResourceFactory.createProperty(URI, "parentOrganization");

}
