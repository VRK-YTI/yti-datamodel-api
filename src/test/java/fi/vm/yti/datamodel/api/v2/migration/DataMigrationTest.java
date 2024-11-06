package fi.vm.yti.datamodel.api.v2.migration;

import fi.vm.yti.common.enums.GraphType;
import fi.vm.yti.datamodel.api.v2.mapper.MapperTestUtils;
import fi.vm.yti.datamodel.api.v2.dto.BaseDTO;
import fi.vm.yti.datamodel.api.v2.dto.DataModelDTO;
import fi.vm.yti.datamodel.api.v2.dto.ResourceType;
import fi.vm.yti.datamodel.api.v2.repository.CoreRepository;
import fi.vm.yti.datamodel.api.v2.service.ClassService;
import fi.vm.yti.datamodel.api.v2.service.DataModelService;
import fi.vm.yti.datamodel.api.v2.service.ResourceService;
import fi.vm.yti.datamodel.api.v2.service.VisualizationService;
import fi.vm.yti.datamodel.api.v2.utils.DataModelURI;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.riot.RDFFormat;
import org.apache.jena.vocabulary.OWL;
import org.apache.jena.vocabulary.RDFS;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

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
        var prefix = "merialsuun";
        var newModelURI = DataModelURI.createModelURI(prefix).getModelURI();

        when(coreRepository.getServiceCategories())
                .thenReturn(MapperTestUtils.getModelFromFile("/service-categories.ttl"));
        var oldData = MapperTestUtils.getModelFromFile("/migration/merialsuun.ttl");

        // external model (tihatos)
        var refModel = ModelFactory.createDefaultModel();
        var refResource = refModel.getResource("http://uri.suomi.fi/datamodel/ns/tihatos#kohdistuu");
        refResource.addProperty(RDFS.range, "http://uri.suomi.fi/datamodel/ns/tihatos#Asia");
        when(coreRepository.fetch("http://uri.suomi.fi/datamodel/ns/tihatos")).thenReturn(refModel);
        when(dataModelService.create(any(DataModelDTO.class), any(GraphType.class))).thenReturn(new URI(newModelURI));

        var newModel = ModelFactory.createDefaultModel();
        newModel.createResource(newModelURI + "MerialuesuunnitelmanKohde");
        newModel.createResource(newModelURI + "Lahtotietoaineisto");
        when(coreRepository.fetch(newModelURI)).thenReturn(newModel);

        migrationService.migrateDatamodel(prefix, oldData);

        verify(dataModelService).create(dataModelCaptor.capture(), eq(GraphType.LIBRARY));
        verify(classService, times(2)).create(eq(prefix), baseDtoCaptor.capture(), eq(false));
        verify(resourceService).create(eq(prefix), baseDtoCaptor.capture(), eq(ResourceType.ATTRIBUTE), eq(false));
        verify(resourceService).create(eq(prefix), baseDtoCaptor.capture(), eq(ResourceType.ASSOCIATION), eq(false));
        verify(coreRepository).put(eq(newModelURI), modelCaptor.capture());

        assertEquals(prefix, dataModelCaptor.getValue().getPrefix());

        var resourcePrefixes = baseDtoCaptor.getAllValues().stream().map(BaseDTO::getIdentifier).toList();

        assertEquals(4, resourcePrefixes.size());
        assertTrue(resourcePrefixes.containsAll(List.of("koostuu", "kohdenimi", "Lahtotietoaineisto", "MerialuesuunnitelmanKohde")));

        // Class Lahtotietoaineisto should have property owl:equivalentClass containing class restrictions
        var result = modelCaptor.getValue();
        assertTrue(result.getResource(newModelURI + "Lahtotietoaineisto").hasProperty(OWL.equivalentClass));

        RDFDataMgr.write(System.out, result, RDFFormat.TURTLE);
    }

    @Test
    void testProfileMigration() throws URISyntaxException {
        var prefix = "fi-dcatap";
        var newModelURI = DataModelURI.createModelURI(prefix).getModelURI();

        when(coreRepository.getServiceCategories())
                .thenReturn(MapperTestUtils.getModelFromFile("/service-categories.ttl"));
        var oldData = MapperTestUtils.getModelFromFile("/migration/fi-dcatap.ttl");

        var newModel = ModelFactory.createDefaultModel();
        newModel.createResource(DataModelURI.createResourceURI(prefix, "CatalogRecord").getResourceURI());

        when(coreRepository.fetch(newModelURI)).thenReturn(newModel);
        when(resourceService.exists(prefix, "primaryTopic")).thenReturn(true);
        when(dataModelService.create(any(DataModelDTO.class), any(GraphType.class))).thenReturn(new URI(newModelURI));

        migrationService.migrateDatamodel(prefix, oldData);

        verify(dataModelService).create(dataModelCaptor.capture(), eq(GraphType.PROFILE));
        verify(classService, times(2)).create(eq(prefix), baseDtoCaptor.capture(), eq(true));
        verify(resourceService).create(eq(prefix), baseDtoCaptor.capture(), eq(ResourceType.ATTRIBUTE), eq(true));
        verify(resourceService).create(eq(prefix), baseDtoCaptor.capture(), eq(ResourceType.ASSOCIATION), eq(true));
        verify(coreRepository).put(eq(newModelURI), modelCaptor.capture());

        var identifiers = baseDtoCaptor.getAllValues().stream().map(BaseDTO::getIdentifier).toList();
        var datasetNodeShape = baseDtoCaptor.getAllValues().stream().filter(b -> b.getIdentifier().equals("Dataset")).findFirst();
        var createdModel = modelCaptor.getValue();
        var nodeShapeResource = createdModel.getResource(DataModelURI.createResourceURI(prefix, "CatalogRecord").getResourceURI());
        var propertyURIs = nodeShapeResource.listProperties().mapWith(p -> p.getObject().toString()).toList();

        assertEquals(prefix, dataModelCaptor.getValue().getPrefix());

        // Assume there is already resource with id primaryTopic, so suffix '-1' is added
        assertTrue(identifiers.containsAll(List.of("CatalogRecord", "Dataset", "primaryTopic-1", "modified")));
        assertEquals(2, propertyURIs.size());
        assertTrue(propertyURIs.containsAll(List.of(
                "https://iri.suomi.fi/model/fi-dcatap/modified",
                "https://iri.suomi.fi/model/fi-dcatap/primaryTopic-1")
        ));

        // should remove invalid language from label
        assertTrue(datasetNodeShape.isPresent());
        assertEquals(1, datasetNodeShape.get().getLabel().keySet().size());
    }
}
