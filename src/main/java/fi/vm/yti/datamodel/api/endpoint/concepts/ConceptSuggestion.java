package fi.vm.yti.datamodel.api.endpoint.concepts;

import fi.vm.yti.datamodel.api.security.AuthorizationManager;
import fi.vm.yti.datamodel.api.service.IDManager;
import fi.vm.yti.datamodel.api.service.JerseyClient;
import fi.vm.yti.datamodel.api.service.JerseyResponseManager;
import fi.vm.yti.datamodel.api.service.ModelManager;
import fi.vm.yti.datamodel.api.utils.*;
import io.swagger.annotations.*;
import org.apache.jena.rdf.model.*;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.SKOS;
import org.apache.jena.vocabulary.SKOSXL;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;
import java.util.UUID;

@Component
@Path("conceptSuggestion")
@Api(tags = {"Concept"}, description = "Create concept suggestions")
public class ConceptSuggestion {

    private final AuthorizationManager authorizationManager;
    private final JerseyResponseManager jerseyResponseManager;
    private final ModelManager modelManager;
    private final IDManager idManager;
    private final JerseyClient jerseyClient;

    @Autowired
    ConceptSuggestion(AuthorizationManager authorizationManager,
                      JerseyResponseManager jerseyResponseManager,
                      ModelManager modelManager,
                      IDManager idManager,
                      JerseyClient jerseyClient) {

        this.authorizationManager = authorizationManager;
        this.jerseyResponseManager = jerseyResponseManager;
        this.modelManager = modelManager;
        this.idManager = idManager;
        this.jerseyClient = jerseyClient;
    }

    @PUT
    @ApiOperation(value = "Create concept suggestion", notes = "Create new concept suggestion")
    @ApiResponses(value = { @ApiResponse(code = 200, message = "New concept is created"),
            @ApiResponse(code = 400, message = "Invalid ID supplied"),
            @ApiResponse(code = 403, message = "Invalid IRI in parameter"),
            @ApiResponse(code = 401, message = "User is not logged in"),
            @ApiResponse(code = 404, message = "Service not found") })
    public Response newConceptSuggestion(
            @ApiParam(value = "Model ID") @QueryParam("modelID") String modelID,
            @ApiParam(value = "GRAPH UUID", required = true) @QueryParam("graphUUID") String graphUUID,
            @ApiParam(value = "Label", required = true) @QueryParam("label") String label,
            @ApiParam(value = "Comment", required = true) @QueryParam("comment") String comment,
            @ApiParam(value = "Initial language", required = true, allowableValues="fi,en") @QueryParam("lang") String lang) {

        if (!authorizationManager.hasRightToSuggestConcept()) {
            return jerseyResponseManager.unauthorized();
        }

        if (modelID!=null && !modelID.equals("undefined")) {
            if (idManager.isInvalid(modelID)) {
                return jerseyResponseManager.invalidIRI();
            }
        }

        UUID conceptUUID = UUID.randomUUID();
        UUID termUUID = UUID.randomUUID();

        Model model = ModelFactory.createDefaultModel();

        Property statusProp = model.createProperty("http://www.w3.org/2003/06/sw-vocab-status/ns#term_status");
        Resource concept = model.createResource("urn:uuid:"+conceptUUID);
        Resource term = model.createResource("urn:uuid:"+termUUID);
        Literal prefLabel = ResourceFactory.createLangLiteral(label.toLowerCase(), lang);
        Literal definition = ResourceFactory.createLangLiteral(comment, lang);
        term.addLiteral(SKOSXL.literalForm, prefLabel);
        term.addProperty(RDF.type,SKOSXL.Label);
        concept.addLiteral(SKOS.definition, definition);
        concept.addProperty(SKOSXL.prefLabel,term);
        concept.addProperty(RDF.type, SKOS.Concept);
        concept.addLiteral(statusProp, "SUGGESTED");
        Property idProp = model.createProperty(LDHelper.getNamespaceWithPrefix("termed")+"id");
        concept.addProperty(idProp, conceptUUID.toString());

        String modelString = modelManager.writeModelToJSONLDString(model);

        jerseyClient.saveConceptSuggestion(modelString,graphUUID);

        return jerseyResponseManager.successUrnUuid(conceptUUID);

    }
}
