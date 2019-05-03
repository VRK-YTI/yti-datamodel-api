/*
 * Licensed under the European Union Public Licence (EUPL) V.1.1 
 */
package fi.vm.yti.datamodel.api.utils;

import java.util.ArrayList;
import java.util.LinkedHashMap;

/**
 *
 * @author jkesanie
 */
public class Frames {


    public static final LinkedHashMap<String, Object> jsonLdKeys;
    public static final LinkedHashMap<String, Object> inScheme;
    public static final LinkedHashMap<String, Object> subject;
    public static final LinkedHashMap<String, Object> comment;
    public static final LinkedHashMap<String, Object> description;    
    public static final LinkedHashMap<String, Object> path;
    public static final LinkedHashMap<String, Object> property;
    public static final LinkedHashMap<String, Object> coreContext;
    public static final LinkedHashMap<String, Object> vocabularyContext;
    public static final LinkedHashMap<String, Object> conceptContext;
    public static final LinkedHashMap<String, Object> classificationContext;
    public static final LinkedHashMap<String, Object> organizationContext;
    public static final LinkedHashMap<String, Object> referenceDataServerContext;
    public static final LinkedHashMap<String, Object> referenceDataContext;
    public static final LinkedHashMap<String, Object> referenceDataCodeContext;
    public static final LinkedHashMap<String, Object> predicateContext;
    public static final LinkedHashMap<String, Object> propertyContext;
    public static final LinkedHashMap<String, Object> namespaceContext;
    public static final LinkedHashMap<String, Object> classContext;
    public static final LinkedHashMap<String, Object> modelContext;
    public static final LinkedHashMap<String, Object> modelPositionContext;
    public static final LinkedHashMap<String, Object> conceptFrame;
    public static final LinkedHashMap<String, Object> classVisualizationFrame;    
    
    static {

        jsonLdKeys = new LinkedHashMap<String, Object>() {
            {
                put("id", "@id");
                put("type", "@type");
            }
        };

        inScheme = new LinkedHashMap<String, Object>() {
            {
              put("@id", "http://www.w3.org/2004/02/skos/core#inScheme");
              put("@type", "@id");                
            }
          
        };

        subject = new LinkedHashMap<String, Object>() {
            {
              put("@id", "http://purl.org/dc/terms/subject");
              put("@type", "@id");                
            }
          
        };

        comment = new LinkedHashMap<String, Object>() {
            {
              put("@id", "http://www.w3.org/2000/01/rdf-schema#comment");
              put("@container", "@language");                
            }
          
        };
        
        description = new LinkedHashMap<String, Object>() {
            {
              put("@id", "http://purl.org/dc/terms/description");
              put("@container", "@language");                
            }
          
        };  
        
        path = new LinkedHashMap<String, Object>() {
            {
              put("@id", "http://www.w3.org/ns/shacl#path");
              put("@type", "@id");                
            }
          
        };                  
        
        property = new LinkedHashMap<String, Object>() {
            {
              put("@id", "http://www.w3.org/ns/shacl#property");
              put("@type", "@id");                
            }
        };

            

        coreContext = new LinkedHashMap<String, Object>() {
            {     
                put("comment", comment);                               
                put("created", new LinkedHashMap<String, Object>() {
                    {
                        put("@id", "http://purl.org/dc/terms/created");
                        put("@type", "http://www.w3.org/2001/XMLSchema#dateTime");                
                    }
                });
                put("definition", new LinkedHashMap<String, Object>() {
                    {
                        put("@id", "http://www.w3.org/2004/02/skos/core#definition");
                        put("@container", "@language");                
                    }
                });
                put("foaf", new LinkedHashMap<String, Object>() {
                    {
                        put("@id", "http://xmlns.com/foaf/0.1/");
                        put("@type", "@id");                
                    }
                });     
                put("hasPart", new LinkedHashMap<String, Object>() {
                    {
                        put("@id", "http://purl.org/dc/terms/hasPart");
                        put("@type", "@id");                
                    }
                });                       
                put("homepage", new LinkedHashMap<String, Object>() {
                    {
                        put("@id", "http://purl.org/dc/terms/homepage");                        
                    }
                });       
                put("identifier", new LinkedHashMap<String, Object>() {
                    {
                        put("@id", "http://purl.org/dc/terms/identifier");               
                    }
                });       
                put("imports", new LinkedHashMap<String, Object>() {
                    {
                        put("@id", "http://www.w3.org/2002/07/owl#imports");
                        put("@type", "@id");                
                    }
                });                
                put("isDefinedBy", new LinkedHashMap<String, Object>() {
                    {
                        put("@id", "http://www.w3.org/2000/01/rdf-schema#isDefinedBy");
                        put("@type", "@id");                
                    }
                });                
                put("isPartOf", new LinkedHashMap<String, Object>() {
                    {
                        put("@id", "http://purl.org/dc/terms/isPartOf");
                        put("@type", "@id");                
                    }
                });                
                put("label", new LinkedHashMap<String, Object>() {
                    {
                        put("@id", "http://www.w3.org/2000/01/rdf-schema#label");
                        put("@container", "@language");                
                    }
                });                
                put("modified", new LinkedHashMap<String, Object>() {
                    {
                        put("@id", "http://purl.org/dc/terms/modified");
                        put("@type", "http://www.w3.org/2001/XMLSchema#dateTime");                
                    }
                });                
                put("nodeKind", new LinkedHashMap<String, Object>() {
                    {
                        put("@id", "http://www.w3.org/ns/shacl#nodeKind");
                        put("@type", "@id");                
                    }
                });                
                put("prefLabel", new LinkedHashMap<String, Object>() {
                    {
                        put("@id", "http://www.w3.org/2004/02/skos/core#prefLabel");
                        put("@container", "@language");                
                    }
                }); 
                put("title", new LinkedHashMap<String, Object>() {
                    {
                        put("@id", "http://purl.org/dc/terms/title");
                        put("@container", "@language");                
                    }
                });    
                put("prov", "http://www.w3.org/ns/prov#");
                put("versionInfo", new LinkedHashMap<String, Object>() {
                    {
                        put("@id", "http://www.w3.org/2002/07/owl#versionInfo");                       
                    }
                }); 
                put("editorialNote", new LinkedHashMap<String, Object>() {
                    {
                        put("@id", "http://www.w3.org/2004/02/skos/core#editorialNote");   
                        put("@container", "@language");                
                    }
                });                  
                put("localName", new LinkedHashMap<String, Object>() {
                    {
                        put("@id", "http://uri.suomi.fi/datamodel/ns/iow#localName");                           
                    }
                });                                  
            }
        };

        vocabularyContext = new LinkedHashMap<String, Object>() {
            {
                putAll(coreContext);
                put("graph", new LinkedHashMap<String, Object>() {
                    {
                        put("@id", "http://termed.thl.fi/meta/graph");
                    }
                }); 
                put("id", new LinkedHashMap<String, Object>() {
                    {
                        put("@id", "http://termed.thl.fi/meta/id");
                    }
                }); 
                put("type", new LinkedHashMap<String, Object>() {
                    {
                        put("@id", "http://termed.thl.fi/meta/type");
                    }
                }); 
                put("uri", new LinkedHashMap<String, Object>() {
                    {
                        put("@id", "http://termed.thl.fi/meta/uri");
                    }
                });                           
                put("description", description);                
            }
        };        
        
        conceptContext = new LinkedHashMap<String, Object>() {
            {
                putAll(coreContext);
                put("inScheme", inScheme);
            }            
        };
        
        classificationContext = new LinkedHashMap<String, Object>() {
            {
                putAll(coreContext);
                put("id", new LinkedHashMap<String, Object>() {
                    {
                        put("@id", "http://www.w3.org/ns/shacl#order");
                    }
                }); 
            }            
        };  
        
        organizationContext = new LinkedHashMap<String, Object>() {
            {
                putAll(coreContext);
                put("description", description);
            }            
        };
        
       referenceDataServerContext = new LinkedHashMap<String, Object>() {
            {
                putAll(coreContext);
                put("description", description);
            }            
        };      
        
        referenceDataContext = new LinkedHashMap<String, Object>() {
            {
                putAll(coreContext);
                put("creator", new LinkedHashMap<String, Object>() {
                    {
                        put("@id", "http://purl.org/dc/terms/creator");                        
                    }
                });  
                put("description", description);
            }            
        };      

        referenceDataCodeContext = new LinkedHashMap<String, Object>() {
            {
                putAll(coreContext);                
            }            
        };      
        
        predicateContext = new LinkedHashMap<String, Object>() {
            {
                putAll(coreContext);
                putAll(conceptContext);
                put("range", new LinkedHashMap<String, Object>() {
                    {
                        put("@id", "http://www.w3.org/2000/01/rdf-schema#range");
                        put("@type", "@id");
                    }
                });    
                put("subPropertyOf", new LinkedHashMap<String, Object>() {
                    {
                        put("@id", "http://www.w3.org/2000/01/rdf-schema#subPropertyOf");
                        put("@type", "@id");
                    }
                });                 
                put("equivalentProperty", new LinkedHashMap<String, Object>() {
                    {
                        put("@id", "http://www.w3.org/2002/07/owl#equivalentProperty");
                        put("@type", "@id");
                    }
                });                 
                put("datatype", new LinkedHashMap<String, Object>() {
                    {
                        put("@id", "http://www.w3.org/ns/shacl#datatype");
                        put("@type", "@id");
                    }
                });                 
                put("subject", subject);
            }            
        };           
      
        propertyContext = new LinkedHashMap<String, Object>() {
            {
                putAll(coreContext);
                putAll(predicateContext);
                putAll(referenceDataContext);
                put("order", new LinkedHashMap<String, Object>() {
                    {
                        put("@id", "http://www.w3.org/ns/shacl#order");
                    }
                });     
                put("example", new LinkedHashMap<String, Object>() {
                    {
                        put("@id", "http://www.w3.org/2004/02/skos/core#example");
                    }
                });                
                put("defaultValue", new LinkedHashMap<String, Object>() {
                    {
                        put("@id", "http://www.w3.org/ns/shacl#defaultValue");
                    }
                });                
                put("maxCount", new LinkedHashMap<String, Object>() {
                    {
                        put("@id", "http://www.w3.org/ns/shacl#maxCount");
                    }
                });                
                put("minCount", new LinkedHashMap<String, Object>() {
                    {
                        put("@id", "http://www.w3.org/ns/shacl#minCount");
                    }
                });                
                put("maxLength", new LinkedHashMap<String, Object>() {
                    {
                        put("@id", "http://www.w3.org/ns/shacl#maxLength");
                    }
                });                
                put("minLength", new LinkedHashMap<String, Object>() {
                    {
                        put("@id", "http://www.w3.org/ns/shacl#minLength");
                    }
                });                
                put("inValues", new LinkedHashMap<String, Object>() {
                    {
                        put("@id", "http://www.w3.org/ns/shacl#in");
                        put("@container", "@list");
                    }
                });                
                put("hasValue", new LinkedHashMap<String, Object>() {
                    {
                        put("@id", "http://www.w3.org/ns/shacl#hasValue");                        
                    }
                });                
                put("pattern", new LinkedHashMap<String, Object>() {
                    {
                        put("@id", "http://www.w3.org/ns/shacl#pattern");                        
                    }
                });                 
                put("type", new LinkedHashMap<String, Object>() {
                    {
                        put("@id", "http://purl.org/dc/terms/type");
                        put("@type", "@id");
                    }
                });
                put("node", new LinkedHashMap<String, Object>() {
                    {
                        put("@id", "http://www.w3.org/ns/shacl#node");
                        put("@type", "@id");
                    }
                });
                put("path", path);
                put("classIn", new LinkedHashMap<String, Object>() {
                    {
                        put("@id", "http://www.w3.org/ns/shacl#classIn");
                        put("@type", "@id");
                    }
                });                
                put("memberOf", new LinkedHashMap<String, Object>() {
                    {
                        put("@id", "http://purl.org/dc/dcam/memberOf");                        
                    }
                });                
                put("stem", new LinkedHashMap<String, Object>() {
                    {
                        put("@id", "http://www.w3.org/ns/shacl#stem");  
                        put("@type", "@id");
                    }
                }); 
                put("languageIn", new LinkedHashMap<String, Object>() {
                    {
                        put("@id", "http://www.w3.org/ns/shacl#languageIn");
                        put("@container", "@list");
                    }
                });                  
                put("isResourceIdentifier", new LinkedHashMap<String, Object>() {
                    {
                        put("@id", "http://uri.suomi.fi/datamodel/ns/iow#isResourceIdentifier");                        
                    }
                });  
                put("uniqueLang", new LinkedHashMap<String, Object>() {
                    {
                        put("@id", "http://www.w3.org/ns/shacl#uniqueLang");                        
                    }
                });  
                put("isXmlWrapper", new LinkedHashMap<String, Object>() {
                    {
                        put("@id", "http://uri.suomi.fi/datamodel/ns/iow#isXmlWrapper");                        
                    }
                });  
                put("isXmlAttribute", new LinkedHashMap<String, Object>() {
                    {
                        put("@id", "http://uri.suomi.fi/datamodel/ns/iow#isXmlAttribute");                        
                    }
                });  
                put("name", new LinkedHashMap<String, Object>() {
                    {
                        put("@id", "http://www.w3.org/ns/shacl#name");
                        put("@container", "@language"); 
                    }
                });                
                put("description", new LinkedHashMap<String, Object>() {
                    {
                        put("@id", "http://www.w3.org/ns/shacl#description");
                        put("@container", "@language"); 
                    }
                }); 
                put("readOnlyValue", new LinkedHashMap<String, Object>() {
                    {
                        put("@id", "http://schema.org/readonlyValue");
                    }
                });                

            }
        };
        
        classContext = new LinkedHashMap<String, Object>() {
            {
                putAll(coreContext);
                putAll(propertyContext);
                putAll(conceptContext);
                put("abstract", new LinkedHashMap<String, Object>() {
                    {
                        put("@id", "http://www.w3.org/ns/shacl#abstract");
                    }
                });
                put("property", property);
                put("targetClass", new LinkedHashMap<String, Object>() {
                    {
                        put("@id", "http://www.w3.org/ns/shacl#targetClass");
                        put("@type", "@id");
                    }
                });
                put("subClassOf", new LinkedHashMap<String, Object>() {
                    {
                        put("@id", "http://www.w3.org/2000/01/rdf-schema#subClassOf");
                        put("@type", "@id");
                    }
                });                
                put("equivalentClass", new LinkedHashMap<String, Object>() {
                    {
                        put("@id", "http://www.w3.org/2002/07/owl#equivalentClass");
                        put("@type", "@id");
                    }
                });
                put("constraint", new LinkedHashMap<String, Object>() {
                    {
                        put("@id", "http://www.w3.org/ns/shacl#constraint");
                        put("@type", "@id");
                    }
                });
                put("orCond", new LinkedHashMap<String, Object>() {
                    {
                        put("@id", "http://www.w3.org/ns/shacl#or");
                        put("@container", "@list");
                    }
                });
                put("andCond", new LinkedHashMap<String, Object>() {
                    {
                        put("@id", "http://www.w3.org/ns/shacl#and");
                        put("@container", "@list");
                    }
                });               
                put("notCond", new LinkedHashMap<String, Object>() {
                    {
                        put("@id", "http://www.w3.org/ns/shacl#not");
                        put("@container", "@list");
                    }
                });
                put("minProperties", new LinkedHashMap<String, Object>() {
                    {
                        put("@id", "http://uri.suomi.fi/datamodel/ns/iow#minProperties");
                    }
                });
                put("maxProperties", new LinkedHashMap<String, Object>() {
                    {
                        put("@id", "http://uri.suomi.fi/datamodel/ns/iow#maxProperties");
                    }
                });
                put("subject", subject);
            }
        };

        namespaceContext = new LinkedHashMap<String, Object>() {
            {
                putAll(coreContext);
                put("preferredXMLNamespaceName", new LinkedHashMap<String, Object>() {
                    {
                        put("@id", "http://purl.org/ws-mmi-dc/terms/preferredXMLNamespaceName");
                    }
                });               
                put("preferredXMLNamespacePrefix", new LinkedHashMap<String, Object>() {
                    {
                        put("@id", "http://purl.org/ws-mmi-dc/terms/preferredXMLNamespacePrefix");                        
                    }
                });               
            }            
        };        

        modelContext = new LinkedHashMap<String, Object>() {
            {
                putAll(coreContext);
                putAll(namespaceContext);
                putAll(referenceDataContext);
                putAll(vocabularyContext);
                put("rootResource", new LinkedHashMap<String, Object>() {
                    {
                        put("@id", "http://rdfs.org/ns/void#rootResource");
                        put("@type", "@id");
                    }
                });               
                put("references", new LinkedHashMap<String, Object>() {
                    {
                        put("@id", "http://purl.org/dc/terms/references");
                        put("@type", "@id");
                    }
                });               
                put("requires", new LinkedHashMap<String, Object>() {
                    {
                        put("@id", "http://purl.org/dc/terms/requires");
                        put("@type", "@id");
                    }
                });               
                put("relations", new LinkedHashMap<String, Object>() {
                    {
                        put("@id", "http://purl.org/dc/terms/relation");
                        put("@container", "@list");
                    }
                });               
                put("codeLists", new LinkedHashMap<String, Object>() {
                    {
                        put("@id", "http://uri.suomi.fi/datamodel/ns/iow#codeLists");
                        put("@type", "@id");
                    }
                });               
                put("language", new LinkedHashMap<String, Object>() {
                    {
                        put("@id", "http://purl.org/dc/terms/language");
                        put("@container", "@list");
                    }
                });                  
            }            
        };
        
        modelPositionContext = new LinkedHashMap<String, Object>() {
            {
                putAll(coreContext);
                put("path", new LinkedHashMap<String, Object>() {
                    {
                        put("@id", "http://www.w3.org/ns/shacl#path");
                        put("@type", "@id");
                    }
                });     
                
                put("property", new LinkedHashMap<String, Object>() {
                    {
                        put("@id", "http://www.w3.org/ns/shacl#property");
                        put("@type", "@id");
                    }
                });               
                put("pointXY", new LinkedHashMap<String, Object>() {
                    {
                        put("@id", "http://uri.suomi.fi/datamodel/ns/iow#pointXY");                        
                    }
                });               
                put("vertexXY", new LinkedHashMap<String, Object>() {
                    {
                        put("@id", "http://uri.suomi.fi/datamodel/ns/iow#vertexXY");
                        put("@container", "@list");
                    }
                });            
            }            
        };

        conceptFrame = new LinkedHashMap<String, Object>() {
            {
                put("@context", conceptContext);
            }
        };

        classVisualizationFrame = new LinkedHashMap<String, Object>() {
            {
                put("@context", classContext);                
                put("@type", new ArrayList<Object>() {
                    {
                        add("rdfs:Class");
                        add("sh:NodeShape");
                    }
                });

                put("property", new LinkedHashMap<String, Object>() {
                    {
                        put("path", new LinkedHashMap<String, Object>() {
                            {
                                put("@embed", false);
                            }
                        });
                        put("node", new LinkedHashMap<String, Object>() {
                            {
                                put("@omitDefault", true);
                                put("@default", new ArrayList());
                                put("@embed", false);
                            }
                        });
                        put("classIn", new LinkedHashMap<String, Object>() {
                            {
                                put("@omitDefault", true);
                                put("@default", new ArrayList());
                                put("@embed", false);
                            }
                        });      
                        put("memberOf", new LinkedHashMap<String, Object>() {
                            {
                                put("@omitDefault", true);
                                put("@default", new ArrayList());
                                put("isPartOf", new LinkedHashMap<String, Object>() {
                                    {
                                        put("@embed", "@always");
                                    }
                                });
                                
                            }
                        });                          
                    }
                });               
                put("subject", new LinkedHashMap<String, Object>() {
                    {
                        put("@embed", false);
                    }
                });               
                put("subClassOf", new LinkedHashMap<String, Object>() {
                    {
                        put("@embed", false);
                    }
                });               
                put("targetClass", new LinkedHashMap<String, Object>() {
                    {
                        put("@embed", false);
                    }
                });               
                put("isDefinedBy", new LinkedHashMap<String, Object>() {
                    {
                        put("@embed", false);
                    }
                });               
            }            
        };        
 
    }


}
