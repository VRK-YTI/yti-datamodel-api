package fi.vm.yti.datamodel.api.v2.dto;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class AttributeRestriction extends PropertyShapeDTO {
    private String dataType;
    private List<String> allowedValues = new ArrayList<>();
    private String defaultValue;
    private String hasValue;
    private Integer maxLength;
    private Integer minLength;
    private Integer minInclusive;
    private Integer maxInclusive;
    private Integer minExclusive;
    private Integer maxExclusive;
    private String pattern;
    private Set<String> languageIn = new HashSet<>();
    private List<String> codeLists = new ArrayList<>();

    public String getDataType() {
        return dataType;
    }

    public void setDataType(String dataType) {
        this.dataType = dataType;
    }

    public List<String> getAllowedValues() {
        return allowedValues;
    }

    public void setAllowedValues(List<String> allowedValues) {
        this.allowedValues = allowedValues;
    }

    public String getDefaultValue() {
        return defaultValue;
    }

    public void setDefaultValue(String defaultValue) {
        this.defaultValue = defaultValue;
    }

    public String getHasValue() {
        return hasValue;
    }

    public void setHasValue(String hasValue) {
        this.hasValue = hasValue;
    }

    public Integer getMaxLength() {
        return maxLength;
    }

    public void setMaxLength(Integer maxLength) {
        this.maxLength = maxLength;
    }

    public Integer getMinLength() {
        return minLength;
    }

    public void setMinLength(Integer minLength) {
        this.minLength = minLength;
    }

    public Integer getMinInclusive() {
        return minInclusive;
    }

    public void setMinInclusive(Integer minInclusive) {
        this.minInclusive = minInclusive;
    }

    public Integer getMaxInclusive() {
        return maxInclusive;
    }

    public void setMaxInclusive(Integer maxInclusive) {
        this.maxInclusive = maxInclusive;
    }

    public Integer getMinExclusive() {
        return minExclusive;
    }

    public void setMinExclusive(Integer minExclusive) {
        this.minExclusive = minExclusive;
    }

    public Integer getMaxExclusive() {
        return maxExclusive;
    }

    public void setMaxExclusive(Integer maxExclusive) {
        this.maxExclusive = maxExclusive;
    }

    public List<String> getCodeLists() {
        return codeLists;
    }

    public void setCodeLists(List<String> codeLists) {
        this.codeLists = codeLists;
    }

    public String getPattern() {
        return pattern;
    }

    public void setPattern(String pattern) {
        this.pattern = pattern;
    }

    public Set<String> getLanguageIn() {
        return languageIn;
    }

    public void setLanguageIn(Set<String> languageIn) {
        this.languageIn = languageIn;
    }
}
