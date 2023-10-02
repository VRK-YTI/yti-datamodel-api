package fi.vm.yti.datamodel.api.v2.migration;

import fi.vm.yti.datamodel.api.mapper.MapperTestUtils;
import fi.vm.yti.datamodel.api.migration.V1DataMigrationService;
import fi.vm.yti.datamodel.api.v2.dto.*;
import fi.vm.yti.datamodel.api.v2.repository.CoreRepository;
import fi.vm.yti.datamodel.api.v2.service.ClassService;
import fi.vm.yti.datamodel.api.v2.service.DataModelService;
import fi.vm.yti.datamodel.api.v2.service.ResourceService;
import fi.vm.yti.datamodel.api.v2.service.VisualizationService;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.riot.RDFFormat;
import org.apache.jena.vocabulary.OWL;
import org.apache.jena.vocabulary.RDFS;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.net.URISyntaxException;
import java.util.List;

import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(SpringExtension.class)
@Import({
        V1DataMigrationService.class
})
class DataMigrationTest {

    @MockBean
    DataModelService dataModelService;
    @MockBean
    ClassService classService;
    @MockBean
    ResourceService resourceService;
    @MockBean
    CoreRepository coreRepository;
    @MockBean
    VisualizationService visualizationService;
    @Captor
    ArgumentCaptor<DataModelDTO> dataModelCaptor;
    @Captor
    ArgumentCaptor<BaseDTO> baseDtoCaptor;
    @Captor
    ArgumentCaptor<Model> modelCaptor;

    @Autowired
    V1DataMigrationService migrationService;

    @Test
    void testLibraryMigration() throws URISyntaxException {
        var modelURI = "http://uri.suomi.fi/datamodel/ns/merialsuun";
        var prefix = "merialsuun";

        when(coreRepository.getServiceCategories())
                .thenReturn(MapperTestUtils.getModelFromFile("/service-categories.ttl"));
        var oldData = MapperTestUtils.getModelFromFile("/migration/merialsuun.ttl");

        // external model (tihatos)
        var refModel = ModelFactory.createDefaultModel();
        var refResource = refModel.getResource("http://uri.suomi.fi/datamodel/ns/tihatos#kohdistuu");
        refResource.addProperty(RDFS.range, "http://uri.suomi.fi/datamodel/ns/tihatos#Asia");
        when(coreRepository.fetch("http://uri.suomi.fi/datamodel/ns/tihatos")).thenReturn(refModel);

        var newModel = ModelFactory.createDefaultModel();
        newModel.createResource(modelURI + ModelConstants.RESOURCE_SEPARATOR + "MerialuesuunnitelmanKohde");
        newModel.createResource(modelURI + ModelConstants.RESOURCE_SEPARATOR + "Lahtotietoaineisto");
        when(coreRepository.fetch(modelURI)).thenReturn(newModel);

        migrationService.migrateLibrary(prefix, oldData);

        verify(dataModelService).create(dataModelCaptor.capture(), eq(ModelType.LIBRARY));
        verify(classService, times(2)).create(eq(prefix), baseDtoCaptor.capture(), eq(false));
        verify(resourceService).create(eq(prefix), baseDtoCaptor.capture(), eq(ResourceType.ATTRIBUTE), eq(false));
        verify(resourceService).create(eq(prefix), baseDtoCaptor.capture(), eq(ResourceType.ASSOCIATION), eq(false));
        verify(coreRepository).put(eq(modelURI), modelCaptor.capture());

        assertEquals(prefix, dataModelCaptor.getValue().getPrefix());

        var resourcePrefixes = baseDtoCaptor.getAllValues().stream().map(BaseDTO::getIdentifier).toList();

        assertEquals(4, resourcePrefixes.size());
        assertTrue(resourcePrefixes.containsAll(List.of("koostuu", "kohdenimi", "Lahtotietoaineisto", "MerialuesuunnitelmanKohde")));

        // Class Lahtotietoaineisto should have property owl:equivalentClass containing class restrictions
        var result = modelCaptor.getValue();
        assertTrue(result.getResource(modelURI + "/Lahtotietoaineisto").hasProperty(OWL.equivalentClass));

        RDFDataMgr.write(System.out, result, RDFFormat.TURTLE);
    }
}
