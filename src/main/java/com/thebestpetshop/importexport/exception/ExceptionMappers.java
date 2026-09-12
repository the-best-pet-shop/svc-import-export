package com.thebestpetshop.importexport.exception;

import com.thebestpetshop.importexport.config.CorrelationFilter;
import com.thebestpetshop.importexport.model.ProblemDetails;
import jakarta.validation.ConstraintViolationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Provider
public class ExceptionMappers implements ExceptionMapper<RuntimeException> {
    @Override public Response toResponse(RuntimeException exception) {
        var status = exception instanceof ImportExportException e ? e.status() : exception instanceof ConstraintViolationException ? 422 : 500;
        var code = exception instanceof ImportExportException e ? e.code() : exception instanceof ConstraintViolationException ? "VALIDATION_ERROR" : "INTERNAL_ERROR";
        var message = status == 500 ? "an unexpected error occurred" : exception.getMessage();
        var id = UUID.randomUUID().toString();
        var body = new ProblemDetails(status, code, message, Map.of(), id, Instant.now());
        return Response.status(status).type(MediaType.APPLICATION_JSON).header("X-Correlation-Id", id).entity(body).build();
    }
}
