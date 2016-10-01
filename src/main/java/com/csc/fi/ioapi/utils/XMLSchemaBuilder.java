/*
 * Licensed under the European Union Public Licence (EUPL) V.1.1 
 */
package com.csc.fi.ioapi.utils;

import java.io.StringWriter;
import java.util.logging.Level;
import java.util.logging.Logger;
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
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 *
 * @author malonen
 */

public class XMLSchemaBuilder {

DocumentBuilderFactory domFactory;
DocumentBuilder domBuilder;
Document document;
Element schema;

public XMLSchemaBuilder() {
    this.domFactory = DocumentBuilderFactory.newInstance();
    this.domFactory.setNamespaceAware(true);
    this.domFactory.setValidating(false);
    try {
        this.domBuilder = this.domFactory.newDocumentBuilder();
        this.document = domBuilder.newDocument();
        this.schema = this.document.createElementNS("http://www.w3.org/2001/XMLSchema", "schema");
        schema.setPrefix("xs");
        schema.setAttribute("xmlns:dcterms", "http://purl.org/dc/terms/");
        this.document.appendChild(schema);
        // schema.setAttribute("xmlns:xs", "http://www.w3.org/2001/XMLSchema");
    } catch (ParserConfigurationException ex) {
        Logger.getLogger(XMLSchemaBuilder.class.getName()).log(Level.SEVERE, null, ex);
    }
}

public Element newComplexType(String name) {
    Element complexType = this.document.createElement("xs:complexType");
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

public Element newSimpleElement(Element sequence, String name) {
    Element simpleElement = this.document.createElement("xs:element");
    simpleElement.setAttribute("name", name);
    sequence.appendChild(simpleElement);
    return simpleElement;
}

public Element newDocumentation(Element elem) {
    Element annotation = this.document.createElement("xs:annotation");
    Element documentation = this.document.createElement("xs:documentation");
    annotation.appendChild(documentation);
    elem.appendChild(annotation);
    return documentation;
}

public void appendElementValue(Element element, String name, String value) {
    Element docElem = this.document.createElement(name);
    docElem.setTextContent(value);
    element.appendChild(docElem);
}

public String toString() {
    try {
        StringWriter sw = new StringWriter();
        StreamResult result = new StreamResult(sw);
        TransformerFactory tf = TransformerFactory.newInstance();
        Transformer transformer = tf.newTransformer();
        transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "no");
        transformer.setOutputProperty(OutputKeys.METHOD, "xml");
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
        //transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");
        DOMSource source = new DOMSource(this.document);
        transformer.transform(source, result);
        return sw.toString();
    } catch (TransformerConfigurationException ex) {
        Logger.getLogger(XMLSchemaBuilder.class.getName()).log(Level.SEVERE, null, ex);
    } catch (TransformerException ex) {
        Logger.getLogger(XMLSchemaBuilder.class.getName()).log(Level.SEVERE, null, ex);
    }
    return null;
}

    
}
