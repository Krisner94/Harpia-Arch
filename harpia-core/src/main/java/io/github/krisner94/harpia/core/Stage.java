package io.github.krisner94.harpia.core;

/**
 * Typed stage of the composed pipeline.
 *
 * <p>Every composition keeps the concrete input and output types, so the
 * pipeline never carries values as {@code Object}.</p>
 *
 * @param <I> stage input
 * @param <O> stage output
 */
@FunctionalInterface
interface Stage<I, O> {

    O execute(I input);
}
