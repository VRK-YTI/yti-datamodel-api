package fi.vm.yti.datamodel.api.v2.opensearch.dto;

import fi.vm.yti.datamodel.api.v2.dto.ResourceType;

import java.util.Set;

public class ResourceSearchRequest extends BaseSearchRequest{

    private String fromAddedNamespaces;
    private Set<String> groups;
    private Set<ResourceType> resourceTypes;

    public String getFromAddedNamespaces() {
        return fromAddedNamespaces;
    }

    public void setFromAddedNamespaces(String fromAddedNamespaces) {
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
}
