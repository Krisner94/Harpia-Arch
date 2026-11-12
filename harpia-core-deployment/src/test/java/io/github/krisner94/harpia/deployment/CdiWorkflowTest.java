package io.github.krisner94.harpia.deployment;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.github.krisner94.harpia.annotation.Activity;
import io.github.krisner94.harpia.annotation.WorkflowDefinition;
import io.github.krisner94.harpia.core.FlowBuilder;
import io.github.krisner94.harpia.core.FlowItem;
import io.github.krisner94.harpia.core.Workflow;
import io.quarkus.test.QuarkusExtensionTest;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import java.util.concurrent.atomic.AtomicInteger;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

class CdiWorkflowTest {

    @RegisterExtension
    static final QuarkusExtensionTest UNIT_TEST = new QuarkusExtensionTest()
            .withApplicationRoot((JavaArchive jar) -> jar.addClasses(IncrementActivity.class,
                    IncrementWorkflow.class));

    @Inject
    Instance<IncrementWorkflow> workflowInstance;

    @Test
    void initializesOnCdiCreationAndExecutesThroughCdi() {
        IncrementWorkflow workflow = workflowInstance.get();

        assertEquals(1, workflow.configurations.get());
        assertEquals(6, workflow.execute(5, null));
        assertEquals(7, workflow.execute(6, null));
        assertEquals(1, workflow.configurations.get());
    }

    @Activity
    static class IncrementActivity implements FlowItem<Integer, Object, Integer> {

        @Override
        public Integer execute(Integer input, Object context) {
            return input + 1;
        }
    }

    @WorkflowDefinition
    static class IncrementWorkflow extends Workflow<Integer, Object, Integer> {

        private final AtomicInteger configurations = new AtomicInteger();

        @Inject
        IncrementActivity activity;

        @Override
        protected void configure() {
            configurations.incrementAndGet();
            flowBuilder = FlowBuilder.builder(Integer.class, Object.class)
                    .step(activity)
                    .build();
        }
    }
}
