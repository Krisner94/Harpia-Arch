package io.github.krisner94.harpia.problem.rest;

import io.github.krisner94.harpia.problem.ProblemDetails;
import io.github.krisner94.harpia.problem.ProblemDetailsException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

/**
 * Maps {@link ProblemDetailsException} to an RFC 9457
 * {@code application/problem+json} response.
 */
@Provider
public class ProblemDetailsExceptionMapper implements ExceptionMapper<ProblemDetailsException> {

    /**
     * Media type of RFC 9457 problem responses.
     */
    public static final MediaType APPLICATION_PROBLEM_JSON =
            MediaType.valueOf("application/problem+json");

    @Override
    public Response toResponse(ProblemDetailsException exception) {
        ProblemDetails problem = exception.problem();
        return Response.status(problem.status())
                .type(APPLICATION_PROBLEM_JSON)
                .entity(problem)
                .build();
    }
}
