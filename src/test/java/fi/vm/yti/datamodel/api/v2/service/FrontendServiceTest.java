package fi.vm.yti.datamodel.api.v2.service;

import fi.vm.yti.datamodel.api.v2.mapper.ModelMapper;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(SpringExtension.class)
@Import({
        FrontendService.class,
        ModelMapper.class
})
public class FrontendServiceTest {

    @MockBean
    private JenaService jenaService;

    @Autowired
    FrontendService service;

    @Test
    void testOrganizations() {
        var model = getModel("/organizations.ttl");
        String sortLanguage = "en";

        when(jenaService.getOrganizations()).thenReturn(model);

        var organizations = service.getOrganizations(sortLanguage, false);
        var orgNames = organizations.stream()
                .map(o -> o.getLabel().get(sortLanguage))
                .collect(Collectors.toList());

        assertEquals(2, organizations.size());
        assertEquals(List.of("Interoperability platform developers", "Test organization"), orgNames);
    }

    @Test
    void testOrganizationsWithChildren() {
        var model = getModel("/organizations.ttl");
        String sortLanguage = "en";

        when(jenaService.getOrganizations()).thenReturn(model);

        var organizations = service.getOrganizations(sortLanguage, true);

        assertEquals(3, organizations.size());
        assertEquals(1, organizations.stream().filter(o -> o.getParentOrganization() != null).count());
    }

    @Test
    void testServiceCategories() {
        var model = getModel("/service-categories.ttl");
        String sortLanguage = "en";

        when(jenaService.getServiceCategories()).thenReturn(model);

        var serviceCategories = service.getServiceCategories(sortLanguage);
        var names = serviceCategories.stream()
                .map(cat -> cat.getLabel().get(sortLanguage))
                .collect(Collectors.toList());

        assertEquals(List.of("Consumer matters", "Industries", "Tourism"), names);
    }

    private Model getModel(String data) {
        var model = ModelFactory.createDefaultModel();
        var stream = getClass().getResourceAsStream(data);
        assertNotNull(stream);
        RDFDataMgr.read(model, stream, Lang.TURTLE);
        return model;
    }
}
