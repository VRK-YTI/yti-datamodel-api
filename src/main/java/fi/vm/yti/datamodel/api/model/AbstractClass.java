package fi.vm.yti.datamodel.api.model;

import fi.vm.yti.datamodel.api.service.GraphManager;
import fi.vm.yti.datamodel.api.utils.LDHelper;

import org.apache.jena.iri.IRI;
import org.apache.jena.rdf.model.*;
import org.apache.jena.vocabulary.DCTerms;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.topbraid.shacl.vocabulary.SH;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public abstract class AbstractClass extends AbstractResource {

    protected String provUUID;
    protected GraphManager graphManager;
    private static final Logger logger = LoggerFactory.getLogger(AbstractClass.class.getName());

    public AbstractClass() {
    }

    public AbstractClass(IRI graphIRI,
                         GraphManager graphManager) {

        this.graphManager = graphManager;

        this.graph = graphManager.getCoreGraph(graphIRI);
        this.id = graphIRI;

        try {

            Statement isDefinedBy = asGraph().getRequiredProperty(ResourceFactory.createResource(getId()), RDFS.isDefinedBy);
            Resource abstractResource = isDefinedBy.getSubject().asResource();
            Resource modelResource = isDefinedBy.getObject().asResource();

            if (!(asGraph().contains(ResourceFactory.createResource(getId()), RDF.type, RDFS.Class) || asGraph().contains(ResourceFactory.createResource(getId()), RDF.type, SH.NodeShape))) {
                throw new IllegalArgumentException("Expected rdfs:Class or sh:NodeShape type");
            }

            logger.info(isDefinedBy.getSubject().toString() + " " + isDefinedBy.getObject().asResource().toString());
            logger.info(abstractResource.toString());

            this.dataModel = new DataModel(LDHelper.toIRI(modelResource.toString()), graphManager);

            if (!this.id.toString().startsWith(getModelId())) {
                throw new IllegalArgumentException("Resource ID should start with model ID!");
            }

            StmtIterator props = abstractResource.listProperties();
            while (props.hasNext()) {
                logger.info(props.next().getPredicate().getURI());
            }

            try {
                this.provUUID = abstractResource.getRequiredProperty(DCTerms.identifier).getLiteral().toString();
            } catch (Exception ex) {
                logger.warn(ex.getMessage(), ex);
                logger.warn(ex.getMessage());
                throw new IllegalArgumentException("Expected 1 provenance ID");
            }

        } catch (Exception ex) {
            logger.warn(ex.getMessage(), ex);
            throw new IllegalArgumentException("Expected 1 resource defined by model");
        }

    }

    public AbstractClass(Model graph,
                         GraphManager graphManager) {

        this.graph = graph;
        this.graphManager = graphManager;

        try {

            ResIterator subjects = asGraph().listSubjectsWithProperty(RDF.type);

            if (!subjects.hasNext()) {
                throw new IllegalArgumentException("Expected at least 1 typed resource");
            }

            Resource classResource = null;

            while (subjects.hasNext()) {
                Resource res = subjects.next();
                if (res.hasProperty(RDF.type, RDFS.Class) || res.hasProperty(RDF.type, SH.NodeShape)) {
                    if (classResource != null) {
                        throw new IllegalArgumentException("Multiple class resources");
                    } else {
                        classResource = res;
                    }
                }
            }

            if (classResource == null) {
                throw new IllegalArgumentException("Expected rdfs:Class or sh:NodeShape");
            }

            // TODO: Check that doesnt contain multiple class resources
            Statement isDefinedBy = classResource.getRequiredProperty(RDFS.isDefinedBy);
            Resource modelResource = isDefinedBy.getObject().asResource();

            this.dataModel = new DataModel(LDHelper.toIRI(modelResource.toString()), graphManager);
            this.id = LDHelper.toIRI(classResource.toString());

            if (!this.id.toString().startsWith(getModelId())) {
                throw new IllegalArgumentException("Class ID should start with model ID!");
            }

            this.provUUID = "urn:uuid:" + UUID.randomUUID().toString();
            classResource.removeAll(DCTerms.identifier);
            classResource.addProperty(DCTerms.identifier, ResourceFactory.createPlainLiteral(provUUID));

        } catch (Exception ex) {
            logger.warn("Error: ", ex);
            throw new IllegalArgumentException("Expected 1 class (isDefinedBy)");
        }

    }

    public String getProvUUID() {
        return this.provUUID;
    }

    public List<UUID> getOrganizations() {
        return this.dataModel.getOrganizations();
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

    public String getModelId() {
        return this.dataModel.getId();
    }

    public Map<String, String> getLabel() {
        return LDHelper.RDFNodeListToMap(this.graph.listObjectsOfProperty(ResourceFactory.createResource(this.getId()), SH.name).toList());
    }

    public Map<String, String> getComment() {
        return LDHelper.RDFNodeListToMap(this.graph.listObjectsOfProperty(ResourceFactory.createResource(this.getId()), SH.description).toList());
    }

    public String getType() {
        return this.graph.contains(ResourceFactory.createResource(this.getId()), RDF.type, RDFS.Class) ? "rdfs:Class" : "sh:NodeShape";
    }

    public IRI getModelIRI() {
        return this.dataModel.getIRI();
    }

    public IRI getIRI() {
        return this.id;
    }

}
