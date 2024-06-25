package fi.vm.yti.datamodel.api.v2.dto;

import java.util.Set;

public class AddCodeListPayloadDTO {
    private String attributeUri;
    private Set<String> codeLists;

    public String getAttributeUri() {
        return attributeUri;
    }

    public void setAttributeUri(String attributeUri) {
        this.attributeUri = attributeUri;
    }

    public Set<String> getCodeLists() {
        return codeLists;
    }

    public void setCodeLists(Set<String> codeLists) {
        this.codeLists = codeLists;
    }
}
