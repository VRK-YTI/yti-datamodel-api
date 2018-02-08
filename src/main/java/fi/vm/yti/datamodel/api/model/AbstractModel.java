package fi.vm.yti.datamodel.api.model;

import fi.vm.yti.datamodel.api.config.EndpointServices;
import fi.vm.yti.datamodel.api.utils.*;
import org.apache.jena.iri.IRI;
import org.apache.jena.rdf.model.*;
import org.apache.jena.vocabulary.DCTerms;
import org.apache.jena.vocabulary.OWL;
import org.apache.jena.vocabulary.RDF;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.logging.Logger;

/**
 * Created by malonen on 17.11.2017.
 */
public abstract class AbstractModel {
    EndpointServices services = new EndpointServices();
    protected Model graph;
    protected String provUUID;
    protected IRI id;
    protected List<UUID> modelOrganizations;
    protected List<String> modelServiceCategories;

    private static final Logger logger = Logger.getLogger(AbstractModel.class.getName());

    public AbstractModel() {}

    public AbstractModel(IRI graphIRI) {
        this.graph = GraphManager.getCoreGraph(graphIRI);
        this.id = graphIRI;

        List<Resource> vocabList = this.graph.listSubjectsWithProperty(OWL.versionInfo).toList();

        if(!(vocabList.size()==1)) {
            logger.warning("Expected 1 versioned resource, got "+vocabList.size());
            throw new IllegalArgumentException("Expected 1 versioned resource");
        }

        Resource modelResource = vocabList.get(0);

        if(!modelResource.hasProperty(RDF.type, OWL.Ontology)) {
            logger.warning("Expected "+getId()+" type as owl:Ontology");
            throw new IllegalArgumentException("Expected model resource");
        }

        NodeIterator orgList = this.graph.listObjectsOfProperty(DCTerms.contributor);
        this.modelOrganizations = new ArrayList<>();
        while(orgList.hasNext()) {
            RDFNode orgRes = orgList.next();
            this.modelOrganizations.add(UUID.fromString(orgRes.asResource().getURI().replaceFirst("urn:uuid:","")));
        }

        List<Statement> provIdList = modelResource.listProperties(DCTerms.identifier).toList();
        if(provIdList == null || provIdList.size()==0 || provIdList.size()>1) {
            logger.warning("Expected only 1 provenance ID, got "+provIdList.size());
            throw new IllegalArgumentException("Expected only 1 provenance ID, got "+provIdList.size());
        } else {
            this.provUUID = provIdList.get(0).getLiteral().toString();
        }
    }

    public AbstractModel(Model graph){
        Model orgModel = RHPOrganizationManager.getOrganizationModel();
        this.graph = graph;

        List<Resource> vocabList = this.graph.listSubjectsWithProperty(OWL.versionInfo).toList();

        if(!(vocabList.size()==1)) {
            logger.warning("Expected 1 versioned resource, got "+vocabList.size());
            throw new IllegalArgumentException("Expected 1 versioned resource");
        }

        Resource modelResource = vocabList.get(0);

        if(!modelResource.hasProperty(RDF.type, OWL.Ontology)) {
            logger.warning("Expected "+getId()+" type as owl:Ontology");
            throw new IllegalArgumentException("Expected model resource");
        }

        // TODO: Validate that namespace is same as in model id
        this.id =  LDHelper.toIRI(modelResource.getURI());

        NodeIterator orgList = this.graph.listObjectsOfProperty(DCTerms.contributor);

        this.modelOrganizations = new ArrayList<>();

        if(!orgList.hasNext()) {
            logger.warning("Expected at least 1 organization");
            throw new IllegalArgumentException("Expected at least 1 organization");
        }

        while(orgList.hasNext()) {
            RDFNode orgRes = orgList.next();
            if(!orgModel.containsResource(orgRes)) {
                logger.warning("Organization does not exists!");
                throw new IllegalArgumentException("Organization does not exist!");
            }
            String orgId = orgRes.asResource().getURI().replaceFirst("urn:uuid:","");
            logger.info("New model is part of "+orgId);
            this.modelOrganizations.add(UUID.fromString(orgId));
        }

        this.provUUID = "urn:uuid:"+UUID.randomUUID().toString();
        modelResource.removeAll(DCTerms.identifier);
        modelResource.addProperty(DCTerms.identifier,ResourceFactory.createPlainLiteral(provUUID));

    }

    public void create() {
        logger.info("Creating model "+getId());
        JenaClient.putModelToCore(getId(), asGraph());

        /* TODO: Test with RDFConnection
        Model nsModel = ModelFactory.createDefaultModel();
        nsModel.setNsPrefixes(asGraph().getNsPrefixMap());
        JenaClient.putModelToCore(getId()+"#NamespaceGraph", nsModel);
        */

        JenaClient.putModelToCore(getId()+"#ExportGraph", asGraph());
    }

    public void update() {
        modifyDatetime();
        Model oldModel = JenaClient.getModelFromCore(getId());
        Model exportModel = JenaClient.getModelFromCore(getId()+"#ExportGraph");

        // OMG: Model.remove() doesnt remove RDFLists
        Statement languageStatement = exportModel.getRequiredProperty(ResourceFactory.createResource(getId()), DCTerms.language);
        RDFList languageList = languageStatement.getObject().as(RDFList.class);
        languageList.removeList();
        languageStatement.remove();

        /* TODO: Test with RDFConnection
        Model nsModel = ModelFactory.createDefaultModel();
        nsModel.setNsPrefixes(asGraph().getNsPrefixMap());
        JenaClient.putModelToCore(getId()+"#NamespaceGraph", nsModel);*/

        exportModel.remove(oldModel);
        exportModel.add(asGraph());
        JenaClient.putModelToCore(getId()+"#ExportGraph", exportModel);
        JenaClient.putModelToCore(getId(), asGraph());
    }

    public void delete() {
        ServiceDescriptionManager.deleteGraphDescription(getId());
        GraphManager.removeModel(getIRI());
    }

    public void modifyDatetime() {
        LDHelper.rewriteLiteral(this.graph, ResourceFactory.createResource(getId()), DCTerms.modified, LDHelper.getDateTimeLiteral());
    }

    public Model asGraph(){
        return this.graph;
    }
    public Model asGraphCopy() { return ModelFactory.createDefaultModel().add(this.graph); }
    public String getId() { return this.id.toString();}
    public IRI getIRI() { return this.id; }
    public String getProvUUID() { return this.provUUID; }
    public List<UUID> getOrganizations() { return this.modelOrganizations; }



}
