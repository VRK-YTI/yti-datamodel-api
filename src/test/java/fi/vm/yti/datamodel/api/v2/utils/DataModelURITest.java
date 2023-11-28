package fi.vm.yti.datamodel.api.v2.utils;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DataModelURITest {

    @Test
    void modelVersionURI() {
        var uri = DataModelURI.createModelURI("test", "1.0.0");

        String modelURI = "https://iri.suomi.fi/model/test/";
        String modelVersionURI = "https://iri.suomi.fi/model/test/1.0.0/";
        assertEquals(modelURI, uri.getModelURI());
        assertEquals(modelVersionURI, uri.getGraphURI());
        assertEquals("1.0.0", uri.getVersion());
        assertEquals("test", uri.getModelId());
        assertEquals(modelURI, uri.getNamespace());
    }

    @Test
    void modelURI() {
        var uri = DataModelURI.createModelURI("test");

        String expected = "https://iri.suomi.fi/model/test/";
        assertEquals(expected, uri.getModelURI());
        assertNull(uri.getVersion());
        assertEquals("test", uri.getModelId());
        assertEquals(expected, uri.getNamespace());
    }

    @Test
    void resourceVersionURI() {
        var uri = DataModelURI.createResourceURI("test", "class-1", "1.0.0");

        String modelURI = "https://iri.suomi.fi/model/test/";
        String modelVersionURI = "https://iri.suomi.fi/model/test/1.0.0/";
        String resourceURI = "https://iri.suomi.fi/model/test/class-1";
        String resourceVersionURI = "https://iri.suomi.fi/model/test/1.0.0/class-1";
        assertEquals(resourceURI, uri.getResourceURI());
        assertEquals(resourceVersionURI, uri.getResourceVersionURI());
        assertEquals(modelVersionURI, uri.getGraphURI());
        assertEquals(modelURI, uri.getModelURI());
        assertEquals("1.0.0", uri.getVersion());
        assertEquals("test", uri.getModelId());
        assertEquals("class-1", uri.getResourceId());
        assertEquals(modelURI, uri.getNamespace());
    }

    @Test
    void resourceURI() {
        var uri = DataModelURI.createResourceURI("test", "class-1");

        String modelURI = "https://iri.suomi.fi/model/test/";
        String resourceURI = "https://iri.suomi.fi/model/test/class-1";
        assertEquals(resourceURI, uri.getResourceURI());
        assertEquals(resourceURI, uri.getResourceVersionURI());
        assertEquals(modelURI, uri.getGraphURI());
        assertEquals(modelURI, uri.getModelURI());
        assertNull(uri.getVersion());
        assertEquals("test", uri.getModelId());
        assertEquals("class-1", uri.getResourceId());
        assertEquals(modelURI, uri.getNamespace());
    }

    @Test
    void createFromResourceVersionURI() {
        var uri = DataModelURI.fromURI("https://iri.suomi.fi/model/test/1.2.3/class-1");

        assertEquals("test", uri.getModelId());
        assertEquals("1.2.3", uri.getVersion());
        assertEquals("class-1", uri.getResourceId());
    }

    @Test
    void createFromResourceURI() {
        var uri = DataModelURI.fromURI("https://iri.suomi.fi/model/test/class-1");

        assertEquals("test", uri.getModelId());
        assertNull(uri.getVersion());
        assertEquals("class-1", uri.getResourceId());
    }

    @Test
    void createFromModelURI() {
        var uri = DataModelURI.fromURI("https://iri.suomi.fi/model/test/");

        assertEquals("test", uri.getModelId());
        assertNull(uri.getVersion());
        assertNull(uri.getResourceId());
    }

    @Test
    void createFromModelVersionURI() {
        var uri = DataModelURI.fromURI("https://iri.suomi.fi/model/test/14.10.12/");

        assertEquals("test", uri.getModelId());
        assertEquals("14.10.12", uri.getVersion());
        assertNull(uri.getResourceId());
    }

    @Test
    void modelURIWithContentType() {
        var url = DataModelURI.fromURI("https://iri.suomi.fi/model/test/1.0.3/test2.ttl");

        assertEquals("test", url.getModelId());
        assertEquals("text/turtle", url.getContentType());
        assertEquals("application/rdf+xml", DataModelURI.fromURI("https://iri.suomi.fi/model/test/14.10.12/test.rdf").getContentType());
        assertEquals("application/ld+json", DataModelURI.fromURI("https://iri.suomi.fi/model/test/14.10.12/test.json").getContentType());
        assertNull(DataModelURI.fromURI("https://iri.suomi.fi/model/test/14.10.12/test").getContentType());
        assertNull(DataModelURI.fromURI("https://iri.suomi.fi/model/test.foo").getContentType());
    }

    @Test
    void externalURI() {
        var uri = DataModelURI.fromURI("http://www.w3.org/2000/01/rdf-schema#Literal");

        assertEquals("http://www.w3.org/2000/01/rdf-schema#", uri.getNamespace());
        assertEquals("Literal", uri.getResourceId());
        assertNull(uri.getVersion());
        assertNull(uri.getModelId());
    }
}
