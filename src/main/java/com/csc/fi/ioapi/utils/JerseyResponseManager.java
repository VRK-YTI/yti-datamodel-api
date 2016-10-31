/*
 * Licensed under the European Union Public Licence (EUPL) V.1.1 
 */
package com.csc.fi.ioapi.utils;

import java.util.UUID;
import javax.ws.rs.core.Response;

/**
 *
 * @author malonen
 */
public class JerseyResponseManager {
    
    public static Response ok() {
        return Response.status(200).build();    
    }
    
    public static Response ok(String content, String contentType) {
        return Response.ok().entity(content).type(contentType).build();
    }
    
    public static Response okUUID(UUID uuid) {
        return Response.status(200).entity("{\"@id\":\"urn:uuid:"+uuid+"\"}").build();
    }
    
    public static Response successUuid(UUID uuid) {
        return Response.status(204).entity("{\"identifier\":\"urn:uuid:"+uuid+"\"}").build();
    }
    
    public static Response langNotDefined() {
        return Response.status(403).entity(ErrorMessage.LANGNOTDEFINED).build();
    }
    
    public static Response unauthorized() {
        return Response.status(403).entity(ErrorMessage.UNAUTHORIZED).build();
    }
    
    public static Response invalidIRI() {
        return Response.status(403).entity(ErrorMessage.INVALIDIRI).build();
    }
    
    public static Response usedIRI() {
        return Response.status(403).entity(ErrorMessage.USEDIRI).build();
    }
    
    public static Response error() {
        return Response.status(403).entity(ErrorMessage.NOTCREATED).build();
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
        return Response.status(Response.Status.NOT_ACCEPTABLE).entity("{}").build();
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
        return Response.status(204).entity("{}").build();
    }
        
    public static Response invalidParameter() {
        return Response.status(403).entity(ErrorMessage.INVALIDPARAMETER).build();
    }
    
    public static Response notFound() {
        return Response.status(403).entity(ErrorMessage.NOTFOUND).build();
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
