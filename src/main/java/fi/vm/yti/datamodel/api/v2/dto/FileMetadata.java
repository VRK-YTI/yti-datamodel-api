package fi.vm.yti.datamodel.api.v2.dto;

public class FileMetadata {
	private String contentType;
	private int size;
	
	public String getContentType() {
		return contentType;
	}
	
	public FileMetadata(String contentType, int size) {
		this.contentType = contentType;
		this.size = size;
	}

	public int getSize() {
		return size;
	}
}
