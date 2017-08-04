/*
 * Licensed under the European Union Public Licence (EUPL) V.1.1 
 */
package com.csc.fi.ioapi.utils;

import org.jboss.forge.roaster.Roaster;
import org.jboss.forge.roaster.model.source.AnnotationElementSource;
import org.jboss.forge.roaster.model.source.AnnotationSource;
import org.jboss.forge.roaster.model.source.JavaAnnotationSource;
import org.jboss.forge.roaster.model.source.JavaInterfaceSource;

/**
 *
 * @author malonen
 */
@Deprecated
public class JavaInterfaceWriter {
    
    public static void main(String[] args) {
        
    /* TODO: Example of automatically generated Java interface */
    
     // https://github.com/forge/roaster
    JavaInterfaceSource ti = Roaster.create( JavaInterfaceSource.class );
    ti.setName("Test");    
    ti.addAnnotation().setName("Shape").setLiteralValue("uri","\"http://ecaxa.org/Class\"");
    
    ti.addMethod("public String getId()").addAnnotation().setName("ID");
    
    System.out.println(ti);
        
        
    }
    

    
}
