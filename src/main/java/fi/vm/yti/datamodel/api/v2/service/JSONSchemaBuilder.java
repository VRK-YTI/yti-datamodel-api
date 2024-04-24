package fi.vm.yti.datamodel.api.v2.service;

import fi.vm.yti.datamodel.api.v2.dto.ModelConstants;
import fi.vm.yti.datamodel.api.v2.mapper.ClassMapper;
import fi.vm.yti.datamodel.api.v2.mapper.MapperUtils;
import fi.vm.yti.datamodel.api.v2.properties.SuomiMeta;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.vocabulary.OWL;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;
import org.everit.json.schema.*;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.topbraid.shacl.vocabulary.SH;

import java.io.StringWriter;
import java.util.Map;

public class JSONSchemaBuilder {

    private static final Logger logger = LoggerFactory.getLogger(ClassMapper.class);

    private enum SchemaType {
        UNKNOWN,
        BOOLEAN,
        NUMBER,
        INTEGER,
        STRING
    }

    private static final Map<String, SchemaType> TYPE_MAP = Map.ofEntries(
            Map.entry("http://www.w3.org/2001/XMLSchema#boolean", SchemaType.BOOLEAN),
            Map.entry("http://www.w3.org/2002/07/owl#real", SchemaType.NUMBER),
            Map.entry("http://www.w3.org/2001/XMLSchema#double", SchemaType.NUMBER),
            Map.entry("http://www.w3.org/2001/XMLSchema#float", SchemaType.NUMBER),
            Map.entry("http://www.w3.org/2001/XMLSchema#decimal", SchemaType.NUMBER),
            Map.entry("http://www.w3.org/2002/07/owl#rational", SchemaType.INTEGER),
            Map.entry("http://www.w3.org/2001/XMLSchema#byte", SchemaType.INTEGER),
            Map.entry("http://www.w3.org/2001/XMLSchema#int", SchemaType.INTEGER),
            Map.entry("http://www.w3.org/2001/XMLSchema#integer", SchemaType.INTEGER),
            Map.entry("http://www.w3.org/2001/XMLSchema#long", SchemaType.INTEGER),
            Map.entry("http://www.w3.org/2001/XMLSchema#negativeInteger", SchemaType.INTEGER),
            Map.entry("http://www.w3.org/2001/XMLSchema#nonNegativeInteger", SchemaType.INTEGER),
            Map.entry("http://www.w3.org/2001/XMLSchema#nonPositiveInteger", SchemaType.INTEGER),
            Map.entry("http://www.w3.org/2001/XMLSchema#positiveInteger", SchemaType.INTEGER),
            Map.entry("http://www.w3.org/2001/XMLSchema#short", SchemaType.INTEGER),
            Map.entry("http://www.w3.org/2001/XMLSchema#unsignedByte", SchemaType.INTEGER),
            Map.entry("http://www.w3.org/2001/XMLSchema#unsignedInt", SchemaType.INTEGER),
            Map.entry("http://www.w3.org/2001/XMLSchema#unsignedLong", SchemaType.INTEGER),
            Map.entry("http://www.w3.org/2001/XMLSchema#unsignedShort", SchemaType.INTEGER)
    );

    private JSONSchemaBuilder() {
    }

    public static void export(StringWriter stringWriter, Model model, String language) {
        var modelSubj = model.listSubjectsWithProperty(RDF.type, SuomiMeta.ApplicationProfile);
        if (!modelSubj.hasNext()) {
            logger.error("No model found");
            return;
        }

        var modelResource = model.getResource(modelSubj.next().getURI());

        final var lang = language == null
                ? ModelConstants.DEFAULT_LANGUAGE
                : language;

        var schemaBuilder = ObjectSchema.builder();
        schemaBuilder.description(getLocalizedValue(modelResource, RDFS.comment, language));
        schemaBuilder.title(getLocalizedValue(modelResource, RDFS.label, language));

        var nodeShapes = model.listSubjectsWithProperty(RDF.type, SH.NodeShape);
        nodeShapes.forEach(nodeShape -> {
            var schema = getNodeShapeSchema(nodeShape, model, lang);
            schemaBuilder.addPropertySchema(nodeShape.getLocalName(), schema);
        });

        var jsonSchema = new JSONObject(schemaBuilder.build().toString());
        stringWriter.write(jsonSchema.toString(4));
    }

    private static ObjectSchema getNodeShapeSchema(Resource nodeShape, Model model, String language) {
        var schemaBuilder = ObjectSchema.builder();
        schemaBuilder.description(getLocalizedValue(nodeShape, RDFS.comment, language));
        schemaBuilder.title(getLocalizedValue(nodeShape, RDFS.label, language));

        handlePropertyShapes(schemaBuilder, nodeShape, model, language);
        return schemaBuilder.build();
    }

    private static void handlePropertyShapes(
            ObjectSchema.Builder schemaBuilder,
            Resource nodeShape,
            Model model,
            String language) {

        var deactivated = model.listSubjectsWithProperty(SH.deactivated)
                .mapWith(Resource::getURI)
                .toList();

        nodeShape.listProperties(SH.property).forEach(p -> {
            var propertyURI = p.getObject().toString();
            var propertyResource = model.getResource(propertyURI);

            if (deactivated.contains(propertyURI) ||
                    !propertyResource.listProperties().hasNext()) {
                return;
            }

            var propertySchema = MapperUtils.hasType(propertyResource, OWL.DatatypeProperty)
                    ? handleAttribute(propertyResource, language)
                    : handleAssociation(model, propertyResource, language);

            schemaBuilder.addPropertySchema(
                    propertyResource.getLocalName(),
                    propertySchema);
        });
    }

    private static Schema handleAssociation(
            Model model,
            Resource propertyResource,
            String language) {

        var description = getLocalizedValue(propertyResource, RDFS.comment, language);
        var title = getLocalizedValue(propertyResource, RDFS.label, language);

        Resource targetClass = null;
        var maxCount = MapperUtils.getLiteral(propertyResource, SH.maxCount, Integer.class);
        var shClass = MapperUtils.propertyToString(propertyResource, SH.class_);

        if (shClass != null) {
            var target = model.listSubjectsWithProperty(SH.targetClass, shClass);
            targetClass = target.hasNext() ? target.next() : null;
        }

        BooleanSchema referredSchema = BooleanSchema.builder().build();

        ReferenceSchema subject = ReferenceSchema.builder()
                .refValue(getRef(targetClass))
                .build();
        subject.setReferredSchema(EmptySchema.builder().build());

        if (maxCount != null && maxCount > 0) {
            ArraySchema schema = ArraySchema.builder()
                    .addItemSchema(subject)
                    .description(description)
                    .title(title)
                    .build();
            return schema;
        } else {
            return subject;
        }
    }

    private static Schema handleAttribute(
            Resource propertyResource,
            String language) {
        var description = getLocalizedValue(propertyResource, RDFS.comment, language);
        var title = getLocalizedValue(propertyResource, RDFS.label, language);
        var type = getDatatype(MapperUtils.propertyToString(propertyResource, SH.datatype));
        switch (type) {
            case NUMBER, INTEGER:
                return NumberSchema.builder()
                        .description(description)
                        .title(title)
                        .build();
            case STRING:
                return StringSchema.builder()
                        .description(description)
                        .title(title)
                        .build();
            case BOOLEAN:
                return BooleanSchema.builder()
                        .description(description)
                        .title(title)
                        .build();
            default:
                return ObjectSchema.builder()
                        .description(description)
                        .title(title)
                        .build();
        }
    }

    private static String getLocalizedValue(Resource resource, Property property, String language) {
        var value = MapperUtils.localizedPropertyToMap(resource, property);
        if (value.isEmpty()) {
            return "";
        }
        var key = value.containsKey(language) ? language : value.keySet().iterator().next();
        return value.get(key);
    }

    private static String getRef(Resource pathResource) {
        return "/properties/" + pathResource.getLocalName();
    }

    private static SchemaType getDatatype(String typeURI) {
        if (typeURI == null) {
            return SchemaType.UNKNOWN;
        }
        return TYPE_MAP.getOrDefault(typeURI, SchemaType.STRING);
    }
}
