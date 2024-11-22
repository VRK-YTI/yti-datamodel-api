package fi.vm.yti.datamodel.api.v2.migration;

import fi.vm.yti.datamodel.api.v2.mapper.MapperTestUtils;
import fi.vm.yti.datamodel.api.v2.dto.AssociationRestriction;
import fi.vm.yti.datamodel.api.v2.dto.AttributeRestriction;
import org.apache.jena.vocabulary.OWL;
import org.apache.jena.vocabulary.RDFS;
import org.junit.jupiter.api.Test;
import org.topbraid.shacl.vocabulary.SH;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class V1DataMapperTest {

    public static final String LIBRARY_GRAPH_URI = "http://uri.suomi.fi/datamodel/ns/merialsuun";
    public static final String NEW_LIBRARY_GRAPH_URI = "https://iri.suomi.fi/model/merialsuun/";

    public static final String PROFILE_GRAPH_URI = "http://uri.suomi.fi/datamodel/ns/fi-dcatap";
    public static final String NEW_PROFILE_GRAPH_URI = "https://iri.suomi.fi/model/fi-dcatap/";

    @Test
    void testMapOldLibraryMetadata() {

        var oldData = MapperTestUtils.getModelFromFile("/migration/merialsuun.ttl");
        var serviceCategories = MapperTestUtils.getModelFromFile("/service-categories.ttl");
        var result = V1DataMapper.getLibraryMetadata(LIBRARY_GRAPH_URI, oldData, serviceCategories, null);

        assertEquals("merialsuun", result.getPrefix());
        assertEquals("Merialuesuunnitelma", result.getLabel().get("fi"));
        assertEquals(Set.of("fi", "sv", "en"), result.getLanguages());
        assertEquals("P11", result.getGroups().iterator().next());
        assertEquals(UUID.fromString("7d3a3c00-5a6b-489b-a3ed-63bb58c26a63"), result.getOrganizations().iterator().next());
        assertEquals("Sample description", result.getDescription().get("fi"));
        assertEquals(Set.of("http://uri.suomi.fi/terminology/rytj/", "http://uri.suomi.fi/terminology/geoinfsan/"), result.getTerminologies());

        // internal namespaces have to add separately without validation
        assertEquals(Set.of(), result.getInternalNamespaces());

        var externalNamespace = result.getExternalNamespaces().stream().filter(e -> e.getPrefix().equals("test_un")).findFirst();
        assertTrue(externalNamespace.isPresent());
        assertEquals("test_un", externalNamespace.get().getPrefix());
        assertEquals("https://vocabulary.uncefact.org/", externalNamespace.get().getNamespace());
        assertEquals("test un", externalNamespace.get().getName().get("fi"));

        assertEquals(2, result.getLinks().size());
        var link = result.getLinks().stream()
                .filter(l -> l.getName().get("fi").equals("Ympäristöministerio/Merialuesuunnittelu"))
                .findFirst();
        assertTrue(link.isPresent());
        assertEquals("Test description", link.get().getDescription().get("fi"));
        assertEquals("https://ym.fi/merialuesuunnittelu", link.get().getUri());

        assertEquals("Sample documentation...", result.getDocumentation().get("fi"));
    }

    @Test
    void mapOldProfileMetadata() {
        var oldData = MapperTestUtils.getModelFromFile("/migration/fi-dcatap.ttl");
        var serviceCategories = MapperTestUtils.getModelFromFile("/service-categories.ttl");
        var result = V1DataMapper.getLibraryMetadata(PROFILE_GRAPH_URI, oldData, serviceCategories, null);

        assertEquals("fi-dcatap", result.getPrefix());
        assertEquals("Avoindata.fi:n DCAT-AP laajennos", result.getLabel().get("fi"));
        assertEquals(Set.of("fi", "en", "sv"), result.getLanguages());
        assertEquals("P11", result.getGroups().iterator().next());
        assertEquals(UUID.fromString("d9c76d52-03d3-4480-8c2c-b66e6d9c57f2"), result.getOrganizations().iterator().next());
        assertEquals("Open Data’s extension is mostly compatible with DCAT-AP with some some exception.", result.getDescription().get("fi"));

        // internal namespaces have to add separately without validation
        assertEquals(Set.of(), result.getInternalNamespaces());
    }

    @Test
    void testMapOldLibraryAttributes() {
        var oldData = MapperTestUtils.getModelFromFile("/migration/merialsuun.ttl");

        var attributes = V1DataMapper.findResourcesByType(oldData, OWL.DatatypeProperty);

        assertEquals(1, attributes.size());

        var dto = V1DataMapper.mapLibraryResource(attributes.get(0));
        assertEquals("kohdenimi", dto.getIdentifier());
        assertEquals("Kohdenimi", dto.getLabel().get("fi"));
        assertEquals("http://www.w3.org/2001/XMLSchema#string", dto.getRange());
        assertEquals("Kohteen nimi suomeksi.", dto.getNote().get("fi"));
    }

    @Test
    void testMapOldLibraryAssociations() {
        var oldData = MapperTestUtils.getModelFromFile("/migration/merialsuun.ttl");

        var associations = V1DataMapper.findResourcesByType(oldData, OWL.ObjectProperty);

        assertEquals(1, associations.size());

        var dto = V1DataMapper.mapLibraryResource(associations.get(0));
        assertEquals("koostuu", dto.getIdentifier());
        assertEquals("Koostuu", dto.getLabel().get("fi"));
        assertEquals(NEW_LIBRARY_GRAPH_URI + "MerialuesuunnitelmanKohde", dto.getRange());
    }

    @Test
    void testMapOldLibraryClasses() {
        var oldData = MapperTestUtils.getModelFromFile("/migration/merialsuun.ttl");

        var classes = V1DataMapper.findResourcesByType(oldData, RDFS.Class);

        assertEquals(2, classes.size());

        var c1 = classes.stream()
                .filter(c -> c.getURI().equals(LIBRARY_GRAPH_URI + "#Lahtotietoaineisto"))
                .findFirst();

        assertTrue(c1.isPresent());

        var dto = V1DataMapper.mapLibraryClass(c1.get());

        assertEquals("Lahtotietoaineisto", dto.getIdentifier());
        assertEquals("Lähtötietoaineisto", dto.getLabel().get("fi"));
        assertEquals("Test description", dto.getNote().get("fi"));
        assertEquals("http://uri.suomi.fi/terminology/rytj-kaava/concept-13", dto.getSubject());
        assertEquals("https://iri.suomi.fi/model/rak/Lahtotietoaineisto", dto.getSubClassOf().iterator().next());
        assertEquals("Test editorial note", dto.getEditorialNote());
    }

    @Test
    void testMapOldProfileNodeShapes() {
        var oldData = MapperTestUtils.getModelFromFile("/migration/fi-dcatap.ttl");

        var nodeShapes = V1DataMapper.findResourcesByType(oldData, SH.NodeShape);

        assertEquals(2, nodeShapes.size());

        var nodeShape = nodeShapes.stream().filter(n -> n.getURI().equals(PROFILE_GRAPH_URI + "#CatalogRecord")).findFirst();

        assertTrue(nodeShape.isPresent());

        var dto = V1DataMapper.mapProfileClass(nodeShape.get());

        assertEquals("CatalogRecord", dto.getIdentifier());
        assertEquals("Tietoaineiston kuvailutietue", dto.getLabel().get("fi"));
        assertEquals("Tiedot, jotka liittävät yksittäisen tietoaineiston datakatalogiin.", dto.getNote().get("fi"));
    }

    @Test
    void testMapOldProfilePropertyShape() {
        var oldData = MapperTestUtils.getModelFromFile("/migration/fi-dcatap.ttl");

        var propertyShapes = V1DataMapper.findResourcesByType(oldData, SH.PropertyShape);

        assertEquals(2, propertyShapes.size());

        var propertyShapeAssociation = propertyShapes.stream()
                .filter(p -> p.getURI().equals("urn:uuid:88caff9f-b060-449b-a8b2-26a276e1fdce"))
                .findFirst();

        var propertyShapeAttribute = propertyShapes.stream()
                .filter(p -> p.getURI().equals("urn:uuid:81f75fa1-561c-477e-b942-eb29edb05033"))
                .findFirst();

        assertTrue(propertyShapeAssociation.isPresent());
        assertTrue(propertyShapeAttribute.isPresent());

        var associationDTO = (AssociationRestriction) V1DataMapper.mapProfileResource(oldData, propertyShapeAssociation.get(), "fi-dcatap", "classResource");
        var attributeDTO = (AttributeRestriction) V1DataMapper.mapProfileResource(oldData, propertyShapeAttribute.get(), "fi-dcatap", "classResource");

        assertTrue(associationDTO != null && attributeDTO != null);

        assertEquals("primaryTopic", associationDTO.getIdentifier());
        assertEquals("aihe", associationDTO.getLabel().get("fi"));
        assertEquals("primary topic", associationDTO.getLabel().get("en"));
        assertEquals("Liittää kuvailutietueen siinä kuvattavaan tietoaineistoon", associationDTO.getNote().get("fi"));
        assertEquals("http://xmlns.com/foaf/0.1/primaryTopic", associationDTO.getPath());
        assertEquals("http://www.w3.org/ns/dcat#Dataset", ((AssociationRestriction) associationDTO).getClassType());
        assertEquals(3, associationDTO.getMaxCount());
        assertEquals(2, associationDTO.getMinCount());

        assertEquals("http://www.w3.org/2001/XMLSchema#dateTime", attributeDTO.getDataType());
        assertEquals("http://uri.suomi.fi/codelist/test", attributeDTO.getCodeLists().get(0));
        assertTrue(attributeDTO.getAllowedValues().containsAll(List.of("test", "test2")));
        assertEquals("pattern", attributeDTO.getPattern());
        assertEquals("default", attributeDTO.getDefaultValue());
        assertEquals(10, attributeDTO.getMaxLength());
        assertEquals(1, attributeDTO.getMinLength());
    }
}
