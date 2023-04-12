package fi.vm.yti.datamodel.api.v2.utils;

import org.apache.jena.arq.querybuilder.ConstructBuilder;
import org.apache.jena.rdf.model.Property;

public class SparqlUtils {


    private SparqlUtils(){
        //Util class
    }

    public static void addConstructProperty(String graphVariable, ConstructBuilder builder, Property property, String propertyName) {
        builder.addConstruct(graphVariable, property, propertyName)
                .addWhere(graphVariable, property, propertyName);
    }

    public static void addConstructOptional(String graphVariable, ConstructBuilder builder, Property property, String propertyName) {
        builder.addConstruct(graphVariable, property, propertyName)
                .addOptional(graphVariable, property, propertyName);
    }
}

