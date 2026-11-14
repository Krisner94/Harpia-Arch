# Performance and concurrency

Harpia Arch is designed so that executing a flow is close to a sequence of direct Java calls. The
relevant cost lies in building the composition and in the actual work of the Activities, not in an
interpretive engine.

## Principles

- **single construction** — `configure()` runs in `@PostConstruct`;
- **immutable composition** — each `.step()` returns a new builder; the final flow does not change;
- **direct calls** — steps are `Stage` closures, not `Method.invoke`;
- **no `Object` in the chain** — concrete types remain in the generated code;
- **no casts** — compatibility is resolved by the compiler;
- **optional context without allocation** — `SimpleFlowItem` does not create an empty context;
- **no dynamic context resolution** — no reflection or type inspection at runtime.

## Cost model

For a flow with `n` steps:

- construction creates `n` intermediate compositions plus the final flow;
- execution traverses `n` function calls;
- there is no per-step allocation in the engine, except what the Activity itself allocates;
- a context-free flow passes no context between steps;
- a nested Workflow adds one `execute` call per level.

`FlowBuilder` is immutable: each `.step()` creates a new `Stage` that references the previous one.
This trades a small construction cost for simple and safe execution.

## Benchmarks

```bash
./mvnw -pl harpia-benchmarks -am package
java -jar harpia-benchmarks/target/harpia-benchmarks.jar
```

To inspect allocations:

```bash
java -jar harpia-benchmarks/target/harpia-benchmarks.jar -prof gc
```

For a quick run:

```bash
java -jar harpia-benchmarks/target/harpia-benchmarks.jar \
  -wi 1 -i 1 -f 1 -w 200ms -r 200ms
```

### Available benchmarks

| Benchmark | What it measures |
|---|---|
| `directCall` | direct `IntUnaryOperator` call |
| `plainFlowItem` | call to a plain `FlowItem` |
| `singleActivity` | single Activity composed by the builder |
| `workflowWithThreeSteps` | Workflow with 3 steps |
| `workflowWithSixteenSteps` | Workflow with 16 steps |
| `nestedWorkflows` | 3 subworkflows, 9 steps |
| `buildComposition` | building a 3-step composition |
| `contextFreeActivity` | single Activity without context |
| `contextFreeWorkflowWithSixteenSteps` | context-free Workflow with 16 steps |
| `contextFreeNestedWorkflows` | 3 context-free subworkflows |
| `buildContextFreeComposition` | building a context-free composition |

### Reference

Environment: 3 warmups, 5 measurements of 500 ms, on this machine.

| Benchmark | Cost |
|---|---|
| Direct call | 0.44 ns/op |
| Plain `FlowItem` | 0.44 ns/op |
| Single composed Activity | 1.79 ns/op |
| Workflow with 3 steps | 5.00 ns/op |
| Workflow with 16 steps | 58.08 ns/op |
| 3 subworkflows (9 steps) | 56.65 ns/op |
| Composition build (3 steps) | 9.69 ns/op, 80 B/op |

Benchmark numbers are specific to hardware, JDK, CPU governor and JMH configuration. Always compare
on the same machine and under the same load.

## Context and allocation

There are three paths:

| Path | Context per execution | Engine allocation |
|---|---|---|
| `FlowItem` with real context | object supplied by the caller | none |
| `SimpleFlowItem` | none | none |
| Explicit `Void` | `null` | none |

The `step(SimpleFlowItem)` overload calls `execute(input)` directly. It does not create a `Void`,
does not create an empty context and does not perform `instanceof`.

## Concurrency

A Workflow is safe for concurrent executions when the supplied context is safe as well:

- the composition is published once in a `volatile` field;
- `execute()` does not modify the `flowBuilder`;
- there is no `ThreadLocal` in the engine;
- the context is always an execution parameter;
- each execution uses its own context instance.

If the context is mutable and shared between threads, safety becomes the application's
responsibility. Prefer immutable contexts or concurrent collections when needed.

## Native Image

The engine does not use reflection to discover Activities or to resolve context. This reduces the
need for native configuration in the core. Application dependencies, such as Hibernate ORM and
REST, remain subject to Quarkus native support.

See [Native Image](native-image.md).

## How to measure a change

1. measure the baseline with the same JMH and the same machine;
2. change one variable at a time;
3. run `-prof gc` to detect unexpected allocations;
4. compare median/error, not just the best result;
5. validate the native path when the change affects startup or reflection.

## Limits

The engine does not implement flow pooling, execution caching, step parallelism or retries. Those
decisions belong to the use case and can be implemented in Activities, Workflows or separate
modules.
