package fi.vm.yti.datamodel.api.v2.service;

import fi.vm.yti.datamodel.api.v2.dto.ModelConstants;
import fi.vm.yti.datamodel.api.v2.dto.PropertyShapeInfoDTO;
import fi.vm.yti.datamodel.api.v2.mapper.MapperUtils;
import fi.vm.yti.datamodel.api.v2.mapper.ResourceMapper;
import fi.vm.yti.datamodel.api.v2.properties.SuomiMeta;
import org.apache.commons.lang3.math.NumberUtils;
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
import java.util.*;

public class JSONSchemaBuilder {

    private static final Logger logger = LoggerFactory.getLogger(JSONSchemaBuilder.class);

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

            var minCount = MapperUtils.getLiteral(propertyResource, SH.minCount, Integer.class);

            if (minCount != null && minCount > 0) {
                schemaBuilder.addRequiredProperty(propertyResource.getLocalName());
            }

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
        var minCount = MapperUtils.getLiteral(propertyResource, SH.minCount, Integer.class);
        var shClass = propertyResource.getProperty(SH.class_);

        if (shClass != null) {
            var target = model.listSubjectsWithProperty(SH.targetClass, shClass.getObject());
            targetClass = target.hasNext() ? target.next() : null;
        }

        var subjectBuilder = ReferenceSchema.builder();
        if (targetClass != null) {
            subjectBuilder.refValue(getRef(targetClass));
        }

        var subject = subjectBuilder
                .title(title)
                .description(description)
                .build();
        subject.setReferredSchema(EmptySchema.builder().build());

        if ((maxCount != null && maxCount > 1) || (minCount != null && minCount > 1)) {
            return ArraySchema.builder()
                    .addItemSchema(subject)
                    .description(description)
                    .title(title)
                    .build();
        } else {
            return subject;
        }
    }

    private static Schema handleAttribute(
            Resource propertyResource,
            String language) {
        var description = getLocalizedValue(propertyResource, RDFS.comment, language);
        var title = getLocalizedValue(propertyResource, RDFS.label, language);
        var minCount = MapperUtils.getLiteral(propertyResource, SH.minCount, Integer.class);
        var maxCount = MapperUtils.getLiteral(propertyResource, SH.maxCount, Integer.class);
        var allowed = MapperUtils.arrayPropertyToList(propertyResource, SH.in);
        var requiredValue = MapperUtils.propertyToString(propertyResource, SH.hasValue);

        var type = getDatatype(MapperUtils.propertyToString(propertyResource, SH.datatype));

        Schema.Builder<? extends Schema> builder;
        if (requiredValue != null) {
            builder = ConstSchema.builder().permittedValue(requiredValue);
        } else if (!allowed.isEmpty()) {
            builder = EnumSchema.builder().possibleValues(new ArrayList<>(allowed));
        } else if (type == SchemaType.NUMBER || type == SchemaType.INTEGER) {
            builder = NumberSchema.builder();
        } else if (type == SchemaType.STRING) {
            builder = StringSchema.builder();
        } else if (type == SchemaType.BOOLEAN) {
            builder = BooleanSchema.builder();
        } else {
            builder = ObjectSchema.builder();
        }

        addRestrictions(builder, propertyResource);

        // handle property as an array if either minCount or maxCount > 1
        if ((minCount != null && minCount > 1) || (maxCount != null && maxCount > 1)) {
            var arraySchemaBuilder = ArraySchema.builder()
                    .allItemSchema(builder.build());

            if (minCount != null) {
                arraySchemaBuilder.minItems(minCount);
            }
            if (maxCount != null) {
                arraySchemaBuilder.maxItems(maxCount);
            }
            builder = arraySchemaBuilder;
        }

        builder
                .description(description)
                .title(title);
        return builder.build();
    }

    private static void addRestrictions(Schema.Builder<? extends Schema> builder, Resource propertyResource) {
        var dto = new PropertyShapeInfoDTO();
        ResourceMapper.mapPropertyShapeRestrictions(dto, propertyResource);

        if (builder instanceof StringSchema.Builder stringBuilder) {
            if (dto.getPattern() != null) {
                stringBuilder.pattern(dto.getPattern());
            }
            if (dto.getMinLength() != null) {
                stringBuilder.minLength(dto.getMinLength());
            }
            if (dto.getMaxLength() != null) {
                stringBuilder.maxLength(dto.getMaxLength());
            }
            if (dto.getDefaultValue() != null) {
                stringBuilder.defaultValue(MapperUtils.propertyToString(propertyResource, SH.defaultValue));
            }
        } else if (builder instanceof NumberSchema.Builder numberSchema) {
            // x >= minInclusive
            if (dto.getMinInclusive() != null) {
                numberSchema.minimum(dto.getMinInclusive());
                numberSchema.exclusiveMinimum(false);
            }
            // x <= maxInclusive
            if (dto.getMaxInclusive() != null) {
                numberSchema.maximum(dto.getMaxInclusive());
                numberSchema.exclusiveMaximum(false);
            }
            // x > minExclusive
            if (dto.getMinExclusive() != null) {
                numberSchema.minimum(dto.getMinExclusive());
                numberSchema.exclusiveMinimum(true);
            }
            // x < maxExclusive
            if (dto.getMaxExclusive() != null) {
                numberSchema.maximum(dto.getMaxExclusive());
                numberSchema.exclusiveMaximum(true);
            }

            if (dto.getDefaultValue() != null) {
                try {
                    numberSchema.defaultValue(NumberUtils.createNumber(dto.getDefaultValue()));
                } catch (NumberFormatException nfe) {
                    logger.warn("Invalid number as a default value: {}, uri: {}", dto.getDefaultValue(), propertyResource.getURI());
                }
            }
        } else if (dto.getDefaultValue() != null) {
            builder.defaultValue(dto.getDefaultValue());
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
        return "#/properties/" + pathResource.getLocalName();
    }

    private static SchemaType getDatatype(String typeURI) {
        if (typeURI == null) {
            return SchemaType.UNKNOWN;
        }
        return TYPE_MAP.getOrDefault(typeURI, SchemaType.STRING);
    }
}
