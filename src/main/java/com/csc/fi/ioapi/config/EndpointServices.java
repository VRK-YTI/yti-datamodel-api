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
    return endpoint+"/core/data";
}

public String getCoreSparqlAddress() {
    return endpoint+"/core/sparql";
}

public String getCoreSparqlUpdateAddress() {
    return endpoint+"/core/update";
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

public String getConceptAPI() {
     return "http://api.finto.fi/rest/v1/data";
}

public String getConceptSearchAPI() {
     return "http://api.finto.fi/rest/v1/search";
}

public String getConceptSearchAPI(String id) {
     return "http://api.finto.fi/rest/v1/"+id+"/search";
}

public String getSchemeSearchAPI() {
     return "http://api.finto.fi/rest/v1/vocabularies";
}

}
