package fi.vm.yti.datamodel.api.v2.service;

import org.apache.jena.atlas.web.HttpException;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.riot.RiotException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class NamespaceResolver {

    private final Logger logger = LoggerFactory.getLogger(NamespaceResolver.class);

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
        } catch (RiotException ex){
            logger.warn("Namespace not resolvable: {}", ex.getMessage());
            return false;
        } catch (HttpException ex){
            logger.warn("Namespace not resolvable due to HTTP error, Status code: {}", ex.getStatusCode());
            return false;
        }
        return false;
    }

    public boolean namespaceAlreadyResolved(String namespace){
        return jenaService.doesResolvedNamespaceExist(namespace);
    }
}
