package fi.vm.yti.datamodel.api.model;

import fi.vm.yti.datamodel.api.config.EndpointServices;
import fi.vm.yti.datamodel.api.utils.GraphManager;
import fi.vm.yti.datamodel.api.utils.LDHelper;
import org.apache.jena.iri.IRI;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.vocabulary.DCTerms;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;

import java.util.List;
import java.util.UUID;

/**
 * Created by malonen on 21.11.2017.
 */
public abstract class AbstractShape extends AbstractResource {

    EndpointServices services = new EndpointServices();
    protected Model graph;
    protected DataModel dataModel;
    protected String provUUID;
    protected IRI id;
    protected List<UUID> modelOrganizations;
    protected List<String> modelServiceCategories;

    public AbstractShape() {}

    public AbstractShape(IRI graphIRI) {
        super(graphIRI);
    }

    public AbstractShape(Model graph){

        this.graph = graph;

        List<Resource> modelList = this.graph.listResourcesWithProperty(RDFS.isDefinedBy).toList();
        if(modelList==null || modelList.size()!=1) {
            throw new IllegalArgumentException("Expected 1 model (isDefinedBy)");
        }

        this.dataModel = new DataModel(modelList.get(0).getURI());
        this.modelOrganizations = dataModel.getOrganizations();

        List<Resource> scopeList = this.graph.listResourcesWithProperty(LDHelper.curieToProperty("sh:scopeClass")).toList();
        if(scopeList==null || scopeList.size()!=1) {
            throw new IllegalArgumentException("Expected 1 Reusable Class (scopeClass)");
        }

        List<Resource> classList = this.graph.listSubjectsWithProperty(RDF.type, ResourceFactory.createResource(LDHelper.curieToURI("sh:Shape"))).toList();
        if(classList == null || classList.size()!=1) {
            throw new IllegalArgumentException("Expected 1 class in graph!");
        }

        Resource shapeResource = classList.get(0);
        // TODO: Validate that namespace is same as in class id
        this.id = LDHelper.toIRI(shapeResource.getURI());

        this.provUUID = "urn:uuid:"+UUID.randomUUID().toString();
        shapeResource.removeAll(DCTerms.identifier);
        shapeResource.addProperty(DCTerms.identifier,ResourceFactory.createResource(provUUID));

    }

    public Model asGraph(){
        return this.graph;
    }
    public String getId() { return this.id.toString();}
    public IRI getIRI() { return this.id; }
    public String getProvUUID() { return this.provUUID; }
    public List<UUID> getOrganizations() { return this.modelOrganizations; }


}
