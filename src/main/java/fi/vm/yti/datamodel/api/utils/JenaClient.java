package fi.vm.yti.datamodel.api.utils;

import fi.vm.yti.datamodel.api.config.EndpointServices;
import org.apache.jena.query.*;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdfconnection.RDFConnectionRemote;
import org.apache.jena.update.UpdateExecutionFactory;
import org.apache.jena.update.UpdateProcessor;
import org.apache.jena.update.UpdateRequest;

import java.util.logging.Logger;

/**
 * Created by malonen on 29.1.2018.
 */
public class JenaClient {

    static EndpointServices services = new EndpointServices();
    public static final DatasetAccessor coreService = DatasetAccessorFactory.createHTTP(services.getCoreReadWriteAddress());
    public static final DatasetAccessor importService = DatasetAccessorFactory.createHTTP(services.getImportsReadWriteAddress());
    public static final DatasetAccessor provService = DatasetAccessorFactory.createHTTP(services.getProvReadWriteAddress());

    static final private Logger logger = Logger.getLogger(JenaClient.class.getName());

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
            logger.warning(ex.getMessage());
        }*/

    public static void putToImports(String graph, Model model) {
        importService.putModel(graph, model);
    }

    public static Model getModelFromCore(String graph) {
        return coreService.getModel(graph);
    }

    public static Model getModelFromProv(String graph) {
        return provService.getModel(graph);
    }

    public static boolean containsCoreModel(String graph) {
        return coreService.containsModel(graph);
    }

    public static boolean containsSchemaModel(String graph) {
        return importService.containsModel(graph);
    }

    public static void deleteModelFromCore(String graph) {
        coreService.deleteModel(graph);
    }

    public static void putModelToCore(String graph, Model model) {
        coreService.putModel(graph, model);
    }

    public static void addModelToCore(String graph, Model model) {
        coreService.add(graph, model);
    }

    public static void putModelToProv(String graph, Model model) {
        provService.putModel(graph, model);
    }

    public static void addModelToProv(String graph, Model model) {
        provService.add(graph, model);
    }

    public static void updateToService(UpdateRequest req, String service) {
        UpdateProcessor qexec = UpdateExecutionFactory.createRemoteForm(req, service);
        qexec.execute();
    }

    public static Model constructFromService(String query, String service) {
        QueryExecution qexec = QueryExecutionFactory.sparqlService(service, query);
        return qexec.execConstruct();
    }

    public static boolean askQuery(String service, Query query, String graph) {
        QueryExecution qexec = QueryExecutionFactory.sparqlService(services.getCoreSparqlAddress(), query, "urn:csc:iow:sd");
        return qexec.execAsk();
    }

    public static Model fetchModelFromCore(String graph) {
         try(RDFConnectionRemote conn = services.getCoreConnection()){
            return conn.fetch(graph);
        } catch(Exception ex) {
            logger.warning(ex.getMessage());
            return null;
        }
    }



}
