package fi.vm.yti.datamodel.api.v2.utils;

import org.apache.jena.arq.querybuilder.ConstructBuilder;
import org.apache.jena.arq.querybuilder.WhereBuilder;
import org.apache.jena.rdf.model.Property;

import java.util.UUID;

public class SparqlUtils {


    private SparqlUtils(){
        //Util class
    }

    public static void addRequiredToGraphConstruct(String subjectVariable, ConstructBuilder builder, WhereBuilder whereBuilder,  Property property){
        var propertyName = "?" + UUID.randomUUID().toString().substring(0, 7);
        builder.addConstruct("?g", property, propertyName);
        whereBuilder.addWhere(subjectVariable, property, propertyName);
    }

    public static void addOptionalToGraphConstruct(String subjectVariable, ConstructBuilder builder, WhereBuilder whereBuilder,  Property property){
        var propertyName = "?" + UUID.randomUUID().toString().substring(0, 7);
        builder.addConstruct("?g", property, propertyName);
        whereBuilder.addOptional(subjectVariable, property, propertyName);
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

