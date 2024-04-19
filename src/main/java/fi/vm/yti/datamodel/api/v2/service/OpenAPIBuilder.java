package fi.vm.yti.datamodel.api.v2.service;

import fi.vm.yti.datamodel.api.v2.dto.ModelConstants;
import fi.vm.yti.datamodel.api.v2.mapper.MapperUtils;
import fi.vm.yti.datamodel.api.v2.properties.DCAP;
import fi.vm.yti.datamodel.api.v2.properties.HTTP;
import fi.vm.yti.datamodel.api.v2.properties.SuomiMeta;
import fi.vm.yti.datamodel.api.v2.utils.DataModelURI;
import io.swagger.v3.core.util.Json;
import io.swagger.v3.oas.models.*;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.parameters.RequestBody;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.vocabulary.*;
import org.springframework.web.util.UriComponentsBuilder;
import org.topbraid.shacl.vocabulary.SH;

import java.io.StringWriter;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class OpenAPIBuilder {

    private OpenAPIBuilder() {
        // static methods
    }

    private static final String RESPONSE_DESCRIPTION = "Successful operation";
    private static final String OBJECT = "object";
    private static final String ARRAY = "array";
    private static final String NUMBER = "number";
    private static final String INTEGER = "integer";
    private static final String BOOLEAN = "boolean";
    private static final String STRING = "string";

    private static final String OPEN_API_VERSION = "3.1.0";
    private static final Pattern pathVariableRegex = Pattern.compile("\\{(\\w+)\\}");
    private static final List<PathItem.HttpMethod> methods = Arrays.asList(
            PathItem.HttpMethod.GET,
            PathItem.HttpMethod.POST,
            PathItem.HttpMethod.PUT,
            PathItem.HttpMethod.DELETE
    );
    private static final Map<String, String> TYPE_MAP = Map.ofEntries(
            Map.entry("http://www.w3.org/2001/XMLSchema#boolean", BOOLEAN),
            Map.entry("http://www.w3.org/2002/07/owl#real", NUMBER),
            Map.entry("http://www.w3.org/2001/XMLSchema#double", NUMBER),
            Map.entry("http://www.w3.org/2001/XMLSchema#float", NUMBER),
            Map.entry("http://www.w3.org/2001/XMLSchema#decimal", NUMBER),
            Map.entry("http://www.w3.org/2002/07/owl#rational", INTEGER),
            Map.entry("http://www.w3.org/2001/XMLSchema#byte", INTEGER),
            Map.entry("http://www.w3.org/2001/XMLSchema#int", INTEGER),
            Map.entry("http://www.w3.org/2001/XMLSchema#integer", INTEGER),
            Map.entry("http://www.w3.org/2001/XMLSchema#long", INTEGER),
            Map.entry("http://www.w3.org/2001/XMLSchema#negativeInteger", INTEGER),
            Map.entry("http://www.w3.org/2001/XMLSchema#nonNegativeInteger", INTEGER),
            Map.entry("http://www.w3.org/2001/XMLSchema#nonPositiveInteger", INTEGER),
            Map.entry("http://www.w3.org/2001/XMLSchema#positiveInteger", INTEGER),
            Map.entry("http://www.w3.org/2001/XMLSchema#short", INTEGER),
            Map.entry("http://www.w3.org/2001/XMLSchema#unsignedByte", INTEGER),
            Map.entry("http://www.w3.org/2001/XMLSchema#unsignedInt", INTEGER),
            Map.entry("http://www.w3.org/2001/XMLSchema#unsignedLong", INTEGER),
            Map.entry("http://www.w3.org/2001/XMLSchema#unsignedShort", INTEGER)
    );

    public static void export(StringWriter stringWriter, Model model, String language) {
        var modelSubj = model.listSubjectsWithProperty(RDF.type, SuomiMeta.ApplicationProfile);
        if (!modelSubj.hasNext()) {
            return;
        }
        var openAPI = new OpenAPI();

        final var lang = language == null
                ? ModelConstants.DEFAULT_LANGUAGE
                : language;

        var nodeShapes = model.listSubjectsWithProperty(RDF.type, SH.NodeShape);
        var pathSubjects = model.listSubjectsWithProperty(HTTP.API_PATH);

        var modelResource = model.getResource(modelSubj.next().getURI());
        var prefix = MapperUtils.propertyToString(modelResource, DCAP.preferredXMLNamespacePrefix);
        var version = MapperUtils.propertyToString(modelResource, OWL.versionInfo);
        var modelURI = version != null
                ? MapperUtils.propertyToString(modelResource, OWL2.versionIRI)
                : modelResource.getURI();

        // metadata
        openAPI
                .openapi(OPEN_API_VERSION)
                .info(new Info()
                        .title(getLocalizedValue(modelResource, RDFS.label, language))
                        .description(getLocalizedValue(modelResource, RDFS.comment, language))
                        .version(version))
                .externalDocs(new ExternalDocumentation()
                        .url(modelURI));

        // /components/schema
        nodeShapes.forEach(nodeShape -> {
            var schema = getNodeShapeSchema(nodeShape, model, lang);
            schema.externalDocs(new ExternalDocumentation().url(
                    DataModelURI.createResourceURI(prefix, nodeShape.getLocalName(), version).getResourceVersionURI()
            ));
            openAPI.schema(nodeShape.getLocalName(), schema);
        });

        // /paths
        pathSubjects.forEach(pathResource -> {
            var path = MapperUtils.propertyToString(pathResource, HTTP.API_PATH);
            if (path == null || path.isBlank()) {
                return;
            }
            var pathItem = getPathItem(pathResource, lang);

            handleParameters(path, pathItem);
            openAPI.path(path.split("\\?")[0], pathItem);
        });

        stringWriter.write(Json.pretty(openAPI));
    }

    private static PathItem getPathItem(Resource pathResource, String language) {
        var pathItem = new PathItem();
        var content = new Content();
        content.put("application/json", new MediaType().schema(
                new Schema<>().$ref(getRef(pathResource)))
        );

        methods.forEach(method -> {
            var operation = new Operation()
                    .tags(List.of(getLocalizedValue(pathResource, RDFS.label, language)));

            var responses = new ApiResponses();
            var response = new ApiResponse().description(RESPONSE_DESCRIPTION);
            switch (method) {
                case GET -> {
                    response.content(content);
                    responses.addApiResponse("200", response);
                }
                case POST -> {
                    operation.requestBody(new RequestBody().content(content));
                    responses.addApiResponse("201", response);
                }
                case PUT -> {
                    operation.requestBody(new RequestBody().content(content));
                    responses.addApiResponse("204", response);
                }
                case DELETE -> responses.addApiResponse("204", response);
                default -> responses.addApiResponse("200", response);
            }
            operation.responses(responses);
            pathItem.operation(method, operation);
        });

        return pathItem;
    }

    private static void handleParameters(String path, PathItem pathItem) {
        Matcher matcher = pathVariableRegex.matcher(path);

        // path parameters
        while (matcher.find()) {
            pathItem.addParametersItem(new Parameter()
                    .name(matcher.group(1))
                    .in("path")
                    .required(true)
                    .schema(new Schema<String>().type("")));
        }

        // query parameters
        UriComponentsBuilder.fromUriString(path).build().getQueryParams().forEach((param, value) ->
                pathItem.addParametersItem(new Parameter()
                        .name(param)
                        .in("query")
                        .schema(new Schema<String>().type(""))));
    }

    private static Schema<?> getNodeShapeSchema(Resource nodeShape, Model model, String lang) {
        var schema = new Schema<>()
                .title(getLocalizedValue(nodeShape, RDFS.label, lang))
                .description(getLocalizedValue(nodeShape, RDFS.comment, lang))
                .type(OBJECT);

        handlePropertyShapes(schema, nodeShape, model, lang);
        handleRequired(schema, model, nodeShape);

        return schema;
    }

    private static void handleRequired(Schema<?> schema, Model model, Resource nodeShapeResource) {
        var required = nodeShapeResource.listProperties(SH.property)
                .mapWith(p -> model.getResource(p.getObject().toString()))
                .filterKeep(p -> p.hasProperty(SH.minCount)
                                 && p.getProperty(SH.minCount).getObject().asLiteral().getInt() > 0)
                .mapWith(Resource::getLocalName)
                .toList();

        if (!required.isEmpty()) {
            schema.required(required);
        }
    }

    private static void handlePropertyShapes(Schema<?> schema, Resource nodeShape, Model model, String lang) {
        var deactivated = model.listSubjectsWithProperty(SH.deactivated)
                .mapWith(Resource::getURI)
                .toList();

        nodeShape.listProperties(SH.property).forEach(p -> {
            var propertyURI = p.getObject().toString();
            var propertyResource = model.getResource(propertyURI);

            if (deactivated.contains(propertyURI) || !propertyResource.listProperties().hasNext()) {
                return;
            }
            var propertySchema = new Schema<>().title(getLocalizedValue(propertyResource, RDFS.label, lang));
            if (MapperUtils.hasType(propertyResource, OWL.DatatypeProperty)) {
                handleAttribute(propertySchema, propertyResource);
            } else {
                handleAssociation(propertySchema, model, propertyResource);
            }
            schema.addProperty(propertyResource.getLocalName(), propertySchema);
        });
    }

    private static void handleAssociation(Schema<?> schema, Model model, Resource propertyResource) {
        Resource targetClass = null;
        var maxCount = MapperUtils.getLiteral(propertyResource, SH.maxCount, Integer.class);
        var shClass = MapperUtils.propertyToString(propertyResource, SH.class_);

        if (shClass != null) {
            var target = model.listSubjectsWithProperty(SH.targetClass, shClass);
            targetClass = target.hasNext() ? target.next() : null;
        }

        if (maxCount != null && maxCount > 0) {
            schema.type(ARRAY);
            if (targetClass != null) {
                schema.items(new Schema<>().$ref(getRef(targetClass)));
            }
        } else if (targetClass != null) {
            schema.$ref(getRef(targetClass));
        }
    }

    private static void handleAttribute(Schema<String> schema, Resource propertyResource) {
        var maxCount = MapperUtils.getLiteral(propertyResource, SH.maxCount, Integer.class);
        var type = getDatatype(MapperUtils.propertyToString(propertyResource, SH.datatype));

        schema
                .minLength(MapperUtils.getLiteral(propertyResource, SH.minLength, Integer.class))
                .maxLength(MapperUtils.getLiteral(propertyResource, SH.maxLength, Integer.class));

        if (maxCount != null && maxCount > 0) {
            schema.type(ARRAY);
            schema.items(new Schema<>().type(type));
        } else {
            schema.type(type);
        }

        if (propertyResource.hasProperty(SH.in)) {
            schema._enum(MapperUtils.arrayPropertyToList(propertyResource, SH.in));
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
        return Components.COMPONENTS_SCHEMAS_REF + pathResource.getLocalName();
    }

    private static String getDatatype(String typeURI) {
        if (typeURI == null) {
            return "";
        }
        return TYPE_MAP.getOrDefault(typeURI, STRING);
    }
}
