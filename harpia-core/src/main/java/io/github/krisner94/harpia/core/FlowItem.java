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
 * @param <O> output type
 */
@FunctionalInterface
public interface FlowItem<I, O> {

    /**
     * Executes this unit.
     *
     * @param input value produced by the previous unit
     * @return value consumed by the next unit
     */
    O execute(I input);

    /**
     * Adapts input and output explicitly.
     *
     * @param item unit to adapt
     * @param inputAdapter conversion from the external input to the unit input
     * @param outputAdapter conversion from the unit output to the external output
     * @param <I> external input
     * @param <O> external output
     * @param <SI> unit input
     * @param <SO> unit output
     * @return the adapted unit
     */
    static <I, O, SI, SO> FlowItem<I, O> adapt(
            FlowItem<SI, SO> item,
            Function<I, SI> inputAdapter,
            Function<SO, O> outputAdapter) {
        Objects.requireNonNull(item, "item");
        Objects.requireNonNull(inputAdapter, "inputAdapter");
        Objects.requireNonNull(outputAdapter, "outputAdapter");
        return input -> outputAdapter.apply(item.execute(inputAdapter.apply(input)));
    }
}
