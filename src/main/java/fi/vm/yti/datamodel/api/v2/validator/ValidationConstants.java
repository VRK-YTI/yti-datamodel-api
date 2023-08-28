package fi.vm.yti.datamodel.api.v2.validator;

import java.util.List;
import java.util.Map;

public class ValidationConstants {

    private ValidationConstants(){
        //Utility class
    }

    public static final String MSG_VALUE_MISSING = "should-have-value";
    public static final String MSG_NOT_ALLOWED_UPDATE =  "not-allowed-update";
    public static final String MSG_OVER_CHARACTER_LIMIT = "value-over-character-limit.";

    public static final int TEXT_FIELD_MAX_LENGTH = 150;
    public static final int EMAIL_FIELD_MAX_LENGTH = 320;
    public static final int TEXT_AREA_MAX_LENGTH = 5000;

    public static final int PREFIX_MIN_LENGTH = 3;
    public static final int PREFIX_MAX_LENGTH = 32;
    public static final int RESOURCE_IDENTIFIER_MAX_LENGTH = 32;
    public static final String PREFIX_REGEX = "^[a-z][a-z0-9-_]{2,}";

    public static final Map<String, String> RESERVED_NAMESPACES = Map.ofEntries(
            Map.entry("owl", "http://www.w3.org/2002/07/owl#"),
            Map.entry("xsd", "http://www.w3.org/2001/XMLSchema#"),
            Map.entry("rdf", "http://www.w3.org/1999/02/22-rdf-syntax-ns#"),
            Map.entry("rdfs", "http://www.w3.org/2000/01/rdf-schema#"),
            Map.entry("dc", "http://purl.org/dc/elements/1.1/"),
            Map.entry("dcterms", "http://purl.org/dc/terms/"),
            Map.entry("dcam", "http://purl.org/dc/dcam/"),
            Map.entry("dcmitype", "http://purl.org/dc/dcmitype/"),
            Map.entry("skos", "http://www.w3.org/2004/02/skos/core#"),
            Map.entry("prov", "http://www.w3.org/ns/prov#"),
            Map.entry("skosxl", "http://www.w3.org/2008/05/skos-xl#"),
            Map.entry("sh", "http://www.w3.org/ns/shacl#"),
            Map.entry("sd", "http://www.w3.org/ns/sparql-service-description#"),
            Map.entry("qb", "http://purl.org/linked-data/cube"),
            Map.entry("oa", "http://www.w3.org/ns/oa#"),
            Map.entry("activitystreams", "https://www.w3.org/ns/activitystreams#"),
            Map.entry("csvw", "http://www.w3.org/ns/csvw#")
        );


    public static final List<String> RESERVED_WORDS = List.of(
            "urn",
            "http",
            "https",
            "abstract",
            "and",
            "andCond",
            "class",
            "classIn",
            "codeLists",
            "comment",
            "contributor",
            "constraint",
            "context",
            "created",
            "creator",
            "datatype",
            "defaultValue",
            "definition",
            "description",
            "editorialNote",
            "equivalentClass",
            "equivalentProperty",
            "example",
            "first",
            "graph",
            "hasPart",
            "hasValue",
            "homepage",
            "id",
            "identifier",
            "imports",
            "inScheme",
            "inValues",
            "isDefinedBy",
            "isPartOf",
            "isResourceIdentifier",
            "isXmlAttribute",
            "isXmlWrapper",
            "last",
            "label",
            "language",
            "languageIn",
            "localName",
            "maxCount",
            "maxLength",
            "memberOf",
            "minCount",
            "name",
            "node",
            "nodeKind",
            "not",
            "notCond",
            "or",
            "orCond",
            "order",
            "path",
            "pattern",
            "pointXY",
            "preferredXMLNamespaceName",
            "preferredXMLNamespacePrefix",
            "prefLabel",
            "property",
            "predicate",
            "range",
            "readOnlyValue",
            "references",
            "relations",
            "requires",
            "rootResource",
            "rest",
            "stem",
            "subClassOf",
            "subject",
            "subPropertyOf",
            "targetClass",
            "title",
            "type",
            "uniqueLang",
            "useContext",
            "uri",
            "versionInfo",
            "vertexXY",
            "xor");
}
