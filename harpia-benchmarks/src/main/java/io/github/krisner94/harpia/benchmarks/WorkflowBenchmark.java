package io.github.krisner94.harpia.benchmarks;

import io.github.krisner94.harpia.core.FlowBuilder;
import io.github.krisner94.harpia.core.FlowItem;
import io.github.krisner94.harpia.core.Workflow;
import java.util.concurrent.TimeUnit;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;

/**
 * Measures the cost of the Harpia Arch composition and execution.
 *
 * <p>Run the packaged benchmark jar, optionally with {@code -prof gc} to inspect
 * allocations per operation.</p>
 */
@State(Scope.Benchmark)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Warmup(iterations = 3, time = 500, timeUnit = TimeUnit.MILLISECONDS)
@Measurement(iterations = 5, time = 500, timeUnit = TimeUnit.MILLISECONDS)
@Fork(1)
public class WorkflowBenchmark {

    private static final Object CONTEXT = new Object();
    private static final FlowItem<Integer, Object, Integer> INCREMENT =
            (input, context) -> input + 1;
    private static final java.util.function.IntUnaryOperator DIRECT = input -> input + 1;
    private static final FlowItem<Integer, Object, Integer> PLAIN = INCREMENT;

    private FlowItem<Integer, Object, Integer> single;
    private FlowItem<Integer, Object, Integer> few;
    private FlowItem<Integer, Object, Integer> many;
    private FlowItem<Integer, Object, Integer> nested;
    private FlowItem<Integer, Object, Integer> nestedWorkflow;

    @Setup
    public void setup() {
        single = FlowBuilder.builder(Integer.class, Object.class)
                .step(INCREMENT)
                .build();
        few = FlowBuilder.builder(Integer.class, Object.class)
                .step(INCREMENT)
                .step(INCREMENT)
                .step(INCREMENT)
                .build();
        many = FlowBuilder.builder(Integer.class, Object.class)
                .step(INCREMENT)
                .step(INCREMENT)
                .step(INCREMENT)
                .step(INCREMENT)
                .step(INCREMENT)
                .step(INCREMENT)
                .step(INCREMENT)
                .step(INCREMENT)
                .step(INCREMENT)
                .step(INCREMENT)
                .step(INCREMENT)
                .step(INCREMENT)
                .step(INCREMENT)
                .step(INCREMENT)
                .step(INCREMENT)
                .step(INCREMENT)
                .build();
        nestedWorkflow = new AddWorkflow(FlowBuilder.builder(Integer.class, Object.class)
                .step(INCREMENT)
                .step(INCREMENT)
                .step(INCREMENT)
                .step(INCREMENT)
                .build());
        nested = FlowBuilder.builder(Integer.class, Object.class)
                .step(nestedWorkflow)
                .step(nestedWorkflow)
                .step(nestedWorkflow)
                .build();
    }

    @Benchmark
    public int singleActivity() {
        return single.execute(0, CONTEXT);
    }

    @Benchmark
    public int directCall() {
        return DIRECT.applyAsInt(0);
    }

    @Benchmark
    public int plainFlowItem() {
        return PLAIN.execute(0, CONTEXT);
    }

    @Benchmark
    public int workflowWithThreeSteps() {
        return few.execute(0, CONTEXT);
    }

    @Benchmark
    public int workflowWithSixteenSteps() {
        return many.execute(0, CONTEXT);
    }

    @Benchmark
    public int nestedWorkflows() {
        return nested.execute(0, CONTEXT);
    }

    @Benchmark
    public FlowItem<Integer, Object, Integer> buildComposition() {
        return FlowBuilder.builder(Integer.class, Object.class)
                .step(INCREMENT)
                .step(INCREMENT)
                .step(INCREMENT)
                .build();
    }

    static final class AddWorkflow extends Workflow<Integer, Object, Integer> {

        private final FlowItem<Integer, Object, Integer> inner;

        AddWorkflow(FlowItem<Integer, Object, Integer> inner) {
            this.inner = inner;
        }

        @Override
        protected void configure() {
            flowBuilder = FlowBuilder.builder(Integer.class, Object.class)
                    .step(inner)
                    .build();
        }
    }
}
