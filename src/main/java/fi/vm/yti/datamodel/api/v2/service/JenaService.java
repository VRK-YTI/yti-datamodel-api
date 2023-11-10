package fi.vm.yti.datamodel.api.v2.service;

import fi.vm.yti.datamodel.api.v2.repository.CoreRepository;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.vocabulary.OWL;
import org.springframework.stereotype.Service;

@Service
public class JenaService {

    private final CoreRepository coreRepository;

    private static final String VERSION_NUMBER_GRAPH = "urn:yti:metamodel:version";

    public JenaService(CoreRepository coreRepository) {
        this.coreRepository = coreRepository;
    }

    public int getVersionNumber() {
        var versionModel = coreRepository.fetch(VERSION_NUMBER_GRAPH);
        return versionModel.getResource(VERSION_NUMBER_GRAPH).getRequiredProperty(OWL.versionInfo).getInt();
    }

    public void setVersionNumber(int version) {
        var versionModel = ModelFactory.createDefaultModel().addLiteral(ResourceFactory.createResource(VERSION_NUMBER_GRAPH), OWL.versionInfo, version);
        coreRepository.put(VERSION_NUMBER_GRAPH, versionModel);
    }

    public boolean isVersionGraphInitialized(){
        return coreRepository.graphExists(VERSION_NUMBER_GRAPH);
    }
}
