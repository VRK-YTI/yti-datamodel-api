package fi.vm.yti.datamodel.api.v2.endpoint.error;

public class MappingError extends RuntimeException {

    public MappingError(String message) {
        super("Error during mapping: " + message);
    }
}
