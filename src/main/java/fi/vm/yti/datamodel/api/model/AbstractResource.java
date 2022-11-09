package fi.vm.yti.datamodel.api.model;

import org.apache.jena.iri.IRI;
import org.apache.jena.rdf.model.*;
import org.apache.jena.vocabulary.DCTerms;
import org.apache.jena.vocabulary.OWL;

import fi.vm.yti.datamodel.api.utils.LDHelper;

public class AbstractResource {

    protected Model graph;
    protected DataModel dataModel;
    protected IRI id;

    public Model asGraph() {
        return this.graph;
    }

    public Model asGraphCopy() {
        return ModelFactory.createDefaultModel().add(this.graph);
    }

    public String getId() {
        return this.id.toString();
    }

    public String getModelId() {
        return this.dataModel.getId();
    }

    public String getStatus() {
        return this.graph.getRequiredProperty(ResourceFactory.createResource(this.getId()), OWL.versionInfo).getString();
    }

    public String getStatusModified() {
        try {
            return this.graph.getProperty(ResourceFactory.createResource(this.getId()), LDHelper.curieToProperty("iow:statusModified")).getString();
        } catch(NullPointerException ex) {
            // This catch fixes legacy data without statusModified fields.
            return null;
        }
    }

    public void setStatusModified() {
        LDHelper.rewriteLiteral(this.graph, ResourceFactory.createResource(this.getId()), LDHelper.curieToProperty("iow:statusModified"), LDHelper.getDateTimeLiteral());
    }

    public String getModified() {
        return this.graph.getRequiredProperty(ResourceFactory.createResource(this.getId()), DCTerms.modified).getString();
    }

    public String getCreated() {
        return this.graph.getRequiredProperty(ResourceFactory.createResource(this.getId()), DCTerms.created).getString();
    }

    public IRI getModelIRI() {
        return this.dataModel.getIRI();
    }

    public IRI getIRI() {
        return this.id;
    }

}
