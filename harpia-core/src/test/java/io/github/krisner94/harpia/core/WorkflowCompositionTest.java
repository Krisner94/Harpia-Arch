package io.github.krisner94.harpia.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;

class WorkflowCompositionTest {

    private static final FlowItem<CustomerInput, CustomerInput> VALIDATE_DOCUMENT =
            input -> {
                if (input.name() == null || input.name().isBlank()) {
                    throw new IllegalArgumentException("name is required");
                }
                return input;
            };

    private static final FlowItem<CustomerInput, ValidatedCustomer> VALIDATE_EMAIL =
            input -> new ValidatedCustomer(input.name().trim());

    @Test
    void composesWorkflowsHierarchically() {
        CustomerWorkflow workflow = new CustomerWorkflow();

        Customer customer = workflow.execute(new CustomerInput("  Ana  "));

        assertEquals("Ana", customer.name());
        assertEquals("PREMIUM", customer.segment());
    }

    @Test
    void reusesCompositionAcrossExecutions() {
        CustomerWorkflow workflow = new CustomerWorkflow();

        workflow.execute(new CustomerInput("Ana"));
        workflow.execute(new CustomerInput("Bia"));

        assertEquals(1, workflow.configurations.get());
    }

    @Test
    void adaptsInputAndOutputExplicitly() {
        FlowItem<Integer, String> doubled = input -> String.valueOf(input * 2);

        FlowItem<String, Integer> adapted = FlowItem.adapt(
                doubled,
                Integer::parseInt,
                Integer::parseInt);

        assertEquals(84, adapted.execute("42"));
    }

    @Test
    void isolatesConcurrentExecutions() throws Exception {
        CustomerWorkflow workflow = new CustomerWorkflow();
        ExecutorService executor = Executors.newFixedThreadPool(8);

        try {
            List<Callable<String>> tasks = IntStream.range(0, 128)
                    .mapToObj(index -> (Callable<String>) () -> {
                        String name = "customer-" + index;
                        Customer customer = workflow.execute(new CustomerInput(name));
                        assertEquals(name, customer.name());
                        return customer.id();
                    })
                    .toList();

            List<Future<String>> results = executor.invokeAll(tasks);
            for (Future<String> result : results) {
                assertNotNull(result.get());
            }
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    void propagatesOriginalFailureWithoutRunningNextSteps() {
        IllegalStateException original = new IllegalStateException("boom");
        List<String> trace = new ArrayList<>();
        FlowItem<String, String> failing = input -> {
            throw original;
        };

        FlowItem<String, String> flow = FlowBuilder.builder(String.class)
                .step(input -> {
                    trace.add("first");
                    return input;
                })
                .step(failing)
                .step(input -> {
                    trace.add("third");
                    return input;
                })
                .build();

        IllegalStateException failure = assertThrows(IllegalStateException.class,
                () -> flow.execute("x"));

        assertSame(original, failure);
        assertEquals(List.of("first"), trace);
    }

    record CustomerInput(String name) {
    }

    record ValidatedCustomer(String name) {
    }

    record EnrichedCustomer(String name, String segment) {
    }

    record Customer(String id, String name, String segment) {
    }

    static final class ValidationWorkflow
            extends Workflow<CustomerInput, ValidatedCustomer> {

        @Override
        protected void configure() {
            flowBuilder = FlowBuilder.from(VALIDATE_DOCUMENT)
                    .step(VALIDATE_EMAIL)
                    .build();
        }
    }

    static final class EnrichmentWorkflow
            extends Workflow<ValidatedCustomer, EnrichedCustomer> {

        @Override
        protected void configure() {
            flowBuilder = FlowBuilder.from((ValidatedCustomer input) ->
                            new EnrichedCustomer(input.name(), "PREMIUM"))
                    .build();
        }
    }

    static final class PersistenceWorkflow
            extends Workflow<EnrichedCustomer, Customer> {

        @Override
        protected void configure() {
            flowBuilder = FlowBuilder.from((EnrichedCustomer input) ->
                            new Customer("id-" + input.name(), input.name(), input.segment()))
                    .build();
        }
    }

    static final class CustomerWorkflow
            extends Workflow<CustomerInput, Customer> {

        private final AtomicInteger configurations = new AtomicInteger();
        private final ValidationWorkflow validation = new ValidationWorkflow();
        private final EnrichmentWorkflow enrichment = new EnrichmentWorkflow();
        private final PersistenceWorkflow persistence = new PersistenceWorkflow();

        @Override
        protected void configure() {
            configurations.incrementAndGet();
            flowBuilder = FlowBuilder.from(validation)
                    .step(enrichment)
                    .step(persistence)
                    .build();
        }
    }
}
