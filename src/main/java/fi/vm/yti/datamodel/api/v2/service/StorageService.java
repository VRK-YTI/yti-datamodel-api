package fi.vm.yti.datamodel.api.v2.service;

import java.util.List;

import fi.vm.yti.datamodel.api.v2.endpoint.error.StorageException;

public interface StorageService {

	public int storeSchemaFile(String schemaPID, String contentType, byte[] data) throws StorageException;
	public StoredFile retrieveSchemaFile(String schemaPID, long fileID);
	public List<StoredFile> retrieveAllSchemaFiles(String schemaPID);
	

	public record StoredFile(String contentType, byte[] data) {}
	
}
