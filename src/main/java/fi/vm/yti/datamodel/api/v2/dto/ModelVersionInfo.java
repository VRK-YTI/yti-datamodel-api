package fi.vm.yti.datamodel.api.v2.dto;

import fi.vm.yti.common.enums.Status;

import java.util.Map;

public class ModelVersionInfo {

    //label, status, version and versionIRI
    private Map<String, String> label;
    private Status status;
    private String version;
    private String versionIRI;


    public Map<String, String> getLabel() {
        return label;
    }

    public void setLabel(Map<String, String> label) {
        this.label = label;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getVersionIRI() {
        return versionIRI;
    }

    public void setVersionIRI(String versionIRI) {
        this.versionIRI = versionIRI;
    }
}
