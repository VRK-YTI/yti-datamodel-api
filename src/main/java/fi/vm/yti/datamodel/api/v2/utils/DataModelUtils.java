package fi.vm.yti.datamodel.api.v2.utils;

import fi.vm.yti.common.Constants;
import fi.vm.yti.common.util.MapperUtils;
import fi.vm.yti.datamodel.api.v2.dto.ModelConstants;
import fi.vm.yti.datamodel.api.v2.properties.DCAP;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
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

        var dataModelURI = DataModelURI.Factory.fromURI(modelURI);
        model.setNsPrefix(dataModelURI.getModelId(), dataModelURI.getNamespace());

        var modelResource = model.getResource(dataModelURI.getModelURI());

        List.of(OWL.imports, DCTerms.requires).forEach(property ->
                modelResource.listProperties(property).forEach(res -> {
                    var uri = DataModelURI.Factory.fromURI(res.getObject().toString());
                    if (uri.isDataModelURI()) {
                        model.setNsPrefix(uri.getModelId(), uri.getGraphURI());
                    } else if (!uri.isTerminologyURI() && !uri.isCodeListURI()) {
                        // handle external namespaces, skip for now terminologies and code lists
                        var extResource = model.getResource(uri.getNamespace());
                        var extPrefix = MapperUtils.propertyToString(extResource, DCAP.preferredXMLNamespacePrefix);
                        if (extPrefix != null) {
                            model.setNsPrefix(extPrefix, uri.getNamespace());
                        }
                    }
                })
        );
    }

    public static Set<String> getInternalReferenceModels(Resource modelResource) {
        return getInternalReferenceModels(null, modelResource);
    }

    public static Set<String> getInternalReferenceModels(String versionUri, Resource modelResource) {
        var includedNamespaces = MapperUtils.arrayPropertyToSet(modelResource, OWL.imports);
        includedNamespaces.addAll(MapperUtils.arrayPropertyToSet(modelResource, DCTerms.requires));

        if (versionUri != null) {
            includedNamespaces.add(versionUri);
        }

        return includedNamespaces.stream()
                .filter(ns -> ns.startsWith(Constants.DATA_MODEL_NAMESPACE))
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
        } else if (!uri.startsWith(Constants.DATA_MODEL_NAMESPACE)) {
            return uri;
        }
        return uri.replaceAll("/[\\d.]+(.*)/", "/");
    }

    /**
     * Recursively find subject for object in case it is anonymous resource (e.g. a part of RDF list)
     * @param statement statement
     * @param model model
     * @return subject for object
     */
    public static Resource findSubjectForObject(Statement statement, Model model) {
        var subj = statement.getSubject();
        if (subj.isAnon()) {
            while (subj.isAnon()) {
                var stmt = model.listStatements(null, null, subj).next();
                subj = stmt.getSubject();
            }
        }
        return subj;
    }
}
