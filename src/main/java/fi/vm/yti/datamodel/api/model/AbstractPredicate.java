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
 * Created by malonen on 29.11.2017.
 */
public class AbstractPredicate extends AbstractResource {


    protected String provUUID;
    private static final Logger logger = Logger.getLogger(AbstractPredicate.class.getName());


    public AbstractPredicate() {}

    public AbstractPredicate(IRI graphIRI) {
       super(graphIRI);
    }

    public AbstractPredicate(Model graph){
        this.graph = graph;

        try {
            Statement isDefinedBy = asGraph().getRequiredProperty(null, RDFS.isDefinedBy);
            Resource predicateResource = isDefinedBy.getSubject().asResource();
            Resource modelResource = isDefinedBy.getObject().asResource();

            if(!(predicateResource.hasProperty(RDF.type, OWL.ObjectProperty) || predicateResource.hasProperty(RDF.type, OWL.DatatypeProperty))) {
                throw new IllegalArgumentException("Expected Property type");
            }

            this.dataModel = new DataModel(LDHelper.toIRI(modelResource.toString()));
            this.id = LDHelper.toIRI(predicateResource.toString());
            this.provUUID = "urn:uuid:"+UUID.randomUUID().toString();

            predicateResource.removeAll(DCTerms.identifier);
            predicateResource.addProperty(DCTerms.identifier,ResourceFactory.createPlainLiteral(provUUID));

        } catch(Exception ex)  {
            logger.warning(ex.getMessage());
            throw new IllegalArgumentException("Expected 1 predicate (isDefinedBy)");

        }

        if(!getId().startsWith(getModelId())) {
            throw new IllegalArgumentException("Predicate ID should start with model ID!");
        }


    }

    public String getProvUUID() { return this.provUUID; }
    public List<UUID> getOrganizations() { return this.dataModel.getOrganizations(); }

}
