package fi.vm.yti.datamodel.api.model;

import fi.vm.yti.datamodel.api.config.EndpointServices;
import fi.vm.yti.datamodel.api.utils.GraphManager;
import fi.vm.yti.datamodel.api.utils.LDHelper;
import fi.vm.yti.datamodel.api.utils.RHPOrganizationManager;
import org.apache.jena.iri.IRI;
import org.apache.jena.rdf.model.*;
import org.apache.jena.vocabulary.DCTerms;
import org.apache.jena.vocabulary.RDFS;

import java.util.List;
import java.util.UUID;
import java.util.logging.Logger;

/**
 * Created by malonen on 29.11.2017.
 */
public class AbstractPredicate extends AbstractResource {


    protected String provUUID;
    private static final Logger logger = Logger.getLogger(AbstractPredicate.class.getName());


    public AbstractPredicate() {}

    public AbstractPredicate(IRI graphIRI) {
        this.graph = GraphManager.getCoreGraph(graphIRI);

        List<RDFNode> modelList = this.graph.listObjectsOfProperty(RDFS.isDefinedBy).toList();
        if(modelList==null || modelList.size()!=1) {
            throw new IllegalArgumentException("Expected 1 model (isDefinedBy)");
        }

        this.dataModel = new DataModel(LDHelper.toIRI(modelList.get(0).asResource().getURI()));

        List<Resource> predicateList = this.graph.listSubjectsWithProperty(RDFS.isDefinedBy).toList();
        if(predicateList == null || predicateList.size()!=1) {
            throw new IllegalArgumentException("Expected 1 predicate defined in graph!");
        }

        Resource predicateResource = predicateList.get(0);
        this.id = LDHelper.toIRI(predicateResource.getURI());

        List<Statement> provIdList = predicateResource.listProperties(DCTerms.identifier).toList();
        if(provIdList == null || provIdList.size()==0 || provIdList.size()>1) {
            throw new IllegalArgumentException("Expected only 1 provenance ID, got "+provIdList.size());
        } else {
            this.provUUID = provIdList.get(0).getLiteral().toString();
        }
    }

    public AbstractPredicate(Model graph){
        this.graph = graph;

        try {
            Statement isDefinedBy = asGraph().getRequiredProperty(null, RDFS.isDefinedBy);
            Resource predicateResource = isDefinedBy.getSubject();

            this.dataModel = new DataModel(LDHelper.toIRI(isDefinedBy.getResource().toString()));
            this.id = LDHelper.toIRI(predicateResource.toString());
            this.provUUID = "urn:uuid:"+UUID.randomUUID().toString();

            predicateResource.removeAll(DCTerms.identifier);
            predicateResource.addProperty(DCTerms.identifier,ResourceFactory.createPlainLiteral(provUUID));


        } catch(Exception ex)  {
            logger.warning(ex.getMessage());
            throw new IllegalArgumentException("Expected 1 predicate (isDefinedBy)");

        }

        if(!getId().startsWith(getModelId())) {
            throw new IllegalArgumentException("Predicate ID should start with model ID!");
        }


    }

    public String getProvUUID() { return this.provUUID; }
    public List<UUID> getOrganizations() { return this.dataModel.getOrganizations(); }

}