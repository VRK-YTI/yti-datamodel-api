/*
 * Licensed under the European Union Public Licence (EUPL) V.1.1
 */
package fi.vm.yti.datamodel.api.service;

import fi.vm.yti.datamodel.api.utils.LDHelper;
import fi.vm.yti.datamodel.api.utils.XMLSchemaBuilder;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.jena.query.*;
import org.apache.jena.sparql.resultset.ResultSetPeekable;

import java.text.SimpleDateFormat;
import java.util.*;

import org.apache.jena.util.SplitIRI;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.w3c.dom.Element;

@Service
public class XMLSchemaWriter {

    private static final Logger logger = LoggerFactory.getLogger(XMLSchemaWriter.class.getName());

    private static final Map<String, String> DATATYPE_MAP =
        Collections.unmodifiableMap(new HashMap<String, String>() {{
            put("http://www.w3.org/2001/XMLSchema#int", "xs:int");
            put("http://www.w3.org/2001/XMLSchema#integer", "xs:integer");
            put("http://www.w3.org/2001/XMLSchema#long", "xs:long");
            put("http://www.w3.org/2001/XMLSchema#float", "xs:float");
            put("http://www.w3.org/2001/XMLSchema#double", "xs:double");
            put("http://www.w3.org/2001/XMLSchema#decimal", "xs:decimal");
            put("http://www.w3.org/2001/XMLSchema#boolean", "xs:boolean");
            put("http://www.w3.org/2001/XMLSchema#date", "xs:date");
            put("http://www.w3.org/2001/XMLSchema#dateTime", "xs:dateTime");
            put("http://www.w3.org/2001/XMLSchema#time", "xs:time");
            put("http://www.w3.org/2001/XMLSchema#gYear", "xs:gYear");
            put("http://www.w3.org/2001/XMLSchema#gMonth", "xs:gMont");
            put("http://www.w3.org/2001/XMLSchema#gDay", "xs:gDay");
            put("http://www.w3.org/2001/XMLSchema#string", "xs:string");
            put("http://www.w3.org/1999/02/22-rdf-syntax-ns#langString", "xs:string");
            put("http://www.w3.org/2000/01/rdf-schema#Literal", "xs:string");
            put("http://www.w3.org/2001/XMLSchema#anyUri", "xs:anyUri");
        }});

    /* 
    Alternative way to document?
        <ccts:Component>
                 <ccts:ComponentType>BBIE</ccts:ComponentType>
                 <ccts:DictionaryEntryName>Winning Party. Rank.
                 Text</ccts:DictionaryEntryName>
                 <ccts:Definition>Indicates the rank obtained in the
                 award.</ccts:Definition>
                 <ccts:Cardinality>0..1</ccts:Cardinality>
                 <ccts:ObjectClass>Winning Party</ccts:ObjectClass>
                 <ccts:PropertyTerm>Rank</ccts:PropertyTerm>
                 <ccts:RepresentationTerm>Text</ccts:RepresentationTerm>
                 <ccts:DataType>Text. Type</ccts:DataType>
     </ccts:Component>
   
    */

    private final EndpointServices endpointServices;
    private final GraphManager graphManager;

    XMLSchemaWriter(EndpointServices endpointServices,
                    GraphManager graphManager) {
        this.endpointServices = endpointServices;
        this.graphManager = graphManager;
    }

    public String newClassSchema(String classID,
                                 String lang) {
        logger.info("Creating schema");
        XMLSchemaBuilder xml = new XMLSchemaBuilder();

        String className = SplitIRI.localname(classID);
        String localClassName = null;

        ParameterizedSparqlString pss = new ParameterizedSparqlString();

        String selectClass =
            "SELECT ?localClassName ?type ?label ?description "
                + "WHERE { "
                + "GRAPH ?resourceID { "
                + "?resourceID a ?type . "
                + "?resourceID ?resourceLabel ?label . "
                + "VALUES ?resourceLabel { rdfs:label sh:name }"
                + "OPTIONAL { ?resourceID iow:localName ?localClassName . } "
                + "OPTIONAL { ?resourceID ?resourceComment ?description . "
                + "VALUES ?resourceComment { rdfs:comment sh:description }"
                + "}"
                + "} "
                + "} ";

        pss.setIri("resourceID", classID);
        if (lang != null) {
            pss.setLiteral("lang", lang);
        }
        pss.setNsPrefixes(LDHelper.PREFIX_MAP);

        pss.setCommandText(selectClass);

        boolean classMetadata = false;

        Element complexType;
        try (QueryExecution qexec = QueryExecutionFactory.sparqlService(endpointServices.getCoreSparqlAddress(), pss.asQuery())) {
            ResultSet results = qexec.execSelect();

            if (!results.hasNext()) {
                logger.debug("Resource results is null");
                return null;
            }

            Map<String, LocalizedData> localizedData = new HashMap<>();

            while (results.hasNext()) {

                QuerySolution soln = results.nextSolution();

                String labelLanguage = soln.getLiteral("label").getLanguage();

                LocalizedData data = getLocalizedData(localizedData, soln, labelLanguage, "label");

                localizedData.put(labelLanguage, data);

                if (soln.contains("localClassName")) {
                    localClassName = soln.getLiteral("localClassName").getString();
                }

                String sType = soln.getResource("type").getLocalName();

                if (sType.equals("Class") || sType.equals("Shape") || sType.equals("NodeShape")) {
                    classMetadata = true;
                }
            }

            complexType = xml.newComplexType(getClassName(className, localClassName), classID);

            createDocumentation(xml, complexType, localizedData);
        }

        if (classMetadata) {

            String selectResources =
                "SELECT ?predicate ?id ?predicateName ?label ?description ?datatype ?shapeRef ?shapeRefName ?min ?max ?minLength ?maxLength ?pattern "
                    + "WHERE { "
                    + "GRAPH ?resourceID {"
                    + "?resourceID sh:property ?property . "
                    + "?property sh:order ?index . "
                    + "?property sh:path ?predicate . "
                    + "OPTIONAL { ?property iow:localName ?id . }"
                    + "?property sh:name ?label . "
                    + "OPTIONAL { ?property sh:description ?description . "
                    + "}"
                    + "OPTIONAL { ?property sh:datatype ?datatype . }"
                    + "OPTIONAL { ?property sh:node ?shapeRef . BIND(afn:localname(?shapeRef) as ?shapeRefName) }"
                    + "OPTIONAL { ?property sh:minCount ?min . }"
                    + "OPTIONAL { ?property sh:maxCount ?max . }"
                    + "OPTIONAL { ?property sh:pattern ?pattern . }"
                    + "OPTIONAL { ?property sh:minLength ?minLength . }"
                    + "OPTIONAL { ?property sh:maxLength ?maxLength . }"
                    + "BIND(afn:localname(?predicate) as ?predicateName)"
                    + "}"
                    + "} ORDER BY ?index ";

            pss.setCommandText(selectResources);
            if (lang != null) pss.setLiteral("lang", lang);

            try (QueryExecution qexec = QueryExecutionFactory.sparqlService(endpointServices.getCoreSparqlAddress(), pss.asQuery())) {

                ResultSet results = qexec.execSelect();

                if (results.hasNext()) {

                    Element seq = xml.newSequence(complexType);

                    Map<String, XmlElementDTO> xmlElements = new HashMap<>();

                    while (results.hasNext()) {

                        QuerySolution soln = results.nextSolution();

                        String predicateName = soln.getLiteral("predicateName").getString();
                        XmlElementDTO dto = xmlElements.getOrDefault(predicateName, new XmlElementDTO());

                        if (soln.contains("id")) {
                            predicateName = soln.getLiteral("id").getString();
                        }

                        populateXmlElementDTO(soln, predicateName, dto, "label");

                        xmlElements.put(predicateName, dto);
                    }

                    createXmlElements(xml, seq, xmlElements);
                }
            }
        }
        return xml.toString();
    }

    public String newModelSchema(String modelID,
                                 String lang) {

        logger.info("Building XML Schema from " + modelID);

        XMLSchemaBuilder xml = new XMLSchemaBuilder();

        ParameterizedSparqlString pss = new ParameterizedSparqlString();

        String selectClass =
            "SELECT ?label ?description "
                + "WHERE { "
                + "GRAPH ?modelID { "
                + "?modelID rdfs:label ?label . "
                + "OPTIONAL { ?modelID rdfs:comment ?description . "
                + "}"
                + "} "
                + "} ";

        pss.setIri("modelID", modelID);
        if (lang != null) {
            pss.setLiteral("lang", lang);
        }
        pss.setNsPrefixes(LDHelper.PREFIX_MAP);
        pss.setCommandText(selectClass);

        try (QueryExecution qexec = QueryExecutionFactory.sparqlService(endpointServices.getCoreSparqlAddress(), pss.toString())) {

            ResultSet results = qexec.execSelect();

            if (!results.hasNext()) {
                logger.info("No model found:" + modelID);
                return null;
            }

            Map<String, LocalizedData> dataModelLocalizedData = new HashMap<>();

            while (results.hasNext()) {
                QuerySolution soln = results.nextSolution();
                String language = soln.getLiteral("label").getLanguage();
                dataModelLocalizedData.put(language, getLocalizedData(dataModelLocalizedData, soln, language, "label"));
            }

            createDocumentation(xml, xml.getRoot(), dataModelLocalizedData, modelID);
        }

        /* Get classes from library */
        pss = new ParameterizedSparqlString();

        String selectResources =
            "SELECT ?resource ?targetClass ?className ?localClassName ?classTitle ?classDescription ?classDeactivated ?property ?propertyDeactivated ?valueList ?schemeList ?predicate ?id ?title ?description ?predicateName ?datatype ?shapeRef ?shapeRefName ?min ?max ?minLength ?maxLength ?pattern "
                + "WHERE { "
                + "GRAPH ?modelPartGraph {"
                + "?model dcterms:hasPart ?resource . "
                + "}"
                + "GRAPH ?resource {"
                + "?resource sh:name ?classTitle . "
                + "OPTIONAL { ?resource sh:deactivated ?classDeactivated . }"
                + "OPTIONAL { ?resource iow:localName ?localClassName . } "
                + "OPTIONAL { ?resource sh:targetClass ?targetClass . }"
                + "OPTIONAL { ?resource sh:description ?classDescription . "
                + "}"
                + "BIND(afn:localname(?resource) as ?className)"
                + "OPTIONAL{"
                + "?resource sh:property ?property . "
                + "?property sh:order ?index . "
                + "?property sh:path ?predicate . "
                + "OPTIONAL { ?property iow:localName ?id . }"
                + "?property sh:name ?title . "
                + "OPTIONAL { ?property sh:description ?description . "
                + "}"
                + "OPTIONAL { ?property sh:deactivated ?propertyDeactivated . }"
                + "OPTIONAL { ?property sh:datatype ?datatype . }"
                + "OPTIONAL { ?property sh:node ?shapeRef . BIND(afn:localname(?shapeRef) as ?shapeRefName) }"
                + "OPTIONAL { ?property sh:maxCount ?max . }"
                + "OPTIONAL { ?property sh:minCount ?min . }"
                + "OPTIONAL { ?property sh:pattern ?pattern . }"
                + "OPTIONAL { ?property sh:minLenght ?minLength . }"
                + "OPTIONAL { ?property sh:maxLength ?maxLength . }"
                + "OPTIONAL { ?property sh:in ?valueList . } "
                + "OPTIONAL { ?property dcam:memberOf ?schemeList . } "
                + "BIND(afn:localname(?predicate) as ?predicateName)"
                + "}"
                + "}"
                + "}"
                + "ORDER BY ?resource ?index";

        pss.setIri("modelPartGraph", modelID + "#HasPartGraph");
        if (lang != null) pss.setLiteral("lang", lang);
        pss.setCommandText(selectResources);
        pss.setNsPrefixes(LDHelper.PREFIX_MAP);

        try (QueryExecution qexec = QueryExecutionFactory.sparqlService(endpointServices.getCoreSparqlAddress(), pss.asQuery())) {

            ResultSet results = qexec.execSelect();
            ResultSetPeekable pResults = ResultSetFactory.makePeekable(results);

            if (pResults.hasNext()) {

                Map<String, XmlComplexTypeDTO> complexTypes = new HashMap<>();

                while (pResults.hasNext()) {
                    QuerySolution soln = pResults.nextSolution();

                    if (!soln.contains("className")) return null;

                    if (!soln.contains("classDeactivated") || (soln.contains("classDeactivated") && !soln.getLiteral("classDeactivated").getBoolean())) {

                        String className = soln.getLiteral("className").getString();

                        String localClassName = soln.contains("localClassName") ? soln.getLiteral("localClassName").getString() : null;
                        String classId = soln.contains("targetClass") ? soln.getResource("targetClass").toString() : soln.getResource("resource").toString();

                        XmlComplexTypeDTO complexTypeDTO = complexTypes.getOrDefault(className, new XmlComplexTypeDTO());
                        complexTypeDTO.setLocalClassName(localClassName);
                        complexTypeDTO.setClassId(classId);

                        String classLanguage = soln.getLiteral("classTitle").getLanguage();

                        LocalizedData classDocumentation = getLocalizedData(complexTypeDTO.getDocumentation(), soln, classLanguage, "classTitle", "classDescription");

                        complexTypeDTO.getDocumentation().put(classLanguage, classDocumentation);

                        if (soln.contains("property") && (!soln.contains("propertyDeactivated") || (soln.contains("propertyDeactivated") && !soln.getLiteral("propertyDeactivated").getBoolean()))) {
                            String predicateName = soln.getLiteral("predicateName").getString();

                            XmlElementDTO xmlElementDTO = complexTypeDTO.getXmlElements().getOrDefault(predicateName, new XmlElementDTO());

                            if (soln.contains("id")) {
                                predicateName = soln.getLiteral("id").getString();
                            }

                            populateXmlElementDTO(soln, predicateName, xmlElementDTO, "title");

                            complexTypeDTO.getXmlElements().put(predicateName, xmlElementDTO);
                        }
                        complexTypes.put(className, complexTypeDTO);
                    }
                }

                for (String classKey : complexTypes.keySet()) {
                    XmlComplexTypeDTO complexTypeDTO = complexTypes.get(classKey);
                    Element complexType = xml.newComplexType(getClassName(classKey, complexTypeDTO.getLocalClassName()),
                            complexTypeDTO.getClassId());

                    createDocumentation(xml, complexType, complexTypeDTO.getDocumentation());
                    Element seq = xml.newSequence(complexType);
                    createXmlElements(xml, seq, complexTypeDTO.getXmlElements());
                }
            }
        }
        return xml.toString();
    }

    private LocalizedData getLocalizedData(Map<String, LocalizedData> localizedDataMap, QuerySolution soln, String language, String titleAttribute, String descAttribute) {
        LocalizedData localizedData = localizedDataMap.getOrDefault(language, new LocalizedData());

        String title = soln.getLiteral(titleAttribute).getString();

        localizedData.setLang(language);
        localizedData.setTitle(title);

        if (soln.contains(descAttribute) && soln.getLiteral(descAttribute).getLanguage().equals(language)) {
            localizedData.setDescription(soln.getLiteral(descAttribute).getString());
        }

        return localizedData;
    }

    private LocalizedData getLocalizedData(Map<String, LocalizedData> localizedDataMap, QuerySolution soln, String language, String titleAttribute) {
        return getLocalizedData(localizedDataMap, soln, language, titleAttribute, "description");
    }

    private void createDocumentation(XMLSchemaBuilder xml, Element newElement, Map<String, LocalizedData> localizedData) {
        createDocumentation(xml, newElement, localizedData, null);
    }

    private void createDocumentation(XMLSchemaBuilder xml, Element newElement, Map<String, LocalizedData> localizedData, String modelID) {
        Element annotation = xml.newAnnotation(newElement);

        for (String langKey : localizedData.keySet()) {
            LocalizedData data = localizedData.get(langKey);
            Element documentation = xml.createLocalizedDocumentation(annotation, langKey);
            xml.appendElementValue(documentation, "dcterms:title", data.getTitle());
            if (data.getDescription() != null) {
                xml.appendElementValue(documentation, "dcterms:description", data.getDescription());
            }

            if (modelID != null) {
                Date modified = graphManager.modelContentModified(modelID);
                SimpleDateFormat format = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz");

                if (modified != null) {
                    String dateModified = format.format(modified);
                    xml.appendElementValue(documentation, "dcterms:modified", dateModified);
                }
            }
        }
    }

    private void populateXmlElementDTO(QuerySolution soln, String predicateName, XmlElementDTO dto, String langAttribute) {
        String language = soln.getLiteral(langAttribute).getLanguage();

        Map<String, LocalizedData> localizedDataMap = dto.getLocalizedData();
        LocalizedData localizedData = getLocalizedData(localizedDataMap, soln, language, langAttribute);

        if (soln.contains("id")) {
            predicateName = soln.getLiteral("id").getString();
        }

        localizedDataMap.put(language, localizedData);

        String predicate = soln.getResource("predicate").getURI();

        dto.setPredicate(predicate);
        dto.setPredicateName(predicateName);
        dto.setLocalizedData(localizedDataMap);

        if (soln.contains("min") && soln.getLiteral("min").getInt() > 0) {
            dto.setMinOccurs(soln.getLiteral("min").getString());
        } else {
            dto.setMinOccurs("0");
        }

        if (soln.contains("datatype")) {
            dto.setDataType(soln.getResource("datatype").toString());
        }

        if (soln.contains("max") && soln.getLiteral("max").getInt() > 0) {
            dto.setMaxOccurs(soln.getLiteral("max").getString());
        } else {
            dto.setMaxOccurs("unbounded");
        }

        /* If shape contains pattern or other type of restriction */
        if (soln.contains("pattern")) {
            dto.setPattern(soln.getLiteral("pattern").toString());
        }
        if (soln.contains("maxLength")) {
            dto.setMaxLength(soln.getLiteral("maxLength").getString());
        }
        if (soln.contains("minLength")) {
            dto.setMinLength(soln.getLiteral("minLength").getString());
        }

        if (soln.contains("shapeRefName")) {
            dto.setShapeRefName(soln.getLiteral("shapeRefName").toString());
        }
    }

    private void createXmlElements(XMLSchemaBuilder xml, Element seq, Map<String, XmlElementDTO> xmlElements) {
        for (XmlElementDTO dto : xmlElements.values()) {

            Element newElement = xml.newSimpleElement(seq, dto.getPredicateName(), dto.getPredicate());

            createDocumentation(xml, newElement, dto.getLocalizedData());

            newElement.setAttribute("minOccurs", dto.getMinOccurs());
            newElement.setAttribute("maxOccurs", dto.getMaxOccurs());

            if (dto.getDataType() != null) {
                newElement.setAttribute("type", DATATYPE_MAP.get(dto.getDataType()));
            }
            if (dto.getShapeRefName() != null) {
                newElement.setAttribute("type", dto.getShapeRefName() + "Type");
            }
            if (dto.getPattern() != null || dto.getMaxLength() != null || dto.getMinLength() != null) {
                Element simpleType = xml.newSimpleType(dto.getPredicateName() + "Type");

                if (dto.getPattern() != null) {
                    Element restriction = xml.newStringRestriction(simpleType);
                    xml.appendElementValueAttribute(restriction, "xs:maxInclusive", dto.getPattern());
                } else {
                    Element restriction = xml.newIntRestriction(simpleType);
                    if (dto.getMaxLength() != null) {
                        xml.appendElementValueAttribute(restriction, "xs:maxInclusive", dto.getMaxLength());
                    }
                    if (dto.getMinLength() != null) {
                        xml.appendElementValueAttribute(restriction, "xs:minInclusive", dto.getMinLength());
                    }
                }
                newElement.setAttribute("type", dto.getPredicateName() + "Type");
            }
        }
    }

    @NotNull
    private String getClassName(String className, String localClassName) {
        return (localClassName != null && localClassName.length() > 0 ? LDHelper.removeInvalidCharacters(localClassName) : className) + "Type";
    }
}

class LocalizedData {
    private String lang;
    private String title;
    private String description;

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getLang() {
        return lang;
    }

    public void setLang(String lang) {
        this.lang = lang;
    }
}

class XmlComplexTypeDTO {
    String localClassName;
    String classId;
    Map<String, LocalizedData> documentation = new HashMap<>();
    Map<String, XmlElementDTO> xmlElements = new HashMap<>();

    public String getLocalClassName() {
        return localClassName;
    }

    public void setLocalClassName(String localClassName) {
        this.localClassName = localClassName;
    }

    public String getClassId() {
        return classId;
    }

    public void setClassId(String classId) {
        this.classId = classId;
    }

    public Map<String, LocalizedData> getDocumentation() {
        return documentation;
    }

    public void setDocumentation(Map<String, LocalizedData> documentation) {
        this.documentation = documentation;
    }

    public Map<String, XmlElementDTO> getXmlElements() {
        return xmlElements;
    }

    public void setXmlElements(Map<String, XmlElementDTO> xmlElements) {
        this.xmlElements = xmlElements;
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }
}

class XmlElementDTO {

    private String predicate;
    private String predicateName;
    private Map<String, LocalizedData> localizedData = new HashMap<>();
    private String minOccurs;
    private String maxOccurs;
    private String dataType;
    private String shapeRefName;
    private String maxLength;
    private String minLength;
    private String pattern;

    public String getPredicate() {
        return predicate;
    }

    public void setPredicate(String predicate) {
        this.predicate = predicate;
    }

    public Map<String, LocalizedData> getLocalizedData() {
        return localizedData;
    }

    public void setLocalizedData(Map<String, LocalizedData> localizedData) {
        this.localizedData = localizedData;
    }

    public String getMinOccurs() {
        return minOccurs;
    }

    public void setMinOccurs(String minOccurs) {
        this.minOccurs = minOccurs;
    }

    public String getMaxOccurs() {
        return maxOccurs;
    }

    public void setMaxOccurs(String maxOccurs) {
        this.maxOccurs = maxOccurs;
    }

    public String getDataType() {
        return dataType;
    }

    public void setDataType(String dataType) {
        this.dataType = dataType;
    }

    public String getPredicateName() {
        return predicateName;
    }

    public void setPredicateName(String predicateName) {
        this.predicateName = predicateName;
    }

    public String getShapeRefName() {
        return shapeRefName;
    }

    public void setShapeRefName(String shapeRefName) {
        this.shapeRefName = shapeRefName;
    }

    public String getMaxLength() {
        return maxLength;
    }

    public void setMaxLength(String maxLength) {
        this.maxLength = maxLength;
    }

    public String getMinLength() {
        return minLength;
    }

    public void setMinLength(String minLength) {
        this.minLength = minLength;
    }

    public String getPattern() {
        return pattern;
    }

    public void setPattern(String pattern) {
        this.pattern = pattern;
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }
}