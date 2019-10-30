package fi.vm.yti.datamodel.api.model;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.apache.jena.iri.IRI;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.ResIterator;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;
import org.apache.jena.vocabulary.DCTerms;
import org.apache.jena.vocabulary.OWL;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fi.vm.yti.datamodel.api.service.GraphManager;
import fi.vm.yti.datamodel.api.utils.LDHelper;

public abstract class AbstractPredicate extends AbstractResource {

    private static final Logger logger = LoggerFactory.getLogger(AbstractPredicate.class.getName());
    protected String provUUID;
    protected GraphManager graphManager;

    public AbstractPredicate() {
    }

    public AbstractPredicate(IRI graphIRI,
                             GraphManager graphManager) {

        this.graphManager = graphManager;
        this.graph = graphManager.getCoreGraph(graphIRI);
        this.id = graphIRI;

        try {

            Statement isDefinedBy = asGraph().getRequiredProperty(ResourceFactory.createResource(getId()), RDFS.isDefinedBy);
            Resource abstractResource = isDefinedBy.getSubject().asResource();
            Resource modelResource = isDefinedBy.getObject().asResource();

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

    public AbstractPredicate(Model graph,
                             GraphManager graphManager) {

        this.graph = graph;

        try {

            ResIterator subjects = asGraph().listSubjectsWithProperty(RDF.type);

            if (!subjects.hasNext()) {
                throw new IllegalArgumentException("Expected at least 1 typed resource");
            }

            Resource predicateResource = null;

            while (subjects.hasNext()) {
                Resource res = subjects.next();
                if (res.hasProperty(RDF.type, OWL.ObjectProperty) || res.hasProperty(RDF.type, OWL.DatatypeProperty)) {
                    if (predicateResource != null) {
                        throw new IllegalArgumentException("Multiple class resources");
                    } else {
                        predicateResource = res;
                    }
                }
            }

            if (predicateResource == null) {
                throw new IllegalArgumentException("Expected 1 ObjectProperty or DatatypeProperty");
            }

            Statement isDefinedBy = predicateResource.getRequiredProperty(RDFS.isDefinedBy);
            Resource modelResource = isDefinedBy.getObject().asResource();

            this.dataModel = new DataModel(LDHelper.toIRI(modelResource.toString()), graphManager);
            this.id = LDHelper.toIRI(predicateResource.toString());
            this.provUUID = "urn:uuid:" + UUID.randomUUID().toString();

            predicateResource.removeAll(DCTerms.identifier);
            predicateResource.addProperty(DCTerms.identifier, ResourceFactory.createPlainLiteral(provUUID));

        } catch (Exception ex) {
            logger.warn(ex.getMessage());
            throw new IllegalArgumentException("Expected 1 predicate (isDefinedBy)");

        }

        if (!getId().startsWith(getModelId())) {
            throw new IllegalArgumentException("Predicate ID should start with model ID!");
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

    public IRI getModelIRI() {
        return this.dataModel.getIRI();
    }

    public Map<String, String> getLabel() {
        return LDHelper.RDFNodeListToMap(this.graph.listObjectsOfProperty(ResourceFactory.createResource(this.getId()), RDFS.label).toList());
    }

    public Map<String, String> getComment() {
        return LDHelper.RDFNodeListToMap(this.graph.listObjectsOfProperty(ResourceFactory.createResource(this.getId()), RDFS.comment).toList());
    }

    public String getType() {
        return this.graph.contains(ResourceFactory.createResource(this.getId()), RDF.type, OWL.ObjectProperty) ? "association" : "attribute";
    }

    public IRI getIRI() {
        return this.id;
    }
}
