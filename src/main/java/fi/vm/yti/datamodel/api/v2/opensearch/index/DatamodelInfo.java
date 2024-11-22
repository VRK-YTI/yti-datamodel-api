package fi.vm.yti.datamodel.api.v2.opensearch.index;

import fi.vm.yti.common.enums.GraphType;
import fi.vm.yti.common.enums.Status;

import java.util.List;
import java.util.Map;

public class DatamodelInfo {
    private Map<String, String> label;
    private List<String> groups;
    private Status status;
    private GraphType modelType;
    private String uri;
    private String version;

    public Map<String, String> getLabel() {
        return label;
    }

    public void setLabel(Map<String, String> label) {
        this.label = label;
    }

    public List<String> getGroups() {
        return groups;
    }

    public void setGroups(List<String> groups) {
        this.groups = groups;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public GraphType getModelType() {
        return modelType;
    }

    public void setModelType(GraphType modelType) {
        this.modelType = modelType;
    }

    public String getUri() {
        return uri;
    }

    public void setUri(String uri) {
        this.uri = uri;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }
}
