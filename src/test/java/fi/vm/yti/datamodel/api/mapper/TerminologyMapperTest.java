package fi.vm.yti.datamodel.api.mapper;

import fi.vm.yti.datamodel.api.v2.dto.Status;
import fi.vm.yti.datamodel.api.v2.dto.TerminologyNodeDTO;
import fi.vm.yti.datamodel.api.v2.mapper.MapperUtils;
import fi.vm.yti.datamodel.api.v2.mapper.TerminologyMapper;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;
import org.apache.jena.vocabulary.SKOS;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class TerminologyMapperTest {

    @Test
    void mapTerminologyToJenaModel() {
        String graph = "http://uri.suomi.fi/terminology/test";
        var dto = new TerminologyNodeDTO();
        var properties = new TerminologyNodeDTO.TerminologyProperties();
        var label = new TerminologyNodeDTO.LocalizedValue();
        label.setValue("Test");
        label.setLang("en");
        properties.setPrefLabel(List.of(label));

        dto.setUri(graph);
        dto.setProperties(properties);

        var model = TerminologyMapper.mapTerminologyToJenaModel(graph, dto, null);
        var resource = model.getResource(graph);

        assertEquals(graph, resource.getURI());
        assertEquals("Test@en", resource.getProperty(RDFS.label).getObject().toString());
    }

    @Test
    void mapTerminologyDTO() {
        var model = MapperTestUtils.getModelFromFile("/terminology.ttl");

        var terminologyDTO = TerminologyMapper.mapToTerminologyDTO(
                "http://uri.suomi.fi/terminology/test/terminological-vocabulary-0", model);

        assertEquals(Map.of("fi", "Testisanasto", "en", "Test terminology"), terminologyDTO.getLabel());
    }

    @Test
    void mapConceptToTerminologyModel() {
        var model = MapperTestUtils.getModelFromFile("/terminology.ttl");

        String terminologyURI = "http://uri.suomi.fi/terminology/test/";
        String conceptURI = "http://uri.suomi.fi/terminology/test/concept-0";
        var conceptDTO = new TerminologyNodeDTO();
        var termDTO = new TerminologyNodeDTO();

        var conceptProperties = new TerminologyNodeDTO.TerminologyProperties();
        var status = getLocalizedValue("", Status.VALID.name());
        var definitionFi = getLocalizedValue("fi", "Test definition fi");
        var definitionEn = getLocalizedValue("en", "Test definition en");

        conceptProperties.setStatus(List.of(status));
        conceptProperties.setDefinition(List.of(definitionFi, definitionEn));

        conceptDTO.setUri(conceptURI);
        conceptDTO.setProperties(conceptProperties);

        var termProperties = new TerminologyNodeDTO.TerminologyProperties();
        termProperties.setPrefLabel(List.of(getLocalizedValue("fi", "Pref label")));

        termDTO.setProperties(termProperties);

        var references = new TerminologyNodeDTO.TerminologyReferences();
        references.setPrefLabelXl(List.of(termDTO));
        conceptDTO.setReferences(references);

        TerminologyMapper.mapConceptToTerminologyModel(model, terminologyURI, conceptURI, conceptDTO);

        var conceptResource = model.getResource(conceptURI);

        assertNotNull(conceptResource);
        var label = MapperUtils.localizedPropertyToMap(conceptResource, SKOS.prefLabel);
        var definition = MapperUtils.localizedPropertyToMap(conceptResource, SKOS.definition);
        assertEquals("Pref label", label.get("fi"));
        assertEquals("Test definition fi", definition.get("fi"));
        assertEquals("Test definition en", definition.get("en"));
        assertEquals(terminologyURI, MapperUtils.propertyToString(conceptResource, SKOS.inScheme));
        assertEquals(conceptURI, conceptResource.getURI());
        assertEquals(SKOS.Concept.toString(), MapperUtils.propertyToString(conceptResource, RDF.type));
    }

    @Test
    void mapToConceptDTO() {
        var conceptURI = "http://uri.suomi.fi/terminology/dd0e10ed/concept-1";
        var model = MapperTestUtils.getModelFromFile("/terminology_with_concept.ttl");

        var conceptDTO = TerminologyMapper.mapToConceptDTO(model, conceptURI);

        assertEquals("käsite", conceptDTO.getLabel().get("fi"));
        assertEquals("concept", conceptDTO.getLabel().get("en"));
        assertEquals("määritelmä", conceptDTO.getDefinition().get("fi"));
        assertEquals("definition", conceptDTO.getDefinition().get("en"));
        assertEquals(conceptURI, conceptDTO.getConceptURI());
        assertEquals(conceptURI.replace("concept-1", ""), conceptDTO.getTerminology().getUri());
        assertEquals("Testisanasto", conceptDTO.getTerminology().getLabel().get("fi"));
        assertEquals("Test terminology", conceptDTO.getTerminology().getLabel().get("en"));
    }

    private static TerminologyNodeDTO.LocalizedValue getLocalizedValue(String lang, String value) {
        var localizedValue = new TerminologyNodeDTO.LocalizedValue();
        localizedValue.setValue(value);
        localizedValue.setLang(lang);
        return localizedValue;
    }
}
