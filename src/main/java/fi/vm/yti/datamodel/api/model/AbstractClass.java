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
import org.topbraid.shacl.vocabulary.SH;

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

                this.dataModel = new DataModel(LDHelper.toIRI(modelResource.toString()));
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
