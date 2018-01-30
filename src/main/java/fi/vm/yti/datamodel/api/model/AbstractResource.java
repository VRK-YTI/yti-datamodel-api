package fi.vm.yti.datamodel.api.model;

import fi.vm.yti.datamodel.api.config.EndpointServices;
import fi.vm.yti.datamodel.api.utils.GraphManager;
import fi.vm.yti.datamodel.api.utils.JenaClient;
import fi.vm.yti.datamodel.api.utils.ModelManager;
import org.apache.jena.iri.IRI;
import org.apache.jena.rdf.model.*;
import org.apache.jena.vocabulary.DCTerms;
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

    public void create() {
        JenaClient.putModelToCore(getId(), asGraph());
        GraphManager.insertNewGraphReferenceToModel(getId(), getModelId());
        Model exportModel = asGraphCopy();
        exportModel.add(exportModel.createResource(getModelId()), DCTerms.hasPart, exportModel.createResource(getId()));
        JenaClient.addModelToCore(getModelId()+"#ExportGraph", exportModel);
    }

    public void update() {
        JenaClient.putModelToCore(getId(), asGraph());
        GraphManager.insertNewGraphReferenceToModel(getId(), getModelId());
        Model oldModel = JenaClient.getModelFromCore(getId());
        Model exportModel = JenaClient.getModelFromCore(getModelId()+"#ExportGraph");
        exportModel = ModelManager.removeListStatements(oldModel, exportModel);
        exportModel.remove(oldModel);
        exportModel.add(asGraph());
        exportModel.add(exportModel.createResource(getModelId()), DCTerms.hasPart, exportModel.createResource(getId()));
        JenaClient.putModelToCore(getModelId()+"#ExportGraph", exportModel);
    }

    public void updateWithNewId(IRI oldIdIRI) {
        JenaClient.putModelToCore(getId(), asGraph());

        Model oldModel = JenaClient.getModelFromCore(oldIdIRI.toString());
        Model exportModel = JenaClient.getModelFromCore(getModelId()+"#ExportGraph");
        exportModel.remove(oldModel);
        exportModel.add(asGraph());

        GraphManager.removeGraph(oldIdIRI);
        GraphManager.renameID(oldIdIRI,getIRI());
        GraphManager.updateReferencesInPositionGraph(getModelIRI(), oldIdIRI, getIRI());
    }

    public void delete() {
        Model exportModel = JenaClient.getModelFromCore(getModelId()+"#ExportGraph");
        exportModel = ModelManager.removeListStatements(asGraph(), exportModel);
        exportModel.remove(asGraph());
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
