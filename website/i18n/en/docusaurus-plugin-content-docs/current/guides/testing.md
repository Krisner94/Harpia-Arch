# Testing and quality

The Harpia Arch testing strategy combines behavior tests, compilation tests and architecture
tests. The goal is not only to cover happy paths, but to prove that the composition is safe
**before** execution.

## Test pyramid

| Level | Module | What it proves |
|---|---|---|
| Unit | `harpia-core` | builder, Workflow, composition, optional context and concurrency |
| Negative compilation | `harpia-core` | incompatible contracts do not compile |
| Core purity | `harpia-core` | no reflection and no casts on the critical path |
| Jandex validation | `harpia-core-deployment` | architectural rules fail the build |
| CDI | `harpia-core-deployment` | beans, `@PostConstruct` and actual execution through ArC |
| JVM integration | `harpia-integration-tests` | REST + Hibernate ORM + H2 |
| Native integration | `harpia-integration-tests` | the same flow in the native executable |
| Benchmarks | `harpia-benchmarks` | engine cost and allocations |

## Existing suites

### `harpia-core`

| Class | Coverage |
|---|---|
| `FlowBuilderTest` | step order, single-step flow, shared context |
| `WorkflowTest` | single construction, `flow()`, failure when `configure()` does not assign |
| `WorkflowCompositionTest` | subworkflows, context adaptation, concurrent isolation, exceptions |
| `OptionalContextTest` | `Void`, `SimpleFlowItem`, `SimpleWorkflow`, mixed composition |
| `ContextlessCompileSafetyTest` | negative compilation with `javax.tools` |
| `CorePurityTest` | absence of reflection and casts in the core code |
| `ArchitecturalAnnotationsTest` | stereotypes and the `@HarpiaApplication` marker |

### `harpia-core-deployment`

| Class | Coverage |
|---|---|
| `ArchitectureValidatorTest` | rules for Activity, Workflow, Controller, Repository and application |
| `CdiWorkflowTest` | CDI initialization and Workflow execution with and without context |

### `harpia-integration-tests`

| Class | Coverage |
|---|---|
| `PersistCustomerWorkflowTest` | Workflow + Repository + Hibernate ORM in `@QuarkusTest` |
| `CustomerWorkflowIT` | `POST /customers` and `GET /customers/{id}` in `@QuarkusIntegrationTest` |

## Commands

```bash
# Full suite
./mvnw test

# Isolated modules
./mvnw -pl harpia-core test
./mvnw -pl harpia-core-deployment test
./mvnw -pl harpia-integration-tests test

# JVM integration
./mvnw -pl harpia-integration-tests verify -Dit

# Native Image
./mvnw -pl harpia-integration-tests verify -Dnative
```

CI runs `./mvnw -B -ntp -Dit verify` on Java 25.

## How to write a composition test

Use concrete types and a simple context. There is no need to mock the engine:

```java
class CreateCustomerTest {

    record TraceContext(List<String> steps) {
    }

    @Test
    void composesActivitiesInOrder() {
        TraceContext context = new TraceContext(new ArrayList<>());

        FlowItem<String, TraceContext, String> trim = (input, trace) -> {
            trace.steps().add("trim");
            return input.trim();
        };

        FlowItem<String, TraceContext, String> upper = (input, trace) -> {
            trace.steps().add("upper");
            return input.toUpperCase(Locale.ROOT);
        };

        FlowItem<String, TraceContext, String> flow = FlowBuilder
                .builder(String.class, TraceContext.class)
                .step(trim)
                .step(upper)
                .build();

        assertEquals("ANA", flow.execute(" ana ", context));
        assertEquals(List.of("trim", "upper"), context.steps());
    }
}
```

Prefer testing the public contract: `execute`, `FlowBuilder` and the Workflow. Do not test internal
details of `Stage`.

## How to test optional context

```java
SimpleFlowItem<String, String> trim = input -> input.trim();

FlowItem<String, TraceContext, String> flow = FlowBuilder
        .builder(String.class, TraceContext.class)
        .step(trim)
        .step((input, context) -> {
            context.steps().add("after-trim");
            return input;
        })
        .build();
```

The test should verify that the context-free Activity works and that the context remains available
to the next step.

## Negative compilation

`ContextlessCompileSafetyTest` compiles invalid sources at test time with
`ToolProvider.getSystemJavaCompiler()` and verifies that the compiler rejects:

- an Activity that requires context in a context-free flow;
- a Workflow that requires context in a context-free flow;
- a context of incompatible type.

This pattern is useful when the guarantee comes from the compiler, not the runtime.

## Critical path purity

`CorePurityTest` scans the source code of the `core` package and fails if it finds:

- `java.lang.reflect`;
- `Class.forName`;
- `getDeclaredMethod`/`getMethod`;
- `isAssignableFrom`;
- `instanceof` on the flow contracts;
- casts to `FlowItem`, `Workflow`, `SimpleFlowItem` or `SimpleWorkflow`;
- a cast to `Object`.

The test is an architecture guard. It does not replace code review, but it prevents obvious
regressions.

## Quarkus integration

`PersistCustomerWorkflowTest` uses `@QuarkusTest` and validates the complete path down to the H2
database. `CustomerWorkflowIT` uses `@QuarkusIntegrationTest` and validates the packaged artifact
over HTTP.

For a new use case, test:

1. the Workflow in isolation with a fake context;
2. the Activity with real dependencies or doubles;
3. the Controller with Rest Assured;
4. the native build when the application uses reflection-sensitive resources.

## Concurrency

`WorkflowCompositionTest.isolatesConcurrentExecutions` runs 128 calls on a pool of 8 threads with
independent contexts. The test proves that:

- the composition is shared;
- the context is isolated per execution;
- the Workflow does not hold request state.

## Benchmark

```bash
./mvnw -pl harpia-benchmarks -am package
java -jar harpia-benchmarks/target/harpia-benchmarks.jar
```

See [Benchmarks](benchmarks.md) and [Performance](performance.md).

## Coverage

The current build does not configure a coverage plugin. Quality is measured by behavior,
compilation, architecture, CDI, integration and native tests. If coverage is added, it must
complement these tests, not replace them.
