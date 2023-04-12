package fi.vm.yti.datamodel.api.v2.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.ArrayList;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class TerminologyNodeDTO {
    private String uri;
    private Type type;
    private TerminologyProperties properties;
    private TerminologyReferences references;

    public Type getType() {
        return type;
    }

    public void setType(Type type) {
        this.type = type;
    }

    public String getUri() {
        return uri;
    }

    public void setUri(String uri) {
        this.uri = uri;
    }

    public TerminologyProperties getProperties() {
        return properties;
    }

    public void setProperties(TerminologyProperties properties) {
        this.properties = properties;
    }

    public TerminologyReferences getReferences() {
        return references;
    }

    public void setReferences(TerminologyReferences references) {
        this.references = references;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class TerminologyProperties {
        private List<LocalizedValue> prefLabel = new ArrayList<>();
        private List<LocalizedValue> status = new ArrayList<>();
        private List<LocalizedValue> definition = new ArrayList<>();

        public List<LocalizedValue> getPrefLabel() {
            return prefLabel;
        }

        public void setPrefLabel(List<LocalizedValue> prefLabel) {
            this.prefLabel = prefLabel;
        }

        public List<LocalizedValue> getStatus() {
            return status;
        }

        public void setStatus(List<LocalizedValue> status) {
            this.status = status;
        }

        public List<LocalizedValue> getDefinition() {
            return definition;
        }

        public void setDefinition(List<LocalizedValue> definition) {
            this.definition = definition;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Type {
        private String id;

        public void setId(String id) {
            this.id = id;
        }

        public String getId() {
            return id;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class LocalizedValue {
        private String lang;
        private String value;

        public void setLang(String lang) {
            this.lang = lang;
        }

        public String getLang() {
            return lang;
        }

        public String getValue() {
            return value;
        }

        public void setValue(String value) {
            this.value = value;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class TerminologyReferences {
        private List<TerminologyNodeDTO> prefLabelXl = new ArrayList<>();

        public List<TerminologyNodeDTO> getPrefLabelXl() {
            return prefLabelXl;
        }

        public void setPrefLabelXl(List<TerminologyNodeDTO> prefLabelXl) {
            this.prefLabelXl = prefLabelXl;
        }
    }
}
