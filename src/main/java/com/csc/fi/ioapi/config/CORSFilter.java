/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.csc.fi.ioapi.config;

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
 
       resp.getHttpHeaders().add("Access-Control-Allow-Origin", "*");
       resp.getHttpHeaders().add("Access-Control-Allow-Credentials","true");
       resp.getHttpHeaders().add("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS, HEAD");
       resp.getHttpHeaders().add("Access-Control-Request-Headers","origin, content-type, accept, authorization");
       resp.getHttpHeaders().add("Access-Control-Allow-Headers","Content-Type, Accept, X-Requested-With");
       
       return resp;
       
        /* Another way to do it? */
        /*
        ResponseBuilder resp = Response.fromResponse(contResp.getResponse());
        resp.header("Access-Control-Allow-Origin", "*")
            .header("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
 
        String reqHead = req.getHeaderValue("Access-Control-Request-Headers");
 
        if(null != reqHead && !reqHead.equals("")){
            resp.header("Access-Control-Allow-Headers", reqHead);
        }
 
        contResp.setResponse(resp.build());
        return contResp;
        */
       
          }
 
}
