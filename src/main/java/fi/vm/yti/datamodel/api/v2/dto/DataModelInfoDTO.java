package fi.vm.yti.datamodel.api.v2.dto;

import fi.vm.yti.common.dto.LinkDTO;
import fi.vm.yti.common.dto.MetaDataInfoDTO;

import java.util.Map;
import java.util.Set;

public class DataModelInfoDTO extends MetaDataInfoDTO {

    private Set<InternalNamespaceDTO> internalNamespaces = Set.of();
    private Set<ExternalNamespaceDTO> externalNamespaces = Set.of();
    private Set<TerminologyDTO> terminologies = Set.of();
    private Set<CodeListDTO> codeLists = Set.of();
    private Map<String, String> documentation = Map.of();
    private Set<LinkDTO> links = Set.of();
    private String version;
    private String versionIri;

    public Set<InternalNamespaceDTO> getInternalNamespaces() {
        return internalNamespaces;
    }

    public void setInternalNamespaces(Set<InternalNamespaceDTO> internalNamespaces) {
        this.internalNamespaces = internalNamespaces;
    }

    public Set<ExternalNamespaceDTO> getExternalNamespaces() {
        return externalNamespaces;
    }

    public void setExternalNamespaces(Set<ExternalNamespaceDTO> externalNamespaces) {
        this.externalNamespaces = externalNamespaces;
    }

    public Set<TerminologyDTO> getTerminologies() {
        return terminologies;
    }

    public void setTerminologies(Set<TerminologyDTO> terminologies) {
        this.terminologies = terminologies;
    }

    public Set<CodeListDTO> getCodeLists() {
        return codeLists;
    }

    public void setCodeLists(Set<CodeListDTO> codeLists) {
        this.codeLists = codeLists;
    }

    public Map<String, String> getDocumentation() {
        return documentation;
    }

    public void setDocumentation(Map<String, String> documentation) {
        this.documentation = documentation;
    }

    public Set<LinkDTO> getLinks() {
        return links;
    }

    public void setLinks(Set<LinkDTO> links) {
        this.links = links;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getVersionIri() {
        return versionIri;
    }

    public void setVersionIri(String versionIri) {
        this.versionIri = versionIri;
    }
}
