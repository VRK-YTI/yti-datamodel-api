package fi.vm.yti.datamodel.api.v2.endpoint.error;

public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String prefix) {
        super("Resource not found: " + prefix);
    }
}
