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
 * @param <C> execution context
 * @param <OUT> output of the last added unit
 */
public final class FlowBuilder<I, C, OUT> {

    private final Stage<I, C, OUT> stage;
    private final int steps;

    private FlowBuilder(Stage<I, C, OUT> stage, int steps) {
        this.stage = stage;
        this.steps = steps;
    }

    /**
     * Starts a composition.
     *
     * <p>The class tokens anchor the generic types of the chain. They do not
     * perform runtime type checks; the guarantee is enforced by the compiler.</p>
     *
     * @param inputType flow input type
     * @param contextType execution context type
     * @param <I> flow input
     * @param <C> execution context
     * @return the first element of the composition
     */
    public static <I, C> Starter<I, C> builder(Class<I> inputType, Class<C> contextType) {
        Objects.requireNonNull(inputType, "inputType");
        Objects.requireNonNull(contextType, "contextType");
        return new Starter<>();
    }

    /**
     * Appends the next unit.
     *
     * @param next unit that consumes the output of the last added unit
     * @param <O> output of the new unit
     * @return a new builder typed with the new output
     */
    public <O> FlowBuilder<I, C, O> step(FlowItem<OUT, C, O> next) {
        Objects.requireNonNull(next, "next");
        Stage<I, C, OUT> previous = stage;
        Stage<I, C, O> composed = (input, context) ->
                next.execute(previous.execute(input, context), context);
        return new FlowBuilder<>(composed, steps + 1);
    }

    /**
     * Builds the immutable flow.
     *
     * @return the executable flow
     */
    public FlowItem<I, C, OUT> build() {
        if (steps == 0) {
            throw new FlowConfigurationException("A flow requires at least one step");
        }
        return stage::execute;
    }

    /**
     * First element of the composition.
     *
     * @param <I> flow input
     * @param <C> execution context
     */
    public static final class Starter<I, C> {

        private Starter() {
        }

        /**
         * Adds the first unit.
         *
         * @param first first unit
         * @param <O> output of the first unit
         * @return a builder typed with the output of the first unit
         */
        public <O> FlowBuilder<I, C, O> step(FlowItem<I, C, O> first) {
            Objects.requireNonNull(first, "first");
            return new FlowBuilder<>(first::execute, 1);
        }
    }
}
