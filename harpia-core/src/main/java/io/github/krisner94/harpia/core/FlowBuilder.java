package io.github.krisner94.harpia.core;

import java.util.Objects;

/**
 * Immutable builder of executable flows.
 *
 * <p>The {@code OUT} parameter is the output type of the last added unit. Each
 * {@link #step(FlowItem)} requires a unit whose input matches {@code OUT} and
 * returns a builder typed with the new output, so an incompatible chain does not
 * compile.</p>
 *
 * @param <I> flow input
 * @param <OUT> output of the last added unit
 */
public final class FlowBuilder<I, OUT> {

    private final Stage<I, OUT> stage;

    private FlowBuilder(Stage<I, OUT> stage) {
        this.stage = stage;
    }

    /**
     * Starts a composition from a named unit.
     *
     * <p>The input and output types are inferred from the unit, so no class
     * token is required.</p>
     *
     * @param first first unit
     * @param <I> flow input
     * @param <O> output of the first unit
     * @return a builder with the first step already added
     */
    public static <I, O> FlowBuilder<I, O> from(FlowItem<I, O> first) {
        Objects.requireNonNull(first, "first");
        return new FlowBuilder<>(first::execute);
    }

    /**
     * Starts a composition anchoring the input type.
     *
     * <p>The class token anchors the generic input type of the chain for inline
     * lambdas; it does not perform runtime type checks. The guarantee is enforced
     * by the compiler.</p>
     *
     * @param inputType flow input type
     * @param <I> flow input
     * @return the first element of the composition
     */
    public static <I> Starter<I> builder(Class<I> inputType) {
        Objects.requireNonNull(inputType, "inputType");
        return new Starter<>();
    }

    /**
     * Appends the next unit.
     *
     * @param next unit that consumes the output of the last added unit
     * @param <O> output of the new unit
     * @return a new builder typed with the new output
     */
    public <O> FlowBuilder<I, O> step(FlowItem<OUT, O> next) {
        Objects.requireNonNull(next, "next");
        Stage<I, OUT> previous = stage;
        Stage<I, O> composed = input -> next.execute(previous.execute(input));
        return new FlowBuilder<>(composed);
    }

    /**
     * Builds the immutable flow.
     *
     * @return the executable flow
     */
    public FlowItem<I, OUT> build() {
        return stage::execute;
    }

    /**
     * First element of the composition.
     *
     * @param <I> flow input
     */
    public static final class Starter<I> {

        private Starter() {
        }

        /**
         * Adds the first unit.
         *
         * @param first first unit
         * @param <O> output of the first unit
         * @return a builder typed with the output of the first unit
         */
        public <O> FlowBuilder<I, O> step(FlowItem<I, O> first) {
            Objects.requireNonNull(first, "first");
            return new FlowBuilder<>(first::execute);
        }
    }
}
