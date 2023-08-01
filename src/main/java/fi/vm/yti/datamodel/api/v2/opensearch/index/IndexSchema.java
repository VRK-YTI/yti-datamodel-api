package fi.vm.yti.datamodel.api.v2.opensearch.index;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonFormat;

import fi.vm.yti.datamodel.api.v2.dto.MSCRType;

public class IndexSchema extends IndexBase {

    private String contentModified;
	private MSCRType type;
    private String prefix;
    private Map<String, String> comment;
    @JsonFormat(with = JsonFormat.Feature.ACCEPT_SINGLE_VALUE_AS_ARRAY)
    private List<UUID> contributor;
    @JsonFormat(with = JsonFormat.Feature.ACCEPT_SINGLE_VALUE_AS_ARRAY)
    private List<String> isPartOf;
    private List<String> language;

    public String getContentModified() {
        return contentModified;
    }

    public void setContentModified(String contentModified) {
        this.contentModified = contentModified;
    }

    public String getPrefix() {
        return prefix;
    }

    public void setPrefix(String prefix) {
        this.prefix = prefix;
    }

    public Map<String, String> getComment() {
        return comment;
    }

    public void setComment(Map<String, String> comment) {
        this.comment = comment;
    }

    public List<UUID> getContributor() {
        return contributor;
    }

    public void setContributor(List<UUID> contributor) {
        this.contributor = contributor;
    }

    public List<String> getIsPartOf() {
        return isPartOf;
    }

    public void setIsPartOf(List<String> isPartOf) {
        this.isPartOf = isPartOf;
    }

    public List<String> getLanguage() {
        return language;
    }

    public void setLanguage(List<String> language) {
        this.language = language;
    }
	
	public MSCRType getType() {
		return type;
	}

	public void setType(MSCRType type) {
		this.type = type;
	}
	
	
}
