package io.github.krisner94.harpia.core;

/**
 * Typed stage of the composed pipeline.
 *
 * <p>Every composition keeps the concrete input and output types, so the
 * pipeline never carries values as {@code Object}.</p>
 *
 * @param <I> stage input
 * @param <C> execution context
 * @param <O> stage output
 */
@FunctionalInterface
interface Stage<I, C, O> {

    O execute(I input, C context);
}
