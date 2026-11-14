# Benchmarks

The `harpia-benchmarks` module uses JMH to measure the real cost of the engine.

## Run

```bash
./mvnw -pl harpia-benchmarks -am package
java -jar harpia-benchmarks/target/harpia-benchmarks.jar
```

The `package` step creates an executable JAR with `org.openjdk.jmh.Main`.

### Allocations

```bash
java -jar harpia-benchmarks/target/harpia-benchmarks.jar -prof gc
```

### Quick run

```bash
java -jar harpia-benchmarks/target/harpia-benchmarks.jar \
  -wi 1 -i 1 -f 1 -w 200ms -r 200ms
```

## Benchmarks

| Benchmark | What it measures |
|---|---|
| `directCall` | direct `IntUnaryOperator` call |
| `plainFlowItem` | call to a plain `FlowItem` |
| `singleActivity` | single Activity composed by the builder |
| `workflowWithThreeSteps` | Workflow with 3 steps |
| `workflowWithSixteenSteps` | Workflow with 16 steps |
| `nestedWorkflows` | 3 subworkflows, 9 steps in total |
| `buildComposition` | cost of building a 3-step composition |
| `contextFreeActivity` | single Activity without context |
| `contextFreeWorkflowWithSixteenSteps` | context-free Workflow with 16 steps |
| `contextFreeNestedWorkflows` | 3 context-free subworkflows |
| `buildContextFreeComposition` | building a context-free composition |

## Reference result

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

The context-free benchmarks measure the path that passes no context between steps. Run them in the
same session to compare with the benchmarks that use context.

## Methodology

- use the same JDK, hardware and CPU governor;
- run with enough warmup before drawing conclusions;
- use `-prof gc` to investigate allocations;
- compare median/error, not just the best result;
- do not compare numbers from different machines.

See [Performance](performance.md) for the cost model and design decisions.
