package io.github.krisner94.harpia.problem;

import java.util.Objects;

/**
 * Runtime exception carrying an RFC 9457 {@link ProblemDetails}.
 *
 * <p>It is mapped to an HTTP response by {@code ProblemDetailsExceptionMapper}.
 * Throw it from a controller (or via {@link Results#orThrow(Result)}) to
 * produce a typed {@code application/problem+json} response.</p>
 */
public class ProblemDetailsException extends RuntimeException {

    private final ProblemDetails problem;

    public ProblemDetailsException(ProblemDetails problem) {
        super(problem.title());
        this.problem = Objects.requireNonNull(problem, "problem");
    }

    /**
     * Creates the exception from a {@link Problem}.
     */
    public static ProblemDetailsException of(Problem problem) {
        return new ProblemDetailsException(problem.toProblemDetails());
    }

    /**
     * @return the problem carried by this exception
     */
    public ProblemDetails problem() {
        return problem;
    }
}
