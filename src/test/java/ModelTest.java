/**
 * Created by malonen on 23.11.2017.
 */

import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.matchers.JsonPathMatchers;
import fi.vm.yti.datamodel.api.utils.ModelManager;
import org.apache.jena.rdf.model.Model;
import org.glassfish.jersey.client.ClientProperties;
import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.FixMethodOrder;
import org.junit.Test;
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

        logger.info(model);

        Model newModel = ModelManager.createJenaModelFromJSONLDString(model);

        if(newModel.size()<=0) {
            Assert.fail();
        } else {
            Response newModelResponse = target.path("model").request().put(Entity.entity(ModelManager.writeModelToString(newModel),"application/ld+json"));
            Assert.assertEquals(200,newModelResponse.getStatus());

            String uuidString = newModelResponse.readEntity(String.class);
            logger.info("Created test model: "+uuidString);

            Assert.assertEquals(403,target.path("model").request().put(Entity.entity(ModelManager.writeModelToString(newModel),"application/ld+json")).getStatus());
        }

    }

}
