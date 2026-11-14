# Errors and diagnostics

Harpia Arch fails early: most errors appear at compilation time or during the Quarkus build, before
the application runs.

## Exception hierarchy

```java
public class FlowConfigurationException extends IllegalStateException {
}
```

It indicates an invalid flow or Workflow configuration. There is no automatic wrapping of Activity
exceptions.

## Compilation errors

### Output incompatible with the next input

```java
FlowBuilder.builder(String.class, CustomerContext.class)
        .step(lengthActivity)   // String -> Integer
        .step(lengthActivity);  // expects FlowItem<Integer, ...>
```

The compiler shows something like:

```text
required: FlowItem<Integer, CustomerContext, O>
found:    FlowItem<String, CustomerContext, Integer>
```

Fix the input type of the next Activity or use `FlowItem.adapt(...)` to convert explicitly.

### Incompatible context

```java
FlowBuilder.builder(String.class, CustomerContext.class)
        .step(new WorkflowWithAnotherContext());
```

A Workflow that requires another context does not compile. Use `adaptContext(...)`:

```java
.step(workflow.adaptContext(other ->
        new CustomerContext(other.correlationId())))
```

### Component with a context in a context-free flow

```java
FlowBuilder.builder(String.class)
        .step(workflowRequiringContext); // does not compile
```

Provide the context explicitly:

```java
FlowItem<String, Void, Customer> adapted =
        workflowRequiringContext.adaptContext((Void ignored) -> new CustomerContext("req-1"));
```

### Activity without context in a flow with context

This is allowed by the `step(SimpleFlowItem)` overload:

```java
FlowBuilder.builder(String.class, CustomerContext.class)
        .step(contextFreeActivity) // compiles; the context is ignored by the Activity
        .step(contextualActivity);
```

## `FlowConfigurationException`

It is thrown when `configure()` does not assign `flowBuilder`:

```java
@WorkflowDefinition
public class BrokenWorkflow extends Workflow<String, Object, String> {

    @Override
    protected void configure() {
        // forgot to assign flowBuilder
    }
}
```

```text
BrokenWorkflow.configure() must assign flowBuilder
```

`FlowBuilder` also throws this exception if `build()` is called without any step. In the public API
this does not happen because the `Starter` requires the first step.

## Architectural validation at build time

`harpia-core-deployment` interrupts the build with a message that contains the class and the rule:

```text
@Activity com.acme.InvalidActivity must implement
io.github.krisner94.harpia.core.FlowItem or
io.github.krisner94.harpia.core.SimpleFlowItem
```

The rules and complete examples are in [Build-time validation](build-validation.md).

## Activity exceptions

The original exception is propagated without wrapping and the following steps do not execute:

```java
FlowItem<String, TestTrace, String> failing = (input, context) -> {
    throw new IllegalStateException("boom");
};
```

If the first step throws `IllegalStateException`, the caller receives the same instance. Harpia Arch
does not perform retry, rollback or exception conversion.

## Quick diagnosis

| Symptom | Where to look |
|---|---|
| Type error in `.step()` | Input/output declared in the Activity and in the Workflow |
| Context error | Context type in the Workflow, `adaptContext` or `SimpleFlowItem.from` |
| Build fails with `must implement` | Does the annotated class implement the correct contract? |
| Build fails with `must extend` | Does the Workflow extend `Workflow` or `SimpleWorkflow`? |
| Build fails with `@Path`/verb | Is the Controller or the Activity declaring a route? |
| `must not depend on @Controller` | Did the Repository receive a Controller through a field or parameter? |
| `must assign flowBuilder` | Does `configure()` assign the composition? |
| CDI test does not inject the bean | Is the annotation one of the registered stereotypes? |

## Useful commands

```bash
./mvnw -pl harpia-core test
./mvnw -pl harpia-core-deployment test
./mvnw -pl harpia-integration-tests test
./mvnw -Dit verify
```

For a Quarkus build failure, run with more details:

```bash
./mvnw -pl harpia-integration-tests -Dit verify -X
```

## Limits

The core does not implement:

- retry;
- compensation/Saga;
- transaction rollback;
- global exception handling;
- automatic observability.

These policies belong to the application. See [FAQ](../faq.md) and
[Persistence](../guides/persistence.md).
