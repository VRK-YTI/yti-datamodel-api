package fi.vm.yti.datamodel.api.v2.service;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdfconnection.RDFConnection;
import org.springframework.stereotype.Service;

@Service
public class JenaService {

    private final RDFConnection coreWrite;
    private final RDFConnection coreRead;

    public JenaService() {
        this.coreWrite = RDFConnection.connect("http://localhost:3030/core/data");
        this.coreRead = RDFConnection.connect("http://localhost:3030/core/get");
    }

    public void createDataModel(String graphName, Model model) {
        coreWrite.put(graphName, model);
    }

    public Model getDataModel(String modelId) {
        return coreRead.fetch(modelId);
    }
}
