package fi.vm.yti.datamodel.api.v2.dto;

import java.util.Map;

public class ConceptDTO {
    private String conceptURI;
    private Map<String, String> label = Map.of();
    private Map<String, String> definition = Map.of();
    private String terminologyURI;
    private Map<String, String> terminologyLabel = Map.of();
    private Status status;

    public Map<String, String> getLabel() {
        return label;
    }

    public void setLabel(Map<String, String> label) {
        this.label = label;
    }

    public Map<String, String> getDefinition() {
        return definition;
    }

    public void setDefinition(Map<String, String> definition) {
        this.definition = definition;
    }

    public String getConceptURI() {
        return conceptURI;
    }

    public void setConceptURI(String conceptURI) {
        this.conceptURI = conceptURI;
    }

    public String getTerminologyURI() {
        return terminologyURI;
    }

    public void setTerminologyURI(String terminologyURI) {
        this.terminologyURI = terminologyURI;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public Map<String, String> getTerminologyLabel() {
        return terminologyLabel;
    }
    public void setTerminologyLabel(Map<String, String> terminologyLabel) {
        this.terminologyLabel = terminologyLabel;
    }
}
