# Workflow engine

## FlowItem

`FlowItem<I, C, O>` is the contract of every executable unit with a context:

```java
@FunctionalInterface
public interface FlowItem<I, C, O> {

    O execute(I input, C context);
}
```

| Type | Meaning |
|---|---|
| `I` | input of the unit |
| `C` | shared context |
| `O` | produced output |

`Workflow` also implements `FlowItem`, which is why it can be used as a step of another workflow.

## Optional context

The context is not mandatory. The developer can choose between two equivalent declaration forms.
The full reference, with the compatibility matrix and the explicit adapters, is in
[Optional context](context.md).

### 1. Explicit `Void`

When the preference is to keep the three-parameter contract, declare `Void`:

```java
FlowItem<Customer, Void, Customer>

Workflow<Void, Void, List<Customer>>
```

The context received by a `Void` unit is always `null`. No context instance is created. A
`Workflow` or `FlowBuilder` with `Void` keeps using the full three-generic-parameter contract.

### 2. Without a Context declaration

It is also possible to omit the context:

```java
SimpleFlowItem<Customer, Customer>

SimpleWorkflow<Void, List<Customer>>
```

`SimpleFlowItem` and `SimpleWorkflow` are the names of the context-less contracts. They are not a
second declaration of `FlowItem`/`Workflow`: Java does not allow overloading interfaces by the
number of generic parameters while keeping the same name in the same package. The context-less
syntax therefore uses its own nominal types.

```java
@FunctionalInterface
public interface SimpleFlowItem<I, O> extends FlowItem<I, Void, O> {

    O execute(I input);

    @Override
    default O execute(I input, Void context) {
        return execute(input);
    }
}
```

```java
public abstract class SimpleWorkflow<I, O> extends Workflow<I, Void, O>
        implements SimpleFlowItem<I, O> {

    @Override
    public final O execute(I input) {
        return execute(input, null);
    }
}
```

Because `SimpleFlowItem` extends `FlowItem<I, Void, O>`, the unit remains compatible with the
three-parameter contract wherever it is needed. The bridge passes `null`; there is no allocation of
an empty context object.

## Activity

An Activity may use a context:

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

Or it may not declare a context:

```java
@Activity
public class TrimCustomerNameActivity
        implements SimpleFlowItem<CustomerInput, CustomerInput> {

    @Override
    public CustomerInput execute(CustomerInput input) {
        return new CustomerInput(input.name().trim());
    }
}
```

An Activity with explicit `Void` is also valid:

```java
@Activity
public class TrimCustomerNameActivity
        implements FlowItem<CustomerInput, Void, CustomerInput> {

    @Override
    public CustomerInput execute(CustomerInput input, Void context) {
        return new CustomerInput(input.name().trim());
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

Without a context:

```java
@WorkflowDefinition
public class CreateCustomerWorkflow
        extends SimpleWorkflow<CustomerInput, Customer> {

    private final TrimCustomerNameActivity trim;
    private final PersistCustomerActivity persist;

    public CreateCustomerWorkflow(TrimCustomerNameActivity trim,
            PersistCustomerActivity persist) {
        this.trim = trim;
        this.persist = persist;
    }

    @Override
    protected void configure() {
        flowBuilder = FlowBuilder.builder(CustomerInput.class)
                .step(trim)
                .step(persist)
                .build();
    }
}
```

### Lifecycle

- `configure()` is called exactly once in `@PostConstruct`.
- The built flow is assigned to `flowBuilder`.
- `execute()` is `final` and uses the built composition.
- Manually instantiated workflows initialize on the first execution.
- An invalid configuration throws `FlowConfigurationException`.

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

`SimpleWorkflow<I, O>` inherits this lifecycle and adds `execute(I)`.

## FlowBuilder

The only operation for adding steps is `.step()`.

With a context:

```java
flowBuilder = FlowBuilder.builder(CustomerInput.class, CustomerContext.class)
        .step(validateCustomer)
        .step(enrichCustomer)
        .step(saveCustomer)
        .build();
```

Without a context:

```java
flowBuilder = FlowBuilder.builder(CustomerInput.class)
        .step(validateCustomer)
        .step(enrichCustomer)
        .step(saveCustomer)
        .build();
```

- `builder(Class)` anchors the input of a context-less flow and returns a `SimpleFlowBuilder`;
- `builder(Class, Class)` anchors input and context;
- `.step()` infers the output from the Activity;
- `.build()` returns the immutable composition;
- the builder is immutable: every `.step()` returns a new builder.

### Type safety

The builder output type changes at every step. An incompatible chain does not compile:

```java
FlowBuilder.builder(String.class, CustomerContext.class)
        .step(lengthActivity)   // String -> Integer
        .step(lengthActivity);  // erro: espera FlowItem<Integer, ...>
```

```text
required: FlowItem<Integer, CustomerContext, O>
found:    FlowItem<String, CustomerContext, Integer>
```

The guarantee is at compile time. Due to type erasure there is no runtime check; the class tokens
passed to `builder()` are inference anchors, not reflective validation.

### Composition across contracts

| Scenario | Result | Mechanism |
|---|---|---|
| Context-less Activity in a Workflow with a context | supported | `step(SimpleFlowItem)` overload, ignores the context |
| Activity with `Void` in a context-less flow | supported | `step(FlowItem<..., Void, ...>)` overload |
| Activity that requires a context in a flow with the same context | supported | `step(FlowItem<..., C, ...>)` |
| Activity that requires a different context | does not compile | there is no implicit conversion |
| Context-less Workflow inside a context-less Workflow | supported | `SimpleWorkflow` is a `SimpleFlowItem` |
| Context-less Workflow inside a Workflow with a context | supported | `step(SimpleFlowItem)` overload |
| Workflow that requires a context in a flow with the same context | supported | `step(FlowItem<..., C, ...>)` |
| Workflow that requires a context in a context-less flow | does not compile | requires an explicit adapter |
| Explicit `Void` context in a flow with a context | explicit adaptation | `SimpleFlowItem.from(...)` |

The absence of a context does not prevent composition. The context is simply ignored by the
context-less unit; it remains available to the following steps.

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

To insert a component that requires a context into a context-less flow, provide the context
explicitly:

```java
FlowItem<CustomerInput, Void, Customer> adapted = contextualWorkflow
        .adaptContext((Void ignored) -> new CustomerContext("req-1"));

SimpleFlowItem<CustomerInput, Customer> contextless = SimpleFlowItem.from(adapted);
```

For a component with explicit `Void` to take part in a flow with a context:

```java
flowBuilder = FlowBuilder.builder(CustomerInput.class, CustomerContext.class)
        .step(SimpleFlowItem.from(voidActivity))
        .step(validateCustomer)
        .build();
```

`SimpleFlowItem.from(...)` only adapts the contract; it does not create a context.

For different input and output:

```java
FlowItem<I, C, O> adapted = FlowItem.adapt(
        item,
        inputAdapter,
        contextAdapter,
        outputAdapter);
```

There is no implicit context conversion and no cast between contracts.

## Concurrency

- the composition is immutable after initialization;
- the context is always a parameter of `execute`;
- there is no `ThreadLocal`, global state or bean field holding request data;
- the same Workflow instance can serve concurrent executions;
- the tests cover 128 concurrent executions with isolated contexts.

## Performance

- built exactly once;
- execution through direct Java calls, without reflection;
- no `Object` between Activities;
- no casts on the execution path;
- no empty context created automatically;
- no dynamic resolution of the context type;
- only one state object per execution when needed;
- compatible with GraalVM Native Image.

See the [benchmarks](../guides/benchmarks.md).

## What is not part of the core

- automatic compensation/Saga;
- automatic retry;
- built-in observability;
- dynamic discovery of Activities.

These behaviors were not specified and can be implemented in the application or in future modules.
