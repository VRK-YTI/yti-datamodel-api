/*
 * Licensed under the European Union Public Licence (EUPL) V.1.1 
 */
package com.csc.fi.ioapi.utils;

/**
 *
 * @author malonen
 */
public class QueryLibrary {
    
        final public static String provModelQuery = 
                "CONSTRUCT {"
                + "?every ?darn ?thing . }"
                + "WHERE {"
                + "GRAPH ?graph {"
                + " ?every ?darn ?thing . }"
                + "}";
    
    final public static String modelQuery = LDHelper.expandSparqlQuery(
                     "CONSTRUCT { "
                     + "?s ?p ?o . "
                     + "?graph dcterms:requires ?req . "
                     + "?req rdfs:label ?reqLabel . "
                     + "?req a ?reqType . "
                     + "?req dcap:preferredXMLNamespaceName ?namespaces . "
                     + "?req dcap:preferredXMLNamespacePrefix ?prefixes . "
                     + "?graph dcterms:isPartOf ?group . "
                     + "?group a foaf:Group . "
                     + "?group rdfs:label ?groupLabel . "
                     + "?group foaf:homepage ?homepage . "
                     + "} WHERE { "
                     + "GRAPH ?graph { "
                     + " ?s ?p ?o . "
                     + "} "
                     + "OPTIONAL { "
                     + "GRAPH ?graph {"
                     + " ?graph dcterms:requires ?req . "
                     + " ?req a ?reqType . "
                     + " VALUES ?reqType { dcap:DCAP dcap:MetadataVocabulary }}"
                     + " GRAPH ?req { "
                     + "  ?req rdfs:label ?reqLabel . "
                     + "  ?req dcap:preferredXMLNamespaceName ?namespaces . "
                     + "  ?req dcap:preferredXMLNamespacePrefix ?prefixes . "
                     + " }"
                     + "}" 
                     + "GRAPH <urn:csc:iow:sd> { "
                     + " ?metaGraph a sd:NamedGraph . "
                     + " ?metaGraph sd:name ?graph . "
                     + " ?metaGraph dcterms:isPartOf ?group . "
                     + "} "
                     + "GRAPH <urn:csc:groups> { "
                     + " ?group a foaf:Group . "
                     + " ?group foaf:homepage ?homepage . "
                     + " ?group rdfs:label ?groupLabel . "
                     + "}"
                     + "}");
    
     final public static String classQuery = LDHelper.expandSparqlQuery(
                     "CONSTRUCT { "
                     + "?s ?p ?o . "
                     + "?graph rdfs:isDefinedBy ?library . "
                     + "?library a ?type . "
                     + "?library rdfs:label ?label . "
                     + "} WHERE { "
                     + "GRAPH ?graph { "
                     + "?s ?p ?o . "
                     + "?graph rdfs:isDefinedBy ?library . "
                     + "}"
                     + "GRAPH ?library { "
                     + " ?library a ?type . "
                     + " ?library rdfs:label ?label . "
                     + "}"
                     + "}");
     
      final public static String predicateQuery = LDHelper.expandSparqlQuery(
                 "CONSTRUCT { "
                 + "?s ?p ?o . "
                 + "?graph rdfs:isDefinedBy ?library . "
                 + "?library a ?type . "
                 + "?library rdfs:label ?label . "
                 + "} WHERE { "
                 + "GRAPH ?graph { "
                 + "?s ?p ?o . "
                 + "?graph rdfs:isDefinedBy ?library . }"
                 + "GRAPH ?library { "
                 + " ?library a ?type . "
                 + " ?library rdfs:label ?label . "
                 + "}"
                 + "}");
      
      
        final public static String hasPartListQuery = LDHelper.expandSparqlQuery(
                 "CONSTRUCT { "
                 + "?model a ?type . "
                 + "?model dcterms:isPartOf ?group . "
                 + "?model dcterms:hasPart ?resource . "
                 + "} WHERE { "
                 + "GRAPH ?hasPartGraph { "
                 + "?model dcterms:hasPart ?resource . "
                 + "}"
                 + "GRAPH ?model { "
                 + "?model dcterms:isPartOf ?group . "
                 + "?model a ?type . "
                 + "}"
                 + "}");  
     
}
