package fi.vm.yti.datamodel.api.model;

import fi.vm.yti.datamodel.api.service.GraphManager;
import fi.vm.yti.datamodel.api.service.RHPOrganizationManager;
import fi.vm.yti.datamodel.api.utils.*;

import org.apache.jena.iri.IRI;
import org.apache.jena.rdf.model.*;
import org.apache.jena.vocabulary.DCTerms;
import org.apache.jena.vocabulary.OWL;
import org.apache.jena.vocabulary.RDF;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.apache.jena.vocabulary.RDFS;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractModel extends AbstractResource {

    protected String provUUID;
    protected List<UUID> modelOrganizations;

    private static final Logger logger = LoggerFactory.getLogger(AbstractModel.class.getName());

    private final GraphManager graphManager;

    public AbstractModel(GraphManager graphManager) {
        this.graphManager = graphManager;
    }

    public AbstractModel(IRI graphIRI,
                         GraphManager graphManager) {

        this.graphManager = graphManager;
        this.graph = graphManager.getCoreGraph(graphIRI);

        if (this.graph == null) {
            throw new IllegalArgumentException("GRAPH not found");
        }

        if (this.graph.size() < 1) {
            throw new IllegalArgumentException("GRAPH is empty");
        }

        this.id = graphIRI;

        Resource modelResource = this.graph.getResource(graphIRI.toString());

        if (!modelResource.hasProperty(RDF.type, OWL.Ontology)) {
            logger.warn("Expected " + getId() + " type as owl:Ontology");
            throw new IllegalArgumentException("Expected model resource");
        }

        NodeIterator orgList = this.graph.listObjectsOfProperty(DCTerms.contributor);
        this.modelOrganizations = new ArrayList<>();
        while (orgList.hasNext()) {
            RDFNode orgRes = orgList.next();
            this.modelOrganizations.add(UUID.fromString(orgRes.asResource().getURI().replaceFirst("urn:uuid:", "")));
        }

        List<Statement> provIdList = modelResource.listProperties(DCTerms.identifier).toList();
        if (provIdList == null) {
            logger.warn("Expected only 1 provenance ID, got null");
            throw new IllegalArgumentException("Expected only 1 provenance ID, got null");
        } else if (provIdList.size() == 0 || provIdList.size() > 1) {
            logger.warn("Expected only 1 provenance ID, got " + provIdList.size());
            throw new IllegalArgumentException("Expected only 1 provenance ID, got " + provIdList.size());
        } else {
            this.provUUID = provIdList.get(0).getLiteral().toString();
        }
    }

    public AbstractModel(Model graph,
                         GraphManager graphManager,
                         RHPOrganizationManager rhpOrganizationManager) {

        this.graphManager = graphManager;
        Model orgModel = rhpOrganizationManager.getOrganizationModel();
        this.graph = graph;

        List<Resource> vocabList = this.graph.listSubjectsWithProperty(DCTerms.contributor).toList();

        if (!(vocabList.size() == 1)) {
            logger.warn("Expected 1 resource with contributors, got " + vocabList.size());
            throw new IllegalArgumentException("Expected 1 resource");
        }

        Resource modelResource = vocabList.get(0);

        if (!modelResource.hasProperty(RDF.type, OWL.Ontology)) {
            logger.warn("Expected " + getId() + " type as owl:Ontology");
            throw new IllegalArgumentException("Expected model resource");
        }

        // TODO: Validate that namespace is same as in model id
        this.id = LDHelper.toIRI(modelResource.getURI());

        NodeIterator orgList = this.graph.listObjectsOfProperty(DCTerms.contributor);

        this.modelOrganizations = new ArrayList<>();

        if (!orgList.hasNext()) {
            logger.warn("Expected at least 1 organization");
            throw new IllegalArgumentException("Expected at least 1 organization");
        }

        while (orgList.hasNext()) {
            RDFNode orgRes = orgList.next();
            if (!orgModel.containsResource(orgRes)) {
                logger.warn("Organization does not exists!");
                throw new IllegalArgumentException("Organization does not exist!");
            }
            String orgId = orgRes.asResource().getURI().replaceFirst("urn:uuid:", "");
            logger.info("New model is part of " + orgId);
            this.modelOrganizations.add(UUID.fromString(orgId));
        }

        this.provUUID = "urn:uuid:" + UUID.randomUUID().toString();
        modelResource.removeAll(DCTerms.identifier);
        modelResource.addProperty(DCTerms.identifier, ResourceFactory.createPlainLiteral(provUUID));

    }

    public Model asGraph() {
        return this.graph;
    }

    public Model asGraphCopy() {
        return ModelFactory.createDefaultModel().add(this.graph);
    }

    public String getId() {
        return this.id.toString();
    }

    public IRI getIRI() {
        return this.id;
    }

    public String getUseContext() {
        return this.graph.getRequiredProperty(ResourceFactory.createResource(this.getId()), LDHelper.curieToProperty("iow:useContext")).getString();
    }

    public String getType() {
        return this.graph.contains(ResourceFactory.createResource(this.getId()), RDF.type, LDHelper.curieToResource("dcap:DCAP")) ? "profile" : "library";
    }

    public Map<String, String> getLabel() {
        return LDHelper.RDFNodeListToMap(this.graph.listObjectsOfProperty(ResourceFactory.createResource(this.getId()), RDFS.label).toList());
    }

    public Map<String, String> getComment() {
        return LDHelper.RDFNodeListToMap(this.graph.listObjectsOfProperty(ResourceFactory.createResource(this.getId()), RDFS.comment).toList());
    }

    public List<String> getDomains() {
        return LDHelper.RDFNodeListToStringList(this.graph.listObjectsOfProperty(ResourceFactory.createResource(this.getId()), DCTerms.isPartOf).toList(), DCTerms.identifier);
    }

    public String getProvUUID() {
        return this.provUUID;
    }

    public List<UUID> getOrganizations() {
        return this.modelOrganizations;
    }

}
