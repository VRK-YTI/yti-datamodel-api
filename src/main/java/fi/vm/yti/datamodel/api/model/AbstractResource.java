package fi.vm.yti.datamodel.api.model;

import org.apache.jena.iri.IRI;
import org.apache.jena.rdf.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AbstractResource {

    protected Model graph;
    protected DataModel dataModel;
    protected IRI id;
    private static final Logger logger = LoggerFactory.getLogger(AbstractResource.class.getName());

    public Model asGraph(){
        return this.graph;
    }
    public Model asGraphCopy() { return ModelFactory.createDefaultModel().add(this.graph); }
    public String getId() { return this.id.toString();}
    public String getModelId() { return this.dataModel.getId(); }
    public IRI getModelIRI() { return this.dataModel.getIRI(); }
    public IRI getIRI() { return this.id; }

}
