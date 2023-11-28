package fi.vm.yti.datamodel.api.v2.service;

import fi.vm.yti.datamodel.api.mapper.MapperTestUtils;
import fi.vm.yti.datamodel.api.security.AuthorizationManager;
import fi.vm.yti.datamodel.api.v2.dto.ModelConstants;
import fi.vm.yti.datamodel.api.v2.dto.visualization.PositionDataDTO;
import fi.vm.yti.datamodel.api.v2.dto.visualization.VisualizationClassDTO;
import fi.vm.yti.datamodel.api.v2.dto.visualization.VisualizationNodeDTO;
import fi.vm.yti.datamodel.api.v2.dto.visualization.VisualizationNodeType;
import fi.vm.yti.datamodel.api.v2.properties.Iow;
import fi.vm.yti.datamodel.api.v2.repository.CoreRepository;
import fi.vm.yti.security.AuthenticatedUserProvider;
import fi.vm.yti.security.AuthorizationException;
import fi.vm.yti.security.YtiUser;
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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(SpringExtension.class)
@Import({
        VisualizationService.class
})
class VisualizationServiceTest {

    @MockBean
    private ResourceService resourceService;

    @MockBean
    private CoreRepository coreRepository;
    @MockBean
    private AuthorizationManager authorizationManager;
    @MockBean
    private AuthenticatedUserProvider userProvider;
    @Captor
    private ArgumentCaptor<Model> modelCaptor;

    @Autowired
    private VisualizationService visualizationService;


    @Test
    void testMapVisualizationDataLibrary() {
        var model = MapperTestUtils.getModelFromFile("/models/test_datamodel_library_visualization.ttl");
        when(coreRepository.fetch(anyString())).thenReturn(model);

        var findResoureceModel = ModelFactory.createDefaultModel();
        var extResourceURI = "https://www.example.com/ns/external/testAssociation";
        var extResource = findResoureceModel
                .createResource(extResourceURI)
                .addProperty(RDF.type, OWL.ObjectProperty);
        when(resourceService.findResource(eq(extResourceURI), anySet())).thenReturn(extResource);

        var visualizationData = visualizationService.getVisualizationData("visu", null);

        // rdfs:class
        assertEquals(4, visualizationData.getNodes().stream().filter(n -> n.getType().equals(VisualizationNodeType.CLASS)).count());
        // attribute with domain (visu:name)
        assertEquals(1, visualizationData.getNodes().stream().filter(n -> n.getType().equals(VisualizationNodeType.ATTRIBUTE)).count());
        // ext:Person, ext:address and ext:someTarget
        assertEquals(3, visualizationData.getNodes().stream().filter(n -> n.getType().equals(VisualizationNodeType.EXTERNAL_CLASS)).count());

        var cls = findClass(visualizationData.getNodes(), "person");

        assertEquals(1, cls.getReferences().size());
        assertEquals(1, cls.getAttributes().size());
        assertEquals(3, cls.getAssociations().size());

        var extClass = findClass(visualizationData.getNodes(), "ext:Person");
        assertNotNull(extClass);

        assertEquals(0, visualizationData.getHiddenNodes().size());
    }

    @Test
    void testMapVisualizationDataProfile() {
        var model = MapperTestUtils.getModelFromFile("/models/test_application_profile_visualization.ttl");
        var positionModel = MapperTestUtils.getModelFromFile("/positions.ttl");
        var externalPropertiesModel = getExternalPropertiesResult();
        var graph = ModelConstants.MODEL_POSITIONS_NAMESPACE + "visuprof" + ModelConstants.RESOURCE_SEPARATOR;

        when(coreRepository.fetch(anyString())).thenReturn(model);
        when(coreRepository.fetch(graph)).thenReturn(positionModel);
        when(resourceService.findResources(anySet(), anySet())).thenReturn(externalPropertiesModel);


        var visualizationData = visualizationService.getVisualizationData("visuprof", null);

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
    void savePositionsNoAuth(){
        assertThrows(AuthorizationException.class,
                () -> visualizationService.savePositionData("test-model", List.of()));
    }

    @Test
    void savePositions() {
        var graphURI = ModelConstants.MODEL_POSITIONS_NAMESPACE + "test-model" + ModelConstants.RESOURCE_SEPARATOR;
        when(coreRepository.graphExists(graphURI)).thenReturn(true);
        when(coreRepository.fetch(anyString())).thenReturn(ModelFactory.createDefaultModel());
        when(authorizationManager.hasRightToModel(anyString(), any(Model.class))).thenReturn(true);
        when(userProvider.getUser()).thenReturn(YtiUser.ANONYMOUS_USER);

        var positionData = new PositionDataDTO();
        positionData.setX(1.0);
        positionData.setY(2.0);
        positionData.setIdentifier("pos-1");

        visualizationService.savePositionData("test-model", List.of(positionData));

        verify(coreRepository).put(eq(graphURI), modelCaptor.capture());

        var resourceURI = graphURI + positionData.getIdentifier();
        var resource = modelCaptor.getValue().getResource(resourceURI);
        assertEquals("pos-1", resource.getProperty(DCTerms.identifier).getString());
    }

    @Test
    void savePositionsVersion() {
        var graphURI = ModelConstants.MODEL_POSITIONS_NAMESPACE + "test-model/1.2.3" + ModelConstants.RESOURCE_SEPARATOR;
        when(coreRepository.graphExists(graphURI)).thenReturn(true);
        when(coreRepository.fetch(anyString())).thenReturn(ModelFactory.createDefaultModel());
        when(authorizationManager.hasRightToModel(anyString(), any(Model.class))).thenReturn(true);
        when(userProvider.getUser()).thenReturn(YtiUser.ANONYMOUS_USER);

        var positionData = new PositionDataDTO();
        positionData.setX(1.0);
        positionData.setY(2.0);
        positionData.setIdentifier("pos-1");

        visualizationService.savePositionData("test-model", List.of(positionData), "1.2.3");

        verify(coreRepository).put(eq(graphURI), modelCaptor.capture());

        var resourceURI = ModelConstants.MODEL_POSITIONS_NAMESPACE + "test-model"
                + ModelConstants.RESOURCE_SEPARATOR
                + positionData.getIdentifier();
        var resource = modelCaptor.getValue().getResource(resourceURI);
        assertEquals("pos-1", resource.getProperty(DCTerms.identifier).getString());
    }

    @Test
    void saveVersionedPositions() {
        var graphURI = ModelConstants.MODEL_POSITIONS_NAMESPACE + "test-model" + ModelConstants.RESOURCE_SEPARATOR;
        when(coreRepository.fetch(anyString())).thenReturn(ModelFactory.createDefaultModel());
        when(authorizationManager.hasRightToModel(anyString(), any(Model.class))).thenReturn(true);

        visualizationService.saveVersionedPositions("test-model", "1.0.0");

        verify(coreRepository).fetch(graphURI);
        verify(coreRepository).put(eq(graphURI + "1.0.0" + ModelConstants.RESOURCE_SEPARATOR), any(Model.class));
    }

    @Test
    void testAddDefaultPosition() {
        var positionURI = ModelConstants.MODEL_POSITIONS_NAMESPACE + "test" + ModelConstants.RESOURCE_SEPARATOR;
        var positionModel = ModelFactory.createDefaultModel();

        positionModel.createResource(positionURI + "res1")
                        .addLiteral(Iow.posX, 10.0)
                        .addLiteral(Iow.posY, 20.0);

        positionModel.createResource(positionURI + "res2")
                .addLiteral(Iow.posX, 120.0)
                .addLiteral(Iow.posY, -10.0);

        when(coreRepository.fetch(positionURI)).thenReturn(positionModel);

        visualizationService.addNewResourceDefaultPosition("test", "test-id");
        verify(coreRepository).put(anyString(), modelCaptor.capture());

        var positionResource = modelCaptor.getValue().getResource(positionURI + "test-id");

        // new position x = 0, y = existing minimum y coordinate - 50
        assertEquals(0.0, positionResource.getProperty(Iow.posX).getDouble());
        assertEquals(-60.0, positionResource.getProperty(Iow.posY).getDouble());
    }

    private static Model getExternalPropertiesResult() {
        var externalPropertiesModel = ModelFactory.createDefaultModel();

        var resource1 = externalPropertiesModel.createResource(ModelConstants.SUOMI_FI_NAMESPACE + "personprof/street");
        resource1.addProperty(DCTerms.identifier, "street");
        resource1.addProperty(RDF.type, OWL.DatatypeProperty);
        resource1.addProperty(RDF.type, SH.PropertyShape);

        var resource2 = externalPropertiesModel.createResource(ModelConstants.SUOMI_FI_NAMESPACE + "personprof/zipcode");
        resource2.addProperty(DCTerms.identifier, "zipcode");
        resource2.addProperty(RDF.type, OWL.DatatypeProperty);
        resource2.addProperty(RDF.type, SH.PropertyShape);
        return externalPropertiesModel;
    }

    private static VisualizationClassDTO findClass(Set<VisualizationNodeDTO> visualizationData, String identifier) {
        return (VisualizationClassDTO) visualizationData.stream()
                .filter(d -> d.getIdentifier().equals(identifier))
                .findFirst()
                .orElse(null);
    }
}
