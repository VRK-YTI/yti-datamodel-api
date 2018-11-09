package fi.vm.yti.datamodel.api.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.jena.query.*;
import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdfconnection.RDFConnectionRemote;
import org.apache.jena.sparql.resultset.ResultSetPeekable;
import org.apache.jena.update.UpdateExecutionFactory;
import org.apache.jena.update.UpdateProcessor;
import org.apache.jena.update.UpdateRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import org.slf4j.Logger;import org.slf4j.LoggerFactory;

import javax.json.*;
import javax.json.stream.JsonParser;
import java.io.ByteArrayOutputStream;
import java.util.*;

@Service
public final class JenaClient {

    static final private Logger logger = LoggerFactory.getLogger(JenaClient.class.getName());

    private final EndpointServices endpointServices;
    private final DatasetAccessor coreService;
    private final DatasetAccessor importService;
    private final DatasetAccessor provService;
    private final DatasetAccessor schemeService;

    // TODO: Or adapters?
   // static final DatasetAdapter coreService = new DatasetAdapter(new DatasetGraphAccessorHTTP(services.getCoreReadWriteAddress()));
   // static final DatasetAdapter importService = new DatasetAdapter(new DatasetGraphAccessorHTTP(services.getImportsReadWriteAddress()));

   // TODO: Issues with RDFConnection: No id encoding and namespaces disappear!
   /* try(RDFConnectionRemote conn = services.getProvConnection()) {
            Txn.executeWrite(conn, ()-> {
                conn.put(provUUID,model);
                conn.update(ProvenanceManager.createProvenanceActivityRequest(id, provUUID, email));
            });
        } catch(Exception ex) {
            logger.warn(ex.getMessage());
        }*/

   @Autowired
    JenaClient(EndpointServices endpointServices) {
        this.endpointServices = endpointServices;
        this.coreService = DatasetAccessorFactory.createHTTP(endpointServices.getCoreReadWriteAddress());
        this.importService = DatasetAccessorFactory.createHTTP(endpointServices.getImportsReadWriteAddress());
        this.provService = DatasetAccessorFactory.createHTTP(endpointServices.getProvReadWriteAddress());
        this.schemeService = DatasetAccessorFactory.createHTTP(endpointServices.getSchemesReadWriteAddress());
    }

    public Model getModelFromSchemes(String graph) {
        logger.debug("Getting model from "+graph);
        return schemeService.getModel(graph);
    }

    public void putToImports(String graph, Model model) {
       logger.debug("Storing import to "+graph);
       importService.putModel(graph, model);
    }

    public Model getModelFromCore(String graph) {
       logger.debug("Getting model from core "+graph);
       return coreService.getModel(graph);
    }

    public Model getModelFromProv(String graph) {
       logger.debug("Getting model from prov "+graph);
       return provService.getModel(graph);
    }

    public boolean containsCoreModel(String graph) {
        return coreService.containsModel(graph);
    }

    public boolean containsSchemaModel(String graph) {
        return importService.containsModel(graph);
    }

    public  void deleteModelFromCore(String graph) {
       logger.debug("Deleting model from "+graph);
       coreService.deleteModel(graph);
    }

    public  void deleteModelFromScheme(String graph) {
        logger.debug("Deleting codelist from "+graph);
        schemeService.deleteModel(graph);
    }

    public boolean isInCore(String graph) {
      return coreService.containsModel(graph);
    }

    public void putModelToCore(String graph, Model model) {
       logger.debug("Putting model to "+graph);
        coreService.putModel(graph, model);
    }

    public void addModelToCore(String graph, Model model) {
       logger.debug("Adding model to "+graph);
       coreService.add(graph, model);
    }

    public void putModelToProv(String graph, Model model) {
       logger.debug("Putting to prov "+graph);
       provService.putModel(graph, model);
    }

    public void addModelToProv(String graph, Model model) {
       logger.debug("Adding to prov "+graph);
       provService.add(graph, model);
    }

    public void updateToService(UpdateRequest req, String service) {
       logger.debug("Sending UpdateRequest to "+service);
        UpdateProcessor qexec = UpdateExecutionFactory.createRemoteForm(req, service);
        qexec.execute();
    }

    public Model constructFromService(String query, String service) {
       logger.debug("Constructing from "+service);
        try(QueryExecution qexec = QueryExecutionFactory.sparqlService(service, query)) {
            return qexec.execConstruct();
        }
    }

    public boolean askQuery(String service, Query query, String graph) {
       logger.debug("Asking from "+service+" in graph "+graph);
        try(QueryExecution qexec = QueryExecutionFactory.sparqlService(service, query, graph)) {
            return qexec.execAsk();
        }
    }

    public boolean askQuery(String service, Query query) {
        logger.debug("Asking from "+service);
        try(QueryExecution qexec = QueryExecutionFactory.sparqlService(service, query)) {
            return qexec.execAsk();
        }
    }

    public ResultSet selectQuery(String service, Query query) {
       logger.debug("Select from "+service);
        try(QueryExecution qexec = QueryExecutionFactory.sparqlService(service, query)) {
            // ResultSet needs to be copied in order to use it after the connection is closed
            return ResultSetFactory.copyResults(qexec.execSelect()) ;
        }
    }

    /**
     * Returns JSON object from given sparql query. First param of the sparql query is considered to be the URI.
     * Reads only localized literals to JSON object. Other duplicate literal values are not stored to result JSON.
     * @param service URI for the sparql service
     * @param query Sparql query
     * @return JSON string
     */

    public String selectJson(String service, Query query) {
        logger.debug("Select json "+service);
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        try(QueryExecution qexec = QueryExecutionFactory.sparqlService(service, query)) {
            ResultSetPeekable results = ResultSetFactory.makePeekable(qexec.execSelect());
            List<String> resultVars = results.getResultVars();
            String idVar = resultVars.get(0);
            List<Map> hashArray = new ArrayList<>();
            Map objectMap = new HashMap<String, Object>();

            while(results.hasNext()) {
                QuerySolution soln = results.next();
                Iterator<String> currentVars = soln.varNames();
                String currentId = soln.get(idVar).toString();
                while (currentVars.hasNext()) {
                    String currentVar = currentVars.next();
                    RDFNode node = soln.get(currentVar);
                    if (node.isLiteral()) {
                        Literal lit = node.asLiteral();
                        if(lit.getString().length()>0) {
                            String litLang = lit.getLanguage();
                            if (litLang != null && litLang.length() > 0) {
                                Map literalMap;
                                if(objectMap.containsKey(currentVar)) {
                                    literalMap = (Map)objectMap.get(currentVar);
                                } else {
                                    literalMap = new HashMap<String, String>();
                                }
                                literalMap.put(litLang, lit.getString());
                                objectMap.put(currentVar, literalMap);
                            } else {
                                objectMap.put(currentVar, lit.getString());
                            }
                        }
                    } else {
                        objectMap.put(currentVar, node.toString());
                    }
                }

                if(results.hasNext() && !results.peek().get(idVar).toString().equals(currentId)) {
                    hashArray.add(objectMap);
                    objectMap = new HashMap<String,Object>();
                }

            }

            ObjectMapper mapper = new ObjectMapper();
            try {
                return mapper.writeValueAsString(hashArray);
            } catch(Exception ex) {
                ex.printStackTrace();
                return null;
            }

        }
    }

    public String selectCSV(String service, Query query) {
       logger.debug("Select csv from "+service);
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        try(QueryExecution qexec = QueryExecutionFactory.sparqlService(service, query)) {
            // ResultSet needs to be copied in order to use it after the connection is closed
            ResultSetFormatter.outputAsCSV(stream,qexec.execSelect());
            return new String(stream.toByteArray());
        }
    }

    // TODO: Refactor update queries here

    public Model fetchModelFromCore(String graph) {
         try(RDFConnectionRemote conn = endpointServices.getCoreConnection()){
            return conn.fetch(graph);
        } catch(Exception ex) {
            logger.warn(ex.getMessage());
            return null;
        }
    }
    
    public EndpointServices getEndpointServices() {
       return this.endpointServices;
    }
}
