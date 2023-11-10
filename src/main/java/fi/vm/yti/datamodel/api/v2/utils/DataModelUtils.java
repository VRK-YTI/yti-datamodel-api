package fi.vm.yti.datamodel.api.v2.utils;

import fi.vm.yti.datamodel.api.v2.dto.ModelConstants;
import fi.vm.yti.datamodel.api.v2.mapper.MapperUtils;
import fi.vm.yti.datamodel.api.v2.properties.DCAP;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.vocabulary.DCTerms;
import org.apache.jena.vocabulary.OWL;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class DataModelUtils {

    private DataModelUtils() {
        //Util class
    }

    public static String encode(String param) {
        return URLEncoder.encode(param, StandardCharsets.UTF_8);
    }

    public static void addPrefixesToModel(String modelURI, Model model) {
        model.clearNsPrefixMap();
        model.setNsPrefixes(ModelConstants.PREFIXES);
        model.setNsPrefix(MapperUtils.getModelIdFromNamespace(modelURI), modelURI + ModelConstants.RESOURCE_SEPARATOR);

        var modelResource = model.getResource(modelURI);

        List.of(OWL.imports, DCTerms.requires).forEach(property ->
                modelResource.listProperties(property).forEach(res -> {
                    var uri = res.getObject().toString();
                    if (uri.startsWith(ModelConstants.SUOMI_FI_NAMESPACE)) {
                        var prefix = MapperUtils.getModelIdFromNamespace(uri);
                        model.setNsPrefix(prefix, uri + ModelConstants.RESOURCE_SEPARATOR);
                    } else if (!uri.contains(ModelConstants.SUOMI_FI_DOMAIN)) {
                        // handle external namespaces, skip for now terminologies and code lists
                        var extResource = model.getResource(uri);
                        var extPrefix = MapperUtils.propertyToString(extResource, DCAP.preferredXMLNamespacePrefix);
                        if (extPrefix != null) {
                            model.setNsPrefix(extPrefix, uri);
                        }
                    }
                })
        );
    }

    public static Set<String> getInternalReferenceModels(String versionUri, Resource modelResource) {
        var includedNamespaces = MapperUtils.arrayPropertyToSet(modelResource, OWL.imports);
        includedNamespaces.addAll(MapperUtils.arrayPropertyToSet(modelResource, DCTerms.requires));
        includedNamespaces.add(versionUri);

        return includedNamespaces.stream()
                .filter(ns -> ns.startsWith(ModelConstants.SUOMI_FI_NAMESPACE))
                .collect(Collectors.toSet());
    }

    public static String removeTrailingSlash(String uri) {
        if (uri == null) {
            return null;
        }
        return uri.replaceAll("/?$", "");
    }

    public static String removeVersionFromURI(String uri) {
        if (uri == null) {
            return null;
        }
        return uri.replaceAll("/[\\d.]+(.*)/", "/");
    }
}
