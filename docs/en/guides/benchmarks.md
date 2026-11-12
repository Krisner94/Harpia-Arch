# Benchmarks

The `harpia-benchmarks` module uses JMH to measure the real cost of the engine.

## Run

```bash
mvn -pl harpia-benchmarks -am package
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
| `workflowWithThreeSteps` | workflow with 3 steps |
| `workflowWithSixteenSteps` | workflow with 16 steps |
| `nestedWorkflows` | 3 subworkflows, 9 steps in total |
| `buildComposition` | cost of building a 3-step composition |

## Reference result

Environment: 3 warmups, 5 measurements of 500 ms.

| Benchmark | Cost |
|---|---|
| Direct call | 0.44 ns/op |
| Plain `FlowItem` | 0.44 ns/op |
| Single composed Activity | 1.79 ns/op |
| Workflow with 3 steps | 5.00 ns/op |
| Workflow with 16 steps | 58.08 ns/op |
| 3 subworkflows (9 steps) | 56.65 ns/op |
| Composition build (3 steps) | 9.69 ns/op, 80 B/op |

Numbers are environment-specific. Always compare on the same machine, with the same JDK and JMH
configuration.
