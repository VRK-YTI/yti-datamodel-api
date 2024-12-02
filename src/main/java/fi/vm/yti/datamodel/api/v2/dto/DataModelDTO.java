package fi.vm.yti.datamodel.api.v2.dto;

import org.apache.commons.lang3.builder.ToStringBuilder;

import java.util.Set;

public class DataModelDTO extends ModelMetaData {

    private Set<String> internalNamespaces = Set.of();
    private Set<ExternalNamespaceDTO> externalNamespaces = Set.of();
    private Set<String> terminologies = Set.of();
    private Set<String> codeLists = Set.of();

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }

    public Set<ExternalNamespaceDTO> getExternalNamespaces() {
        return externalNamespaces;
    }

    public void setExternalNamespaces(Set<ExternalNamespaceDTO> externalNamespaces) {
        this.externalNamespaces = externalNamespaces;
    }

    public Set<String> getInternalNamespaces() {
        return internalNamespaces;
    }

    public void setInternalNamespaces(Set<String> internalNamespaces) {
        this.internalNamespaces = internalNamespaces;
    }

    public Set<String> getTerminologies() {
        return terminologies;
    }

    public void setTerminologies(Set<String> terminologies) {
        this.terminologies = terminologies;
    }

    public Set<String> getCodeLists() {
        return codeLists;
    }

    public void setCodeLists(Set<String> codeLists) {
        this.codeLists = codeLists;
    }

}
