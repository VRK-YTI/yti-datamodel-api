package fi.vm.yti.datamodel.api.v2.endpoint.error;

import org.springframework.http.HttpStatus;

public class ApiGenericError extends ApiError {

    private String message;

    private String details;

    public ApiGenericError(HttpStatus status) {
        super(status);
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getDetails() {
        return details;
    }

    public void setDetails(String details) {
        this.details = details;
    }
}

