package fi.vm.yti.datamodel.api.v2.dto;

import java.util.Map;
import java.util.Set;

public class SimpleResourceDTO {

    private String uri;
    private String versionIri;
    private String version;
    private Map<String, String> label;
    private String identifier;
    private String modelId;
    private String curie;
    private ConceptDTO concept;
    private Map<String, String> note;
    private UriDTO range;
    private Set<String> codeLists = Set.of();

    public String getUri() {
        return uri;
    }

    public void setUri(String uri) {
        this.uri = uri;
    }

    public String getVersionIri() {
        return versionIri;
    }

    public void setVersionIri(String versionIri) {
        this.versionIri = versionIri;
    }

    public Map<String, String> getLabel() {
        return label;
    }

    public void setLabel(Map<String, String> label) {
        this.label = label;
    }

    public String getIdentifier() {
        return identifier;
    }

    public void setIdentifier(String identifier) {
        this.identifier = identifier;
    }

    public String getModelId() {
        return modelId;
    }

    public void setModelId(String namespace) {
        this.modelId = namespace;
    }

    public String getCurie() {
        return curie;
    }

    public void setCurie(String curie) {
        this.curie = curie;
    }

    public ConceptDTO getConcept() {
        return concept;
    }

    public void setConcept(ConceptDTO concept) {
        this.concept = concept;
    }

    public Map<String, String> getNote() {
        return note;
    }

    public void setNote(Map<String, String> note) {
        this.note = note;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public UriDTO getRange() {
        return range;
    }

    public void setRange(UriDTO range) {
        this.range = range;
    }

    public Set<String> getCodeLists() {
        return codeLists;
    }

    public void setCodeLists(Set<String> codeLists) {
        this.codeLists = codeLists;
    }
}
