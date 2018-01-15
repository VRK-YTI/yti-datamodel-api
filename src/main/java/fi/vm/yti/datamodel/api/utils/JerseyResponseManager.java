/*
 * Licensed under the European Union Public Licence (EUPL) V.1.1 
 */
package fi.vm.yti.datamodel.api.utils;

import javax.ws.rs.core.Application;
import javax.ws.rs.core.Response;

import fi.vm.yti.datamodel.api.config.ApplicationProperties;
import org.apache.jena.rdf.model.Model;

import java.util.UUID;

/**
 *
 * @author malonen
 */
public class JerseyResponseManager {
    
    public static Response ok() {
        return Response.status(200).build();    
    }
    
    public static Response okModel(Model model) {
        return Response.status(200).entity(ModelManager.writeModelToString(model)).build();
    }
    
    public static Response ok(String content, String contentType) {
        return Response.ok().entity(content).type(contentType).build();
    }
    
    public static Response okUUID(UUID uuid) {
        return Response.status(200).entity("{\"@id\":\"urn:uuid:"+uuid+"\"}").build();
    }

    public static Response okUUID(String uuid) {
        return Response.status(200).entity("{\"@id\":\""+uuid+"\"}").build();
    }
    
    public static Response successUuid(UUID uuid) {
        return Response.status(200).entity("{\"identifier\":\"urn:uuid:"+uuid+"\"}").build();
    }

    public static Response successUuid(String uuid, String id) {
        return Response.status(200).entity("{\"@id\":\"" + id + "\", \"identifier\":\"urn:uuid:"+uuid+"\"}").build();
    }

    public static Response successUuid(String uuid) {
        return Response.status(200).entity("{\"identifier\":\""+uuid+"\"}").build();
    }

    public static Response config() {
        return Response.status(200).entity("{\"groups\":\""+ ApplicationProperties.getPublicGroupManagementAPI()+"\", \"concepts\":\""+ApplicationProperties.getPublicTermAPI()+"\"}").build();
    }
    
    public static Response langNotDefined() {
        return Response.status(403).entity(ErrorMessage.LANGNOTDEFINED).build();
    }
    
    public static Response unauthorized() {
        return Response.status(401).entity(ErrorMessage.UNAUTHORIZED).build();
    }
    
    public static Response invalidIRI() {
        return Response.status(403).entity(ErrorMessage.INVALIDIRI).build();
    }
    
    public static Response usedIRI() {
        return Response.status(403).entity(ErrorMessage.USEDIRI).build();
    }

    public static Response usedIRI(String id) {
        return Response.status(403).entity("{\"errorMessage\":\""+id+" ID is already in use\"}").build();
    }
    
    public static Response error() {
        return Response.status(400).entity(ErrorMessage.NOTACCEPTED).build();
    }
    
    public static Response serverError() {
        return Response.serverError().entity("{}").build();
    }
    
    public static Response unexpected() {
        return Response.status(403).entity(ErrorMessage.UNEXPECTED).build();
    }
    
    public static Response notCreated() {
        return Response.status(403).entity(ErrorMessage.NOTCREATED).build();
    }
    
    public static Response notAcceptable() {
        return Response.status(Response.Status.NOT_ACCEPTABLE).entity(ErrorMessage.NOTACCEPTED).build();
    }
    
    public static Response notCreated(int status) {
        return Response.status(status).entity(ErrorMessage.NOTCREATED).build();
    }
    
    public static Response cannotRemove() {
        return Response.status(406).entity(ErrorMessage.STATUS).build();
    }
    
    public static Response unexpected(int status) {
        return Response.status(status).entity(ErrorMessage.UNEXPECTED).build();
    }
        
    public static Response okNoContent() {
        return Response.status(204).build();
    }
    
    public static Response okEmptyContent() {
        return Response.status(200).entity("{}").build();
    }
        
    public static Response invalidParameter() {
        return Response.status(403).entity(ErrorMessage.INVALIDPARAMETER).build();
    }
    
    public static Response notFound() {
        return Response.status(404).entity(ErrorMessage.NOTFOUND).build();
    }
    
    public static Response depedencies() {
        return Response.status(403).entity(ErrorMessage.DEPEDENCIES).build();
    }
    
    public static Response locked() {
        return Response.status(423).entity(ErrorMessage.LOCKED).build();
    }
    
    public static Response sendBoolean(boolean status) {
       return Response.status(Response.Status.OK).entity(status).build();
    }
    
    
}
