package io.github.krisner94.harpia.problem;

/**
 * Contract of a domain error that knows how to describe itself as an RFC 9457
 * {@link ProblemDetails}.
 *
 * <p>Implement this interface on the error types used by {@link Result} so they
 * can be turned into HTTP problem responses by {@link Results#orThrow(Result)}.
 * </p>
 */
public interface Problem {

    /**
     * @return the problem representation of this error
     */
    ProblemDetails toProblemDetails();
}
