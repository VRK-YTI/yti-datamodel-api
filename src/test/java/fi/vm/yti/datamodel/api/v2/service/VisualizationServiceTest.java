package fi.vm.yti.datamodel.api.v2.service;

import fi.vm.yti.datamodel.api.mapper.MapperTestUtils;
import fi.vm.yti.datamodel.api.v2.dto.ModelConstants;
import fi.vm.yti.datamodel.api.v2.dto.PositionDataDTO;
import fi.vm.yti.datamodel.api.v2.dto.VisualizationClassDTO;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.vocabulary.DCTerms;
import org.apache.jena.vocabulary.OWL;
import org.apache.jena.vocabulary.RDF;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.topbraid.shacl.vocabulary.SH;

import java.util.List;
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

    @Captor
    private ArgumentCaptor<Model> modelCaptor;

    @Autowired
    private VisualizationService visualizationService;

    @Test
    void testMapVisualizationData() {
        var model = MapperTestUtils.getModelFromFile("/models/test_application_profile_visualization.ttl");
        var positionModel = MapperTestUtils.getModelFromFile("/positions.ttl");
        var externalPropertiesModel = getExternalPropertiesResult();

        when(jenaService.getDataModel(anyString())).thenReturn(model);
        when(jenaService.getDataModelPositions(anyString())).thenReturn(positionModel);
        when(jenaService.findResources(anySet())).thenReturn(externalPropertiesModel);

        var visualizationData = visualizationService.getVisualizationData("visuprof");

        assertEquals(3, visualizationData.getNodes().size());

        var person = findClass(visualizationData.getNodes(), "person");
        assertNotNull(person);
        assertEquals(1, person.getAssociations().size());
        assertEquals(2, person.getAttributes().size());

        var address = findClass(visualizationData.getNodes(), "address");
        assertNotNull(address);
        assertEquals(0, address.getAssociations().size());
        assertEquals(2, address.getAttributes().size());

        var ext = findClass(visualizationData.getNodes(), "personprof:address");
        assertNotNull(ext);

        assertEquals(1, visualizationData.getHiddenNodes().size());
    }

    @Test
    void savePositions() {
        var graphURI = ModelConstants.MODEL_POSITIONS_NAMESPACE + "test-model";
        when(jenaService.doesDataModelExist(graphURI)).thenReturn(true);

        var positionData = new PositionDataDTO();
        positionData.setX(1.0);
        positionData.setY(2.0);
        positionData.setIdentifier("pos-1");

        visualizationService.savePositionData("test-model", List.of(positionData));

        verify(jenaService).deleteDataModel(graphURI);
        verify(jenaService).putDataModelToCore(eq(graphURI), modelCaptor.capture());

        var resourceURI = graphURI + ModelConstants.RESOURCE_SEPARATOR + positionData.getIdentifier();
        var resource = modelCaptor.getValue().getResource(resourceURI);
        assertEquals("pos-1", resource.getProperty(DCTerms.identifier).getString());
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
