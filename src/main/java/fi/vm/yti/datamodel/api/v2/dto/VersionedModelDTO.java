package fi.vm.yti.datamodel.api.v2.dto;

import java.util.Map;

public class VersionedModelDTO {

    private Status status;
    private Map<String, String> documentation;

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public Map<String, String> getDocumentation() {
        return documentation;
    }

    public void setDocumentation(Map<String, String> documentation) {
        this.documentation = documentation;
    }
}
