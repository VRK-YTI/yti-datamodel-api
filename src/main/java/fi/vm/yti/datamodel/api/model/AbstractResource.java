package fi.vm.yti.datamodel.api.model;

import fi.vm.yti.datamodel.api.config.EndpointServices;
import fi.vm.yti.datamodel.api.utils.GraphManager;
import fi.vm.yti.datamodel.api.utils.JenaClient;
import fi.vm.yti.datamodel.api.utils.LDHelper;
import fi.vm.yti.datamodel.api.utils.ModelManager;
import org.apache.jena.iri.IRI;
import org.apache.jena.rdf.model.*;
import org.apache.jena.vocabulary.DCTerms;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;

import java.util.logging.Logger;

/**
 * Created by malonen on 13.12.2017.
 */
public class AbstractResource {

    protected Model graph;
    protected DataModel dataModel;
    protected String provUUID;
    protected IRI id;
    private static final Logger logger = Logger.getLogger(AbstractResource.class.getName());

    public AbstractResource() {}

    public AbstractResource(IRI graphIRI) {

        this.graph = GraphManager.getCoreGraph(graphIRI);
        this.id = graphIRI;

        try {

            Statement isDefinedBy = asGraph().getRequiredProperty(ResourceFactory.createResource(getId()), RDFS.isDefinedBy);
            Resource abstractResource = isDefinedBy.getSubject().asResource();
            Resource modelResource = isDefinedBy.getObject().asResource();

            logger.info(isDefinedBy.getSubject().toString()+" "+isDefinedBy.getObject().asResource().toString());
            logger.info(abstractResource.toString());

            this.dataModel = new DataModel(LDHelper.toIRI(modelResource.toString()));

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
                logger.warning(ex.getMessage());
                throw new IllegalArgumentException("Expected 1 provenance ID");
            }

        } catch(Exception ex)  {
            ex.printStackTrace();
            throw new IllegalArgumentException("Expected 1 resource defined by model");
        }

    }

    public void create() {
        JenaClient.putModelToCore(getId(), asGraph());
        GraphManager.insertNewGraphReferenceToModel(getId(), getModelId());
        Model exportModel = asGraphCopy();
        exportModel.add(exportModel.createResource(getModelId()), DCTerms.hasPart, exportModel.createResource(getId()));
        JenaClient.addModelToCore(getModelId()+"#ExportGraph", exportModel);
    }

    public void update() {
        Model oldModel = JenaClient.getModelFromCore(getId());
        Model exportModel = JenaClient.getModelFromCore(getModelId()+"#ExportGraph");

        exportModel = ModelManager.removeResourceStatements(oldModel, exportModel);
        exportModel.add(asGraph());

        JenaClient.putModelToCore(getModelId()+"#ExportGraph", exportModel);
        JenaClient.putModelToCore(getId(), asGraph());
    }

    public void updateWithNewId(IRI oldIdIRI) {
        Model oldModel = JenaClient.getModelFromCore(oldIdIRI.toString());
        Model exportModel = JenaClient.getModelFromCore(getModelId()+"#ExportGraph");

        exportModel = ModelManager.removeResourceStatements(oldModel, exportModel);
        exportModel.add(asGraph());
        JenaClient.putModelToCore(getModelId()+"#ExportGraph", exportModel);

        JenaClient.putModelToCore(getId(), asGraph());

        GraphManager.removeGraph(oldIdIRI);
        GraphManager.renameID(oldIdIRI,getIRI());
        GraphManager.updateReferencesInPositionGraph(getModelIRI(), oldIdIRI, getIRI());
    }

    public void delete() {
        Model exportModel = JenaClient.getModelFromCore(getModelId()+"#ExportGraph");
        exportModel = ModelManager.removeResourceStatements(asGraph(), exportModel);
        exportModel.remove(exportModel.createResource(getModelId()), DCTerms.hasPart, exportModel.createResource(getId()));
        JenaClient.putModelToCore(getModelId()+"#ExportGraph", exportModel);
        GraphManager.deleteGraphReferenceFromModel(getIRI(),getModelIRI());
        JenaClient.deleteModelFromCore(getId());
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
