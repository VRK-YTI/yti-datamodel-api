package fi.vm.yti.datamodel.api.v2.opensearch.dto;

import java.util.Set;

public class ClassSearchRequest extends BaseSearchRequest{
    private String language;

    private Set<String> fromNamespaces;

    private Set<String> groups;


    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public Set<String> getFromNamespaces() {
        return fromNamespaces;
    }

    public void setFromNamespaces(Set<String> fromNamespaces) {
        this.fromNamespaces = fromNamespaces;
    }

    public Set<String> getGroups() {
        return groups;
    }

    public void setGroups(Set<String> groups) {
        this.groups = groups;
    }
}
