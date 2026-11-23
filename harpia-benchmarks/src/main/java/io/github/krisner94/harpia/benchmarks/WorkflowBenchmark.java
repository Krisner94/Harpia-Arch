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

    private static final FlowItem<Integer, Integer> INCREMENT = input -> input + 1;
    private static final java.util.function.IntUnaryOperator DIRECT = input -> input + 1;
    private static final FlowItem<Integer, Integer> PLAIN = INCREMENT;

    private FlowItem<Integer, Integer> single;
    private FlowItem<Integer, Integer> few;
    private FlowItem<Integer, Integer> many;
    private FlowItem<Integer, Integer> nested;
    private FlowItem<Integer, Integer> nestedWorkflow;

    @Setup
    public void setup() {
        single = FlowBuilder.from(INCREMENT)
                .build();
        few = FlowBuilder.from(INCREMENT)
                .step(INCREMENT)
                .step(INCREMENT)
                .build();
        many = FlowBuilder.from(INCREMENT)
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
        nestedWorkflow = new AddWorkflow(FlowBuilder.from(INCREMENT)
                .step(INCREMENT)
                .step(INCREMENT)
                .step(INCREMENT)
                .build());
        nested = FlowBuilder.from(nestedWorkflow)
                .step(nestedWorkflow)
                .step(nestedWorkflow)
                .build();
    }

    @Benchmark
    public int singleActivity() {
        return single.execute(0);
    }

    @Benchmark
    public int directCall() {
        return DIRECT.applyAsInt(0);
    }

    @Benchmark
    public int plainFlowItem() {
        return PLAIN.execute(0);
    }

    @Benchmark
    public int workflowWithThreeSteps() {
        return few.execute(0);
    }

    @Benchmark
    public int workflowWithSixteenSteps() {
        return many.execute(0);
    }

    @Benchmark
    public int nestedWorkflows() {
        return nested.execute(0);
    }

    @Benchmark
    public FlowItem<Integer, Integer> buildComposition() {
        return FlowBuilder.from(INCREMENT)
                .step(INCREMENT)
                .step(INCREMENT)
                .build();
    }

    static final class AddWorkflow extends Workflow<Integer, Integer> {

        private final FlowItem<Integer, Integer> inner;

        AddWorkflow(FlowItem<Integer, Integer> inner) {
            this.inner = inner;
        }

        @Override
        protected void configure() {
            flowBuilder = FlowBuilder.from(inner)
                    .build();
        }
    }
}
