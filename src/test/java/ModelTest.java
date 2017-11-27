/**
 * Created by malonen on 23.11.2017.
 */
import org.glassfish.jersey.client.ClientProperties;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import java.net.URI;
import java.util.logging.Logger;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class ModelTest  {

    private static final Logger logger = Logger.getLogger(ModelTest.class.getName());
    private static Client testClient = ClientBuilder.newClient().property(ClientProperties.FOLLOW_REDIRECTS,Boolean.TRUE);
    private static WebTarget target = testClient.target("http://localhost:8084/api/");

    public ModelTest() {
    }


    @Test
    public void test1_login() {
        Response loginTest = target.path("login").request().get();
        Assert.assertEquals("Login test",200,loginTest.getStatus());
    }

    @Test
    public void test2_loginStatus() {
        Assert.assertEquals("Loginstatus","true",target.path("rest/loginstatus").request().get().readEntity(String.class));
    }

    @Test
    public void test3_user() {
        Assert.assertEquals("User",200,target.path("rest/user").request().get().getStatus());
    }


}
