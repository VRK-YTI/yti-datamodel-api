/*
 * Licensed under the European Union Public Licence (EUPL) V.1.1
 */
package fi.vm.yti.datamodel.api.utils;

import java.io.StringWriter;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import fi.vm.yti.datamodel.api.service.XMLSchemaWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * @author malonen
 */

/* 

TODO: Add Literal Type. Add xml:lang parameter.

*/

public class XMLSchemaBuilder {

    Logger logger = LoggerFactory.getLogger(XMLSchemaBuilder.class);
    DocumentBuilderFactory domFactory;
    DocumentBuilder domBuilder;
    Document document;
    Element schema;

    public XMLSchemaBuilder() {
        this.domFactory = DocumentBuilderFactory.newInstance();
        this.domFactory.setNamespaceAware(true);
        this.domFactory.setValidating(false);
        this.domFactory.setIgnoringElementContentWhitespace(true);
        try {
            this.domBuilder = this.domFactory.newDocumentBuilder();
            this.document = domBuilder.newDocument();
            this.schema = this.document.createElementNS("http://www.w3.org/2001/XMLSchema", "schema");
            schema.setPrefix("xs");
            schema.setAttribute("xmlns:dcterms", "http://purl.org/dc/terms/");
            schema.setAttribute("xmlns:sawsdl", "http://www.w3.org/ns/sawsdl");
            this.document.appendChild(schema);
        } catch (ParserConfigurationException ex) {
            logger.error("Parser error", ex);
        }
    }

    public Element getRoot() {
        return this.schema;
    }

    public Element newComplexType(String name,
                                  String id) {
        Element complexType = this.document.createElement("xs:complexType");
        complexType.setAttribute("sawsdl:modelReference", id);
        complexType.setAttribute("name", name);
        this.schema.appendChild(complexType);
        return complexType;
    }

    public Element newSequence(Element complexType) {
        // Element complexType = this.document.getElementById(name);
        Element sequence = this.document.createElement("xs:sequence");
        complexType.appendChild(sequence);
        return sequence;
    }

    public Element newSimpleElement(Element sequence,
                                    String name,
                                    String id) {
        Element simpleElement = this.document.createElement("xs:element");
        simpleElement.setAttribute("sawsdl:modelReference", id);
        simpleElement.setAttribute("name", name);
        sequence.appendChild(simpleElement);
        return simpleElement;
    }

    public Element newSimpleType(String name) {
        Element simpleType = this.document.createElement("xs:simpleType");
        simpleType.setAttribute("name", name);
        this.schema.appendChild(simpleType);
        return simpleType;
    }

    public Element newIntRestriction(Element simpleType) {
        Element restriction = this.document.createElement("xs:restriction");
        simpleType.setAttribute("base", "xs:integer");
        simpleType.appendChild(restriction);
        return restriction;
    }

    public Element newStringRestriction(Element simpleType) {
        Element restriction = this.document.createElement("xs:restriction");
        simpleType.setAttribute("base", "xs:string");
        simpleType.appendChild(restriction);
        return restriction;
    }

    public Element newAnnotation(Element elem) {
        Element annotation = this.document.createElement("xs:annotation");
        elem.appendChild(annotation);
        return annotation;
    }

    public Element createLocalizedDocumentation(Element annotation, String language) {
        Element documentation = this.document.createElement("xs:documentation");
        documentation.setAttribute("xml:lang", language);
        annotation.appendChild(documentation);
        return documentation;
    }

    public Element newDocumentation(Element elem) {
        Element annotation = this.document.createElement("xs:annotation");
        Element documentation = this.document.createElement("xs:documentation");
        annotation.appendChild(documentation);
        elem.appendChild(annotation);

        return documentation;
    }

    public void appendElementValue(Element element,
                                   String name,
                                   String value) {
        value = value.trim();
        Element docElem = this.document.createElement(name);
        docElem.setTextContent(value);
        element.appendChild(docElem);
    }

    public void appendElementValueAttribute(Element element,
                                            String name,
                                            String value) {
        value = value.trim();
        Element docElem = this.document.createElement(name);
        docElem.setAttribute("value", value);
        element.appendChild(docElem);
    }

    public String toString() {
        try {

            StringWriter sw = new StringWriter();
            StreamResult result = new StreamResult(sw);
            TransformerFactory tf = TransformerFactory.newInstance();
            //tf.setAttribute("indent-number", new Integer(10));
            Transformer transformer = tf.newTransformer();
            transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "no");
            transformer.setOutputProperty(OutputKeys.METHOD, "xml");
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
            transformer.setOutputProperty(OutputKeys.DOCTYPE_PUBLIC, "yes");
            transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "6");

            DOMSource source = new DOMSource(this.document);
            transformer.transform(source, result);
            return sw.toString();
        } catch (TransformerConfigurationException ex) {
            logger.warn("Config error", ex);
        } catch (TransformerException ex) {
            logger.warn("Transform error", ex);
        }
        return "<xs:schema></xs:schema>";
    }

}
