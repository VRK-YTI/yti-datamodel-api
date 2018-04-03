package fi.vm.yti.datamodel.api.service;

import org.apache.jena.query.*;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdfconnection.RDFConnectionRemote;
import org.apache.jena.update.UpdateExecutionFactory;
import org.apache.jena.update.UpdateProcessor;
import org.apache.jena.update.UpdateRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import org.slf4j.Logger;import org.slf4j.LoggerFactory;

@Service
public final class JenaClient {

    static final private Logger logger = LoggerFactory.getLogger(JenaClient.class.getName());

    private final EndpointServices endpointServices;
    private final DatasetAccessor coreService;
    private final DatasetAccessor importService;
    private final DatasetAccessor provService;

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
    }


    public void putToImports(String graph, Model model) {
        importService.putModel(graph, model);
    }

    public Model getModelFromCore(String graph) {
        return coreService.getModel(graph);
    }

    public Model getModelFromProv(String graph) {
        return provService.getModel(graph);
    }

    public boolean containsCoreModel(String graph) {
        return coreService.containsModel(graph);
    }

    public boolean containsSchemaModel(String graph) {
        return importService.containsModel(graph);
    }

    public  void deleteModelFromCore(String graph) {
        coreService.deleteModel(graph);
    }

    public void putModelToCore(String graph, Model model) {
        coreService.putModel(graph, model);
    }

    public void addModelToCore(String graph, Model model) {
        coreService.add(graph, model);
    }

    public void putModelToProv(String graph, Model model) {
        provService.putModel(graph, model);
    }

    public void addModelToProv(String graph, Model model) {
        provService.add(graph, model);
    }

    public void updateToService(UpdateRequest req, String service) {
        UpdateProcessor qexec = UpdateExecutionFactory.createRemoteForm(req, service);
        qexec.execute();
    }

    public Model constructFromService(String query, String service) {
        QueryExecution qexec = QueryExecutionFactory.sparqlService(service, query);
        return qexec.execConstruct();
    }

    public boolean askQuery(String service, Query query, String graph) {
        QueryExecution qexec = QueryExecutionFactory.sparqlService(endpointServices.getCoreSparqlAddress(), query, "urn:csc:iow:sd");
        return qexec.execAsk();
    }

    public Model fetchModelFromCore(String graph) {
         try(RDFConnectionRemote conn = endpointServices.getCoreConnection()){
            return conn.fetch(graph);
        } catch(Exception ex) {
            logger.warn(ex.getMessage());
            return null;
        }
    }
}
