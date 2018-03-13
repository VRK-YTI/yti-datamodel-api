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

import java.util.logging.Logger;

public class AbstractResource {

    protected Model graph;
    protected DataModel dataModel;
    protected String provUUID;
    protected IRI id;
    private static final Logger logger = Logger.getLogger(AbstractResource.class.getName());

    private final GraphManager graphManager;
    private final JenaClient jenaClient;
    private final ModelManager modelManager;

    public AbstractResource(GraphManager graphManager,
                            JenaClient jenaClient,
                            ModelManager modelManager) {

        this.graphManager = graphManager;
        this.jenaClient = jenaClient;
        this.modelManager = modelManager;
    }

    public AbstractResource(IRI graphIRI,
                            GraphManager graphManager,
                            ServiceDescriptionManager serviceDescriptionManager,
                            JenaClient jenaClient,
                            ModelManager modelManager) {

        this.graphManager = graphManager;
        this.jenaClient = jenaClient;
        this.modelManager = modelManager;

        this.graph = graphManager.getCoreGraph(graphIRI);
        this.id = graphIRI;

        try {

            Statement isDefinedBy = asGraph().getRequiredProperty(ResourceFactory.createResource(getId()), RDFS.isDefinedBy);
            Resource abstractResource = isDefinedBy.getSubject().asResource();
            Resource modelResource = isDefinedBy.getObject().asResource();

            logger.info(isDefinedBy.getSubject().toString()+" "+isDefinedBy.getObject().asResource().toString());
            logger.info(abstractResource.toString());

            this.dataModel = new DataModel(LDHelper.toIRI(modelResource.toString()), graphManager, serviceDescriptionManager, jenaClient);

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
        jenaClient.putModelToCore(getId(), asGraph());
        graphManager.insertNewGraphReferenceToModel(getId(), getModelId());
        Model exportModel = asGraphCopy();
        exportModel.add(exportModel.createResource(getModelId()), DCTerms.hasPart, exportModel.createResource(getId()));
        jenaClient.addModelToCore(getModelId()+"#ExportGraph", exportModel);
    }

    public void update() {
        Model oldModel = jenaClient.getModelFromCore(getId());
        Model exportModel = jenaClient.getModelFromCore(getModelId()+"#ExportGraph");

        exportModel = modelManager.removeResourceStatements(oldModel, exportModel);
        exportModel.add(asGraph());

        jenaClient.putModelToCore(getModelId()+"#ExportGraph", exportModel);
        jenaClient.putModelToCore(getId(), asGraph());
    }

    public void updateWithNewId(IRI oldIdIRI) {
        Model oldModel = jenaClient.getModelFromCore(oldIdIRI.toString());
        Model exportModel = jenaClient.getModelFromCore(getModelId()+"#ExportGraph");

        exportModel = modelManager.removeResourceStatements(oldModel, exportModel);
        exportModel.add(asGraph());
        jenaClient.putModelToCore(getModelId()+"#ExportGraph", exportModel);

        jenaClient.putModelToCore(getId(), asGraph());

        graphManager.removeGraph(oldIdIRI);
        graphManager.renameID(oldIdIRI,getIRI());
        graphManager.updateReferencesInPositionGraph(getModelIRI(), oldIdIRI, getIRI());
    }

    public void delete() {
        Model exportModel = jenaClient.getModelFromCore(getModelId()+"#ExportGraph");
        exportModel = modelManager.removeResourceStatements(asGraph(), exportModel);
        exportModel.remove(exportModel.createResource(getModelId()), DCTerms.hasPart, exportModel.createResource(getId()));
        jenaClient.putModelToCore(getModelId()+"#ExportGraph", exportModel);
        graphManager.deleteGraphReferenceFromModel(getIRI(),getModelIRI());
        jenaClient.deleteModelFromCore(getId());
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
