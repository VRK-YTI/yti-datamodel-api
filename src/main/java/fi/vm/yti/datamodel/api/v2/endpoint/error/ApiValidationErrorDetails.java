package fi.vm.yti.datamodel.api.v2.endpoint.error;

public class ApiValidationErrorDetails {

    private final String field;

    private final Object rejectedValue;

    private final String message;

    public ApiValidationErrorDetails(String message, String field, String rejectedValue) {
        this.message = message;
        this.rejectedValue = rejectedValue;
        this.field = field;
    }

    public String getField() {
        return field;
    }

    public Object getRejectedValue() {
        return rejectedValue;
    }

    public String getMessage() {
        return message;
    }
}

