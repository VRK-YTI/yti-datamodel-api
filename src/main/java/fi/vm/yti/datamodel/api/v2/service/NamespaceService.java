package fi.vm.yti.datamodel.api.v2.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class NamespaceService {

    private final NamespaceResolver namespaceResolver;
    @Value("${namespaces.resolveDefault:false}")
    private boolean resolveDefault;

    @Autowired
    public NamespaceService(NamespaceResolver namespaceResolver) {
        this.namespaceResolver = namespaceResolver;
    }

    private static final Map<String, String> DEFAULT_NAMESPACES = Map.ofEntries(
            Map.entry("oa", "http://www.w3.org/ns/oa#"),
            Map.entry("dcterms", "http://purl.org/dc/terms/"),
            Map.entry("skos", "http://www.w3.org/2004/02/skos/core#"),
            Map.entry("skosxl", "http://www.w3.org/2008/05/skos-xl#"),
            Map.entry("owl", "http://www.w3.org/2002/07/owl#"),
            Map.entry("prov", "http://www.w3.org/ns/prov#"),
            Map.entry("rdf", "http://www.w3.org/1999/02/22-rdf-syntax-ns#"),
            Map.entry("rdfs", "http://www.w3.org/2000/01/rdf-schema#"),
            //Map.entry("xsd", "http://www.w3.org/2001/XMLSchema#"), UNRESOLVABLE
            Map.entry("rr", "http://www.w3.org/ns/r2rml#"),
            Map.entry("ordl", "http://www.w3.org/ns/odrl/2/"),
            Map.entry("dcat", "http://www.w3.org/ns/dcat#"),
            Map.entry("dc", "http://purl.org/dc/elements/1.1/"),
            Map.entry("sd", "http://www.w3.org/ns/sparql-service-description#"),
            Map.entry("sh", "http://www.w3.org/ns/shacl#"),
            Map.entry("shsh", "http://www.w3.org/ns/shacl-shacl")
    );

    public void resolveDefaultNamespaces() {
        if(resolveDefault){
            DEFAULT_NAMESPACES.forEach((prefix, uri) -> namespaceResolver.resolveNamespace(uri));
        }
    }
}
