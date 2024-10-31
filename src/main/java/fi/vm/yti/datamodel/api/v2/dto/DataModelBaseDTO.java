package fi.vm.yti.datamodel.api.v2.dto;

import fi.vm.yti.common.dto.BaseDTO;
import org.apache.commons.lang3.builder.ToStringBuilder;

import java.util.Map;

public abstract class DataModelBaseDTO extends BaseDTO {
    private Map<String, String> label;
    private String subject;
    private Map<String, String> note;

    public Map<String, String> getLabel() {
        return label;
    }

    public void setLabel(Map<String, String> label) {
        this.label = label;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public Map<String, String> getNote() {
        return note;
    }

    public void setNote(Map<String, String> note) {
        this.note = note;
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }
}
