/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

import com.csc.fi.ioapi.api.usermanagement.Group;
import com.csc.fi.ioapi.config.ApplicationProperties;
import java.util.logging.Logger;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.junit.After;
import org.junit.AfterClass;
import static org.junit.Assert.fail;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 *
 * @author malonen
 */
public class EndpointConnectionTest {
    
    public EndpointConnectionTest() {
    }
    
    @BeforeClass
    public static void setUpClass() {
    }
    
    @AfterClass
    public static void tearDownClass() {
    }
    
    @Before
    public void setUp() {
    }
    
    @After
    public void tearDown() {
    }

    private static final Logger logger = Logger.getLogger(EndpointConnectionTest.class.getName());
        
    // TODO add test methods here.
    // The methods must be annotated with annotation @Test. For example:
    //
    @Test
    public void testEndpoint() {
        String queryString = "SELECT ?s ?p ?o WHERE { ?s ?p ?o } LIMIT 1";
    
         String endpoint = ApplicationProperties.getEndpoint()+"/core/sparql";
        
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
