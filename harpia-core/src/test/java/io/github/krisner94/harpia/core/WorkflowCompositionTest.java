package io.github.krisner94.harpia.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;

class WorkflowCompositionTest {

    private static final FlowItem<CustomerInput, CustomerContext, CustomerInput> VALIDATE_DOCUMENT =
            (input, context) -> {
                context.trace().add("validate-document");
                if (input.name() == null || input.name().isBlank()) {
                    throw new IllegalArgumentException("name is required");
                }
                return input;
            };

    private static final FlowItem<CustomerInput, CustomerContext, ValidatedCustomer> VALIDATE_EMAIL =
            (input, context) -> {
                context.trace().add("validate-email");
                return new ValidatedCustomer(input.name().trim());
            };

    @Test
    void composesWorkflowsHierarchically() {
        CustomerWorkflow workflow = new CustomerWorkflow();
        CustomerContext context = new CustomerContext("corr-1", new ArrayList<>());

        Customer customer = workflow.execute(new CustomerInput("  Ana  "), context);

        assertEquals("Ana", customer.name());
        assertEquals("PREMIUM", customer.segment());
        assertEquals(List.of("validate-document", "validate-email", "enrich", "map"), context.trace());
        assertEquals("corr-1", context.correlationId());
    }

    @Test
    void reusesCompositionAcrossExecutions() {
        CustomerWorkflow workflow = new CustomerWorkflow();

        workflow.execute(new CustomerInput("Ana"), new CustomerContext("1", new ArrayList<>()));
        workflow.execute(new CustomerInput("Bia"), new CustomerContext("2", new ArrayList<>()));

        assertEquals(1, workflow.configurations.get());
    }

    @Test
    void adaptsContextExplicitly() {
        EnrichmentWorkflow workflow = new EnrichmentWorkflow();
        CustomerContext outer = new CustomerContext("corr-2", new ArrayList<>());

        FlowItem<ValidatedCustomer, CustomerContext, EnrichedCustomer> adapted =
                workflow.adaptContext(context ->
                        new EnrichmentContext(context.correlationId(), context.trace()));

        EnrichedCustomer enriched = adapted.execute(new ValidatedCustomer("Ana"), outer);

        assertEquals("PREMIUM", enriched.segment());
        assertEquals(List.of("enrich"), outer.trace());
    }

    @Test
    void adaptsInputAndOutputExplicitly() {
        FlowItem<Integer, Object, String> doubled = (input, context) -> String.valueOf(input * 2);

        FlowItem<String, Object, Integer> adapted = FlowItem.adapt(
                doubled,
                Integer::parseInt,
                context -> context,
                Integer::parseInt);

        assertEquals(84, adapted.execute("42", new Object()));
    }

    @Test
    void isolatesConcurrentExecutions() throws Exception {
        CustomerWorkflow workflow = new CustomerWorkflow();
        ExecutorService executor = Executors.newFixedThreadPool(8);

        try {
            List<Callable<String>> tasks = IntStream.range(0, 128)
                    .mapToObj(index -> (Callable<String>) () -> {
                        String name = "customer-" + index;
                        CustomerContext context = new CustomerContext(
                                "corr-" + index, new CopyOnWriteArrayList<>());
                        Customer customer = workflow.execute(new CustomerInput(name), context);
                        assertEquals(name, customer.name());
                        assertEquals("corr-" + index, context.correlationId());
                        assertEquals(List.of("validate-document", "validate-email", "enrich", "map"),
                                context.trace());
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
        TestTrace trace = new TestTrace();
        FlowItem<String, TestTrace, String> failing = (input, context) -> {
            throw original;
        };

        FlowItem<String, TestTrace, String> flow = FlowBuilder
                .builder(String.class, TestTrace.class)
                .step((input, context) -> {
                    context.add("first");
                    return input;
                })
                .step(failing)
                .step((input, context) -> {
                    context.add("third");
                    return input;
                })
                .build();

        IllegalStateException failure = assertThrows(IllegalStateException.class,
                () -> flow.execute("x", trace));

        assertSame(original, failure);
        assertEquals(List.of("first"), trace.steps());
    }

    record CustomerInput(String name) {
    }

    record ValidatedCustomer(String name) {
    }

    record EnrichedCustomer(String name, String segment) {
    }

    record Customer(String id, String name, String segment) {
    }

    record CustomerContext(String correlationId, List<String> trace) {
    }

    record EnrichmentContext(String correlationId, List<String> trace) {
    }

    static final class ValidationWorkflow
            extends Workflow<CustomerInput, CustomerContext, ValidatedCustomer> {

        @Override
        protected void configure() {
            flowBuilder = FlowBuilder.builder(CustomerInput.class, CustomerContext.class)
                    .step(VALIDATE_DOCUMENT)
                    .step(VALIDATE_EMAIL)
                    .build();
        }
    }

    static final class EnrichmentWorkflow
            extends Workflow<ValidatedCustomer, EnrichmentContext, EnrichedCustomer> {

        @Override
        protected void configure() {
            flowBuilder = FlowBuilder.builder(ValidatedCustomer.class, EnrichmentContext.class)
                    .step((input, context) -> {
                        context.trace().add("enrich");
                        return new EnrichedCustomer(input.name(), "PREMIUM");
                    })
                    .build();
        }
    }

    static final class PersistenceWorkflow
            extends Workflow<EnrichedCustomer, CustomerContext, Customer> {

        @Override
        protected void configure() {
            flowBuilder = FlowBuilder.builder(EnrichedCustomer.class, CustomerContext.class)
                    .step((input, context) -> {
                        context.trace().add("map");
                        return new Customer("id-" + input.name(), input.name(), input.segment());
                    })
                    .build();
        }
    }

    static final class CustomerWorkflow
            extends Workflow<CustomerInput, CustomerContext, Customer> {

        private final AtomicInteger configurations = new AtomicInteger();
        private final ValidationWorkflow validation = new ValidationWorkflow();
        private final EnrichmentWorkflow enrichment = new EnrichmentWorkflow();
        private final PersistenceWorkflow persistence = new PersistenceWorkflow();

        @Override
        protected void configure() {
            configurations.incrementAndGet();
            flowBuilder = FlowBuilder.builder(CustomerInput.class, CustomerContext.class)
                    .step(validation)
                    .step(enrichment.adaptContext(context ->
                            new EnrichmentContext(context.correlationId(), context.trace())))
                    .step(persistence)
                    .build();
        }
    }
}
