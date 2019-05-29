package fi.vm.yti.datamodel.api.model;

import fi.vm.yti.datamodel.api.service.*;
import fi.vm.yti.datamodel.api.utils.*;

import org.apache.jena.iri.IRI;
import org.apache.jena.query.ParameterizedSparqlString;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;
import org.apache.jena.util.ResourceUtils;
import org.apache.jena.util.SplitIRI;

import java.util.Iterator;
import java.util.List;
import java.util.UUID;

import org.apache.jena.vocabulary.DCTerms;
import org.apache.jena.vocabulary.OWL;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.topbraid.shacl.vocabulary.SH;

public class Shape extends AbstractShape {

    private static final Logger logger = LoggerFactory.getLogger(Shape.class.getName());

    public Shape(IRI shapeId,
                 GraphManager graphManager) {
        super(shapeId, graphManager);
    }

    public Shape(String jsonld,
                 GraphManager graphManager,
                 ModelManager modelManager) {
        super(modelManager.createJenaModelFromJSONLDString(jsonld), graphManager);
    }

    public Shape(IRI classIRI,
                 IRI shapeIRI,
                 IRI profileIRI,
                 GraphManager graphManager,
                 EndpointServices endpointServices) {

        logger.info("Creating shape from " + classIRI.toString() + " to " + shapeIRI.toString());

        if (!graphManager.isExistingServiceGraph(SplitIRI.namespace(classIRI.toString()))) {

            // Shape from external class

            ParameterizedSparqlString pss = new ParameterizedSparqlString();
            pss.setNsPrefixes(LDHelper.PREFIX_MAP);
            String queryString;
            String service = "imports";
            logger.info("Using ext query:");
            queryString = QueryLibrary.externalShapeQuery;
            pss.setCommandText(queryString);
            pss.setIri("classIRI", classIRI);
            pss.setIri("model", profileIRI);
            pss.setIri("modelService", endpointServices.getLocalhostCoreSparqlAddress());
            pss.setLiteral("draft", "DRAFT");
            pss.setIri("shapeIRI", shapeIRI);
            this.graph = graphManager.constructModelFromService(pss.toString(), endpointServices.getImportsSparqlAddress());

        } else {

            // Shape from internal class
            this.graph = graphManager.getCoreGraph(classIRI);

            if (this.graph == null || this.graph.size() < 1) {
                throw new IllegalArgumentException();
            }

            Resource shape = this.graph.getResource(classIRI.toString());
            ResourceUtils.renameResource(shape, shapeIRI.toString());

            shape = this.graph.getResource(shapeIRI.toString());
            shape.removeAll(RDF.type);
            shape.removeAll(OWL.versionInfo);
            shape.addProperty(RDF.type, SH.NodeShape);
            shape.addLiteral(OWL.versionInfo, "DRAFT");
            shape.addProperty(SH.targetClass, ResourceFactory.createResource(classIRI.toString()));

            Resource modelResource = shape.getPropertyResourceValue(RDFS.isDefinedBy);
            modelResource.removeProperties();
            ResourceUtils.renameResource(modelResource, profileIRI.toString());

            LDHelper.rewriteLiteral(this.graph, shape, DCTerms.created, LDHelper.getDateTimeLiteral());
            LDHelper.rewriteLiteral(this.graph, shape, DCTerms.modified, LDHelper.getDateTimeLiteral());
            shape.removeAll(DCTerms.identifier);

            // Rename property UUIDs
            StmtIterator nodes = shape.listProperties(SH.property);
            List<Statement> propertyShapeList = nodes.toList();

            for (Iterator<Statement> i = propertyShapeList.iterator(); i.hasNext(); ) {
                Resource propertyShape = i.next().getObject().asResource();
                ResourceUtils.renameResource(propertyShape, "urn:uuid:" + UUID.randomUUID().toString());
            }

        }

    }

}
