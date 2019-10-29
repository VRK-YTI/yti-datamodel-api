/*
 * Licensed under the European Union Public Licence (EUPL) V.1.1
 */
package fi.vm.yti.datamodel.api.service;

import fi.vm.yti.datamodel.api.config.ApplicationProperties;
import fi.vm.yti.datamodel.api.utils.ErrorMessage;

import org.apache.jena.rdf.model.Model;
import org.springframework.stereotype.Service;

import javax.ws.rs.core.Response;

import java.util.UUID;

@Service
public class JerseyResponseManager {

    private final ModelManager modelManager;
    private final ApplicationProperties properties;

    JerseyResponseManager(ModelManager modelManager,
                          ApplicationProperties properties) {
        this.modelManager = modelManager;
        this.properties = properties;
    }

    public Response ok() {
        return Response.status(200).build();
    }

    public Response ok(Object obj) {
        return Response.status(200).entity(obj).build();
    }

    public Response okModel(Model model) {
        return Response.status(200).entity(modelManager.writeModelToJSONLDString(model)).build();
    }

    public Response ok(String content,
                       String contentType) {
        return Response.ok().entity(content).type(contentType).build();
    }

    public Response okUrnUUID(UUID uuid) {
        return Response.status(200).entity("{\"@id\":\"urn:uuid:" + uuid + "\"}").build();
    }

    public Response okUrnUUID(String uuid) {
        return Response.status(200).entity("{\"@id\":\"" + uuid + "\"}").build();
    }

    public Response successUri(String uri) {
        return Response.status(200).entity("{\"uri\":\""+ uri + "\"}").build();
    }

    public Response successUrnUuid(UUID uuid) {
        return Response.status(200).entity("{\"identifier\":\"urn:uuid:" + uuid + "\"}").build();
    }

    public Response successUrnUuid(String uuid,
                                   String id) {
        return Response.status(200).entity("{\"@id\":\"" + id + "\", \"identifier\":\"urn:uuid:" + uuid + "\"}").build();
    }

    public Response successUuid(String uuid) {
        return Response.status(200).entity("{\"identifier\":\"" + uuid + "\"}").build();
    }

    public Response config() {
        return Response.status(200).entity("{  \"groups\":\"" + properties.getPublicGroupManagementAPI() + "\"," +
            "\"groupsFrontend\":\"" + properties.getPublicGroupManagementFrontend() + "\"," +
            "\"conceptsFrontend\":\"" + properties.getPublicTerminologyFrontend() + "\", " +
            "\"codes\":\"" + properties.getDefaultSuomiCodeServerAPI() + "\", " +
            "\"codesFrontend\":\"" + properties.getPublicSuomiCodeServerFrontend() + "\", " +
            "\"commentsFrontend\":\"" + properties.getPublicCommentsFrontend() + "\", " +
            "\"messagingEnabled\":" + properties.getMessagingEnabled() + ", " +
            "\"dev\":" + properties.getDevMode() + ", " +
            "\"env\":\"" + properties.getEnv() + "\"}").build();
    }

    public Response langNotDefined() {
        return Response.status(403).entity(ErrorMessage.LANGNOTDEFINED).build();
    }

    public Response unauthorized() {
        return Response.status(401).entity(ErrorMessage.UNAUTHORIZED).build();
    }

    public Response invalidIRI() {
        return Response.status(403).entity(ErrorMessage.INVALIDIRI).build();
    }

    public Response usedIRI() {
        return Response.status(403).entity(ErrorMessage.USEDIRI).build();
    }

    public Response usedIRI(String id) {
        return Response.status(403).entity("{\"errorMessage\":\"" + id + " ID is already in use\"}").build();
    }

    public Response error() {
        return Response.status(400).entity(ErrorMessage.NOTACCEPTED).build();
    }

    public Response serverError() {
        return Response.serverError().entity("{}").build();
    }

    public Response unexpected() {
        return Response.status(403).entity(ErrorMessage.UNEXPECTED).build();
    }

    public Response notCreated() {
        return Response.status(403).entity(ErrorMessage.NOTCREATED).build();
    }

    public Response notAcceptable() {
        return Response.status(Response.Status.NOT_ACCEPTABLE).entity(ErrorMessage.NOTACCEPTED).build();
    }

    public Response notCreated(int status) {
        return Response.status(status).entity(ErrorMessage.NOTCREATED).build();
    }

    public Response cannotRemove() {
        return Response.status(406).entity(ErrorMessage.STATUS).build();
    }

    public Response unexpected(int status) {
        return Response.status(status).entity(ErrorMessage.UNEXPECTED).build();
    }

    public Response okNoContent() {
        return Response.status(204).build();
    }

    public Response okEmptyContent() {
        return Response.status(200).entity("{}").build();
    }

    public Response invalidParameter() {
        return Response.status(403).entity(ErrorMessage.INVALIDPARAMETER).build();
    }

    public Response notFound() {
        return Response.status(404).entity(ErrorMessage.NOTFOUND).build();
    }

    public Response depedencies() {
        return Response.status(403).entity(ErrorMessage.DEPEDENCIES).build();
    }

    public Response locked() {
        return Response.status(423).entity(ErrorMessage.LOCKED).build();
    }

    public Response sendBoolean(boolean status) {
        return Response.status(Response.Status.OK).entity(status).build();
    }
}
