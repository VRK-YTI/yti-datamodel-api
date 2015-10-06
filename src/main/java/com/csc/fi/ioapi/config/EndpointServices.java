/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.csc.fi.ioapi.config;

/**
 *
 * @author malonen
 */
public class EndpointServices {
    
private String endpoint;
    
public EndpointServices() {
    this.endpoint = ApplicationProperties.getEndpoint();
}

public String getCoreReadAddress() {
    return endpoint+"/core/get";
}

public String getCoreReadWriteAddress() {
    return endpoint+"/search/data";
}

public String getCoreSparqlAddress() {
    return endpoint+"/search/sparql";
}

public String getCoreSparqlUpdateAddress() {
    return endpoint+"/search/update";
}

public String getUsersReadAddress() {
    return endpoint+"/users/get";
}

public String getUsersReadWriteAddress() {
    return endpoint+"/users/data";
}

public String getUsersSparqlAddress() {
    return endpoint+"/users/sparql";
}

public String getUsersSparqlUpdateAddress() {
    return endpoint+"/users/update";
}

}
