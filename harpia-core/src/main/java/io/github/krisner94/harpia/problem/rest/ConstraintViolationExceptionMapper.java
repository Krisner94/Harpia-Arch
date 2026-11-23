package io.github.krisner94.harpia.problem.rest;

import io.github.krisner94.harpia.problem.ProblemDetails;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Maps Bean Validation failures to an RFC 9457 {@code 400} problem with an
 * {@code errors} extension listing each violated property.
 */
@Provider
public class ConstraintViolationExceptionMapper
        implements ExceptionMapper<ConstraintViolationException> {

    @Override
    public Response toResponse(ConstraintViolationException exception) {
        Map<String, String> errors = new LinkedHashMap<>();
        for (ConstraintViolation<?> violation : exception.getConstraintViolations()) {
            errors.put(violation.getPropertyPath().toString(), violation.getMessage());
        }

        ProblemDetails problem = ProblemDetails.builder(400, "Validation Failed")
                .detail("The request failed validation")
                .extension("errors", errors)
                .build();

        return Response.status(problem.status())
                .type(ProblemDetailsExceptionMapper.APPLICATION_PROBLEM_JSON)
                .entity(problem)
                .build();
    }
}
