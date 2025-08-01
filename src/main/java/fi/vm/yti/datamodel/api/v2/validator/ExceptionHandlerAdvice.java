package fi.vm.yti.datamodel.api.v2.validator;

import fi.vm.yti.common.exception.*;
import fi.vm.yti.datamodel.api.v2.endpoint.error.*;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import static org.springframework.http.HttpStatus.BAD_REQUEST;

@Order(Ordered.HIGHEST_PRECEDENCE)
@ControllerAdvice
public class ExceptionHandlerAdvice extends fi.vm.yti.common.exception.ExceptionHandlerAdvice
{
    @ExceptionHandler(OpenSearchException.class)
    protected  ResponseEntity<Object> handleOpenSearchException(OpenSearchException exception) {
        var apiError = new ApiGenericError(BAD_REQUEST);
        apiError.setMessage(exception.getMessage());
        apiError.setDetails("Index: " + exception.getIndex());
        return ResponseEntity.badRequest().body(apiError);
        //TODO is this really a BAD request since its caused by IO exception to opensearch
    }

    @ExceptionHandler(ResolvingException.class)
    protected  ResponseEntity<Object> handleMappingError(ResolvingException error) {
        var apiError = new ApiGenericError(BAD_REQUEST);
        apiError.setMessage(error.getMessage());
        apiError.setDetails(error.getDetails());
        return ResponseEntity.badRequest().body(apiError);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    protected ResponseEntity<Object> handleIllegalArgumentException(IllegalArgumentException ex) {
        var apiError = new ApiError(BAD_REQUEST);
        apiError.setMessage(ex.getMessage());
        return buildResponseEntity(apiError);
    }
}
