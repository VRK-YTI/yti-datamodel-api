package fi.vm.yti.datamodel.api.model;

import fi.vm.yti.datamodel.api.service.GraphManager;
import fi.vm.yti.datamodel.api.service.JenaClient;
import fi.vm.yti.datamodel.api.service.ModelManager;
import fi.vm.yti.datamodel.api.service.ServiceDescriptionManager;
import fi.vm.yti.datamodel.api.utils.LDHelper;
import org.apache.jena.iri.IRI;
import org.apache.jena.rdf.model.*;
import org.apache.jena.vocabulary.DCTerms;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;
import org.topbraid.shacl.vocabulary.SH;

import java.util.List;
import java.util.UUID;

public abstract class AbstractClass extends AbstractResource {

    protected String provUUID;

    public AbstractClass(GraphManager graphManager,
                         JenaClient jenaClient,
                         ModelManager modelManager) {
        super(graphManager, jenaClient, modelManager);
    }

    public AbstractClass(IRI graphIRI,
                         GraphManager graphManager,
                         ServiceDescriptionManager serviceDescriptionManager,
                         JenaClient jenaClient,
                         ModelManager modelManager) {
        super(graphIRI, graphManager, serviceDescriptionManager, jenaClient, modelManager);
    }

    public AbstractClass(Model graph,
                         GraphManager graphManager,
                         ServiceDescriptionManager serviceDescriptionManager,
                         JenaClient jenaClient,
                         ModelManager modelManager) {
        super(graphManager, jenaClient, modelManager);

        this.graph = graph;

        try {

            ResIterator subjects = asGraph().listSubjectsWithProperty(RDF.type);

            if(!subjects.hasNext()) {
                throw new IllegalArgumentException("Expected at least 1 typed resource");
            }

            Resource classResource = null;

            while(subjects.hasNext()) {
                Resource res = subjects.next();
                if(res.hasProperty(RDF.type, RDFS.Class) || res.hasProperty(RDF.type, SH.Shape)) {
                    if(classResource!=null) {
                        throw new IllegalArgumentException("Multiple class resources");
                    } else {
                        classResource = res;
                    }
                }
            }

            if(classResource==null) {
                throw new IllegalArgumentException("Expected rdfs:Class or sh:Shape");
            }

            // TODO: Check that doesnt contain multiple class resources
            Statement isDefinedBy = classResource.getRequiredProperty(RDFS.isDefinedBy);
            Resource modelResource = isDefinedBy.getObject().asResource();

            this.dataModel = new DataModel(LDHelper.toIRI(modelResource.toString()), graphManager, serviceDescriptionManager, jenaClient);
            this.id = LDHelper.toIRI(classResource.toString());

            if(!this.id.toString().startsWith(getModelId())) {
                throw new IllegalArgumentException("Class ID should start with model ID!");
            }

            this.provUUID = "urn:uuid:"+UUID.randomUUID().toString();
            classResource.removeAll(DCTerms.identifier);
            classResource.addProperty(DCTerms.identifier,ResourceFactory.createPlainLiteral(provUUID));

        } catch(Exception ex)  {
            ex.printStackTrace();
            throw new IllegalArgumentException("Expected 1 class (isDefinedBy)");
        }

    }

    public String getProvUUID() { return this.provUUID; }
    public List<UUID> getOrganizations() { return this.dataModel.getOrganizations(); }

}
