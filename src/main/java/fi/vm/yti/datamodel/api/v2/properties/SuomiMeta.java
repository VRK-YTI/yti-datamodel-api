package fi.vm.yti.datamodel.api.v2.properties;

import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.ResourceFactory;

public class SuomiMeta {


    private SuomiMeta(){
        //property class
    }

    public static final String URI ="http://uri.suomi.fi/datamodel/ns/suomi-meta/";

    public static final Property publicationStatus = ResourceFactory.createProperty(URI, "publicationStatus");
}
