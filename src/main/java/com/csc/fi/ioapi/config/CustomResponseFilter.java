/*
 * Licensed under the European Union Public Licence (EUPL) V.1.1 
 */
package com.csc.fi.ioapi.config;

import com.google.common.net.HttpHeaders;
import java.io.IOException;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
import org.glassfish.jersey.server.ContainerRequest;
import org.glassfish.jersey.server.ContainerResponse;

/**
 *
 * @author malonen
 */
public class CustomResponseFilter implements ContainerResponseFilter {

    @Override
    public void filter(ContainerRequestContext request, ContainerResponseContext resp) throws IOException {
        resp.getHeaders().add("Cache-Control", "no-cache, no-store, must-revalidate");
        resp.getHeaders().add("Pragma", "no-cache");
        resp.getHeaders().add("Expires", "0");
    }

}
