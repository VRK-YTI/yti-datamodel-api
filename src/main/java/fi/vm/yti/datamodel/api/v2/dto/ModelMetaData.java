package fi.vm.yti.datamodel.api.v2.dto;

import fi.vm.yti.common.dto.LinkDTO;
import fi.vm.yti.common.dto.MetaDataDTO;

import java.util.Map;
import java.util.Set;

public class ModelMetaData extends MetaDataDTO {

    private Map<String, String> documentation = Map.of();
    private Set<LinkDTO> links = Set.of();

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
}
