package fi.vm.yti.datamodel.api.v2.service;

import fi.vm.yti.datamodel.api.v2.repository.ImportsRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@Service
public class NamespaceService {

    private final NamespaceResolver namespaceResolver;
    private final ImportsRepository importsRepository;

    @Value("${namespaces.resolveDefault:false}")
    private boolean resolveDefault;

    @Autowired
    public NamespaceService(NamespaceResolver namespaceResolver, ImportsRepository importsRepository) {
        this.namespaceResolver = namespaceResolver;
        this.importsRepository = importsRepository;
    }

    public static final Map<String, String> DEFAULT_NAMESPACES = Map.ofEntries(
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
            Map.entry("shsh", "http://www.w3.org/ns/shacl-shacl"),
            Map.entry("foaf", "http://xmlns.com/foaf/0.1/"),
            Map.entry("vcard", "http://www.w3.org/2006/vcard/ns#"),
            Map.entry("elm", "http://data.europa.eu/snb/model/elm"),
            Map.entry("unece", "https://vocabulary.uncefact.org/"),
            Map.entry("gs1", "https://gs1.org/voc/"),
            Map.entry("m8g", "http://data.europa.eu/m8g")
    );

    // Resolvable URIs by namespace name
    public static final Map<String, String> DEFAULT_NAMESPACE_RESOLVABLE_URIS = Map.of(
            "https://gs1.org/voc/", "https://www.gs1.org/docs/gs1-smartsearch/gs1Voc_v1_10.ttl",
            "http://data.europa.eu/949/", "https://data-interop.era.europa.eu/era-vocabulary/ontology.ttl"
    );

    public void resolveDefaultNamespaces() {
        if(resolveDefault){
            DEFAULT_NAMESPACES.forEach((prefix, uri) -> namespaceResolver.resolveNamespace(uri));
        }
    }

    public Set<String> getResolvedNamespaces() {
        String query = """
                SELECT ?g WHERE {
                  GRAPH ?g {}
                }
                """;
        var result = new HashSet<String>();
        importsRepository.querySelect(query, (var row) -> result.add(row.get("g").toString()));

        return result;
    }
}
