/*
 * Licensed under the European Union Public Licence (EUPL) V.1.1
 */
package fi.vm.yti.datamodel.api.utils;

/**
 * @author malonen
 */
public class QueryLibrary {

    final public static String listContainersByPreflabelQuery =
        "SELECT ?uri ?prefLabel ?description ?status ?modified WHERE {" +
            "GRAPH ?uri { ?uri a owl:Ontology . " +
            "?uri rdfs:label ?prefLabel . " +
            "OPTIONAL { ?uri rdfs:label ?prefLabelLang . FILTER( lang(?prefLabelLang) = ?language ) }" +
            "?uri owl:versionInfo ?status . " +
            "VALUES ?status {?statusList}" +
            "OPTIONAL{?uri rdfs:comment ?description . }" +
            "?uri dcterms:modified ?modified . " +
            "}} ORDER BY (!bound(?prefLabelLang)) ?prefLabelLang";

    final public static String listContainersByModifiedQuery =
        "SELECT ?uri ?prefLabel ?description ?status ?modified WHERE {" +
            "GRAPH ?uri { ?uri a owl:Ontology . " +
            "?uri rdfs:label ?prefLabel . " +
            "?uri owl:versionInfo ?status . " +
            "VALUES ?status {?statusList}" +
            "OPTIONAL{?uri rdfs:comment ?description . }" +
            "?uri dcterms:modified ?modified . " +
            "}} ORDER BY DESC(?modified)";

    final public static String listResourcesByPreflabelQuery =
        "SELECT ?uri ?prefLabel ?description ?status ?modified WHERE {" +
            "GRAPH ?uri { ?uri a ?classType . " +
            "VALUES ?classType { rdfs:Class sh:Shape sh:NodeShape } " +
            "?uri rdfs:isDefinedBy ?model . " +
            "?uri sh:name ?prefLabel . " +
            "OPTIONAL { ?uri rdfs:label ?prefLabelLang . FILTER( lang(?prefLabelLang) = ?language ) }" +
            "?uri owl:versionInfo ?status . " +
            "VALUES ?status {?statusList}" +
            "OPTIONAL{?uri sh:description ?description . }" +
            "?uri dcterms:modified ?modified . " +
            "}} ORDER BY (!bound(?prefLabelLang)) ?prefLabelLang";

    final public static String listResourcesByModifiedQuery =
        "SELECT ?uri ?prefLabel ?description ?status ?modified WHERE {" +
            "GRAPH ?uri { ?uri a ?classType . " +
            "VALUES ?classType { rdfs:Class sh:Shape sh:NodeShape } " +
            "?uri rdfs:isDefinedBy ?model . " +
            "?uri sh:name ?prefLabel . " +
            "?uri owl:versionInfo ?status . " +
            "VALUES ?status {?statusList}" +
            "OPTIONAL{?uri sh:description ?description . }" +
            "?uri dcterms:modified ?modified . " +
            "}} ORDER BY DESC(?modified)";

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

    final public static String constructServiceCategories = LDHelper.expandSparqlQuery(true,
        "CONSTRUCT {" +
            "?concept rdfs:label ?label . " +
            "?concept rdf:type foaf:Group . " +
            "?concept dcterms:identifier ?id . " +
            "?concept dcterms:description ?note . " +
            "?concept sh:order ?order . " +
            "?concept dcterms:hasPart ?subConcept . " +
            "} WHERE {" +
            "GRAPH <urn:yti:servicecategories> { " +
            "?concept skos:prefLabel ?label . " +
            "?concept skos:notation ?id . " +
            "?concept skos:note ?note . " +
            "FILTER NOT EXISTS { ?concept skos:broader ?topConcept . }" +
            "?concept sh:order ?order . " +
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
            + "?graphName owl:versionInfo ?versionInfo . "
            + "?graphName rdfs:label ?label . "
            + "?graphName rdfs:comment ?comment . "
            + "?graphName a ?type . "
            + "?graphName iow:useContext ?useContext . "
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
            + "?graphName owl:versionInfo ?versionInfo . "
            + "?graphName dcterms:isPartOf ?group . "
            + "?graphName a ?type . "
            + "?graphName rdfs:label ?label . "
            + "OPTIONAL {?graphName rdfs:comment ?comment . }"
            + "OPTIONAL {?graphName iow:useContext ?useContext . }"
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
            + "?class owl:versionInfo ?versionInfo . "
            + "?class sh:description ?description . "
            + "?class a ?type . "
            + "?class dcterms:modified ?modified . "
            + "?class rdfs:isDefinedBy ?source . "
            + "?source rdfs:label ?sourceLabel . "
            + "?source a ?sourceType . "
            + "?source dcterms:isPartOf ?group . "
            + "?group a foaf:Group . "
            + "?group dcterms:identifier ?groupId . "
            + "?group rdfs:label ?groupLabel . "
            + "} WHERE { "
            + "GRAPH ?hasPartGraph { "
            + "?library dcterms:hasPart ?class . } "
            + "GRAPH ?class { "
            + "?class dcterms:modified ?modified . "
            + "?class sh:name ?label . "
            + "?class owl:versionInfo ?versionInfo . "
            + "OPTIONAL { ?class sh:description ?description . } "
            + "?class a ?type . "
            + "VALUES ?type { rdfs:Class sh:NodeShape } "
            + "?class rdfs:isDefinedBy ?source .  } "
            + "GRAPH ?source { "
            + "?source a ?sourceType . "
            + "?source rdfs:label ?sourceLabel . "
            + "?source dcterms:isPartOf ?group . "
            + "?group dcterms:identifier ?groupId . "
            + "?group rdfs:label ?groupLabel . "
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
            + "?property owl:versionInfo ?versionInfo . "
            + "?property rdfs:comment ?description . "
            + "?property a ?type . "
            + "?property rdfs:isDefinedBy ?source . "
            + "?source rdfs:label ?sourceLabel . "
            + "?source a ?sourceType . "
            + "?source dcterms:isPartOf ?group . "
            + "?group a foaf:Group . "
            + "?group dcterms:identifier ?groupId . "
            + "?group rdfs:label ?groupLabel . "
            + "?property dcterms:modified ?date . } "
            + "WHERE { "
            + "GRAPH ?hasPartGraph { "
            + "?library dcterms:hasPart ?property . } "
            + "GRAPH ?property { ?property rdfs:label ?label . "
            + "?property owl:versionInfo ?versionInfo . "
            + "OPTIONAL { ?property rdfs:comment ?description . } "
            + "VALUES ?type { owl:ObjectProperty owl:DatatypeProperty } "
            + "?property a ?type . "
            + "?property rdfs:isDefinedBy ?source . "
            + "OPTIONAL {?property dcterms:modified ?date . } "
            + "} "
            + "GRAPH ?source { ?source a ?sourceType . "
            + "?source dcterms:isPartOf ?group . "
            + "?group dcterms:identifier ?groupId . "
            + "?group rdfs:label ?groupLabel . "
            + "?source rdfs:label ?sourceLabel .  }}"
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

    final public static String commonExternalClassQuery =
        "SERVICE ?modelService { "
            + "GRAPH ?model { "
            + "?model dcterms:requires ?externalModel . "
            + "?externalModel rdfs:label ?externalModelLabel . "
            + "}}"
            + "GRAPH ?externalModel {"

            /* Check that class if defined in the same namespace */
            //  FIXME: Not working in DCAT?
            //+ "FILTER(STRSTARTS(STR(?classIRI), STR(?externalModel)))"

            + "?classIRI a ?type . "
            + "FILTER(!isBlank(?classIRI)) "
            + "VALUES ?type { rdfs:Class owl:Class } "

            /* Get class label */
            + "OPTIONAL{{?classIRI ?labelProp ?labelStr . FILTER(LANG(?labelStr) = '') BIND(STRLANG(?labelStr,'en') as ?label) "
            + "VALUES ?labelProp { rdfs:label sh:name dc:title dcterms:title } }"
            + "UNION"
            + "{ ?classIRI ?labelProp ?label . FILTER(LANG(?label)!='') "
            + "VALUES ?labelProp { rdfs:label sh:name dc:title dcterms:title } }}"

            /* Get class comment */
            + "OPTIONAL {{ ?classIRI ?commentPred ?commentStr . "
            + "VALUES ?commentPred { rdfs:comment skos:definition dcterms:description dc:description prov:definition sh:description }"
            + "FILTER(LANG(?commentStr) = '') BIND(STRLANG(STR(?commentStr),'en') as ?comment) }"
            + "UNION"
            + "{ ?classIRI ?commentPred ?comment . "
            + "VALUES ?commentPred { rdfs:comment skos:definition dcterms:description dc:description prov:definition sh:description }"
            + " FILTER(LANG(?comment)!='') } }"

            /* Class properties */
            + "OPTIONAL { "
            + "?classIRI rdfs:subClassOf* ?superclass . "
            /* Use schema:domainIncludes is found, else use rdfs:domain */
            + "BIND(IF(EXISTS {?predicate schema:domainIncludes ?superclass}, schema:domainIncludes, rdfs:domain) AS ?domainProperty)"
            + "?predicate ?domainProperty ?superclass . "
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
            + "VALUES ?literalValue { rdfs:Literal xsd:String xsd:string xsd:boolean xsd:decimal xsd:float xsd:double xsd:dateTime xsd:time xsd:date xsd:gYearMonth xsd:gYear xsd:gMonthDay xsd:gDay xsd:gMonth xsd:hexBinary xsd:base64Binary xsd:normalizedString xsd:integer xsd:nonPositiveInteger xsd:negativeInteger xsd:long xsd:int xsd:short xsd:byte xsd:nonNegativeInteger xsd:unsignedLong xsd:unsignedInt xsd:unsignedShort xsd:unsignedByte xsd:positiveInteger }"
            /* IF Predicate Type is rdf:Property and range is rdfs:Literal = DatatypeProperty */
            + "?predicate a rdf:Property . "
            + "?predicate ?rangeType ?literalValue ."
            // + "BIND(?literalValue as ?datatype) "
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
            + "VALUES ?rangeClassType { skos:Concept owl:Thing rdfs:Class }"
            + "BIND(owl:ObjectProperty as ?propertyType) "
            + "}"

            + "OPTIONAL { ?predicate a owl:DatatypeProperty . VALUES ?rangeType { rdfs:range schema:rangeIncludes } ?predicate ?rangeType ?datatype . FILTER (!isBlank(?datatype))  } "
            + "OPTIONAL { ?predicate a owl:ObjectProperty . ?predicate rdfs:range ?valueClass . FILTER (!isBlank(?valueClass)) } "

            /* Predicate label - if lang unknown create english tag */
            + "OPTIONAL {"
            + "VALUES ?propertyLabelPred { rdfs:label sh:name dc:title dcterms:title }"
            + "?predicate ?propertyLabelPred ?propertyLabelStr . FILTER(LANG(?propertyLabelStr) = '') BIND(STRLANG(?propertyLabelStr,'en') as ?propertyLabel) }"
            + "OPTIONAL { "
            + "VALUES ?propertyLabelPred { rdfs:label sh:name dc:title dcterms:title }"
            + "?predicate ?propertyLabelPred ?propertyLabel . FILTER(LANG(?propertyLabel)!='') }"

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

            + "} }";

    final public static String externalClassQuery = LDHelper.expandSparqlQuery(
        "CONSTRUCT { "
            + "?classIRI rdfs:isDefinedBy ?externalModel . "
            + "?externalModel rdfs:label ?externalModelLabel . "
            + "?externalModel a dcterms:Standard . "
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
            + commonExternalClassQuery);

    final public static String dummyExternalShapeQuery = LDHelper.expandSparqlQuery(true,
        "CONSTRUCT { "
            + "?shapeIRI sh:targetClass ?classIRI . "
            + "?shapeIRI rdfs:isDefinedBy ?model . "
            + "?model rdfs:label ?externalModelLabel . "
            + "?shapeIRI a sh:NodeShape . "
            + "?shapeIRI dcterms:modified ?modified . "
            + "?shapeIRI dcterms:created ?creation . "
            + "} WHERE { "
            + "GRAPH ?model { "
            + "?model dcterms:requires ?externalModel . "
            + "?externalModel rdfs:label ?externalModelLabel . "
            + "}"
            + "BIND(now() as ?creation) "
            + "BIND(now() as ?modified) " +
            "}");

    final public static String externalShapeQuery = LDHelper.expandSparqlQuery(true,
        "CONSTRUCT { "
            + "?shapeIRI sh:targetClass ?classIRI . "
            + "?shapeIRI rdfs:isDefinedBy ?model . "
            + "?model rdfs:label ?externalModelLabel . "
            + "?shapeIRI a sh:NodeShape . "
            + "?shapeIRI owl:versionInfo ?draft . "
            + "?shapeIRI sh:name ?label . "
            + "?shapeIRI sh:description ?comment . "
            + "?shapeIRI dcterms:modified ?modified . "
            + "?shapeIRI dcterms:created ?creation . "
            + "?shapeIRI sh:property ?property . "
            + "?property a sh:PropertyShape . "
            + "?property sh:datatype ?datatype . "
            + "?property dcterms:type ?propertyType . "
            + "?property sh:node ?valueClass . "
            + "?property sh:path ?predicate . "
            + "?property sh:name ?propertyLabel . "
            + "?property sh:description ?propertyComment . "
            + "} WHERE { "
            + "BIND(now() as ?creation) "
            + "BIND(now() as ?modified) "
            + commonExternalClassQuery);

}
