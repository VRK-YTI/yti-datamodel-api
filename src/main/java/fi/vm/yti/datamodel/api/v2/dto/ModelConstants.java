package fi.vm.yti.datamodel.api.v2.dto;

import java.util.Map;

public class ModelConstants {

    private ModelConstants(){
        //utility class
    }

    public static final String SUOMI_FI_NAMESPACE = "http://uri.suomi.fi/datamodel/ns/";
    public static final String URN_UUID = "urn:uuid:";
    public static final String DEFAULT_LANGUAGE = "fi";

    public static final Map<String, String> PREFIXES = Map.of(
            "rdfs", "http://www.w3.org/2000/01/rdf-schema#",
            "rdf", "http://www.w3.org/1999/02/22-rdf-syntax-ns#", //TODO this can be removed once migration for old rdf lists are done
            "dcterms", "http://purl.org/dc/terms/",
            "owl", "http://www.w3.org/2002/07/owl#",
            "dcap", "http://purl.org/ws-mmi-dc/terms/",
            "xsd", "http://www.w3.org/2001/XMLSchema#",
            "iow", "http://uri.suomi.fi/datamodel/ns/iow#",
            "skos", "http://www.w3.org/2004/02/skos/core#"
    );
}
