package fi.vm.yti.datamodel.api.index.model;

import java.util.Map;
import java.util.regex.Pattern;

import fi.vm.yti.datamodel.api.model.AbstractClass;

public class IndexResourceDTO {

    private String id;
    private String isDefinedBy;
    private String status;
    private String modified;
    private String type;
    private Map<String, String> label;
    private Map<String, String> comment;
    private String range;

    public IndexResourceDTO() {
    }

    public IndexResourceDTO(final String id,
                            final String isDefinedBy,
                            final String status,
                            final String modified,
                            final String type,
                            final String range,
                            final Map<String, String> label,
                            final Map<String, String> comment) {
        this.id = id;
        this.isDefinedBy = isDefinedBy;
        this.status = status;
        this.modified = modified;
        this.type = type;
        this.range = range;
        this.label = label;
        this.comment = comment;
    }

    public IndexResourceDTO(AbstractClass classResource) {
        this.id = classResource.getId();
        this.isDefinedBy = classResource.getModelId();
        this.status = classResource.getStatus();
        this.modified = classResource.getModified();
        this.type = classResource.getType();
        this.label = classResource.getLabel();
        this.comment = classResource.getComment();
    }

    public String getId() {
        return id;
    }

    public void setId(final String id) {
        this.id = id;
    }

    public String getIsDefinedBy() {
        return isDefinedBy;
    }

    public void setIsDefinedBy(final String isDefinedBy) {
        this.isDefinedBy = isDefinedBy;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(final String status) {
        this.status = status;
    }

    public String getModified() {
        return modified;
    }

    public void setModified(final String modified) {
        this.modified = modified;
    }

    public String getType() {
        return type;
    }

    public void setType(final String type) {
        this.type = type;
    }

    public String getRange() {
        return range;
    }

    public void setRange(final String range) {
        this.range = range;
    }

    public void highlightLabels(String highlightText) {
        if (highlightText != null && highlightText.length() > 0) {
            String[] highLights = highlightText.split("\\s+");
            for(String highLight : highLights) {
                this.label.forEach((lang, label) -> {
                    String matchString = Pattern.quote(highLight);
                    this.label.put(lang, label.replaceAll("(?i)(?<text>\\b" + matchString + "|"+ matchString+"\\b)", "<b>${text}</b>"));
                });
            }
        }
    }

    public Map<String, String> getLabel() {
        return label;
    }

    public void setLabel(final Map<String, String> label) {
        this.label = label;
    }

    public void putLabel(String lang,
                         String label) {
        this.label.put(lang, label);
    }

    public Map<String, String> getComment() {
        return comment;
    }

    public void setComment(final Map<String, String> comment) {
        this.comment = comment;
    }

    @Override
    public String toString() {
        return "IndexResourceDTO{" +
            "id='" + id + '\'' +
            ", isDefinedBy='" + isDefinedBy + '\'' +
            ", status='" + status + '\'' +
            ", modified='" + modified + '\'' +
            ", type='" + type + '\'' +
            ", label=" + label +
            ", comment=" + comment +
            ", range='" + range + '\'' +
            '}';
    }
}
