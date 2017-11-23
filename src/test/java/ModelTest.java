/**
 * Created by malonen on 23.11.2017.
 */
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import java.net.URI;
import java.util.logging.Logger;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class ModelTest  {

    private static final Logger logger = Logger.getLogger(ModelTest.class.getName());
    private static Client test = ClientBuilder.newClient();
    private static String path = "http://localhost:8084/api/rest/";

    public ModelTest() {
    }


    @Test
    public void test1_login() {
      //  test.target("http://localhost:8084/api/login").request().get();
    }

    @Test
    public void test2_loginStatus() {
        //TODO: Login not working in test
      //  Assert.assertEquals("true",test.target(path + "loginstatus").request().get().readEntity(String.class));
    }


}
