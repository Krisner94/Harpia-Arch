package io.github.krisner94.harpia.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class WorkflowTest {

    @Test
    void configuresOnceAndReusesTheFlow() {
        RecordingWorkflow workflow = new RecordingWorkflow();
        TestTrace first = new TestTrace();
        TestTrace second = new TestTrace();

        assertEquals(4, workflow.execute(1, first));
        assertEquals(12, workflow.execute(5, second));
        assertEquals(1, workflow.configurations.get());
        assertEquals(List.of("one:1", "two:2"), first.steps());
        assertEquals(List.of("one:5", "two:6"), second.steps());
    }

    @Test
    void exposesBuiltFlow() {
        RecordingWorkflow workflow = new RecordingWorkflow();

        assertNotNull(workflow.flow());
        assertEquals(1, workflow.configurations.get());
    }

    @Test
    void failsWhenConfigureDoesNotAssignAFlow() {
        BrokenWorkflow workflow = new BrokenWorkflow();

        FlowConfigurationException failure = assertThrows(FlowConfigurationException.class,
                () -> workflow.execute("input", new TestTrace()));

        assertEquals(BrokenWorkflow.class.getName() + ".configure() must assign flowBuilder",
                failure.getMessage());
    }

    static final class RecordingWorkflow extends Workflow<Integer, TestTrace, Integer> {

        private final AtomicInteger configurations = new AtomicInteger();

        @Override
        protected void configure() {
            configurations.incrementAndGet();
            flowBuilder = FlowBuilder.builder(Integer.class, TestTrace.class)
                    .step((input, context) -> {
                        context.add("one:" + input);
                        return input + 1;
                    })
                    .step((input, context) -> {
                        context.add("two:" + input);
                        return input * 2;
                    })
                    .build();
        }
    }

    static final class BrokenWorkflow extends Workflow<String, TestTrace, String> {

        @Override
        protected void configure() {
        }
    }
}
