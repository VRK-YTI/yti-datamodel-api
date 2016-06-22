/*
 * Licensed under the European Union Public Licence (EUPL) V.1.1 
 */
package com.csc.fi.ioapi.utils;
import com.predic8.schema.*;
import static com.predic8.schema.Schema.*;
import javax.xml.namespace.QName;
 
public class SchemaUtil {
 
  public static void main(String[] args) {
 
    Schema schema = new Schema("http://predic8.com/schema/person/");
     
    schema.newElement("person", "personType");
    
    ComplexType personType = schema.newComplexType("personType");
    personType.newAttribute("id", INT);
    Sequence seq = personType.newSequence();
    
    seq.newElement("name", new QName("http://www.w3.org/2001/XMLSchema","duration"));
    seq.newElement("lastname", STRING);
    seq.newElement("date-of-birth", DATE);
    seq.newElement("address").newComplexType().newSequence().newElement("country", STRING);
    seq.newElement("diipadaa").setType(new groovy.xml.QName("http://predic8.com/schema/person/","personType"));
     
    
    
    
    System.out.println(schema.getAsString());
  }
  
  
}
    
    