package fi.vm.yti.datamodel.api.v2.dto;

import java.util.List;
import java.util.Map;

public class ModelConstants {

    private ModelConstants(){
        //utility class
    }

    public static final String SUOMI_FI_NAMESPACE = "https://iri.suomi.fi/model/";
    public static final String CODELIST_NAMESPACE = "http://uri.suomi.fi/codelist/";
    public static final String TERMINOLOGY_NAMESPACE = "http://uri.suomi.fi/terminology/";
    public static final String MODEL_POSITIONS_NAMESPACE = "https://iri.suomi.fi/model-positions/";
    public static final String SUOMI_FI_DOMAIN = "uri.suomi.fi";

    public static final String RESOURCE_SEPARATOR = "/";
    public static final String URN_UUID = "urn:uuid:";
    public static final String DEFAULT_LANGUAGE = "fi";
    public static final List<String> USED_LANGUAGES = List.of("fi", "sv", "en");
    public static final String ORGANIZATION_GRAPH = "urn:yti:organizations";
    public static final String SERVICE_CATEGORY_GRAPH = "urn:yti:servicecategories";
    public static final Map<String, String> PREFIXES = Map.of(
            "rdfs", "http://www.w3.org/2000/01/rdf-schema#",
            "rdf", "http://www.w3.org/1999/02/22-rdf-syntax-ns#",
            "dcterms", "http://purl.org/dc/terms/",
            "owl", "http://www.w3.org/2002/07/owl#",
            "dcap", "http://purl.org/ws-mmi-dc/terms/",
            "xsd", "http://www.w3.org/2001/XMLSchema#",
            "suomi-meta", "https://iri.suomi.fi/model/suomi-meta/",
            "skos", "http://www.w3.org/2004/02/skos/core#",
            "sh", "http://www.w3.org/ns/shacl#",
            "http", "http://www.w3.org/2011/http#"
    );
    public static final List<String> SUPPORTED_DATA_TYPES = List.of(
            "http://www.w3.org/2002/07/owl#rational",
            "http://www.w3.org/2002/07/owl#real",
            "http://www.w3.org/1999/02/22-rdf-syntax-ns#PlainLiteral",
            "http://www.w3.org/1999/02/22-rdf-syntax-ns#XMLLiteral",
            "http://www.w3.org/2000/01/rdf-schema#Literal",
            "http://www.w3.org/2001/XMLSchema#anyURI",
            "http://www.w3.org/2001/XMLSchema#base64Binary",
            "http://www.w3.org/2001/XMLSchema#boolean",
            "http://www.w3.org/2001/XMLSchema#byte",
            "http://www.w3.org/2001/XMLSchema#dateTime",
            "http://www.w3.org/2001/XMLSchema#dateTimeStamp",
            "http://www.w3.org/2001/XMLSchema#decimal",
            "http://www.w3.org/2001/XMLSchema#double",
            "http://www.w3.org/2001/XMLSchema#float",
            "http://www.w3.org/2001/XMLSchema#hexBinary",
            "http://www.w3.org/2001/XMLSchema#int",
            "http://www.w3.org/2001/XMLSchema#integer",
            "http://www.w3.org/2001/XMLSchema#language",
            "http://www.w3.org/2001/XMLSchema#long",
            "http://www.w3.org/2001/XMLSchema#Name",
            "http://www.w3.org/2001/XMLSchema#NCName",
            "http://www.w3.org/2001/XMLSchema#negativeInteger",
            "http://www.w3.org/2001/XMLSchema#NMTOKEN",
            "http://www.w3.org/2001/XMLSchema#nonNegativeInteger",
            "http://www.w3.org/2001/XMLSchema#nonPositiveInteger",
            "http://www.w3.org/2001/XMLSchema#normalizedString",
            "http://www.w3.org/2001/XMLSchema#positiveInteger",
            "http://www.w3.org/2001/XMLSchema#short",
            "http://www.w3.org/2001/XMLSchema#string",
            "http://www.w3.org/2001/XMLSchema#token",
            "http://www.w3.org/2001/XMLSchema#unsignedByte",
            "http://www.w3.org/2001/XMLSchema#unsignedInt",
            "http://www.w3.org/2001/XMLSchema#unsignedLong",
            "http://www.w3.org/2001/XMLSchema#unsignedShort"
    );
}
