# Workflow engine

## FlowItem

`FlowItem<I, C, O>` is the contract of every executable unit:

```java
@FunctionalInterface
public interface FlowItem<I, C, O> {

    O execute(I input, C context);
}
```

| Type | Meaning |
|---|---|
| `I` | unit input |
| `C` | shared context |
| `O` | produced output |

`Workflow` also implements `FlowItem`, so it can be used as a step of another workflow.

## Activity

An Activity implements `FlowItem`:

```java
@Activity
public class ValidateCustomerActivity
        implements FlowItem<CustomerInput, CustomerContext, ValidatedCustomer> {

    @Override
    public ValidatedCustomer execute(CustomerInput input, CustomerContext context) {
        return new ValidatedCustomer(input.name().trim());
    }
}
```

## Workflow

```java
@WorkflowDefinition
public class CreateCustomerWorkflow
        extends Workflow<CustomerInput, CustomerContext, Customer> {

    private final ValidationWorkflow validation;
    private final PersistenceWorkflow persistence;

    public CreateCustomerWorkflow(ValidationWorkflow validation,
            PersistenceWorkflow persistence) {
        this.validation = validation;
        this.persistence = persistence;
    }

    @Override
    protected void configure() {
        flowBuilder = FlowBuilder.builder(CustomerInput.class, CustomerContext.class)
                .step(validation)
                .step(persistence)
                .build();
    }
}
```

### Lifecycle

- `configure()` is called once in `@PostConstruct`.
- The built flow is assigned to `flowBuilder`.
- `execute()` is `final` and uses the built composition.
- Manually instantiated workflows initialize on the first execution.
- Invalid configuration throws `FlowConfigurationException`.

```java
public abstract class Workflow<I, C, O> implements FlowItem<I, C, O> {

    protected volatile FlowItem<I, C, O> flowBuilder;

    protected abstract void configure();

    @PostConstruct
    final void initialize() { ... }

    @Override
    public final O execute(I input, C context) { ... }
}
```

## FlowBuilder

The only operation that adds steps is `.step()`:

```java
flowBuilder = FlowBuilder.builder(CustomerInput.class, CustomerContext.class)
        .step(validateCustomer)
        .step(enrichCustomer)
        .step(saveCustomer)
        .build();
```

- `builder(Class, Class)` anchors the input and context types;
- `.step()` infers the output from the Activity;
- `.build()` returns the immutable composition;
- the builder is immutable: each `.step()` returns a new builder.

### Type safety

The builder output type changes at every step. An incompatible chain does not compile:

```java
FlowBuilder.builder(String.class, CustomerContext.class)
        .step(lengthActivity)   // String -> Integer
        .step(lengthActivity);  // error: expects FlowItem<Integer, ...>
```

```text
required: FlowItem<Integer, CustomerContext, O>
found:    FlowItem<String, CustomerContext, Integer>
```

The guarantee is compile-time. Due to type erasure there is no runtime validation; the class
tokens in `builder()` are inference anchors, not reflective validation.

## Hierarchical composition

Because `Workflow` implements `FlowItem`, one workflow can be a step of another:

```java
flowBuilder = FlowBuilder.builder(CustomerInput.class, CustomerContext.class)
        .step(validationWorkflow)
        .step(enrichmentWorkflow)
        .step(persistenceWorkflow)
        .build();
```

Each subworkflow has its own input, context and output types.

### Different contexts

Adapt explicitly:

```java
flowBuilder = FlowBuilder.builder(CustomerInput.class, CustomerContext.class)
        .step(validationWorkflow)
        .step(enrichmentWorkflow.adaptContext(context ->
                new EnrichmentContext(context.correlationId())))
        .step(persistenceWorkflow)
        .build();
```

For different input and output types:

```java
FlowItem<I, C, O> adapted = FlowItem.adapt(
        item,
        inputAdapter,
        contextAdapter,
        outputAdapter);
```

There is no implicit context conversion.

## Concurrency

- the composition is immutable after initialization;
- the context is always an `execute` parameter;
- there is no `ThreadLocal`, global state or request data in bean fields;
- the same Workflow instance can serve concurrent executions;
- tests cover 128 concurrent executions with isolated contexts.

## Performance

- built once;
- executed through direct Java calls, without reflection;
- no `Object` between Activities;
- no casts on the execution path;
- only one state object per execution when needed;
- compatible with GraalVM Native Image.

See the [benchmarks](../guides/benchmarks.md).

## Out of scope

- automatic compensation/Saga;
- automatic retry;
- built-in observability;
- dynamic Activity discovery.

These behaviors were not specified and can be implemented by the application or in future modules.
