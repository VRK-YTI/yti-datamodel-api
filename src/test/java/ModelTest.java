/**
 * Created by malonen on 23.11.2017.
 */

import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.matchers.JsonPathMatchers;
import fi.vm.yti.datamodel.api.model.ReusableClass;
import fi.vm.yti.datamodel.api.model.ReusablePredicate;
import fi.vm.yti.datamodel.api.utils.ModelManager;
import org.apache.jena.rdf.model.Model;
import org.glassfish.jersey.client.ClientProperties;
import org.hamcrest.Matchers;
import org.junit.*;
import org.junit.runners.MethodSorters;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;
import java.util.logging.Logger;

//TODO: Add categories? https://github.com/junit-team/junit4/wiki/Categories

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class ModelTest  {

    private static final Logger logger = Logger.getLogger(ModelTest.class.getName());
    private static Client testClient = ClientBuilder.newClient().property(ClientProperties.FOLLOW_REDIRECTS,Boolean.TRUE);
    private static WebTarget target = testClient.target("http://localhost:8084/api/rest/");
    private static String testModelId;

    public ModelTest() {
    }
    @Test
    public void test1_user() {

        String user = target.path("user").request().get().readEntity(String.class);
        Assert.assertThat(user,JsonPathMatchers.hasJsonPath("$.email",Matchers.notNullValue()));
        Assert.assertThat(user,JsonPathMatchers.hasJsonPath("$.firstName"));
        Assert.assertThat(user,JsonPathMatchers.hasJsonPath("$.lastName"));

    }

    @Test
    public void test2_createNewModel() {

        String model = target.path("modelCreator")
                .queryParam("prefix","junittest")
                .queryParam("label","JUNIT Test Model")
                .queryParam("orgList","7d3a3c00-5a6b-489b-a3ed-63bb58c26a63")
                .queryParam("serviceList","EDUC")
                .queryParam("lang","en")
                .request()
                .get()
                .readEntity(String.class);

        Model newModel = ModelManager.createJenaModelFromJSONLDString(model);

        if(newModel.size()<=0) {
            Assert.fail();
        } else {
            Response newModelResponse = target.path("model").request().put(Entity.entity(ModelManager.writeModelToString(newModel),"application/ld+json"));
            Assert.assertEquals(200,newModelResponse.getStatus());

            String uuidString = newModelResponse.readEntity(String.class);
            logger.info("Created test model: "+uuidString);

            Object jsonObject = Configuration.defaultConfiguration().jsonProvider().parse(uuidString);

            testModelId = JsonPath.read(jsonObject, "$.@id");

            Assert.assertEquals(403,target.path("model").request().put(Entity.entity(ModelManager.writeModelToString(newModel),"application/ld+json")).getStatus());



        }

    }

    @Test
    public void test3_createNewClassToModel() {

        String testClass = target.path("classCreator")
                .queryParam("modelID",testModelId)
                .queryParam("classLabel","JUNIT Test Class")
               // .queryParam("conceptID","http://pid.suomi.fi/terminology/oksa/tmpOKSAID518")
                .queryParam("lang","en")
                .request()
                .get()
                .readEntity(String.class);

        Model classModel = ModelManager.createJenaModelFromJSONLDString(testClass);
        String classString = ModelManager.writeModelToString(classModel);

        Response newClassResponse = target.path("class").request().put(Entity.entity(classString,"application/ld+json"));
        Assert.assertEquals(200,newClassResponse.getStatus());
        String classId = JsonPath.read(newClassResponse.readEntity(String.class),"$.@id");

        logger.info("Created "+classId);

        Response classResponse = target.path("class").queryParam("id",classId).request().get();
        Assert.assertEquals(200,classResponse.getStatus());
        String classResponseString = classResponse.readEntity(String.class);

        DocumentContext json = JsonPath.parse(classResponseString);
        String jsonPath = "$.@graph[?(@.['@id']=='"+classId+"')].label.@value";
        json.set(jsonPath,"Test 2" ).jsonString();

         ReusableClass updateClass = new ReusableClass(json.jsonString());

         Response updateClassResponse = target.path("class").queryParam("id",updateClass.getId()).queryParam("model",updateClass.getModelId()).request().post(Entity.entity(ModelManager.writeModelToString(updateClass.asGraph()),"application/ld+json"));

         Assert.assertEquals(200,updateClassResponse.getStatus());

    }

    @Test
    public void test4_createNewPredicateToModel(){

        String testPredicate = target.path("predicateCreator")
                .queryParam("modelID",testModelId)
                .queryParam("predicateLabel","JUNIT Test Predicate")
                .queryParam("type","owl:DatatypeProperty")
                // .queryParam("conceptID","http://pid.suomi.fi/terminology/oksa/tmpOKSAID518")
                .queryParam("lang","en")
                .request()
                .get()
                .readEntity(String.class);

        Model predicateModel = ModelManager.createJenaModelFromJSONLDString(testPredicate);
        String predicateString = ModelManager.writeModelToString(predicateModel);

        Response newPredicateResponse = target.path("predicate").request().put(Entity.entity(predicateString,"application/ld+json"));
        Assert.assertEquals(200,newPredicateResponse.getStatus());
        String newPredicateString = newPredicateResponse.readEntity(String.class);
        logger.info(newPredicateString);
        String predicateId = JsonPath.read(newPredicateString,"$.@id");

        logger.info("Created "+predicateId);

        Response predicateResponse = target.path("predicate").queryParam("id",predicateId).request().get();
        Assert.assertEquals(200,predicateResponse.getStatus());
        String predicateResponseString = predicateResponse.readEntity(String.class);

        DocumentContext json = JsonPath.parse(predicateResponseString);
        String jsonPath = "$.@graph[?(@.['@id']=='"+predicateId+"')].label.@value";
        json.set(jsonPath,"Test 2 Edit" ).jsonString();

        ReusablePredicate updatePredicate = new ReusablePredicate(json.jsonString());
        Response updatePredicateResponse = target.path("predicate").queryParam("id",updatePredicate.getId()).queryParam("model",updatePredicate.getModelId()).request().post(Entity.entity(ModelManager.writeModelToString(updatePredicate.asGraph()),"application/ld+json"));

        Assert.assertEquals(200,updatePredicateResponse.getStatus());
    }


    @AfterClass
    public static void deleteModel() {
        target.path("model").queryParam("id",testModelId).request().delete().getStatus();
    }


}
