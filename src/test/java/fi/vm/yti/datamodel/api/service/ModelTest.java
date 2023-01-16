package fi.vm.yti.datamodel.api.service;

import com.jayway.jsonpath.matchers.JsonPathMatchers;

import org.apache.jena.iri.IRI;
import org.apache.jena.rdf.model.Model;
import org.glassfish.jersey.client.ClientProperties;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import fi.vm.yti.datamodel.api.config.ApplicationProperties;
import fi.vm.yti.datamodel.api.model.DataModel;
import fi.vm.yti.datamodel.api.model.ReusableClass;
import fi.vm.yti.datamodel.api.model.ReusablePredicate;
import fi.vm.yti.datamodel.api.security.AuthorizationManager;
import fi.vm.yti.datamodel.api.utils.LDHelper;
import fi.vm.yti.security.YtiUser;
import fi.vm.yti.security.AuthenticatedUserProvider;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.*;


@TestMethodOrder(MethodOrderer.MethodName.class)
@ActiveProfiles("junit")
@TestPropertySource("classpath:application-test.properties")
@SpringBootTest
@Disabled
//TODO: this is part of v1 is this in use anymore
class ModelTest  {

    private static final Logger logger = LoggerFactory.getLogger(ModelTest.class.getName());
    private static Client testClient = ClientBuilder.newClient().property(ClientProperties.FOLLOW_REDIRECTS,Boolean.TRUE);
    private static WebTarget target = testClient.target("http://localhost:9004/datamodel-api/api/v1/");
    private static IRI testModelId;

    @Autowired
    private ApplicationProperties applicationProperties;

    @Autowired
    private ModelManager modelManager;

    @Autowired
    private GraphManager graphManager;

    @Autowired
    private RHPOrganizationManager rhpOrganizationManager;

    @Autowired
    private  ServiceDescriptionManager serviceDescriptionManager;

    @Autowired
    private AuthenticatedUserProvider authenticatedUserProvider;

    @Autowired
    private AuthorizationManager authorizationManager;

    public ModelTest() {
    }

    @Test
    void test1_user() {

        String user = target.path("user").request().get().readEntity(String.class);
        assertThat(user,JsonPathMatchers.hasJsonPath("$.email",Matchers.notNullValue()));
        assertThat(user,JsonPathMatchers.hasJsonPath("$.firstName"));
        assertThat(user,JsonPathMatchers.hasJsonPath("$.lastName"));

    }

    @Test
    void test2_createNewModel() {

        testModelId = LDHelper.toIRI(applicationProperties.getDefaultNamespace()+"junit5");

        rhpOrganizationManager.initTestOrganizations();

        YtiUser user = authenticatedUserProvider.getUser();

        logger.debug(user.getEmail());

        Response creatorResponse = target.path("modelCreator")
                .queryParam("prefix","junit5")
                .queryParam("label","JUNIT Test Model")
                .queryParam("orgList","7d3a3c00-5a6b-489b-a3ed-63bb58c26a63")
                .queryParam("serviceList","P1")
                .queryParam("lang","en")
                .request()
                .get();

        if(!creatorResponse.getStatusInfo().getFamily().equals(Response.Status.Family.SUCCESSFUL)) {
            System.out.println(target.path("modelCreator").getUri());
            System.out.println(creatorResponse.getStatus());
            System.out.println(creatorResponse.getStatusInfo().getReasonPhrase());
             fail();
        }

        String model = creatorResponse.readEntity(String.class);

        Model newModel = modelManager.createJenaModelFromJSONLDString(model);

        DataModel dataModel = new DataModel(newModel, graphManager, rhpOrganizationManager);

        logger.debug("Created model");

        logger.debug("Has right to edit: "+authorizationManager.hasRightToEdit(dataModel));

        if(newModel.size()<=0) {
            fail();
        } else {
            graphManager.createModel(dataModel);
            serviceDescriptionManager.createGraphDescription(dataModel.getId(), user.getId(), dataModel.getOrganizations());
            assertTrue(graphManager.isExistingGraph(dataModel.getIRI()));
        }

    }

    @Test
    void test3_createNewClassToModel() {

        String testClass = target.path("classCreator")
                .queryParam("modelID",testModelId)
               .queryParam("classLabel","JUNIT Test Class")
                .queryParam("lang","en")
              .request()
               .get()
                .readEntity(String.class);

        Model classModel = modelManager.createJenaModelFromJSONLDString(testClass);

        ReusableClass reusableClass = new ReusableClass(classModel, graphManager);

        graphManager.createResource(reusableClass);

        assertTrue(graphManager.isExistingGraph(reusableClass.getIRI()));

    }

    @Test
    void test4_createNewPredicateToModel(){

       String testPredicate = target.path("predicateCreator")
                .queryParam("modelID",testModelId)
                .queryParam("predicateLabel","JUNIT Test Predicate")
               .queryParam("type","owl:DatatypeProperty")
                .queryParam("lang","en")
               .request()
               .get()
           .readEntity(String.class);

        Model predicateModel = modelManager.createJenaModelFromJSONLDString(testPredicate);

        ReusablePredicate reusablePredicate = new ReusablePredicate(predicateModel, graphManager);

        graphManager.createResource(reusablePredicate);

        assertTrue(graphManager.isExistingGraph(reusablePredicate.getIRI()));

    }

    @Test
    void test5_removeModel() {

        graphManager.removeModel(testModelId);
        serviceDescriptionManager.deleteGraphDescription(testModelId.toString());
        assertFalse(graphManager.isExistingGraph(testModelId));
        rhpOrganizationManager.initOrganizationsFromRHP();

    }


}
