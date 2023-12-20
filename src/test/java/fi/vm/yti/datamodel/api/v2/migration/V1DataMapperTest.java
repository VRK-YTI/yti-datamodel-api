package fi.vm.yti.datamodel.api.v2.migration;

import fi.vm.yti.datamodel.api.mapper.MapperTestUtils;
import fi.vm.yti.datamodel.api.migration.V1DataMapper;
import org.apache.jena.vocabulary.OWL;
import org.apache.jena.vocabulary.RDFS;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class V1DataMapperTest {

    public static final String GRAPH_URI = "http://uri.suomi.fi/datamodel/ns/merialsuun";
    public static final String NEW_GRAPH_URI = "https://iri.suomi.fi/model/merialsuun/";

    @Test
    void testMapOldLibraryMetadata() {

        var oldData = MapperTestUtils.getModelFromFile("/migration/merialsuun.ttl");
        var serviceCategories = MapperTestUtils.getModelFromFile("/service-categories.ttl");
        var result = V1DataMapper.getLibraryMetadata(GRAPH_URI, oldData, serviceCategories, null);

        assertEquals("merialsuun", result.getPrefix());
        assertEquals("Merialuesuunnitelma", result.getLabel().get("fi"));
        assertEquals(Set.of("fi", "sv", "en"), result.getLanguages());
        assertEquals("P11", result.getGroups().iterator().next());
        assertEquals(UUID.fromString("7d3a3c00-5a6b-489b-a3ed-63bb58c26a63"), result.getOrganizations().iterator().next());
        assertEquals("Sample description", result.getDescription().get("fi"));
        assertEquals(Set.of("http://uri.suomi.fi/terminology/rytj/", "http://uri.suomi.fi/terminology/geoinfsan/"), result.getTerminologies());

        // internal namespaces have to add separately without validation
        assertEquals(Set.of(), result.getInternalNamespaces());

        var externalNamespace = result.getExternalNamespaces().iterator().next();
        assertEquals("test_un", externalNamespace.getPrefix());
        assertEquals("https://vocabulary.uncefact.org/", externalNamespace.getNamespace());
        assertEquals("test un", externalNamespace.getName().get("fi"));

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
        assertEquals(NEW_GRAPH_URI + "MerialuesuunnitelmanKohde", dto.getRange());
    }

    @Test
    void testMapOldLibraryClasses() {
        var oldData = MapperTestUtils.getModelFromFile("/migration/merialsuun.ttl");

        var classes = V1DataMapper.findResourcesByType(oldData, RDFS.Class);

        assertEquals(2, classes.size());

        var c1 = classes.stream()
                .filter(c -> c.getURI().equals(GRAPH_URI + "#Lahtotietoaineisto"))
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
}
