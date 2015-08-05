/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.csc.fi.ioapi.utils;

import com.github.jsonldjava.utils.JsonUtils;
import com.hp.hpl.jena.query.ParameterizedSparqlString;
import com.sun.jersey.api.uri.UriComponent;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.jena.iri.IRIFactory;

/**
 *
 * @author malonen
 */
public class LDHelper {
   
  
   
   public static final Map<String, String> PREFIX_MAP = 
    Collections.unmodifiableMap(new HashMap<String, String>() {{ 
        put("owl", "http://www.w3.org/2002/07/owl#");
        put("xsd", "http://www.w3.org/2001/XMLSchema#");
        put("rdf","http://www.w3.org/1999/02/22-rdf-syntax-ns#");
        put("rdfs","http://www.w3.org/2000/01/rdf-schema#");
        put("foaf","http://xmlns.com/foaf/0.1/");
        put("dcterms","http://purl.org/dc/terms/");
        put("adms","http://www.w3.org/ns/adms#");
        put("dc","http://purl.org/dc/elements/1.1/");
        put("vann","http://purl.org/vocab/vann/");
        put("void","http://rdfs.org/ns/void#");
        put("sd","http://www.w3.org/ns/sparql-service-description#");
    }});
    
    public final static String prefix =   "PREFIX owl: <http://www.w3.org/2002/07/owl#> "+
                            "PREFIX xsd: <http://www.w3.org/2001/XMLSchema#> "+
                            "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#> "+
                            "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> "+
                            "PREFIX foaf: <http://xmlns.com/foaf/0.1/> "+
                            "PREFIX dcterms: <http://purl.org/dc/terms/> "+
                            "PREFIX adms: <http://www.w3.org/ns/adms#> "+
                            "PREFIX dc: <http://purl.org/dc/elements/1.1/> "+
                            "PREFIX vann: <http://purl.org/vocab/vann/> "+
                            "PREFIX void: <http://rdfs.org/ns/void#> "+
                            "PREFIX sd: <http://www.w3.org/ns/sparql-service-description#> ";
    
    
    ParameterizedSparqlString pss = new ParameterizedSparqlString();
   
    
    static String query(String queryString) {
        queryString = prefix+queryString;
        return  UriComponent.encode(queryString,UriComponent.Type.QUERY_PARAM); // URLEncoder.encode(queryString, "UTF-8");
    }
    
    static String resourceName(String name) {
        name = StringUtils.capitalize(name);
        name = name.replaceAll("[^a-zA-Z0-9_-]", "");
        return name;
    }
    
    
      public static InputStream getDefaultGraphInputStream() {
           return LDHelper.class.getClassLoader().getResourceAsStream("defaultGraph.json");
    }
    
    public static Object getUserContext() {
       try {
           //  return Thread.currentThread().getContextClassLoader().getResourceAsStream("userContext.json");
           return JsonUtils.fromInputStream(LDHelper.class.getClassLoader().getResourceAsStream("userContext.json"));
       } catch (IOException ex) {
           Logger.getLogger(LDHelper.class.getName()).log(Level.SEVERE, null, ex);
           return null;
       }
    }
    
        public static Object getDescriptionContext() {
       try {
           //  return Thread.currentThread().getContextClassLoader().getResourceAsStream("userContext.json");
           return JsonUtils.fromInputStream(LDHelper.class.getClassLoader().getResourceAsStream("descriptionContext.json"));
       } catch (IOException ex) {
           Logger.getLogger(LDHelper.class.getName()).log(Level.SEVERE, null, ex);
           return null;
       }
    }
    
        public static Object getGroupContext() {
       try {
           //  return Thread.currentThread().getContextClassLoader().getResourceAsStream("userContext.json");
           return JsonUtils.fromInputStream(LDHelper.class.getClassLoader().getResourceAsStream("groupContext.json"));
       } catch (IOException ex) {
           Logger.getLogger(LDHelper.class.getName()).log(Level.SEVERE, null, ex);
           return null;
       }
    }
    
    
}
