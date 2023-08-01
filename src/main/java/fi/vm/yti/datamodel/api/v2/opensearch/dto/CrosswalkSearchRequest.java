package fi.vm.yti.datamodel.api.v2.opensearch.dto;

import java.util.Set;

public class CrosswalkSearchRequest extends ModelSearchRequest {

	
	private Set<String> sourceSchemas;
	private Set<String> targetSchemas;
	
	public Set<String> getSourceSchemas() {
		return sourceSchemas;
	}
	public void setSourceSchemas(Set<String> sourceSchemas) {
		this.sourceSchemas = sourceSchemas;
	}
	public Set<String> getTargetSchemas() {
		return targetSchemas;
	}
	public void setTargetSchemas(Set<String> targetSchemas) {
		this.targetSchemas = targetSchemas;
	}
	
	
}
