package fi.vm.yti.datamodel.api.v2.dto;

import org.apache.commons.lang3.builder.EqualsBuilder;

import java.util.Objects;

public class ResourceReferenceDTO {
    private UriDTO resourceURI;
    private String property;
    private ResourceType type;

    public UriDTO getResourceURI() {
        return resourceURI;
    }

    public void setResourceURI(UriDTO resourceURI) {
        this.resourceURI = resourceURI;
    }

    public String getProperty() {
        return property;
    }

    public void setProperty(String property) {
        this.property = property;
    }

    public ResourceType getType() {
        return type;
    }

    public void setType(ResourceType type) {
        this.type = type;
    }

    @Override
    public boolean equals(Object o) {
        return EqualsBuilder.reflectionEquals(o, this, "property");
    }

    @Override
    public int hashCode() {
        return Objects.hash(resourceURI, property, type);
    }
}
