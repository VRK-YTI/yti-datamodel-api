package fi.vm.yti.datamodel.api.model;

import fi.vm.yti.datamodel.api.config.EndpointServices;
import fi.vm.yti.datamodel.api.utils.GraphManager;
import fi.vm.yti.datamodel.api.utils.LDHelper;
import fi.vm.yti.datamodel.api.utils.RHPOrganizationManager;
import org.apache.jena.iri.IRI;
import org.apache.jena.rdf.model.*;
import org.apache.jena.vocabulary.DCTerms;
import org.apache.jena.vocabulary.OWL;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;
import org.apache.jena.web.DatasetAdapter;
import org.apache.jena.web.DatasetGraphAccessorHTTP;

import java.util.List;
import java.util.UUID;
import java.util.logging.Logger;

/**
 * Created by malonen on 17.11.2017.
 */
public abstract class AbstractClass extends AbstractResource {

        protected String provUUID;
        private static final Logger logger = Logger.getLogger(AbstractClass.class.getName());

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

            List<Statement> provIdList = classResource.listProperties(DCTerms.identifier).toList();
            if(provIdList == null || provIdList.size()==0 || provIdList.size()>1) {
                throw new IllegalArgumentException("Expected only 1 provenance ID, got "+provIdList.size());
            } else {
                this.provUUID = provIdList.get(0).getResource().getURI();
            }
        }

        public AbstractClass(Model graph){
            this.graph = graph;

            List<RDFNode> modelList = this.graph.listObjectsOfProperty(RDFS.isDefinedBy).toList();
            if(modelList==null || modelList.size()!=1) {
                throw new IllegalArgumentException("Expected 1 class (isDefinedBy)");
            }

            this.dataModel = new DataModel(LDHelper.toIRI(modelList.get(0).asResource().getURI()));

            List<Resource> classList = this.graph.listSubjectsWithProperty(RDF.type, ResourceFactory.createResource(LDHelper.curieToURI("rdfs:Class"))).toList();
            if(classList == null || classList.size()!=1) {
                throw new IllegalArgumentException("Expected 1 class in graph!");
            }

            Resource classResource = classList.get(0);
            this.id = LDHelper.toIRI(classResource.getURI());

            if(!this.id.toString().startsWith(getModelId())) {
                throw new IllegalArgumentException("Class ID should start with model ID!");
            }

            this.provUUID = "urn:uuid:"+UUID.randomUUID().toString();
            classResource.removeAll(DCTerms.identifier);
            classResource.addProperty(DCTerms.identifier,ResourceFactory.createResource("urn:uuid:"+provUUID));

        }

        public String getProvUUID() { return this.provUUID; }
        public List<UUID> getOrganizations() { return this.dataModel.getOrganizations(); }

}
