package fi.vm.yti.datamodel.api.model;

import fi.vm.yti.datamodel.api.service.*;
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
import java.util.UUID;

/**
 * Created by malonen on 21.11.2017.
 */
public abstract class AbstractShape extends AbstractResource {

    protected String provUUID;
    protected GraphManager graphManager;
    private static final Logger logger = LoggerFactory.getLogger(AbstractShape.class.getName());

    public AbstractShape() {
    }

    public AbstractShape(IRI graphIRI,
                         GraphManager graphManager) {

        this.graphManager = graphManager;
        this.graph = graphManager.getCoreGraph(graphIRI);
        this.id = graphIRI;

        try {

            Statement isDefinedBy = asGraph().getRequiredProperty(ResourceFactory.createResource(getId()), RDFS.isDefinedBy);

            if (!asGraph().contains(ResourceFactory.createResource(getId()), RDF.type, SH.Shape)) {
                throw new IllegalArgumentException("Expected sh:Shape type");
            }

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

    public AbstractShape(Model graph,
                         GraphManager graphManager) {

        this.graphManager = graphManager;
        this.graph = graph;

        List<Resource> modelList = this.graph.listResourcesWithProperty(RDFS.isDefinedBy).toList();
        if (modelList == null || modelList.size() != 1) {
            throw new IllegalArgumentException("Expected 1 model (isDefinedBy)");
        }

        this.dataModel = new DataModel(LDHelper.toIRI(modelList.get(0).getURI()), graphManager);

        List<Resource> scopeList = this.graph.listResourcesWithProperty(SH.targetClass).toList();
        if (scopeList == null || scopeList.size() != 1) {
            throw new IllegalArgumentException("Expected 1 Reusable Class (targetClass)");
        }

        List<Resource> classList = this.graph.listSubjectsWithProperty(RDF.type, SH.NodeShape).toList();
        if (classList == null || classList.size() != 1) {
            throw new IllegalArgumentException("Expected 1 class in graph!");
        }

        Resource shapeResource = classList.get(0);
        // TODO: Validate that namespace is same as in class id
        this.id = LDHelper.toIRI(shapeResource.getURI());

        this.provUUID = "urn:uuid:" + UUID.randomUUID().toString();
        shapeResource.removeAll(DCTerms.identifier);
        shapeResource.addProperty(DCTerms.identifier, ResourceFactory.createResource(provUUID));

    }

    public String getProvUUID() {
        return this.provUUID;
    }

    public List<UUID> getOrganizations() {
        return this.dataModel.getOrganizations();
    }

}
