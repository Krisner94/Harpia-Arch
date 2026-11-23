package io.github.krisner94.harpia.problem;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;
import java.util.NoSuchElementException;
import org.junit.jupiter.api.Test;

class ResultTest {

    @Test
    void distinguishesSuccessFromFailure() {
        Result<String, ProblemDetails> success = Result.success("ok");
        Result<String, ProblemDetails> failure = Result.failure(ProblemDetails.of(404, "Not Found"));

        assertTrue(success.isSuccess());
        assertFalse(success.isFailure());
        assertFalse(failure.isSuccess());
        assertTrue(failure.isFailure());
    }

    @Test
    void exposesValueAndError() {
        Result<String, ProblemDetails> success = Result.success("ok");
        Result<String, ProblemDetails> failure = Result.failure(ProblemDetails.of(404, "Not Found"));

        assertEquals("ok", success.value());
        assertEquals("Not Found", failure.error().title());
        assertThrows(NoSuchElementException.class, success::error);
        assertThrows(NoSuchElementException.class, failure::value);
    }

    @Test
    void mapsBothSides() {
        Result<Integer, ProblemDetails> success = Result.<Integer, ProblemDetails>success(2)
                .map(value -> value * 3);
        Result<Integer, String> mappedError = Result.<Integer, String>failure("boom")
                .mapError(String::toUpperCase);

        assertEquals(6, success.value());
        assertEquals("BOOM", mappedError.error());
    }

    @Test
    void foldsIntoASingleValue() {
        Result<Integer, String> success = Result.success(2);
        Result<Integer, String> failure = Result.failure("boom");

        assertEquals("2", success.fold(String::valueOf, error -> error));
        assertEquals("boom", failure.fold(String::valueOf, error -> error));
    }

    @Test
    void flatMapsSuccessAndShortCircuitsFailure() {
        Result<Integer, String> success = Result.<Integer, String>success(2)
                .flatMap(value -> Result.success(value + 1));
        Result<Integer, String> failure = Result.<Integer, String>failure("boom")
                .flatMap(value -> Result.success(value + 1));

        assertEquals(3, success.value());
        assertEquals("boom", failure.error());
    }

    @Test
    void orThrowUnwrapsSuccess() {
        Result<String, ProblemDetails> success = Result.success("ok");

        assertEquals("ok", Results.orThrow(success));
    }

    @Test
    void orThrowThrowsProblemDetailsExceptionOnFailure() {
        ProblemDetails problem = ProblemDetails.of(422, "Unprocessable Entity", "name is required");
        Result<String, ProblemDetails> failure = Result.failure(problem);

        ProblemDetailsException thrown = assertThrows(ProblemDetailsException.class,
                () -> Results.orThrow(failure));

        assertSame(problem, thrown.problem());
    }

    @Test
    void orThrowAcceptsDomainErrorsImplementingProblem() {
        Result<String, NotFoundError> failure = Result.failure(new NotFoundError("customer", "1"));

        ProblemDetailsException thrown = assertThrows(ProblemDetailsException.class,
                () -> Results.orThrow(failure));

        assertEquals(404, thrown.problem().status());
        assertEquals("customer 1 not found", thrown.problem().detail());
    }

    @Test
    void buildsProblemDetailsWithExtensions() {
        ProblemDetails problem = ProblemDetails.builder(400, "Validation Failed")
                .detail("The request failed validation")
                .extension("errors", Map.of("name", "must not be blank"))
                .build();

        assertEquals(400, problem.status());
        assertEquals(Map.of("errors", Map.of("name", "must not be blank")), problem.extensions());
    }

    @Test
    void problemDetailsImplementsProblem() {
        ProblemDetails problem = ProblemDetails.of(500, "Internal Server Error");

        assertSame(problem, problem.toProblemDetails());
    }

    record NotFoundError(String entity, String id) implements Problem {

        @Override
        public ProblemDetails toProblemDetails() {
            return ProblemDetails.builder(404, "Not Found")
                    .detail(entity + " " + id + " not found")
                    .build();
        }
    }
}
