package fi.vm.yti.datamodel.api.v2.dto;

import org.apache.commons.lang3.builder.ToStringBuilder;

import java.util.Set;

public class ResourceDTO extends BaseDTO {

    private Set<String> subResourceOf = Set.of();
    private Set<String> equivalentResource = Set.of();
    private String domain;
    private String range;
    private Boolean functionalProperty;
    private Boolean transitiveProperty;
    private Boolean reflexiveProperty;

    public Set<String> getSubResourceOf() {
        return subResourceOf;
    }

    public void setSubResourceOf(Set<String> subResourceOf) {
        this.subResourceOf = subResourceOf;
    }

    public Set<String> getEquivalentResource() {
        return equivalentResource;
    }

    public void setEquivalentResource(Set<String> equivalentResource) {
        this.equivalentResource = equivalentResource;
    }

    public String getDomain() {
        return domain;
    }

    public void setDomain(String domain) {
        this.domain = domain;
    }

    public String getRange() {
        return range;
    }

    public void setRange(String range) {
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

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }
}
