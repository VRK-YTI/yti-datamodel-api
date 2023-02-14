package fi.vm.yti.datamodel.api.v2.endpoint.error;

public class ResolvingException extends RuntimeException {

    private final String details;
    public ResolvingException(String message, String details) {
        super("Error during resolution: " + message);
        this.details = details;
    }

    public String getDetails() {
        return details;
    }
}
