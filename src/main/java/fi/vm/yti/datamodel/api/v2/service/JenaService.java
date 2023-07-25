package fi.vm.yti.datamodel.api.v2.service;

import fi.vm.yti.datamodel.api.v2.dto.ModelConstants;
import fi.vm.yti.datamodel.api.v2.repository.CoreRepository;
import fi.vm.yti.datamodel.api.v2.repository.ImportsRepository;
import org.apache.jena.arq.querybuilder.AskBuilder;
import org.apache.jena.arq.querybuilder.ConstructBuilder;
import org.apache.jena.atlas.web.HttpException;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.vocabulary.OWL;
import org.apache.jena.vocabulary.RDF;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;

@Service
public class JenaService {

    private final CoreRepository coreRepository;
    private final ImportsRepository importsRepository;

    private static final String VERSION_NUMBER_GRAPH = "urn:yti:metamodel:version";

    public JenaService(CoreRepository coreRepository,
                       ImportsRepository importsRepository) {
        this.coreRepository = coreRepository;
        this.importsRepository = importsRepository;
    }

     /**
     * Check if resource uri is one of given types
     * @param resourceUri Resource uri
     * @param types List of types to check
     * @param checkImports Should imports be checked instead of core
     * @return true if resource is one of types
     */
    public boolean checkIfResourceIsOneOfTypes(String resourceUri, List<Resource> types, boolean checkImports) {
        var askBuilder  =new AskBuilder()
                .addWhere(NodeFactory.createURI(resourceUri), RDF.type, "?type")
                .addValueVar("?type", types.toArray());
        try{
            if(checkImports){
                return importsRepository.queryAsk(askBuilder.build());
            }else {
                return coreRepository.queryAsk(askBuilder.build());
            }
        }catch(HttpException ex){
            throw new JenaQueryException();
        }
    }

    public Model findResources(Set<String> resourceURIs) {
        if (resourceURIs == null || resourceURIs.isEmpty()) {
            return ModelFactory.createDefaultModel();
        }
        var coreBuilder = new ConstructBuilder()
                .addPrefixes(ModelConstants.PREFIXES);
        var importsBuilder = new ConstructBuilder()
                .addPrefixes(ModelConstants.PREFIXES);

        var iterator = resourceURIs.iterator();
        var count = 0;
        while (iterator.hasNext()) {
            var pred = "?p" + count;
            var obj = "?o" + count;
            String uri = iterator.next();
            var resource = ResourceFactory.createResource(uri);
            if (uri.startsWith(ModelConstants.SUOMI_FI_NAMESPACE)) {
                coreBuilder.addConstruct(resource, pred, obj);
                coreBuilder.addOptional(resource, pred, obj);
            } else {
                importsBuilder.addConstruct(resource, pred, obj);
                importsBuilder.addOptional(resource, pred, obj);
            }
            count++;
        }

        var resultModel = coreRepository.queryConstruct(coreBuilder.build());
        var importsModel = importsRepository.queryConstruct(importsBuilder.build());

        resultModel.add(importsModel);
        return resultModel;
    }

    public int getVersionNumber() {
        var versionModel = coreRepository.fetch(VERSION_NUMBER_GRAPH);
        return versionModel.getResource(VERSION_NUMBER_GRAPH).getRequiredProperty(OWL.versionInfo).getInt();
    }

    public void setVersionNumber(int version) {
        var versionModel = ModelFactory.createDefaultModel().addLiteral(ResourceFactory.createResource(VERSION_NUMBER_GRAPH), OWL.versionInfo, version);
        versionModel.setNsPrefix("owl", "http://www.w3.org/2002/07/owl#");
        coreRepository.put(VERSION_NUMBER_GRAPH, versionModel);
    }

    public boolean isVersionGraphInitialized(){
        return coreRepository.graphExists(VERSION_NUMBER_GRAPH);
    }
}
