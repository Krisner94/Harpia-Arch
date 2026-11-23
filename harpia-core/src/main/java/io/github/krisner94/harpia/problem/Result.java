package io.github.krisner94.harpia.problem;

import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.function.Function;

/**
 * Outcome of an operation that either succeeds with a value or fails with a
 * typed error.
 *
 * <p>It is a sealed interface with two implementations: {@link Success} and
 * {@link Failure}. Using it as the output type of an {@code Activity} makes the
 * error a part of the contract instead of a side effect.</p>
 *
 * @param <O> success value type
 * @param <E> error type
 */
public sealed interface Result<O, E> permits Result.Success, Result.Failure {

    /**
     * Creates a successful result.
     *
     * @param value success value
     * @param <O> success value type
     * @param <E> error type
     * @return a successful result
     */
    static <O, E> Result<O, E> success(O value) {
        return new Success<>(value);
    }

    /**
     * Creates a failed result.
     *
     * @param error failure error
     * @param <O> success value type
     * @param <E> error type
     * @return a failed result
     */
    static <O, E> Result<O, E> failure(E error) {
        return new Failure<>(error);
    }

    /**
     * @return {@code true} when this is a {@link Success}
     */
    boolean isSuccess();

    /**
     * @return {@code true} when this is a {@link Failure}
     */
    boolean isFailure();

    /**
     * Returns the success value.
     *
     * @return the success value
     * @throws NoSuchElementException when this is a {@link Failure}
     */
    O value();

    /**
     * Returns the failure error.
     *
     * @return the failure error
     * @throws NoSuchElementException when this is a {@link Success}
     */
    E error();

    /**
     * Maps the success value, leaving a failure untouched.
     */
    <R> Result<R, E> map(Function<? super O, ? extends R> mapper);

    /**
     * Maps the failure error, leaving a success untouched.
     */
    <F> Result<O, F> mapError(Function<? super E, ? extends F> mapper);

    /**
     * Chains a function that returns another result, leaving a failure untouched.
     */
    <R> Result<R, E> flatMap(Function<? super O, ? extends Result<R, E>> mapper);

    /**
     * Collapses both cases into a single value.
     */
    <R> R fold(Function<? super O, ? extends R> onSuccess,
               Function<? super E, ? extends R> onFailure);

    /**
     * Successful result.
     */
    record Success<O, E>(O value) implements Result<O, E> {

        public Success {
            Objects.requireNonNull(value, "value");
        }

        @Override
        public boolean isSuccess() {
            return true;
        }

        @Override
        public boolean isFailure() {
            return false;
        }

        @Override
        public E error() {
            throw new NoSuchElementException("Success has no error");
        }

        @Override
        public <R> Result<R, E> map(Function<? super O, ? extends R> mapper) {
            return new Success<>(mapper.apply(value));
        }

        @Override
        public <F> Result<O, F> mapError(Function<? super E, ? extends F> mapper) {
            return new Success<>(value);
        }

        @Override
        public <R> Result<R, E> flatMap(Function<? super O, ? extends Result<R, E>> mapper) {
            return mapper.apply(value);
        }

        @Override
        public <R> R fold(Function<? super O, ? extends R> onSuccess,
                          Function<? super E, ? extends R> onFailure) {
            return onSuccess.apply(value);
        }
    }

    /**
     * Failed result.
     */
    record Failure<O, E>(E error) implements Result<O, E> {

        public Failure {
            Objects.requireNonNull(error, "error");
        }

        @Override
        public boolean isSuccess() {
            return false;
        }

        @Override
        public boolean isFailure() {
            return true;
        }

        @Override
        public O value() {
            throw new NoSuchElementException("Failure has no value");
        }

        @Override
        public <R> Result<R, E> map(Function<? super O, ? extends R> mapper) {
            return new Failure<>(error);
        }

        @Override
        public <F> Result<O, F> mapError(Function<? super E, ? extends F> mapper) {
            return new Failure<>(mapper.apply(error));
        }

        @Override
        public <R> Result<R, E> flatMap(Function<? super O, ? extends Result<R, E>> mapper) {
            return new Failure<>(error);
        }

        @Override
        public <R> R fold(Function<? super O, ? extends R> onSuccess,
                          Function<? super E, ? extends R> onFailure) {
            return onFailure.apply(error);
        }
    }
}
