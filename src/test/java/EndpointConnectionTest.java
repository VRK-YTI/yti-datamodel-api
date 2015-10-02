/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */


import com.csc.fi.ioapi.config.ApplicationProperties;
import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.QueryExecutionFactory;
import com.hp.hpl.jena.query.QueryFactory;
import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.query.ResultSet;
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

    // TODO add test methods here.
    // The methods must be annotated with annotation @Test. For example:
    //
    @Test
    public void testEndpoint() {
        String queryString = "SELECT ?s ?p ?o WHERE { ?s ?p ?o } LIMIT 1";
    
         String endpoint = ApplicationProperties.getEndpoint()+"/core/sparql";
         System.out.println();
        
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
