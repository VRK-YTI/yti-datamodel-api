/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.csc.fi.ioapi.utils;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.jsonldjava.utils.JsonUtils;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.lang3.StringUtils;
import org.apache.jena.query.ParameterizedSparqlString;
import com.sun.jersey.api.uri.UriComponent;
import java.text.Normalizer;
import java.text.Normalizer.Form;
import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.lang3.text.WordUtils;

/**
 *
 * @author malonen
 */
public class LDHelper {
    
   public static final String[] UNRESOLVABLE = {"xsd","iow","text","sh","afn","schema","dcap"};
   
   public static boolean isPrefixResolvable(String item) {
      return !Arrays.stream(UNRESOLVABLE).anyMatch(item::equals);
   }
   
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
        put("void","http://rdfs.org/ns/void#");
        put("sd","http://www.w3.org/ns/sparql-service-description#");
        put("text","http://jena.apache.org/text#");
        put("sh","http://www.w3.org/ns/shacl#");
        put("iow","http://iow.csc.fi/ns/iow#");
        put("skos","http://www.w3.org/2004/02/skos/core#");
        put("prov","http://www.w3.org/ns/prov#");
        put("dcap","http://purl.org/ws-mmi-dc/terms/");
        put("afn","http://jena.hpl.hp.com/ARQ/function#");
        put("schema","http://schema.org/");
        put("ts","http://www.w3.org/2003/06/sw-vocab-status/ns#");
        put("dcam","http://purl.org/dc/dcam/");
    }});
   
    public static final Map<String, Object> CONTEXT_MAP = 
    Collections.unmodifiableMap(new HashMap<String, Object>() {{
        put("subClassOf", jsonObject("{ '@id': 'http://www.w3.org/2000/01/rdf-schema#subClassOf', '@type': '@id' }"));
        put("property", jsonObject("{ '@id': 'http://www.w3.org/ns/shacl#property', '@type': '@id' }"));
        put("predicate", jsonObject("{ '@id': 'http://www.w3.org/ns/shacl#predicate', '@type': '@id' }"));
    }});
    
    
        public static final Map<String, Object> OPH_MAP = 
    Collections.unmodifiableMap(new HashMap<String, Object>() {{
        put("koodistos", jsonObject("{ '@id': 'http://www.w3.org/2000/01/rdf-schema#subClassOf', '@type': '@id' }"));
        put("koodistoUrl", jsonObject("{ '@id': 'http://www.w3.org/ns/shacl#property', '@type': '@id' }"));
        put("predicate", jsonObject("{ '@id': 'http://www.w3.org/ns/shacl#predicate', '@type': '@id' }"));
    }});

    private static Map<String,Object> jsonObject(String json) {
       try {
           ObjectMapper mapper = new ObjectMapper();
           mapper.configure(JsonParser.Feature.ALLOW_SINGLE_QUOTES, true);
           return mapper.readValue(json, new TypeReference<HashMap<String,Object>>() {});
       } catch (IOException ex) {
           System.out.println(ex.toString());
           return null;
       }
    }
    
    public final static String prefix =   "PREFIX owl: <http://www.w3.org/2002/07/owl#> "+
                            "PREFIX xsd: <http://www.w3.org/2001/XMLSchema#> "+
                            "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#> "+
                            "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> "+
                            "PREFIX foaf: <http://xmlns.com/foaf/0.1/> "+
                            "PREFIX dcterms: <http://purl.org/dc/terms/> "+
                            "PREFIX adms: <http://www.w3.org/ns/adms#> "+
                            "PREFIX dc: <http://purl.org/dc/elements/1.1/> "+
                            "PREFIX void: <http://rdfs.org/ns/void#> "+
                            "PREFIX sd: <http://www.w3.org/ns/sparql-service-description#> "+
                            "PREFIX text: <http://jena.apache.org/text#> "+
                            "PREFIX sh: <http://www.w3.org/ns/shacl#> "+
                            "PREFIX skos: <http://www.w3.org/2004/02/skos/core#> "+
                            "PREFIX prov: <http://www.w3.org/ns/prov#> " +
                            "PREFIX iow: <http://iow.csc.fi/ns/iow#>" +
                            "PREFIX dcap: <http://purl.org/ws-mmi-dc/terms/> " +
                            "PREFIX afn: <http://jena.hpl.hp.com/ARQ/function#>"+
                            "PREFIX schema: <http://schema.org/>"+
                            "PREFIX ts: <http://www.w3.org/2003/06/sw-vocab-status/ns#>"+
                            "PREFIX dcam: <http://purl.org/dc/dcam/>";
    
   
    ParameterizedSparqlString pss = new ParameterizedSparqlString();

    static String query(String queryString) {
        queryString = prefix+queryString;
        return  UriComponent.encode(queryString,UriComponent.Type.QUERY_PARAM); // URLEncoder.encode(queryString, "UTF-8");
    }
    
    public static boolean isAlphaString(String name) {
        return name.matches("[a-zA-Z]+");
    }
    
    public static String modelName(String name) {
        name = name.toLowerCase();
        return removeInvalidCharacters(name);
    }
    
    public static String propertyName(String name) {
        name = StringUtils.uncapitalize(name);
        return removeInvalidCharacters(name);
    }
    
    public static String resourceName(String name) {
         name = WordUtils.capitalize(name);
         return removeInvalidCharacters(name);
    }
    
    public static String removeInvalidCharacters(String name) {
        name = removeAccents(name);
        name = name.replaceAll("[^a-zA-Z0-9_-]", "");
        return name;
    }
    
   public static String removeAccents(String text) {
    return text == null ? null :
        Normalizer.normalize(text, Form.NFD)
            .replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
    }
    
    
    public static final InputStream getDefaultGraphInputStream() {
           return LDHelper.class.getClassLoader().getResourceAsStream("defaultGraph.json");
    }
    
    public static final InputStream getDefaultSchemes() {
           return LDHelper.class.getClassLoader().getResourceAsStream("defaultSchemes.json");
    }
    
    public static final InputStream getDefaultCodeServers() {
           return LDHelper.class.getClassLoader().getResourceAsStream("defaultCodeServers.json");
    }
      
    public static final InputStream getDefaultGroupsInputStream() {
           return LDHelper.class.getClassLoader().getResourceAsStream("defaultGroups.json");
    }
    
    public static Object getUserContext() {
       try {
           return JsonUtils.fromInputStream(LDHelper.class.getClassLoader().getResourceAsStream("userContext.json"));
       } catch (IOException ex) {
           Logger.getLogger(LDHelper.class.getName()).log(Level.SEVERE, null, ex);
           return null;
       }
    }
    
    public static Object getDescriptionContext() {
       try {
           return JsonUtils.fromInputStream(LDHelper.class.getClassLoader().getResourceAsStream("descriptionContext.json"));
       } catch (IOException ex) {
           Logger.getLogger(LDHelper.class.getName()).log(Level.SEVERE, null, ex);
           return null;
       }
    }
    
    public static Object getGroupContext() {
       try {
           return JsonUtils.fromInputStream(LDHelper.class.getClassLoader().getResourceAsStream("groupContext.json"));
       } catch (IOException ex) {
           Logger.getLogger(LDHelper.class.getName()).log(Level.SEVERE, null, ex);
           return null;
       }
    }
    
    public static Object getExportContext() {
       try {
           return JsonUtils.fromInputStream(LDHelper.class.getClassLoader().getResourceAsStream("export.json"));
       } catch (IOException ex) {
           Logger.getLogger(LDHelper.class.getName()).log(Level.SEVERE, null, ex);
           return null;
       }
    }
    
    public static Object getOPHContext() {
       try {
           return JsonUtils.fromInputStream(LDHelper.class.getClassLoader().getResourceAsStream("oph.json"));
       } catch (IOException ex) {
           Logger.getLogger(LDHelper.class.getName()).log(Level.SEVERE, null, ex);
           return null;
       }
    }
   
   public static String expandSparqlQuery(String query) {
       return expandSparqlQuery(query,LDHelper.PREFIX_MAP);
   }
    
   public static String expandSparqlQuery(String query, Map<String, String> prefix_map) {
  
    for(Map.Entry<String, String> namespaces : prefix_map.entrySet()) {
        StringBuffer sb = new StringBuffer();
        /* Find curies starting with whitespace */
        String prefiz = " "+namespaces.getKey().concat(":");
        String REGEX = prefiz+"\\w*\\b";
        Pattern p = Pattern.compile(REGEX);
        Matcher m = p.matcher(query); 

        while(m.find()){
            StringBuffer replacement = new StringBuffer();
            replacement.append(" <").append(m.group().replace(prefiz, namespaces.getValue())).append(">");
            m.appendReplacement(sb, replacement.toString());
        }
            
       m.appendTail(sb);
       query = sb.toString();
  
     }
    
     return query;
 }    
        
    
}
