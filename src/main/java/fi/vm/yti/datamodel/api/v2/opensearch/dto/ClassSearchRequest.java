package fi.vm.yti.datamodel.api.v2.opensearch.dto;

import java.util.Set;

public class ClassSearchRequest extends BaseSearchRequest{

    private String fromAddedNamespaces;
    private Set<String> groups;

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
}
