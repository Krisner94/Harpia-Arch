# Java API

The Javadoc documentation is published together with the artifacts.

## Javadoc

- [`harpia-core`](https://javadoc.io/doc/io.github.krisner94/harpia-core/1.0.0/)
- [`harpia-core-deployment`](https://javadoc.io/doc/io.github.krisner94/harpia-core-deployment/1.0.0/)

> The links become available after the version is published to Maven Central.

## Public types

### Engine

| Type | Role |
|---|---|
| `io.github.krisner94.harpia.core.FlowItem` | Execution contract with a context |
| `io.github.krisner94.harpia.core.SimpleFlowItem` | Execution contract without a context |
| `io.github.krisner94.harpia.core.Workflow` | Base for workflows with a context |
| `io.github.krisner94.harpia.core.SimpleWorkflow` | Base for workflows without a context |
| `io.github.krisner94.harpia.core.FlowBuilder` | Typed builder with a context |
| `io.github.krisner94.harpia.core.SimpleFlowBuilder` | Typed builder without a context |
| `io.github.krisner94.harpia.core.FlowConfigurationException` | Configuration failure |

`Stage` is package-internal and is not part of the public API.

## `FlowItem<I, C, O>`

```java
@FunctionalInterface
public interface FlowItem<I, C, O> {

    O execute(I input, C context);

    default <C2> FlowItem<I, C2, O> adaptContext(Function<C2, C> adapter);

    static <I, C, O, SI, SC, SO> FlowItem<I, C, O> adapt(
            FlowItem<SI, SC, SO> item,
            Function<I, SI> inputAdapter,
            Function<C, SC> contextAdapter,
            Function<SO, O> outputAdapter);
}
```

| Member | Semantics |
|---|---|
| `execute(input, context)` | Executes the unit and returns the output |
| `adaptContext(adapter)` | Adapts the outer context to the inner context |
| `FlowItem.adapt(...)` | Adapts input, context and output explicitly |

`adaptContext` and `adapt` are explicit adaptations. There is no implicit conversion.

## `SimpleFlowItem<I, O>`

```java
@FunctionalInterface
public interface SimpleFlowItem<I, O> extends FlowItem<I, Void, O> {

    O execute(I input);

    @Override
    default O execute(I input, Void context) {
        return execute(input);
    }

    default <C> FlowItem<I, C, O> ignoringContext();

    static <I, O> SimpleFlowItem<I, O> from(FlowItem<I, Void, O> item);
}
```

| Member | Semantics |
|---|---|
| `execute(input)` | Executes without a context |
| `execute(input, null)` | Bridge that ignores the context and calls `execute(input)` |
| `ignoringContext()` | Explicit view for a flow with a context |
| `from(item)` | Adapts a `FlowItem<I, Void, O>` to the context-less contract |

## `Workflow<I, C, O>`

```java
public abstract class Workflow<I, C, O> implements FlowItem<I, C, O> {

    protected volatile FlowItem<I, C, O> flowBuilder;

    protected abstract void configure();

    @PostConstruct
    final void initialize();

    @Override
    public final O execute(I input, C context);

    protected final FlowItem<I, C, O> flow();
}
```

| Member | Semantics |
|---|---|
| `flowBuilder` | Composition built by `configure()` |
| `configure()` | Declares the composition; must assign `flowBuilder` |
| `initialize()` | Runs `configure()` exactly once in `@PostConstruct` |
| `execute(input, context)` | Executes the composition, initializing it if necessary |
| `flow()` | Returns the built composition |

`execute()` is `final` so that subclasses cannot bypass the composition.

## `SimpleWorkflow<I, O>`

```java
public abstract class SimpleWorkflow<I, O>
        extends Workflow<I, Void, O>
        implements SimpleFlowItem<I, O> {

    @Override
    public final O execute(I input);
}
```

It inherits the `Workflow` lifecycle and adds `execute(input)`.

## `FlowBuilder<I, C, OUT>`

```java
public final class FlowBuilder<I, C, OUT> {

    public static <I, C> Starter<I, C> builder(Class<I> inputType, Class<C> contextType);

    public static <I> SimpleFlowBuilder.Starter<I> builder(Class<I> inputType);

    public <O> FlowBuilder<I, C, O> step(FlowItem<OUT, C, O> next);

    public <O> FlowBuilder<I, C, O> step(SimpleFlowItem<OUT, O> next);

    public FlowItem<I, C, OUT> build();
}
```

The `builder(Class)` overload delegates to `SimpleFlowBuilder`. The `step(SimpleFlowItem)` overload
ignores the flow context.

## `SimpleFlowBuilder<I, OUT>`

```java
public final class SimpleFlowBuilder<I, OUT> {

    public <O> SimpleFlowBuilder<I, O> step(SimpleFlowItem<OUT, O> next);

    public <O> SimpleFlowBuilder<I, O> step(FlowItem<OUT, Void, O> next);

    public SimpleFlowItem<I, OUT> build();
}
```

The `step(FlowItem<OUT, Void, O>)` overload accepts units with explicit `Void` and passes `null`.

## `FlowConfigurationException`

```java
public class FlowConfigurationException extends IllegalStateException {
}
```

Used when `configure()` does not assign `flowBuilder` or when an empty composition is built.

## Builder matrix

| Flow | Input | Accepted steps | Output of `build()` |
|---|---|---|---|
| With context | `builder(Input.class, C.class)` | `FlowItem<OUT, C, O>`, `SimpleFlowItem<OUT, O>` | `FlowItem<I, C, OUT>` |
| Without context | `builder(Input.class)` | `SimpleFlowItem<OUT, O>`, `FlowItem<OUT, Void, O>` | `SimpleFlowItem<I, OUT>` |

## Annotations

| Type | Role |
|---|---|
| `@HarpiaApplication` | Main class; marker without CDI |
| `@Controller` | HTTP adapter |
| `@Service` | Business rules |
| `@Repository` | Persistence |
| `@Activity` | Workflow step |
| `@WorkflowDefinition` | Workflow |
| `@Component` | Generic component |

Package: `io.github.krisner94.harpia.annotation`.

All role annotations use `@Stereotype` + `@ApplicationScoped`, except `@HarpiaApplication`, which is
only a marker with runtime retention.

## Source code

- [GitHub](https://github.com/Krisner94/Harpia-Arch)
- [Integration examples](https://github.com/Krisner94/Harpia-Arch/tree/main/harpia-integration-tests)
- [Benchmarks](https://github.com/Krisner94/Harpia-Arch/tree/main/harpia-benchmarks)
