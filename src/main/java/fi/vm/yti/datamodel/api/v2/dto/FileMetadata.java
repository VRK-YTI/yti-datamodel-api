package fi.vm.yti.datamodel.api.v2.dto;

public class FileMetadata {
	private String contentType;
	private int size;
	private long fileID;
	
	
	public FileMetadata(String contentType, int size, long fileID) {
		this.contentType = contentType;
		this.size = size;
		this.fileID = fileID;
	}
	
	public String getContentType() {
		return contentType;
	}

	public long getFileID() {
		return fileID;
	}
	
	public int getSize() {
		return size;
	}
	
	
}
