package fi.vm.yti.datamodel.api.v2.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import fi.vm.yti.common.enums.Status;

import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public class CodeListDTO {

    private String id;
    private Map<String, String> prefLabel;
    private Status status;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Map<String, String> getPrefLabel() {
        return prefLabel;
    }

    public void setPrefLabel(Map<String, String> prefLabel) {
        this.prefLabel = prefLabel;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }
}
