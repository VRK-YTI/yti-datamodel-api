package fi.vm.yti.datamodel.api.v2.opensearch.dto;

import fi.vm.yti.common.enums.GraphType;
import fi.vm.yti.common.opensearch.BaseSearchRequest;
import fi.vm.yti.datamodel.api.v2.dto.ResourceType;

import java.util.Set;

public class ResourceSearchRequest extends BaseSearchRequest {

    private String limitToDataModel;
    private boolean fromAddedNamespaces;
    private Set<String> groups;
    private Set<ResourceType> resourceTypes;
    private GraphType limitToModelType;
    private String targetClass;
    private Set<String> additionalResources;
    private String fromVersion;
    private Set<String> includeDraftFrom;

    public String getLimitToDataModel() {
        return limitToDataModel;
    }

    public void setLimitToDataModel(String limitToDataModel) {
        this.limitToDataModel = limitToDataModel;
    }

    public boolean isFromAddedNamespaces() {
        return fromAddedNamespaces;
    }

    public void setFromAddedNamespaces(boolean fromAddedNamespaces) {
        this.fromAddedNamespaces = fromAddedNamespaces;
    }

    public Set<String> getGroups() {
        return groups;
    }

    public void setGroups(Set<String> groups) {
        this.groups = groups;
    }

    public Set<ResourceType> getResourceTypes() {
        return resourceTypes;
    }

    public void setResourceTypes(Set<ResourceType> resourceTypes) {
        this.resourceTypes = resourceTypes;
    }

    public GraphType getLimitToModelType() {
        return limitToModelType;
    }

    public void setLimitToModelType(GraphType limitToModelType) {
        this.limitToModelType = limitToModelType;
    }

    public String getTargetClass() {
        return targetClass;
    }

    public void setTargetClass(String targetClass) {
        this.targetClass = targetClass;
    }

    public Set<String> getAdditionalResources() {
        return additionalResources;
    }

    public void setAdditionalResources(Set<String> additionalResources) {
        this.additionalResources = additionalResources;
    }

    public String getFromVersion() {
        return fromVersion;
    }

    public void setFromVersion(String fromVersion) {
        this.fromVersion = fromVersion;
    }

    public Set<String> getIncludeDraftFrom() {
        return includeDraftFrom;
    }

    public void setIncludeDraftFrom(Set<String> includeDraftFrom) {
        this.includeDraftFrom = includeDraftFrom;
    }
}
