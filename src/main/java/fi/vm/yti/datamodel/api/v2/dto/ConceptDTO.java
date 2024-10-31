package fi.vm.yti.datamodel.api.v2.dto;

import fi.vm.yti.common.enums.Status;

import java.util.Map;

public class ConceptDTO {
    private String conceptURI;
    private Map<String, String> label = Map.of();
    private Map<String, String> definition = Map.of();
    private TerminologyDTO terminology;
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

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public TerminologyDTO getTerminology() {
        return terminology;
    }

    public void setTerminology(TerminologyDTO terminology) {
        this.terminology = terminology;
    }
}
