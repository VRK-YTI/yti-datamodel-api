package fi.vm.yti.datamodel.api.v2.dto;

import java.util.Set;

import org.apache.commons.lang3.builder.ToStringBuilder;

public class SchemaInfoDTO extends DataModelInfoDTO {
		
	private SchemaFormat format;
	private String aggregationKey;
	private Set<FileMetadata> fileMetadata = Set.of();
	private String PID;

	public String getPID() {
		return PID;
	}
	public void setPID(String pID) {
		PID = pID;
	}
	
	public SchemaFormat getFormat() {
		return format;
	}

	public void setFormat(SchemaFormat type) {
		this.format = type;
	}

  public String getAggregationKey() {
		return aggregationKey;
	}

	public void setAggregationKey(String aggregationKey) {
		this.aggregationKey = aggregationKey;
	}
  
	public Set<FileMetadata> getFileMetadata() {
		return fileMetadata;
	}

	public void setFileMetadata(Set<FileMetadata> fileMetadata) {
		this.fileMetadata = fileMetadata;
	}  

	@Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }	
}
