package fi.vm.yti.datamodel.api.v2.service;

import java.util.List;

import fi.vm.yti.datamodel.api.v2.dto.ModelType;

public interface StorageService {

	public int storeSchemaFile(String schemaPID, String contentType, byte[] data);

	public int storeCrosswalkFile(String schemaPID, String contentType, byte[] data);

	public StoredFile retrieveSchemaFile(String schemaPID, long fileID);

	public List<StoredFile> retrieveAllSchemaFiles(String schemaPID);

	public record StoredFile(String contentType, byte[] data, long fileID, ModelType type) {
	}

	public List<StoredFile> retrieveAllCrosswalkFiles(String pid);

}
