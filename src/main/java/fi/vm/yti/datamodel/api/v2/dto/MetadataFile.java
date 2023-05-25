package fi.vm.yti.datamodel.api.v2.dto;

public class MetadataFile {
	private String contentType;
	private int size;
	
	public String getContentType() {
		return contentType;
	}
	
	public void setContentType(String contentType) {
		this.contentType = contentType;
	}
	
	public int getSize() {
		return size;
	}
	
	public void setSize(int size) { 
		this.size = size;
	}
}
