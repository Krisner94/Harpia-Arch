package io.github.krisner94.harpia.problem;

/**
 * Bridges a {@link Result} to HTTP problem responses.
 *
 * <p>Use {@link #orThrow(Result)} in a controller to unwrap the success value
 * or throw a {@link ProblemDetailsException} carrying the typed error.</p>
 */
public final class Results {

    private Results() {
    }

    /**
     * Unwraps the success value or throws a {@link ProblemDetailsException}.
     *
     * @param result the result to unwrap
     * @param <O> success value type
     * @param <E> error type, which must be a {@link Problem}
     * @return the success value
     * @throws ProblemDetailsException when the result is a {@link Result.Failure}
     */
    public static <O, E extends Problem> O orThrow(Result<O, E> result) {
        return result.fold(
                value -> value,
                error -> {
                    throw new ProblemDetailsException(error.toProblemDetails());
                });
    }
}
