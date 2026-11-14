# Optional context

The context exists to share information during execution — correlation, user, tenant, trace or any
state of the use case. It is **not mandatory**.

Harpia Arch supports two equivalent forms:

1. **explicit `Void`** — keeps the three-parameter contract;
2. **no context declaration** — uses the nominal contracts `SimpleFlowItem`/`SimpleWorkflow`.

## Why are there two names?

Java does not allow declaring at the same time:

```java
FlowItem<I, O>
FlowItem<I, C, O>
```

Interfaces cannot be overloaded by the number of generic parameters while keeping the same name in
the same package. Harpia Arch does not simulate that overloading. The context-free form uses its own
nominal types:

```java
SimpleFlowItem<I, O>
SimpleWorkflow<I, O>
```

## Contract without context

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

The two-parameter method is a bridge: it receives `null`, ignores the value and calls
`execute(input)`. No empty context is instantiated.

```java
public abstract class SimpleWorkflow<I, O>
        extends Workflow<I, Void, O>
        implements SimpleFlowItem<I, O> {

    @Override
    public final O execute(I input) {
        return execute(input, null);
    }
}
```

## Explicit `Void`

When the team prefers to keep the full contract:

```java
FlowItem<Customer, Void, Customer>
Workflow<Void, Void, List<Customer>>
```

The context received is always `null`. The `Workflow` keeps the three generic parameters and can be
executed with `execute(input, null)`.

## Activity with context

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

## Activity without context

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

## Workflow without context

```java
@WorkflowDefinition
public class TrimCustomerWorkflow extends SimpleWorkflow<CustomerInput, CustomerInput> {

    @Override
    protected void configure() {
        flowBuilder = FlowBuilder.builder(CustomerInput.class)
                .step(new TrimCustomerNameActivity())
                .build();
    }
}
```

## Builder

With a context:

```java
FlowBuilder.builder(CustomerInput.class, CustomerContext.class)
        .step(validateCustomer)
        .step(saveCustomer)
        .build();
```

Without a context:

```java
FlowBuilder.builder(CustomerInput.class)
        .step(validateCustomer)
        .step(saveCustomer)
        .build();
```

The `step(SimpleFlowItem<OUT, O>)` overload allows a context-free unit to be used inside a flow with
a context. The context remains available to the following steps.

## Compatibility matrix

| Scenario | Compiles? | Behavior |
|---|---|---|
| `SimpleFlowItem` Activity in a Workflow with context | Yes | The context is ignored by the step |
| `FlowItem<..., Void, ...>` Activity in a context-free flow | Yes | Receives `null` |
| `FlowItem<..., Void, ...>` Activity in a flow with context | With `SimpleFlowItem.from(...)` | Explicit adaptation, without creating a context |
| Activity with a context in a flow with the same context | Yes | Shared context |
| Activity with another context | No | There is no implicit conversion |
| `SimpleWorkflow` Workflow in a Workflow with context | Yes | The outer context is ignored by the subworkflow |
| `SimpleWorkflow` Workflow in a context-free Workflow | Yes | Direct composition |
| Workflow that requires a context in a context-free flow | No | Requires an explicit `adaptContext(...)` |

## Explicit adaptations

### Ignoring the context of a flow

```java
FlowItem<CustomerInput, CustomerContext, CustomerOutput> step =
        contextFreeActivity.ignoringContext();
```

### Adapting a `Void` item to a flow with context

```java
FlowItem<CustomerInput, Void, CustomerOutput> voidActivity = ...;

flowBuilder = FlowBuilder.builder(CustomerInput.class, CustomerContext.class)
        .step(SimpleFlowItem.from(voidActivity))
        .build();
```

`SimpleFlowItem.from(...)` only adapts the contract. It does not create a context.

### Providing a context to a component that requires one

```java
FlowItem<CustomerInput, Void, CustomerOutput> adapted =
        contextualWorkflow.adaptContext((Void ignored) -> new CustomerContext("req-1"));
```

`adaptContext` receives the function that produces the real context. Creation is explicit and is
the application's responsibility. For different input and output, use `FlowItem.adapt(...)`.

## What not to do

```java
// Do not use a cast to "resolve" the context.
FlowItem<CustomerInput, CustomerContext, CustomerOutput> step =
        (FlowItem<CustomerInput, CustomerContext, CustomerOutput>) simpleItem;
```

```java
// Do not use Object as the context to avoid the contract.
FlowItem<CustomerInput, Object, CustomerOutput> step = (input, context) -> ...;
```

The first case is an unsafe cast. The second gives up typing and is not recommended.

## Performance

- the context-free flow does not receive a context parameter;
- the composition overload calls `execute(input)` directly;
- no empty context object is allocated;
- there is no reflection to discover whether the Activity uses a context;
- there is no dynamic resolution of the context type;
- compatibility is resolved by the compiler.

## Testing

The `harpia-core` tests cover:

- Activity with `Void`;
- Activity without a context declaration;
- Workflow with `Void`;
- Workflow without context;
- Workflow with a real context;
- context-free Activity inside a Workflow with context;
- context-free Workflow inside a Workflow with context;
- an attempt to insert a component that requires a context into a context-free flow (compilation
  failure);
- absence of casts and reflection on the critical path.

See [Testing](../guides/testing.md).
