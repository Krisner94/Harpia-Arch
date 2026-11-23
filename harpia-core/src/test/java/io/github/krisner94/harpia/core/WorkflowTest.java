package io.github.krisner94.harpia.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class WorkflowTest {

    @Test
    void configuresOnceAndReusesTheFlow() {
        RecordingWorkflow workflow = new RecordingWorkflow();

        assertEquals(4, workflow.execute(1));
        assertEquals(12, workflow.execute(5));
        assertEquals(1, workflow.configurations.get());
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
                () -> workflow.execute("input"));

        assertEquals(BrokenWorkflow.class.getName() + ".configure() must assign flowBuilder",
                failure.getMessage());
    }

    static final class RecordingWorkflow extends Workflow<Integer, Integer> {

        private final AtomicInteger configurations = new AtomicInteger();

        @Override
        protected void configure() {
            configurations.incrementAndGet();
            flowBuilder = FlowBuilder.builder(Integer.class)
                    .step(input -> input + 1)
                    .step(input -> input * 2)
                    .build();
        }
    }

    static final class BrokenWorkflow extends Workflow<String, String> {

        @Override
        protected void configure() {
        }
    }
}
