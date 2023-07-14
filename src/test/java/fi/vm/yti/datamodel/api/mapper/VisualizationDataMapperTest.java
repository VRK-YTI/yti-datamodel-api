package fi.vm.yti.datamodel.api.mapper;

import fi.vm.yti.datamodel.api.v2.dto.VisualizationNodeShapeDTO;
import fi.vm.yti.datamodel.api.v2.dto.VisualizationPropertyShapeAssociationDTO;
import fi.vm.yti.datamodel.api.v2.dto.VisualizationPropertyShapeAttributeDTO;
import fi.vm.yti.datamodel.api.v2.mapper.VisualizationMapper;
import org.apache.jena.rdf.model.ModelFactory;
import org.junit.jupiter.api.Test;

import java.util.LinkedList;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class VisualizationDataMapperTest {

    private final Map<String, String> libraryNamespaces = Map.of(
            "http://uri.suomi.fi/datamodel/ns/yti-model", "yti-model",
            "https://www.example.com/ns/external", "ext");

    private final Map<String, String> profileNamespaces = Map.of(
            "http://uri.suomi.fi/datamodel/ns/personprof", "personprof",
            "http://uri.suomi.fi/datamodel/ns/ytm", "ytm"
    );

    @Test
    void mapLibraryClassWithAttributes() {
        var positions = ModelFactory.createDefaultModel();
        var model = MapperTestUtils.getModelFromFile("/models/test_datamodel_visualization.ttl");

        var classDTO = VisualizationMapper.mapClass("http://uri.suomi.fi/datamodel/ns/test/testclass1", model, positions, libraryNamespaces);
        assertEquals("Label 1", classDTO.getLabel().get("fi"));
        assertEquals("testclass1", classDTO.getIdentifier());
        assertEquals("yti-model:TestClass", classDTO.getParentClasses().iterator().next());

        var attribute = model.getResource("http://uri.suomi.fi/datamodel/ns/test/attribute-1");
        var association = model.getResource("http://uri.suomi.fi/datamodel/ns/test/association-1");

        VisualizationMapper.mapResource(classDTO, attribute, model, positions, libraryNamespaces);
        VisualizationMapper.mapResource(classDTO, association, model, positions, libraryNamespaces);

        assertEquals(1, classDTO.getAttributes().size());
        assertEquals(1, classDTO.getAssociations().size());

        assertEquals("testiattribuutti", classDTO.getAttributes().get(0).getLabel().get("fi"));
        assertEquals("testiassosiaatio", classDTO.getAssociations().get(0).getLabel().get("fi"));
        assertEquals("attribute-1", classDTO.getAttributes().get(0).getIdentifier());
        assertEquals("association-1", classDTO.getAssociations().get(0).getIdentifier());
        assertEquals("testclass2", classDTO.getAssociations().get(0).getRoute().get(0));
    }

    @Test
    void mapLibraryClassWithExternalResources() {
        var positions = ModelFactory.createDefaultModel();
        var model = MapperTestUtils.getModelFromFile("/models/test_datamodel_visualization.ttl");

        var classDTO = VisualizationMapper.mapClass("http://uri.suomi.fi/datamodel/ns/test/testclass3", model, positions, libraryNamespaces);

        var association = model.getResource("http://uri.suomi.fi/datamodel/ns/test/association-3");

        VisualizationMapper.mapResource(classDTO, association, model, positions, libraryNamespaces);

        assertEquals("ext:ExternalClass", classDTO.getParentClasses().iterator().next());
        assertEquals("ext:ExternalClass", classDTO.getAssociations().get(0).getRoute().get(0));
    }

    @Test
    void mapExternalClass() {
        var positions = ModelFactory.createDefaultModel();
        String identifier = "ext:TestClass";

        var external = VisualizationMapper.mapExternalClass(identifier, Set.of("fi", "en"), positions);

        assertEquals(identifier, external.getIdentifier());
        assertEquals(Map.of("fi", identifier, "en", identifier), external.getLabel());
    }

    @Test
    void mapApplicationProfileClass() {
        var positions = ModelFactory.createDefaultModel();
        var model = MapperTestUtils.getModelFromFile("/models/test_application_profile_visualization.ttl");

        var classDTO = (VisualizationNodeShapeDTO) VisualizationMapper
                .mapClass("http://uri.suomi.fi/datamodel/ns/visuprof/person", model, positions, profileNamespaces);

        assertEquals("person", classDTO.getIdentifier());
        assertEquals("Henkilö", classDTO.getLabel().get("fi"));
        assertEquals("ytm:person", classDTO.getTargetClass());

        var attribute = model.getResource("http://uri.suomi.fi/datamodel/ns/visuprof/age");
        var association = model.getResource("http://uri.suomi.fi/datamodel/ns/visuprof/is-address");

        VisualizationMapper.mapResource(classDTO, attribute, model, positions, profileNamespaces);
        VisualizationMapper.mapResource(classDTO, association, model, positions, profileNamespaces);

        var mappedAttribute = (VisualizationPropertyShapeAttributeDTO) classDTO.getAttributes().get(0);
        var mappedAssociation = (VisualizationPropertyShapeAssociationDTO) classDTO.getAssociations().get(0);

        assertNotNull(mappedAttribute);
        assertNotNull(mappedAssociation);

        assertEquals("Ikä", mappedAttribute.getLabel().get("fi"));
        assertEquals("age", mappedAttribute.getIdentifier());
        assertEquals("xsd:integer", mappedAttribute.getDataType());
        assertEquals("ytm:age", mappedAttribute.getPath());
        assertEquals(1, mappedAttribute.getMinCount());
        assertEquals(1, mappedAttribute.getMaxCount());

        assertEquals("onOsoite", mappedAssociation.getLabel().get("fi"));
        assertEquals("is-address", mappedAssociation.getIdentifier());
        assertEquals("ytm:is-address", mappedAssociation.getPath());
        assertEquals(1, mappedAssociation.getMinCount());
        assertEquals(2, mappedAssociation.getMaxCount());
        assertEquals("address", ((LinkedList<String>)mappedAssociation.getRoute()).getLast());
    }
}
