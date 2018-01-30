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
          super(graphIRI);
        }

        public AbstractClass(Model graph){
            this.graph = graph;

            try {
                Statement isDefinedBy = asGraph().getRequiredProperty(null, RDFS.isDefinedBy);
                Resource classResource = isDefinedBy.getSubject().asResource();
                Resource modelResource = isDefinedBy.getObject().asResource();

                if(!classResource.hasProperty(RDF.type, RDFS.Class)) {
                    throw new IllegalArgumentException("Expected rdfs:Class type");
                }

                this.dataModel = new DataModel(LDHelper.toIRI(modelResource.toString()));
                this.id = LDHelper.toIRI(classResource.toString());

                if(!this.id.toString().startsWith(getModelId())) {
                    throw new IllegalArgumentException("Class ID should start with model ID!");
                }

                this.provUUID = "urn:uuid:"+UUID.randomUUID().toString();
                classResource.removeAll(DCTerms.identifier);
                classResource.addProperty(DCTerms.identifier,ResourceFactory.createPlainLiteral(provUUID));

            } catch(Exception ex)  {
                logger.warning(ex.getMessage());
                throw new IllegalArgumentException("Expected 1 class (isDefinedBy)");
            }

        }

        public String getProvUUID() { return this.provUUID; }
        public List<UUID> getOrganizations() { return this.dataModel.getOrganizations(); }

}
