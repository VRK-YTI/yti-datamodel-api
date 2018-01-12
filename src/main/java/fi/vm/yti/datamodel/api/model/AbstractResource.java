package fi.vm.yti.datamodel.api.model;

import fi.vm.yti.datamodel.api.config.EndpointServices;
import fi.vm.yti.datamodel.api.utils.GraphManager;
import fi.vm.yti.datamodel.api.utils.ProvenanceManager;
import org.apache.jena.iri.IRI;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.vocabulary.DCTerms;
import org.apache.jena.web.DatasetAdapter;
import org.apache.jena.web.DatasetGraphAccessorHTTP;
import java.util.logging.Logger;

/**
 * Created by malonen on 13.12.2017.
 */
public class AbstractResource {

    EndpointServices services = new EndpointServices();
    protected Model graph;
    protected DataModel dataModel;
    protected String provUUID;
    protected IRI id;
    private static final Logger logger = Logger.getLogger(AbstractResource.class.getName());

    public void create() {
        DatasetGraphAccessorHTTP accessor = new DatasetGraphAccessorHTTP(services.getCoreReadWriteAddress());
        DatasetAdapter adapter = new DatasetAdapter(accessor);
        adapter.putModel(getId(), asGraph());
        GraphManager.insertNewGraphReferenceToModel(getId(), getModelId());
        Model exportModel = asGraph();
        exportModel.add(exportModel.createResource(getModelId()), DCTerms.hasPart, exportModel.createResource(getId()));
        adapter.add(getModelId()+"#ExportGraph", exportModel);
    }

    public void update() {
        DatasetGraphAccessorHTTP accessor = new DatasetGraphAccessorHTTP(services.getCoreReadWriteAddress());
        DatasetAdapter adapter = new DatasetAdapter(accessor);
        adapter.putModel(getId(), asGraph());
        GraphManager.insertNewGraphReferenceToModel(getId(), getModelId());
        Model oldModel = adapter.getModel(getId());
        Model exportModel = adapter.getModel(getModelId()+"#ExportGraph");
        exportModel.remove(oldModel);
        exportModel.add(asGraph());
        exportModel.add(exportModel.createResource(getModelId()), DCTerms.hasPart, exportModel.createResource(getId()));
        adapter.putModel(getModelId()+"#ExportGraph", exportModel);
     }

     public void updateWithNewId(IRI oldIdIRI) {
         DatasetGraphAccessorHTTP accessor = new DatasetGraphAccessorHTTP(services.getCoreReadWriteAddress());
         DatasetAdapter adapter = new DatasetAdapter(accessor);
         adapter.putModel(getId(), asGraph());

         Model oldModel = adapter.getModel(oldIdIRI.toString());
         Model exportModel = adapter.getModel(getModelId()+"#ExportGraph");
         exportModel.remove(oldModel);
         exportModel.add(asGraph());

         GraphManager.removeGraph(oldIdIRI);
         GraphManager.renameID(oldIdIRI,getIRI());
         GraphManager.updateReferencesInPositionGraph(getModelIRI(), oldIdIRI, getIRI());

     }

    public void delete() {
        DatasetGraphAccessorHTTP accessor = new DatasetGraphAccessorHTTP(services.getCoreReadWriteAddress());
        DatasetAdapter adapter = new DatasetAdapter(accessor);
        Model exportModel = adapter.getModel(getModelId()+"#ExportGraph");
        exportModel.remove(asGraph());
        exportModel.remove(exportModel.createResource(getModelId()), DCTerms.hasPart, exportModel.createResource(getId()));
        adapter.putModel(getModelId()+"#ExportGraph", exportModel);
        GraphManager.deleteGraphReferenceFromModel(getIRI(),getModelIRI());
        adapter.deleteModel(getId());
    }

    public Model asGraph(){
        return this.graph;
    }
    public String getId() { return this.id.toString();}
    public String getModelId() { return this.dataModel.getId(); }
    public IRI getModelIRI() { return this.dataModel.getIRI(); }
    public IRI getIRI() { return this.id; }

}
