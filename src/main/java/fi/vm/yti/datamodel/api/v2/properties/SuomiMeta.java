package fi.vm.yti.datamodel.api.v2.properties;

import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.ResourceFactory;

public class SuomiMeta {


    private SuomiMeta(){
        //property class
    }

    public static final String URI ="https://iri.suomi.fi/model/suomi-meta/";

    public static final Property publicationStatus = ResourceFactory.createProperty(URI, "publicationStatus");
    public static final Property contentModified = ResourceFactory.createProperty(URI, "contentModified");
    public static final Property documentation = ResourceFactory.createProperty(URI, "documentation");
    public static final Property parentOrganization = ResourceFactory.createProperty(URI, "parentOrganization");
    public static final Property contact = ResourceFactory.createProperty(URI, "contact");
    public static final Property creator = ResourceFactory.createProperty(URI, "creator");
    public static final Property modifier = ResourceFactory.createProperty(URI, "modifier");
    public static final Property codeList = ResourceFactory.createProperty(URI, "codeList");
    public static final Property CodeScheme = ResourceFactory.createProperty(URI, "CodeScheme");
    public static final Property ApplicationProfile = ResourceFactory.createProperty(URI, "ApplicationProfile");

    public static final Property posX = ResourceFactory.createProperty(URI, "posX");
    public static final Property posY = ResourceFactory.createProperty(URI, "posY");
    public static final Property referenceTarget = ResourceFactory.createProperty(URI, "referenceTarget");
}
