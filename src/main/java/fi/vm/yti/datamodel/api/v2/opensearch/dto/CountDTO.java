package fi.vm.yti.datamodel.api.v2.opensearch.dto;

import java.util.Collections;
import java.util.Map;

public class CountDTO {

    private Map<String, Long> statuses;
    private Map<String, Long> languages;

    private Map<String, Long> groups;

    private Map<String, Long> types;

    public CountDTO() {
        this.statuses = Collections.emptyMap();
        this.languages = Collections.emptyMap();
        this.types = Collections.emptyMap();
        this.groups = Collections.emptyMap();
    }

    public CountDTO(
            final Map<String, Long> statuses,
            final Map<String, Long> languages,
            final Map<String, Long> types,
            final Map<String, Long> groups) {
        this.statuses = statuses;
        this.languages = languages;
        this.types = types;
        this.groups = groups;
    }



    public Map<String, Long> getStatuses() {
        return statuses;
    }

    public void setStatuses(Map<String, Long> statuses) {
        this.statuses = statuses;
    }

    public Map<String, Long> getLanguages() {
        return languages;
    }

    public void setLanguages(Map<String, Long> languages) {
        this.languages = languages;
    }

    public Map<String, Long> getTypes() {
        return types;
    }

    public void setTypes(Map<String, Long> types){
        this.types = types;
    }

    public Map<String, Long> getGroups() {
        return groups;
    }

    public void setGroups(Map<String, Long> groups) {
        this.groups = groups;
    }
}
