package fi.vm.yti.datamodel.api.v2.dto;

import java.util.Set;

public class ResourceInfoDTO extends ResourceInfoBaseDTO {

    private ResourceType type;
    private Set<UriDTO> subResourceOf;
    private Set<UriDTO> equivalentResource;
    private UriDTO domain;
    private UriDTO range;
    private Boolean functionalProperty;
    private Boolean transitiveProperty;
    private Boolean reflexiveProperty;

    public ResourceType getType() {
        return type;
    }

    public void setType(ResourceType type) {
        this.type = type;
    }

    public Set<UriDTO> getSubResourceOf() {
        return subResourceOf;
    }

    public void setSubResourceOf(Set<UriDTO> subResourceOf) {
        this.subResourceOf = subResourceOf;
    }

    public Set<UriDTO> getEquivalentResource() {
        return equivalentResource;
    }

    public void setEquivalentResource(Set<UriDTO> equivalentResource) {
        this.equivalentResource = equivalentResource;
    }

    public UriDTO getDomain() {
        return domain;
    }

    public void setDomain(UriDTO domain) {
        this.domain = domain;
    }

    public UriDTO getRange() {
        return range;
    }

    public void setRange(UriDTO range) {
        this.range = range;
    }

    public Boolean getFunctionalProperty() {
        return functionalProperty;
    }

    public void setFunctionalProperty(Boolean functionalProperty) {
        this.functionalProperty = functionalProperty;
    }

    public Boolean getTransitiveProperty() {
        return transitiveProperty;
    }

    public void setTransitiveProperty(Boolean transitiveProperty) {
        this.transitiveProperty = transitiveProperty;
    }

    public Boolean getReflexiveProperty() {
        return reflexiveProperty;
    }

    public void setReflexiveProperty(Boolean reflexiveProperty) {
        this.reflexiveProperty = reflexiveProperty;
    }
}
