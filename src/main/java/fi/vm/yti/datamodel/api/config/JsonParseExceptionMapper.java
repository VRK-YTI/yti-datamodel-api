package fi.vm.yti.datamodel.api.config;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

import com.fasterxml.jackson.core.JsonParseException;

@Provider
class JsonParseExceptionMapper implements ExceptionMapper<JsonParseException> {
    @Override
    public Response toResponse(final JsonParseException jpe) {
        return Response.status(Response.Status.BAD_REQUEST)
            .entity("Invalid JSON!").build();
    }
}