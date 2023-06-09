package fi.vm.yti.datamodel.api.v2.opensearch.index;

import java.util.Map;

public class ConceptInfo {
    private String conceptURI;
    private Map<String, String> conceptLabel;
    private Map<String, String> terminologyLabel;

    public String getConceptURI() {
        return conceptURI;
    }

    public void setConceptURI(String conceptURI) {
        this.conceptURI = conceptURI;
    }

    public Map<String, String> getConceptLabel() {
        return conceptLabel;
    }

    public void setConceptLabel(Map<String, String> conceptLabel) {
        this.conceptLabel = conceptLabel;
    }

    public Map<String, String> getTerminologyLabel() {
        return terminologyLabel;
    }

    public void setTerminologyLabel(Map<String, String> terminologyLabel) {
        this.terminologyLabel = terminologyLabel;
    }
}
