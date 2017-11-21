package fi.vm.yti.datamodel.api.model;

import fi.vm.yti.datamodel.api.config.EndpointServices;
import fi.vm.yti.datamodel.api.utils.GraphManager;
import fi.vm.yti.datamodel.api.utils.LDHelper;
import fi.vm.yti.datamodel.api.utils.RHPOrganizationManager;
import org.apache.jena.iri.IRI;
import org.apache.jena.rdf.model.*;
import org.apache.jena.vocabulary.DCTerms;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;
import java.util.List;
import java.util.UUID;

/**
 * Created by malonen on 17.11.2017.
 */
public abstract class AbstractClass {

        EndpointServices services = new EndpointServices();
        protected Model graph;
        protected DataModel dataModel;
        protected String provUUID;
        protected IRI id;
        protected List<UUID> modelOrganizations;
        protected List<String> modelServiceCategories;

        public AbstractClass() {}

        public AbstractClass(IRI graphIRI) {
            this.graph = GraphManager.getCoreGraph(graphIRI);

            List<Resource> modelList = this.graph.listResourcesWithProperty(RDFS.isDefinedBy).toList();
            if(modelList==null || modelList.size()!=1) {
                throw new IllegalArgumentException("Expected 1 model (isDefinedBy)");
            }

            this.dataModel = new DataModel(modelList.get(0).getURI());

            List<Resource> classList = this.graph.listSubjectsWithProperty(RDF.type, ResourceFactory.createResource(LDHelper.curieToURI("rdfs:Class"))).toList();
            if(classList == null || classList.size()!=1) {
                throw new IllegalArgumentException("Expected 1 class in graph!");
            }

            Resource classResource = classList.get(0);
            this.id = LDHelper.toIRI(classResource.getURI());

           this.modelOrganizations = dataModel.getOrganizations();

            List<Statement> provIdList = classResource.listProperties(DCTerms.identifier).toList();
            if(provIdList == null || provIdList.size()==0 || provIdList.size()>1) {
                throw new IllegalArgumentException("Expected only 1 provenance ID, got "+provIdList.size());
            } else {
                this.provUUID = provIdList.get(0).getResource().getURI();
            }
        }

        public AbstractClass(Model graph){
            Model orgModel = RHPOrganizationManager.getOrganizationModel();
            this.graph = graph;

            List<Resource> modelList = this.graph.listResourcesWithProperty(RDFS.isDefinedBy).toList();
            if(modelList==null || modelList.size()!=1) {
                throw new IllegalArgumentException("Expected 1 model (isDefinedBy)");
            }

            this.dataModel = new DataModel(modelList.get(0).getURI());

            List<Resource> classList = this.graph.listSubjectsWithProperty(RDF.type, ResourceFactory.createResource(LDHelper.curieToURI("rdfs:Class"))).toList();
            if(classList == null || classList.size()!=1) {
                throw new IllegalArgumentException("Expected 1 class in graph!");
            }

            Resource classResource = classList.get(0);
            // TODO: Validate that namespace is same as in class id
            this.id = LDHelper.toIRI(classResource.getURI());

            this.provUUID = "urn:uuid:"+UUID.randomUUID().toString();
            classResource.removeAll(DCTerms.identifier);
            classResource.addProperty(DCTerms.identifier,ResourceFactory.createResource("urn:uuid:"+provUUID));

        }

        public Model asGraph(){
            return this.graph;
        }
        public String getId() { return this.id.toString();}
        public IRI getIRI() { return this.id; }
        public String getProvUUID() { return this.provUUID; }
        public List<UUID> getOrganizations() { return this.modelOrganizations; }

}
