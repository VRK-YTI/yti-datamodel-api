package fi.vm.yti.datamodel.api.v2.service;

import fi.vm.yti.datamodel.api.v2.mapper.ResourceMapper;
import fi.vm.yti.datamodel.api.v2.opensearch.index.OpenSearchIndexer;
import org.apache.jena.atlas.web.HttpException;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.riot.RiotException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Objects;

@Service
public class NamespaceResolver {

    private final Logger logger = LoggerFactory.getLogger(NamespaceResolver.class);

    private final JenaService jenaService;
    private final OpenSearchIndexer indexer;

    public NamespaceResolver(JenaService jenaService, OpenSearchIndexer indexer) {
        this.jenaService = jenaService;
        this.indexer = indexer;
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
                var indexResources = model.listSubjects()
                        .mapWith(resource -> ResourceMapper.mapExternalToIndexResource(model, resource))
                        .toList()
                        .stream().filter(Objects::nonNull)
                        .toList();
                logger.info("Namespace {} resolved, add {} items to index", namespace, indexResources.size());
                indexer.bulkInsert(OpenSearchIndexer.OPEN_SEARCH_INDEX_EXTERNAL, indexResources);
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
