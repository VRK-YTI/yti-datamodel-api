package fi.vm.yti.datamodel.api.v2.dto;

import com.google.common.base.Objects;
import org.apache.commons.lang3.builder.ToStringBuilder;

import java.util.Map;

public class UriDTO {
    private String uri;
    private String curie;
    private Map<String, String> label;

    public UriDTO(String uri) {
        this.uri = uri;
    }

    public UriDTO(String uri, String curie) {
        this.uri = uri;
        this.curie = curie;
    }

    public UriDTO(String uri, String curie, Map<String, String> label) {
        this.uri = uri;
        this.curie = curie;
        this.label = label;
    }

    public Map<String, String> getLabel() {
        return label;
    }

    public void setLabel(Map<String, String> label) {
        this.label = label;
    }

    public String getCurie() {
        return curie;
    }

    public String getUri() {
        return uri;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        UriDTO uriDTO = (UriDTO) o;
        return Objects.equal(uri, uriDTO.uri);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(uri);
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }
}
