package fi.vm.yti.datamodel.api.v2.opensearch.index;

public class IndexCrosswalk extends IndexModel {
	
	private String sourceSchema;
	private String targetSchema;
	
	public String getSourceSchema() {
		return sourceSchema;
	}
	public void setSourceSchema(String sourceSchema) {
		this.sourceSchema = sourceSchema;
	}
	public String getTargetSchema() {
		return targetSchema;
	}
	public void setTargetSchema(String targetSchema) {
		this.targetSchema = targetSchema;
	}
	
	
}
