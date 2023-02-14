package fi.vm.yti.datamodel.api.v2.service;

import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.riot.RiotException;
import org.springframework.stereotype.Service;

@Service
public class NamespaceResolver {

    private final JenaService jenaService;

    public NamespaceResolver(JenaService jenaService) {
        this.jenaService = jenaService;
    }

    /**
     * Resolved namespace
     * @param namespace Namespace uri
     * @return true if resolved
     */
    public boolean resolveNamespace(String namespace){
        var model = ModelFactory.createDefaultModel();
        try{
            model.read(namespace);
            //if namespace contained any triplets we can put the statements into fuseki
            if(model.size() > 0){
                jenaService.putNamespaceToImports(namespace, model);
                return true;
            }
        } catch (RiotException exception){
            //if namespace resolution fails for any reason we send false back
            //example: url not real, url can't find anything, url does not contain valid syntax
            return false;
        }
        return false;
    }

    public boolean namespaceAlreadyResolved(String namespace){
        return jenaService.doesResolvedNamespaceExist(namespace);
    }
}
