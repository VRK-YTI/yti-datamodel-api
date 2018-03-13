import fi.vm.yti.datamodel.api.config.ApplicationProperties;
import org.apache.jena.query.*;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.logging.Logger;

import static org.junit.Assert.fail;

@RunWith(SpringRunner.class)
@ContextConfiguration(classes = TestConfiguration.class)
@ActiveProfiles("test")
@TestPropertySource("classpath:application-test.properties")
public class EndpointConnectionTest {

    @Autowired
    private ApplicationProperties applicationProperties;

    private static final Logger logger = Logger.getLogger(EndpointConnectionTest.class.getName());

    // TODO add test methods here.
    // The methods must be annotated with annotation @Test. For example:
    //
    @Test
    public void testEndpoint() {

        String queryString = "SELECT ?s ?p ?o WHERE { ?s ?p ?o } LIMIT 1";

        String endpoint = applicationProperties.getEndpoint()+"/core/sparql";

        logger.info("Testing "+endpoint);

        Query query = QueryFactory.create(queryString);
        QueryExecution qexec = QueryExecutionFactory.sparqlService(endpoint, query);
        try
        {
            ResultSet results = qexec.execSelect();
            while(results.hasNext()) {
                QuerySolution soln = results.nextSolution();
                System.out.println(soln);
            }
        } catch(Exception ex) {
            fail("FAILED TO SEND: "+queryString+" to EDNPOINT: "+endpoint);
        }
        finally{
            qexec.close();
        }
    }
}
