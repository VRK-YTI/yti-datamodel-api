package fi.vm.yti.datamodel.api.v2.validator;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;

import fi.vm.yti.datamodel.api.v2.endpoint.error.ApiError;
import fi.vm.yti.datamodel.api.v2.endpoint.error.ApiValidationError;
import fi.vm.yti.datamodel.api.v2.endpoint.error.ApiValidationErrorDetails;
import fi.vm.yti.datamodel.api.v2.endpoint.error.ResourceNotFoundException;
import org.hibernate.validator.internal.engine.path.PathImpl;
import org.jetbrains.annotations.NotNull;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.util.stream.Collectors;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@Order(Ordered.HIGHEST_PRECEDENCE)
@ControllerAdvice
public class ExceptionHandlerAdvice extends ResponseEntityExceptionHandler {

    @ExceptionHandler(Throwable.class)
    public void logAll(Throwable throwable,
                       HttpServletRequest request) throws Throwable {
        logger.warn("Rogue catchable thrown while handling request to \"" + request.getServletPath() + "\"", throwable);
        throw throwable;
    }

    @Override
    protected @NotNull ResponseEntity<Object> handleHttpMessageNotReadable(
            @NotNull HttpMessageNotReadableException ex,
            @NotNull HttpHeaders headers,
            @NotNull HttpStatusCode status,
            @NotNull WebRequest request) {
        return buildResponseEntity(new ApiError(HttpStatus.BAD_REQUEST, "Malformed JSON request", ex));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    protected ResponseEntity<Object> handleConstraintViolationException(
            ConstraintViolationException ex) {
        var apiError = new ApiValidationError(BAD_REQUEST);
        apiError.setMessage("Object validation failed");
        var errors = ex.getConstraintViolations().stream()
                .map(c -> new ApiValidationErrorDetails(
                        c.getMessage(),
                        ((PathImpl) c.getPropertyPath()).getLeafNode().getName(),
                        c.getInvalidValue().toString()))
                .collect(Collectors.toList());

        apiError.setDetails(errors);
        return buildResponseEntity(apiError);
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    protected ResponseEntity<Object> handleNotFoundException(ResourceNotFoundException ex) {
        var apiError = new ApiError(NOT_FOUND);
        apiError.setMessage(ex.getMessage());
        return buildResponseEntity(apiError);
    }

    private ResponseEntity<Object> buildResponseEntity(ApiError apiError) {
        return new ResponseEntity<>(apiError, apiError.getStatus());
    }
}
