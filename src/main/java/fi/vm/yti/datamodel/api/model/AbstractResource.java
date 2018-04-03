package fi.vm.yti.datamodel.api.model;

import fi.vm.yti.datamodel.api.service.GraphManager;
import fi.vm.yti.datamodel.api.service.JenaClient;
import fi.vm.yti.datamodel.api.service.ModelManager;
import fi.vm.yti.datamodel.api.service.ServiceDescriptionManager;
import fi.vm.yti.datamodel.api.utils.LDHelper;
import org.apache.jena.iri.IRI;
import org.apache.jena.rdf.model.*;
import org.apache.jena.vocabulary.DCTerms;
import org.apache.jena.vocabulary.RDFS;

import org.slf4j.Logger;import org.slf4j.LoggerFactory;

public class AbstractResource {

    protected Model graph;
    protected DataModel dataModel;
    protected String provUUID;
    protected IRI id;
    private static final Logger logger = LoggerFactory.getLogger(AbstractResource.class.getName());

    private final GraphManager graphManager;

    public AbstractResource(GraphManager graphManager) {

        this.graphManager = graphManager;
    }

    public AbstractResource(IRI graphIRI,
                            GraphManager graphManager) {

        this.graphManager = graphManager;

        this.graph = graphManager.getCoreGraph(graphIRI);
        this.id = graphIRI;

        try {

            Statement isDefinedBy = asGraph().getRequiredProperty(ResourceFactory.createResource(getId()), RDFS.isDefinedBy);
            Resource abstractResource = isDefinedBy.getSubject().asResource();
            Resource modelResource = isDefinedBy.getObject().asResource();

            logger.info(isDefinedBy.getSubject().toString()+" "+isDefinedBy.getObject().asResource().toString());
            logger.info(abstractResource.toString());

            this.dataModel = new DataModel(LDHelper.toIRI(modelResource.toString()), graphManager);

            if(!this.id.toString().startsWith(getModelId())) {
                throw new IllegalArgumentException("Resource ID should start with model ID!");
            }

            StmtIterator props = abstractResource.listProperties();
            while(props.hasNext()) {
                logger.info(props.next().getPredicate().getURI());
            }

            try {
                this.provUUID = abstractResource.getRequiredProperty(DCTerms.identifier).getLiteral().toString();
            } catch(Exception ex) {
                ex.printStackTrace();
                logger.warn(ex.getMessage());
                throw new IllegalArgumentException("Expected 1 provenance ID");
            }

        } catch(Exception ex)  {
            ex.printStackTrace();
            throw new IllegalArgumentException("Expected 1 resource defined by model");
        }

    }

    public Model asGraph(){
        return this.graph;
    }
    public Model asGraphCopy() { return ModelFactory.createDefaultModel().add(this.graph); }
    public String getId() { return this.id.toString();}
    public String getModelId() { return this.dataModel.getId(); }
    public IRI getModelIRI() { return this.dataModel.getIRI(); }
    public IRI getIRI() { return this.id; }

}
