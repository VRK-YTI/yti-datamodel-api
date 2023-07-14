package fi.vm.yti.datamodel.api.v2.service;

import fi.vm.yti.datamodel.api.mapper.MapperTestUtils;
import fi.vm.yti.datamodel.api.v2.dto.VisualizationClassDTO;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.vocabulary.DCTerms;
import org.apache.jena.vocabulary.OWL;
import org.apache.jena.vocabulary.RDF;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.topbraid.shacl.vocabulary.SH;

import java.util.Set;

import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(SpringExtension.class)
@Import({
        VisualizationService.class
})
class VisualizationServiceTest {

    @MockBean
    private JenaService jenaService;

    @Autowired
    private VisualizationService visualizationService;

    @Test
    void testMapVisualizationData() {
        var model = MapperTestUtils.getModelFromFile("/models/test_application_profile_visualization.ttl");
        var externalPropertiesModel = getExternalPropertiesResult();

        when(jenaService.getDataModel(anyString())).thenReturn(model);
        when(jenaService.findResources(anySet())).thenReturn(externalPropertiesModel);

        var visualizationData = visualizationService.getVisualizationData("visuprof");

        assertEquals(3, visualizationData.size());

        var person = findClass(visualizationData, "person");
        assertNotNull(person);
        assertEquals(1, person.getAssociations().size());
        assertEquals(2, person.getAttributes().size());

        var address = findClass(visualizationData, "address");
        assertNotNull(address);
        assertEquals(0, address.getAssociations().size());
        assertEquals(2, address.getAttributes().size());

        var ext = findClass(visualizationData, "personprof:address");
        assertNotNull(ext);
    }

    private static Model getExternalPropertiesResult() {
        var externalPropertiesModel = ModelFactory.createDefaultModel();

        var resource1 = externalPropertiesModel.createResource("http://uri.suomi.fi/datamodel/ns/personprof/street");
        resource1.addProperty(DCTerms.identifier, "street");
        resource1.addProperty(RDF.type, OWL.DatatypeProperty);
        resource1.addProperty(RDF.type, SH.PropertyShape);

        var resource2 = externalPropertiesModel.createResource("http://uri.suomi.fi/datamodel/ns/personprof/zipcode");
        resource2.addProperty(DCTerms.identifier, "zipcode");
        resource2.addProperty(RDF.type, OWL.DatatypeProperty);
        resource2.addProperty(RDF.type, SH.PropertyShape);
        return externalPropertiesModel;
    }

    private static VisualizationClassDTO findClass(Set<VisualizationClassDTO> visualizationData, String identifier) {
        return visualizationData.stream()
                .filter(d -> d.getIdentifier().equals(identifier))
                .findFirst()
                .orElse(null);
    }
}
