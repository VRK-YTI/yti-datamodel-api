package fi.vm.yti.datamodel.api.v2.dto;

import org.apache.commons.lang3.builder.ToStringBuilder;

public class SchemaDTO extends DataModelDTO {

	private SchemaFormat format;
	private String aggregationKey;
	
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

	@Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }
}
