package io.github.krisner94.harpia.core;

import java.util.Objects;
import java.util.function.Function;

/**
 * Contract of an executable unit of a workflow.
 *
 * <p>Every activity and every workflow implements this interface. The types are
 * explicit: the output of a unit is the input of the next one.</p>
 *
 * @param <I> input type
 * @param <C> execution context type
 * @param <O> output type
 */
@FunctionalInterface
public interface FlowItem<I, C, O> {

    /**
     * Executes this unit.
     *
     * @param input value produced by the previous unit
     * @param context execution context
     * @return value consumed by the next unit
     */
    O execute(I input, C context);

    /**
     * Adapts this unit to another context type.
     *
     * @param adapter conversion from the external context to this unit context
     * @param <C2> external context type
     * @return the unit with the external context contract
     */
    default <C2> FlowItem<I, C2, O> adaptContext(Function<C2, C> adapter) {
        Objects.requireNonNull(adapter, "adapter");
        return (input, context) -> execute(input, adapter.apply(context));
    }

    /**
     * Adapts input, context and output explicitly.
     *
     * @param item unit to adapt
     * @param inputAdapter conversion from the external input to the unit input
     * @param contextAdapter conversion from the external context to the unit context
     * @param outputAdapter conversion from the unit output to the external output
     * @param <I> external input
     * @param <C> external context
     * @param <O> external output
     * @param <SI> unit input
     * @param <SC> unit context
     * @param <SO> unit output
     * @return the adapted unit
     */
    static <I, C, O, SI, SC, SO> FlowItem<I, C, O> adapt(
            FlowItem<SI, SC, SO> item,
            Function<I, SI> inputAdapter,
            Function<C, SC> contextAdapter,
            Function<SO, O> outputAdapter) {
        Objects.requireNonNull(item, "item");
        Objects.requireNonNull(inputAdapter, "inputAdapter");
        Objects.requireNonNull(contextAdapter, "contextAdapter");
        Objects.requireNonNull(outputAdapter, "outputAdapter");
        return (input, context) -> outputAdapter.apply(item.execute(
                inputAdapter.apply(input),
                contextAdapter.apply(context)));
    }
}
