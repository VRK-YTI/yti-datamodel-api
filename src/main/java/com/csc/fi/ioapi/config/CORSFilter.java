/*
 * Licensed under the European Union Public Licence (EUPL) V.1.1 
 */
package com.csc.fi.ioapi.config;

import com.google.common.net.HttpHeaders;
import com.sun.jersey.spi.container.ContainerRequest;
import com.sun.jersey.spi.container.ContainerResponse;
import com.sun.jersey.spi.container.ContainerResponseFilter;

/**
 *
 * @author malonen
 */
public class CORSFilter implements ContainerResponseFilter {

    @Override
    public ContainerResponse filter(ContainerRequest req, ContainerResponse resp) {

        resp.getHttpHeaders().add(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "*");
        resp.getHttpHeaders().add(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS, "true");
        resp.getHttpHeaders().add(HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS, "GET, POST, PUT, DELETE, OPTIONS, HEAD");
        resp.getHttpHeaders().add(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "origin, content-type, accept, authorization");
        resp.getHttpHeaders().add(HttpHeaders.ACCESS_CONTROL_ALLOW_HEADERS, "Content-Type, Accept, X-Requested-With");
        resp.getHttpHeaders().add(HttpHeaders.CACHE_CONTROL, "no-cache");

        return resp;

    }

}
