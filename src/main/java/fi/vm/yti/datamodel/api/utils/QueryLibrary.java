/*
 * Licensed under the European Union Public Licence (EUPL) V.1.1 
 */
package fi.vm.yti.datamodel.api.utils;

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


    final public static String listClassInRows =
            "SELECT DISTINCT ?mlabel ?label ?plabel WHERE {" +
                    " ?s a sh:NodeShape . " +
                    "?s rdfs:isDefinedBy ?model . " +
                    "?model rdfs:label ?mlabel . " +
                    "?s sh:name ?label . " +
                    "?s sh:property ?prop . " +
                    "?prop sh:name ?plabel . " +
                    "?prop sh:path ?p . " +
                    "FILTER(lang(?mlabel)=?lang) " +
                    "FILTER(lang(?label)=?lang) " +
                    "FILTER(lang(?plabel)=?lang)}";

    final public static String conceptQuery = LDHelper.expandSparqlQuery(true,
            "CONSTRUCT {" +
                    "?concept skos:prefLabel ?label . " +
                    "?concept rdf:type skos:Concept . " +
                    "?concept skos:definition ?definition . " +
                    "?concept skos:inScheme ?scheme . " +
                    "?scheme a skos:ConceptScheme . " +
                    "?scheme skos:prefLabel ?title . " +
                    "?concept termed:graph ?graph . " +
                    "?graph termed:id ?schemeUUID . }" +
                    "WHERE {" +
                    "?concept skosxl:prefLabel ?xlLabel . " +
                    "?concept skos:definition ?definition . " +
                    "?xlLabel skosxl:literalForm ?label . " +
                    "?concept termed:graph ?graph . " +
                    "?graph termed:id ?schemeUUID . " +
                    "?scheme termed:graph ?graph . " +
                    "?scheme skos:prefLabel ?title . " +
                    "?scheme a skos:ConceptScheme . " +
                    "}" +
                    "");

    final public static String skosXlToSkos = LDHelper.expandSparqlQuery(
            "CONSTRUCT {" +
                    "?concept skos:prefLabel ?label . " +
                    "?concept rdf:type skos:Concept . " +
                    "?concept skos:definition ?definition . " +
                    "?concept skos:inScheme ?terminology . " +
                    "?concept termed:graph ?graphID . " +
                    "?concept termed:id ?conceptID . " +
                    "?terminology skos:prefLabel ?title . " +
                    "} WHERE {" +
                    "?concept skosxl:prefLabel ?xlLabel . " +
                    "?concept skos:definition ?definition . " +
                    "?concept termed:graph ?graphID . " +
                    "?concept termed:id ?conceptID . "+
                    "?xlLabel skosxl:literalForm ?label . " +
                    "OPTIONAL {?terminology termed:graph ?graphID . " +
                    "?terminology skos:prefLabel ?title . }" +
                    "}");

    final public static String constructServiceCategories = LDHelper.expandSparqlQuery(true,
            "CONSTRUCT {" +
                    "?concept rdfs:label ?label . " +
                    "?concept rdf:type foaf:Group . " +
                    "?concept dcterms:identifier ?id . " +
                    "?concept dcterms:description ?note . " +
                    "?concept sh:order ?order . "+
                    "?concept dcterms:hasPart ?subConcept . "+
                    "} WHERE {" +
                    "GRAPH <urn:yti:servicecategories> { " +
                    "?concept skos:prefLabel ?label . " +
                    "?concept skos:notation ?id . " +
                    "?concept skos:note ?note . "+
                    "FILTER NOT EXISTS { ?concept skos:broader ?topConcept . }"+
                    "?concept sh:order ?order . "+
                   //  "OPTIONAL { ?concept skos:narrower ?subConcept . }"+
                   // "FILTER langMatches(lang(?label),?lang)" +
                   // "VALUES ?lang { 'fi' 'sv' 'en'}" +
                    "}}");

    final public static String fullModelQuery = LDHelper.expandSparqlQuery(true,
            "CONSTRUCT { "
                    + "?s ?p ?o . "
                    + "?graph owl:versionInfo ?versionInfo . "
                    + "?graph dcterms:requires ?req . "
                    + "?req rdfs:label ?reqLabel . "
                    + "?req a ?reqType . "
                    + "?req dcap:preferredXMLNamespaceName ?namespaces . "
                    + "?req dcap:preferredXMLNamespacePrefix ?prefixes . "
                    + "?graph dcterms:isPartOf ?group . "
                    + "?group a foaf:Group . "
                    + "?group rdfs:label ?groupLabel . "
                    + "?group dcterms:identifier ?code . "
                    + "?graph dcterms:contributor ?org . "
                    + "?org skos:prefLabel ?orgLabel . "
                    + "?org rdf:type foaf:Organization . "
                    + "?org dcterms:description ?orgDescription . "
                    + "} WHERE { "
                    + "GRAPH ?graph { "
                    + " ?s ?p ?o . "
                    + " ?graph dcterms:isPartOf ?group . "
                    + " ?graph dcterms:contributor ?org . "
                    + "OPTIONAL { ?org skos:prefLabel ?orgLabel1 . }"
                    + "OPTIONAL { ?org dcterms:description ?orgDescription1 . }"
                    + "} "
                    + "OPTIONAL { "
                    + "GRAPH ?graph { "
                    + " ?graph owl:versionInfo ?versionInfo . "
                    + " ?graph dcterms:requires ?req . "
                    + " ?req a ?reqType . "
                    + " VALUES ?reqType { dcap:DCAP dcap:MetadataVocabulary }}"
                    + " GRAPH ?req { "
                    + "  ?req rdfs:label ?reqLabel . "
                    + "  ?req dcap:preferredXMLNamespaceName ?namespaces . "
                    + "  ?req dcap:preferredXMLNamespacePrefix ?prefixes . "
                    + " }"
                    + "}"
                    + "GRAPH <urn:yti:servicecategories> { "
                    + "?group skos:notation ?code . "
                    + "?group skos:prefLabel ?groupLabel . "
                    + "}"
                    + "}");

    final public static String modelQuery = LDHelper.expandSparqlQuery(true,
                     "CONSTRUCT { "
                     + "?s ?p ?o . "
                     + "?graph owl:versionInfo ?versionInfo . "
                     + "?graph dcterms:requires ?req . "
                     + "?req rdfs:label ?reqLabel . "
                     + "?req a ?reqType . "
                     + "?req dcap:preferredXMLNamespaceName ?namespaces . "
                     + "?req dcap:preferredXMLNamespacePrefix ?prefixes . "
                     + "?graph dcterms:isPartOf ?group . "
                     + "?group a foaf:Group . "
                     + "?group rdfs:label ?groupLabel . "
                     + "?group dcterms:identifier ?code . "
                     + "?graph dcterms:contributor ?org . "
                     + "?org skos:prefLabel ?orgLabel . "
                     + "?org rdf:type foaf:Organization . "
                     + "?org dcterms:description ?orgDescription . "
                     + "} WHERE { "
                     + "GRAPH ?graph { "
                     + " ?s ?p ?o . "
                     + " ?graph dcterms:isPartOf ?group . "
                     + " ?graph dcterms:contributor ?org . "
                     + "OPTIONAL { ?org skos:prefLabel ?orgLabel1 . }"
                     + "OPTIONAL { ?org dcterms:description ?orgDescription1 . }"
                     + "} "
                     + "OPTIONAL { "
                     + "GRAPH ?graph { "
                     + " ?graph owl:versionInfo ?versionInfo . "
                     + " FILTER NOT EXISTS { ?graph owl:versionInfo 'INCOMPLETE' } "
                     + " ?graph dcterms:requires ?req . "
                     + " ?req a ?reqType . "
                     + " VALUES ?reqType { dcap:DCAP dcap:MetadataVocabulary }}"
                     + " GRAPH ?req { "
                     + "  ?req rdfs:label ?reqLabel . "
                     + "  ?req dcap:preferredXMLNamespaceName ?namespaces . "
                     + "  ?req dcap:preferredXMLNamespacePrefix ?prefixes . "
                     + " }"
                     + "}"
                     + "GRAPH <urn:yti:servicecategories> { "
                     + "?group skos:notation ?code . "
                     + "?group skos:prefLabel ?groupLabel . "
                     + "}"
                     // TODO : Not working because ?s ?p ?o gets also the old label and description
                     //+ "GRAPH <urn:yti:organizations> { "
                     //+ "OPTIONAL { ?org skos:prefLabel ?orgLabel2 . }"
                     //+ "OPTIONAL { ?org dcterms:description ?orgDescription2 . }"
                     //+ "}"
                     //+ "BIND(IF(BOUND(?orgLabel2), ?orgLabel2, ?orgLabel1) as ?orgLabel)"
                     //+ "BIND(IF(BOUND(?orgDescription2), ?orgDescription2, ?orgDescription1) as ?orgDescription)"
                     + "}");

    final public static String modelsByGroupQuery = LDHelper.expandSparqlQuery(true,
            "CONSTRUCT { "
                + "?graph owl:versionInfo ?versionInfo . "
                + "?graphName rdfs:label ?label . "
                + "?graphName rdfs:comment ?comment . "
                + "?graphName a ?type . "
                + "?graphName dcterms:isPartOf ?group . "
                + "?graphName dcap:preferredXMLNamespaceName ?namespace . "
                + "?graphName dcap:preferredXMLNamespacePrefix ?prefix .  "
                + "?group a foaf:Group . "
                + "?group rdfs:label ?groupLabel . "
                + "?graphName dcterms:contributor ?org . "
                + "?org skos:prefLabel ?orgLabel . "
                + "?org a foaf:Organization . "
                + "} WHERE { "
                + "GRAPH <urn:csc:iow:sd> { "
                + " ?metaGraph a sd:NamedGraph . "
                + " ?metaGraph sd:name ?graphName . "
                + "} "
                + "GRAPH <urn:yti:servicecategories> { "
                + "?group skos:notation ?groupCode . "
                + "?group skos:prefLabel ?groupLabel . "
                + "}"
                + "GRAPH ?graphName { "
                + " ?graph owl:versionInfo ?versionInfo . "
                + " FILTER NOT EXISTS { ?graph owl:versionInfo 'INCOMPLETE' } "
                + "?graphName dcterms:isPartOf ?group . "
                + "?graphName a ?type . "
                + "?graphName rdfs:label ?label . "
                + "OPTIONAL { ?graphName rdfs:comment ?comment . }"
                + "?graphName dcterms:contributor ?org . "
                + "OPTIONAL { ?org skos:prefLabel ?orgLabel1 . }"
                + "?graphName dcap:preferredXMLNamespaceName ?namespace . "
                + "?graphName dcap:preferredXMLNamespacePrefix ?prefix .  "
                + "}"
                + "OPTIONAL { GRAPH <urn:yti:organizations> { "
                + "?org skos:prefLabel ?orgLabel2 . "
                + "}}"
                + "BIND(IF(BOUND(?orgLabel2), ?orgLabel2, ?orgLabel1) as ?orgLabel)"
                + "}");


    final public static String fullModelsByGroupQuery = LDHelper.expandSparqlQuery(true,
            "CONSTRUCT { "
                    + "?graph owl:versionInfo ?versionInfo . "
                    + "?graphName rdfs:label ?label . "
                    + "?graphName rdfs:comment ?comment . "
                    + "?graphName a ?type . "
                    + "?graphName dcterms:isPartOf ?group . "
                    + "?graphName dcap:preferredXMLNamespaceName ?namespace . "
                    + "?graphName dcap:preferredXMLNamespacePrefix ?prefix .  "
                    + "?group a foaf:Group . "
                    + "?group rdfs:label ?groupLabel . "
                    + "?graphName dcterms:contributor ?org . "
                    + "?org skos:prefLabel ?orgLabel . "
                    + "?org a foaf:Organization . "
                    + "} WHERE { "
                    + "GRAPH <urn:csc:iow:sd> { "
                    + " ?metaGraph a sd:NamedGraph . "
                    + " ?metaGraph sd:name ?graphName . "
                    + "} "
                    + "GRAPH <urn:yti:servicecategories> { "
                    + "?group skos:notation ?groupCode . "
                    + "?group skos:prefLabel ?groupLabel . "
                    + "}"
                    + "GRAPH ?graphName { "
                    + " ?graph owl:versionInfo ?versionInfo . "
                    + "?graphName dcterms:isPartOf ?group . "
                    + "?graphName a ?type . "
                    + "?graphName rdfs:label ?label . "
                    + "OPTIONAL {?graphName rdfs:comment ?comment . }"
                    + "?graphName dcterms:contributor ?org . "
                    + "OPTIONAL { ?org skos:prefLabel ?orgLabel1 . }"
                    + "?graphName dcap:preferredXMLNamespaceName ?namespace . "
                    + "?graphName dcap:preferredXMLNamespacePrefix ?prefix .  "
                    + "}"
                    + "OPTIONAL { GRAPH <urn:yti:organizations> { "
                    + "?org skos:prefLabel ?orgLabel2 . "
                    + "}}"
                    + "BIND(IF(BOUND(?orgLabel2), ?orgLabel2, ?orgLabel1) as ?orgLabel)"
                    + "}");

    final public static String listClassesQuery = LDHelper.expandSparqlQuery(
                    "CONSTRUCT { "
                    + "?class sh:name ?label . "
                    + "?class sh:description ?description . "
                    + "?class a ?type . "
                    + "?class dcterms:modified ?modified . "
                    + "?class rdfs:isDefinedBy ?source . "
                    + "?source rdfs:label ?sourceLabel . "
                    + "?source a ?sourceType . "
                    + "} WHERE { "
                    + "GRAPH ?hasPartGraph { "
                    + "?library dcterms:hasPart ?class . } "
                    + "GRAPH ?class { "
                    + "?class dcterms:modified ?modified . "
                    + "?class sh:name ?label . "
                    + "OPTIONAL { ?class sh:description ?description . } "
                    + "?class a ?type . "
                    + "VALUES ?type { rdfs:Class sh:NodeShape } "
                    + "?class rdfs:isDefinedBy ?source .  } "
                    + "GRAPH ?source { "
                    + "?source a ?sourceType . "
                    + "?source rdfs:label ?sourceLabel . "
                    + "}}"      
        );
    
     final public static String classQuery = LDHelper.expandSparqlQuery(
                     "CONSTRUCT { "
                     + "?s ?p ?o . "
                     + "?graph rdfs:isDefinedBy ?library . "
                     + "?library a ?type . "
                     + "?library rdfs:label ?label . "
                     + "} WHERE { "
                        + "{"
                        + "GRAPH ?graph {"
                        + "?graph a sh:NodeShape ."
                        + "?graph sh:targetClass ?refGraph ."
                        + "?s ?p ?o . "
                        + "?graph rdfs:isDefinedBy ?library . "
                        + "} "
                        + "GRAPH ?library { "
                        + " ?library a ?type . "
                        + " ?library rdfs:label ?label . }"
                     + "} UNION {"
                        + "GRAPH ?graph { "
                        + "?graph a rdfs:Class . "
                        + "FILTER NOT EXISTS { ?graph sh:targetClass ?any . }"
                        + "?s ?p ?o . "
                        + "?graph rdfs:isDefinedBy ?library . "
                        + "}"
                        + "GRAPH ?library { "
                        + " ?library a ?type . "
                        + " ?library rdfs:label ?label . "
                        + "}"
                     + "}"
                     + "}");
     
     
     final public static String listPredicatesQuery = LDHelper.expandSparqlQuery(
             "CONSTRUCT { "
                + "?property rdfs:label ?label . "
                + "?property rdfs:comment ?description . "
                + "?property a ?type . "
                + "?property rdfs:isDefinedBy ?source . "
                + "?source rdfs:label ?sourceLabel . "
                + "?source a ?sourceType . "
                + "?property dcterms:modified ?date . } "
                + "WHERE { "
                + "GRAPH ?hasPartGraph { "
                + "?library dcterms:hasPart ?property . } "
                + "GRAPH ?property { ?property rdfs:label ?label . "
                    + "OPTIONAL { ?property rdfs:comment ?description . } "
                    + "VALUES ?type { owl:ObjectProperty owl:DatatypeProperty } "
                    + "?property a ?type . "
                    + "?property rdfs:isDefinedBy ?source . "
                    + "OPTIONAL {?property dcterms:modified ?date . } "
                + "} "
                + "GRAPH ?source { ?source a ?sourceType . ?source rdfs:label ?sourceLabel . }}"
     );
     
      final public static String predicateQuery = LDHelper.expandSparqlQuery(
                 "CONSTRUCT { "
                 + "?s ?p ?o . "
                 + "?graph rdfs:isDefinedBy ?library . "
                 + "?library a ?type . "
                 + "?library rdfs:label ?label . "
                 + "} WHERE { "
                 + "GRAPH ?graph { "
                 + "?s ?p ?o . "
                 + "?graph rdfs:isDefinedBy ?library . } "
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
        
        final public static String externalClassQuery = LDHelper.expandSparqlQuery(
                    "CONSTRUCT { "
                    + "?externalModel rdfs:label ?externalModelLabel . "
                    + "?classIRI rdfs:isDefinedBy ?externalModel . "
                    + "?classIRI a rdfs:Class . "
                    + "?classIRI owl:versionInfo ?draft . "
                    + "?classIRI sh:name ?label . "
                    + "?classIRI sh:description ?comment . "
                    + "?classIRI sh:property ?property . "
                    + "?property a sh:PropertyShape . "
                    + "?property sh:datatype ?datatype . "
                    + "?property dcterms:type ?propertyType . "
                    + "?property sh:node ?valueClass . "
                    + "?property sh:path ?predicate . "
                    + "?property sh:name ?propertyLabel . "
                    + "?property sh:description ?propertyComment . "
                     + "} WHERE { "
                     + "SERVICE ?modelService { "
                     + "GRAPH ?library { "
                     + "?library dcterms:requires ?externalModel . "
                    + "?externalModel rdfs:label ?externalModelLabel . "
                     + "}}"
                    + "GRAPH ?externalModel {"
                    + "?classIRI a ?type . "
                    + "FILTER(STRSTARTS(STR(?classIRI), STR(?externalModel)))"
                    + "VALUES ?type { rdfs:Class owl:Class sh:NodeShape } "
                    /* Get class label */
                     + "{?classIRI ?labelProp ?labelStr . FILTER(LANG(?labelStr) = '') BIND(STRLANG(?labelStr,'en') as ?label) "
                     + "VALUES ?labelProp { rdfs:label sh:name } }"
                     + "UNION"
                     + "{ ?classIRI ?labelProp ?label . FILTER(LANG(?label)!='') "
                     + "VALUES ?labelProp { rdfs:label sh:name } }"
                     /* Get class comment */
                    + "{ ?classIRI ?commentPred ?commentStr . "
                     + "VALUES ?commentPred { rdfs:comment skos:definition dcterms:description dc:description prov:definition sh:description }"
                     + "FILTER(LANG(?commentStr) = '') BIND(STRLANG(STR(?commentStr),'en') as ?comment) }"
                     + "UNION"
                     + "{ ?classIRI ?commentPred ?comment . "
                     + "VALUES ?commentPred { rdfs:comment skos:definition dcterms:description dc:description prov:definition sh:description }"
                     + " FILTER(LANG(?comment)!='') }"
                    
                    + "OPTIONAL { "
                    + "?classIRI rdfs:subClassOf* ?superclass . "
                    + "?predicate rdfs:domain ?superclass . "
                    + "BIND(UUID() AS ?property)"
                    + "{"
                    + "?predicate a owl:DatatypeProperty . "
                    + "FILTER NOT EXISTS { ?predicate a owl:ObjectProperty }"
                    + "BIND(owl:DatatypeProperty as ?propertyType) "
                    + "} UNION {"
                    + "?predicate a owl:ObjectProperty . "
                    + "FILTER NOT EXISTS { ?predicate a owl:DatatypeProperty }"
                    + "BIND(owl:ObjectProperty as ?propertyType) "
                    + "} UNION {"
                    /* Treat owl:AnnotationProperty as DatatypeProperty */
                    + "?predicate a owl:AnnotationProperty. "
                    + "?predicate rdfs:label ?atLeastSomeLabel . "
                    + "FILTER NOT EXISTS { ?predicate a owl:DatatypeProperty }"
                    + "BIND(owl:DatatypeProperty as ?propertyType) "
                    + "} UNION {"
                    /* TODO: Add all XSD types? */
                    + "VALUES ?literalValue { rdfs:Literal xsd:String }"
                    /* IF Predicate Type is rdf:Property and range is rdfs:Literal = DatatypeProperty */
                    + "?predicate a rdf:Property . "
                    + "?predicate rdfs:range ?literalValue ."
                    + "BIND(owl:DatatypeProperty as ?propertyType) "
                    + "FILTER NOT EXISTS { ?predicate a ?multiType . VALUES ?multiType { owl:DatatypeProperty owl:ObjectProperty } }"
                     + "} UNION {"
                    /* IF Predicate Type is rdf:Property and range is rdfs:Resource then property is object property */
                    + "?predicate a rdf:Property . "
                    + "?predicate rdfs:range rdfs:Resource ."
                    + "BIND(owl:ObjectProperty as ?propertyType) "
                    + "FILTER NOT EXISTS { ?predicate a ?multiType . VALUES ?multiType { owl:DatatypeProperty owl:ObjectProperty } }"
                    + "} UNION {"
                    /* IF Predicate Type is rdf:Property and range is resource that is class or thing */
                    + "?predicate a rdf:Property . "
                    + "FILTER NOT EXISTS { ?predicate a ?multiType . VALUES ?multiType { owl:DatatypeProperty owl:ObjectProperty } }"
                    + "?predicate rdfs:range ?rangeClass . "
                    + "FILTER(?rangeClass!=rdfs:Literal)"
                    + "?rangeClass a ?rangeClassType . "
                    + "VALUES ?rangeClassType { skos:Concept owl:Thing }"
                    + "BIND(owl:ObjectProperty as ?propertyType) "
                    + "}"
                    
                    + "OPTIONAL { ?predicate a owl:DatatypeProperty . ?predicate rdfs:range ?datatype . FILTER (!isBlank(?datatype))  } "
                    + "OPTIONAL { ?predicate a owl:ObjectProperty . ?predicate rdfs:range ?valueClass . FILTER (!isBlank(?valueClass)) } "

                    /* Predicate label - if lang unknown create english tag */
                    + "OPTIONAL {?predicate rdfs:label ?propertyLabelStr . FILTER(LANG(?propertyLabelStr) = '') BIND(STRLANG(?propertyLabelStr,'en') as ?propertyLabel) }"
                    + "OPTIONAL { ?predicate rdfs:label ?propertyLabel . FILTER(LANG(?propertyLabel)!='') }"
                   
                    /* Predicate comments - if lang unknown create english tag */
                    + "OPTIONAL { "
                    + "VALUES ?predicateCommentPred { rdfs:comment skos:definition dcterms:description dc:description }"
                    + "?predicate ?predicateCommentPred ?propertyCommentStr . FILTER(LANG(?propertyCommentStr) = '') "
                    + "BIND(STRLANG(STR(?propertyCommentStr),'en') as ?propertyComment) }"
                    + "OPTIONAL { "
                    + "VALUES ?predicateCommentPred { rdfs:comment skos:definition dcterms:description dc:description }"
                    + "?predicate ?predicateCommentPred ?propertyCommentToStr . FILTER(LANG(?propertyCommentToStr)!='') "
                    + "BIND(?propertyCommentToStr as ?propertyComment) }"

                    + "}"
                    + "} }");
        
        final public static String externalClassQueryWithSchemaOrg = LDHelper.expandSparqlQuery(
                    "CONSTRUCT { "
                    + "?externalModel rdfs:label ?externalModelLabel . "
                    + "?classIRI owl:versionInfo ?draft . "
                    + "?classIRI rdfs:isDefinedBy ?externalModel . "
                    + "?classIRI a rdfs:Class . "
                    + "?classIRI sh:name ?label . "
                    + "?classIRI sh:description ?comment . "
                    + "?classIRI sh:property ?property . "
                    + "?property a sh:PropertyShape . "
                    + "?property sh:datatype ?datatype . "
                    + "?property dcterms:type ?propertyType . "
                    + "?property sh:node ?valueClass . "
                    + "?property sh:path ?predicate . "
                    + "?property sh:name ?propertyLabel . "
                    + "?property sh:description ?propertyComment . "
                     + "} WHERE { "
                     + "SERVICE ?modelService { "
                     + "GRAPH ?library { "
                     + "?library dcterms:requires ?externalModel . "
                    + "?externalModel rdfs:label ?externalModelLabel . "
                     + "}}"
                    + "GRAPH ?externalModel {"
                    + "?classIRI a ?type . "
                    + "FILTER(STRSTARTS(STR(?classIRI), STR(?externalModel)))"
                    + "VALUES ?type { rdfs:Class owl:Class sh:NodeShape } "
                    /* Get class label */
                     + "{?classIRI ?labelProp ?labelStr . FILTER(LANG(?labelStr) = '') BIND(STRLANG(?labelStr,'en') as ?label) "
                     + "VALUES ?labelProp { rdfs:label sh:name } }"
                     + "UNION"
                     + "{ ?classIRI ?labelProp ?label . FILTER(LANG(?label)!='') "
                     + "VALUES ?labelProp { rdfs:label sh:name } }"
                     /* Get class comment */
                     + "{ ?classIRI ?commentPred ?commentStr . "
                     + "VALUES ?commentPred { rdfs:comment skos:definition dcterms:description dc:description prov:definition sh:description }"
                     + "FILTER(LANG(?commentStr) = '') BIND(STRLANG(STR(?commentStr),'en') as ?comment) }"
                     + "UNION"
                     + "{ ?classIRI ?commentPred ?comment . "
                     + "VALUES ?commentPred { rdfs:comment skos:definition dcterms:description dc:description prov:definition sh:description }"
                     + " FILTER(LANG(?comment)!='') }"
                    
                    + "OPTIONAL { "
                    + "{?classIRI rdfs:subClassOf* ?superclass . "
                    + "?predicate rdfs:domain ?superclass . } UNION {"
                    + "?classIRI a ?schemaClass . "
                    + "?predicate schema:domainIncludes ?classIRI ."
                    + "}"
                    + "BIND(UUID() AS ?property)"
                    + "VALUES ?range { rdfs:range schema:rangeIncludes }"
                    + "{"
                    + "?predicate a owl:DatatypeProperty . "
                    + "FILTER NOT EXISTS { ?predicate a owl:ObjectProperty }"
                    + "BIND(owl:DatatypeProperty as ?propertyType) "
                    + "} UNION {"
                    + "?predicate a owl:ObjectProperty . "
                    + "FILTER NOT EXISTS { ?predicate a owl:DatatypeProperty }"
                    + "BIND(owl:ObjectProperty as ?propertyType) "
                    + "} UNION {"
                    /* Treat owl:AnnotationProperty as DatatypeProperty */
                    + "?predicate a owl:AnnotationProperty. "
                    + "?predicate rdfs:label ?atLeastSomeLabel . "
                    + "FILTER NOT EXISTS { ?predicate a owl:DatatypeProperty }"
                    + "BIND(owl:DatatypeProperty as ?propertyType) "
                    + "} UNION {"
                    + "VALUES ?literalValue { rdfs:Literal schema:Text schema:Integer schema:DateTime schema:Boolean schema:Date }"
                    /* IF Predicate Type is rdf:Property and range is rdfs:Literal = DatatypeProperty */
                    + "?predicate a rdf:Property . "
                    + "?predicate ?range ?literalValue ."
                    + "BIND(owl:DatatypeProperty as ?propertyType) "
                    + "FILTER NOT EXISTS { ?predicate a ?multiType . VALUES ?multiType { owl:DatatypeProperty owl:ObjectProperty } }"
                     + "} UNION {"
                    /* IF Predicate Type is rdf:Property and range is rdfs:Resource then property is object property */
                    + "?predicate a rdf:Property . "
                    + "?predicate ?range rdfs:Resource ."
                    + "BIND(owl:ObjectProperty as ?propertyType) "
                    + "FILTER NOT EXISTS { ?predicate a ?multiType . VALUES ?multiType { owl:DatatypeProperty owl:ObjectProperty } }"
                    + "} UNION {"
                    /* IF Predicate Type is rdf:Property and range is resource that is class or thing */
                    + "?predicate a rdf:Property . "
                    + "FILTER NOT EXISTS { ?predicate a ?multiType . VALUES ?multiType { owl:DatatypeProperty owl:ObjectProperty } }"
                    + "?predicate ?range ?rangeClass . "
                    + "FILTER(?rangeClass!=rdfs:Literal)"
                    + "?rangeClass a ?rangeClassType . "
                    + "FILTER(?rangeClassType!=schema:DataType)"
                    + "VALUES ?rangeClassType { skos:Concept owl:Thing rdfs:Class }"
                    + "BIND(owl:ObjectProperty as ?propertyType) "
                    + "}"
                    
                    + "OPTIONAL { ?predicate a owl:DatatypeProperty . ?predicate rdfs:range ?datatype . FILTER (!isBlank(?datatype))  } "
                    + "OPTIONAL { ?predicate a owl:ObjectProperty . ?predicate ?range ?valueClass . FILTER (!isBlank(?valueClass))} "

                    /* GET PROPERTY LABEL */
                    + "{ ?predicate ?propertyLabelPred ?propertyLabelStr . "
                    + "VALUES ?propertyLabelPred { rdfs:label sh:name dc:title dcterms:title }"
                    + "FILTER(LANG(?propertyLabelStr) = '') BIND(STRLANG(STR(?propertyLabelStr),'en') as ?propertyLabel) }"
                    + "UNION"
                    + "{ ?predicate ?propertyLabelPred ?propertyLabel . "
                    + "VALUES ?propertyLabelPred { rdfs:label sh:name dc:title dcterms:title }"
                    + " FILTER(LANG(?propertyLabel)!='') }"

                    /* GET PROPERTY COMMENT */
                    + "{ ?predicate ?propertyCommentPred ?propertyCommentStr . "
                    + "VALUES ?propertyCommentPred { rdfs:comment skos:definition dcterms:description dc:description prov:definition sh:description }"
                    + "FILTER(LANG(?propertyCommentStr) = '') BIND(STRLANG(STR(?propertyCommentStr),'en') as ?propertyComment) }"
                    + "UNION"
                    + "{ ?predicate ?propertyCommentPred ?propertyComment . "
                    + "VALUES ?propertyCommentPred { rdfs:comment skos:definition dcterms:description dc:description prov:definition sh:description }"
                    + "FILTER(LANG(?propertyComment)!='') }"

                    /* Predicate label - if lang unknown create english tag */
                    //+ "OPTIONAL {?predicate rdfs:label ?propertyLabelStr . FILTER(LANG(?propertyLabelStr) = '') BIND(STRLANG(?propertyLabelStr,'en') as ?propertyLabel) }"
                   // + "OPTIONAL { ?predicate rdfs:label ?propertyLabel . FILTER(LANG(?propertyLabel)!='') }"
                   
                    /* Predicate comments - if lang unknown create english tag */
                    //+ "OPTIONAL { "
                    //+ "VALUES ?predicateCommentPred { rdfs:comment skos:definition dcterms:description dc:description }"
                    //+ "?predicate ?predicateCommentPred ?propertyCommentStr . FILTER(LANG(?propertyCommentStr) = '') "
                    //+ "BIND(STRLANG(STR(?propertyCommentStr),'en') as ?propertyComment) }"
                    //+ "OPTIONAL { "
                    // "VALUES ?predicateCommentPred { rdfs:comment skos:definition dcterms:description dc:description }"
                    //+ "?predicate ?predicateCommentPred ?propertyCommentToStr . FILTER(LANG(?propertyCommentToStr)!='') "
                    //+ "BIND(?propertyCommentToStr as ?propertyComment) }"

                    + "}"
                    + "} }");
        
                final public static String externalShapeQuery = LDHelper.expandSparqlQuery(
                    "CONSTRUCT  { "
                    + "?shapeIRI owl:versionInfo ?draft . "
                    + "?shapeIRI dcterms:modified ?modified . "
                    + "?shapeIRI dcterms:created ?creation . "
                    + "?shapeIRI sh:targetClass ?classIRI . "
                    + "?shapeIRI a rdfs:Class . "
                    + "?shapeIRI a sh:NodeShape . "
                    + "?shapeIRI rdfs:isDefinedBy ?model . "
                    + "?model rdfs:label ?externalModelLabel . "
                    + "?shapeIRI sh:name ?label . "
                    + "?shapeIRI sh:description ?comment . "
                    + "?shapeIRI sh:property ?property . "
                    + "?property a sh:PropertyShape . "
                    + "?property dcterms:type ?propertyType . "    
                    + "?property sh:path ?predicate . "
                    + "?property sh:name ?propertyLabel .  "
                    + "?property sh:description ?propertyComment .  "
                    /* TODO: Fix pointing to AP classes? */
                    + "?property sh:node ?valueClass . "
                    + "?property sh:class ?valueClass . "
                    + "?property sh:datatype ?datatype . "
                    + "} WHERE { "
                    + "BIND(now() as ?creation) "
                    + "BIND(now() as ?modified) "
                    + "SERVICE ?modelService { "
                    + "GRAPH ?model { "
                    + "?model dcterms:requires ?externalModel . "
                    + "?externalModel rdfs:label ?externalModelLabel . "
                    + "}}"
                    + "GRAPH ?externalModel { "
                    + "OPTIONAL {"

                    /* Labels */
                     + "{?classIRI ?labelProp ?labelStr . FILTER(LANG(?labelStr) = '') BIND(STRLANG(?labelStr,'en') as ?label) "
                     + "VALUES ?labelProp { rdfs:label sh:name dc:title dcterms:title } }"
                     + "UNION"
                     + "{ ?classIRI ?labelProp ?label . FILTER(LANG(?label)!='') "
                     + "VALUES ?labelProp { rdfs:label sh:name dc:title dcterms:title } }"

                     /* Comments */
                     + "{ ?classIRI ?commentPred ?commentStr . "
                     + "VALUES ?commentPred { rdfs:comment skos:definition dcterms:description dc:description prov:definition sh:description }"
                     + "FILTER(LANG(?commentStr) = '') BIND(STRLANG(STR(?commentStr),'en') as ?comment) }"
                     + "UNION"
                     + "{ ?classIRI ?commentPred ?comment . "
                     + "VALUES ?commentPred { rdfs:comment skos:definition dcterms:description dc:description prov:definition sh:description }"
                     + " FILTER(LANG(?comment)!='') }"
                     + "}"
                            
                    + "OPTIONAL { "
                    + "?classIRI rdfs:subClassOf* ?superclass . "
                    + "?predicate rdfs:domain ?superclass .  "
                    + "BIND(UUID() AS ?property)"   
                            
                    /* Types of properties */        
                    + "{"
                    + "?predicate a owl:DatatypeProperty . "
                    + "FILTER NOT EXISTS { ?predicate a owl:ObjectProperty }"
                    + "BIND(owl:DatatypeProperty as ?propertyType) "
                    + "} UNION {"
                    + "?predicate a owl:ObjectProperty . "
                    + "FILTER NOT EXISTS { ?predicate a owl:DatatypeProperty }"
                    + "BIND(owl:ObjectProperty as ?propertyType) "
                    + "} UNION {"
                    + "?predicate a owl:AnnotationProperty. "
                    + "?predicate rdfs:label ?atLeastSomeLabel . "
                    + "FILTER NOT EXISTS { ?predicate a owl:DatatypeProperty }"
                    + "BIND(owl:DatatypeProperty as ?propertyType) "
                    + "} UNION {"
                    + "?predicate a rdf:Property . "
                    + "?predicate rdfs:range rdfs:Literal ."
                    + "BIND(owl:DatatypeProperty as ?propertyType) "
                    + "FILTER NOT EXISTS { ?predicate a ?multiType . VALUES ?multiType { owl:DatatypeProperty owl:ObjectProperty } }"
                     + "} UNION {"
                     + "?predicate a rdf:Property . "
                    + "?predicate rdfs:range rdfs:Resource ."
                    + "BIND(owl:ObjectProperty as ?propertyType) "
                    + "FILTER NOT EXISTS { ?predicate a ?multiType . VALUES ?multiType { owl:DatatypeProperty owl:ObjectProperty } }"
                    + "}UNION {"
                     + "?predicate a rdf:Property . "
                    + "FILTER NOT EXISTS { ?predicate a ?multiType . VALUES ?multiType { owl:DatatypeProperty owl:ObjectProperty } }"
                    + "?predicate rdfs:range ?rangeClass . "
                    + "FILTER(?rangeClass!=rdfs:Literal)"
                    + "?rangeClass a ?rangeClassType . "
                    + "VALUES ?rangeClassType { skos:Concept owl:Thing rdfs:Class }"
                    + "BIND(owl:ObjectProperty as ?propertyType) "
                    + "}"

                    + "OPTIONAL { ?predicate a owl:DatatypeProperty . ?predicate rdfs:range ?datatype . FILTER (!isBlank(?datatype))  } "
                    + "OPTIONAL { ?predicate a owl:ObjectProperty . ?predicate rdfs:range ?valueClass . FILTER (!isBlank(?valueClass)) } "

                    /* GET PROPERTY LABEL */
                    + "{ ?predicate ?propertyLabelPred ?propertyLabelStr . "
                    + "VALUES ?propertyLabelPred { rdfs:label sh:name dc:title dcterms:title }"
                    + "FILTER(LANG(?propertyLabelStr) = '') BIND(STRLANG(STR(?propertyLabelStr),'en') as ?propertyLabel) }"
                    + "UNION"
                    + "{ ?predicate ?propertyLabelPred ?propertyLabel . "
                    + "VALUES ?propertyLabelPred { rdfs:label sh:name dc:title dcterms:title }"
                    + " FILTER(LANG(?propertyLabel)!='') }"

                    /* GET PROPERTY COMMENT */
                    + "{ ?predicate ?propertyCommentPred ?propertyCommentStr . "
                    + "VALUES ?propertyCommentPred { rdfs:comment skos:definition dcterms:description dc:description prov:definition sh:description }"
                    + "FILTER(LANG(?propertyCommentStr) = '') BIND(STRLANG(STR(?propertyCommentStr),'en') as ?propertyComment) }"
                    + "UNION"
                    + "{ ?predicate ?propertyCommentPred ?propertyComment . "
                    + "VALUES ?propertyCommentPred { rdfs:comment skos:definition dcterms:description dc:description prov:definition sh:description }"
                    + " FILTER(LANG(?propertyComment)!='') }"


                    /* Predicate label - if lang unknown create english tag */
                   // + "OPTIONAL {?predicate rdfs:label ?propertyLabelStr . FILTER(LANG(?propertyLabelStr) = '') BIND(STRLANG(?propertyLabelStr,'en') as ?propertyLabel) }"
                   // + "OPTIONAL { ?predicate rdfs:label ?propertyLabel . FILTER(LANG(?propertyLabel)!='') }"

                    /* Predicate comments - if lang unknown create english tag */
                    //+ "OPTIONAL { "
                    //+ "VALUES ?predicateCommentPred { rdfs:comment skos:definition dcterms:description dc:description }"
                    //+ "?predicate ?predicateCommentPred ?propertyCommentStr . FILTER(LANG(?propertyCommentStr) = '') "
                    //+ "BIND(STRLANG(STR(?propertyCommentStr),'en') as ?propertyComment) }"
                    //+ "OPTIONAL { "
                    //+ "VALUES ?predicateCommentPred { rdfs:comment skos:definition dcterms:description dc:description }"
                    //+ "?predicate ?predicateCommentPred ?propertyCommentToStr . FILTER(LANG(?propertyCommentToStr)!='') "
                    //+ "BIND(?propertyCommentToStr as ?propertyComment) }"
                

                    + "}"    
                    + "}"
                    + "}");
     
}
