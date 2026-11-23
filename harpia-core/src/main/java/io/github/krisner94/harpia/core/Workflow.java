package io.github.krisner94.harpia.core;

import jakarta.annotation.PostConstruct;

/**
 * Composition of executable units that is also a {@link FlowItem}, so a
 * workflow can be used as a step of another workflow.
 *
 * <p>The composition is declared once in {@link #configure()}, during
 * {@code @PostConstruct}, and reused by every execution.</p>
 *
 * @param <I> workflow input
 * @param <O> workflow output
 */
public abstract class Workflow<I, O> implements FlowItem<I, O> {

    /**
     * Flow built by {@link #configure()}.
     */
    protected volatile FlowItem<I, O> flowBuilder;

    /**
     * Declares and builds the flow. Must assign {@link #flowBuilder}.
     */
    protected abstract void configure();

    /**
     * Builds the composition once, after CDI injection.
     *
     * @throws FlowConfigurationException when {@link #configure()} does not assign a flow
     */
    @PostConstruct
    final void initialize() {
        if (flowBuilder != null) {
            return;
        }
        synchronized (this) {
            if (flowBuilder != null) {
                return;
            }
            configure();
            if (flowBuilder == null) {
                throw new FlowConfigurationException(
                        getClass().getName() + ".configure() must assign flowBuilder");
            }
        }
    }

    /**
     * Executes the workflow through the composition built by {@link #configure()}.
     *
     * @param input value passed to the first unit
     * @return value produced by the last unit
     */
    @Override
    public final O execute(I input) {
        FlowItem<I, O> flow = flowBuilder;
        if (flow == null) {
            initialize();
            flow = flowBuilder;
        }
        return flow.execute(input);
    }

    /**
     * Returns the built flow, initializing it when necessary.
     *
     * @return the immutable flow
     */
    protected final FlowItem<I, O> flow() {
        initialize();
        return flowBuilder;
    }
}
