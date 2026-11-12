# Java API

Javadoc is published together with the artifacts.

## Javadoc

- [`harpia-core`](https://javadoc.io/doc/io.github.krisner94/harpia-core/1.0.0/)
- [`harpia-core-deployment`](https://javadoc.io/doc/io.github.krisner94/harpia-core-deployment/1.0.0/)

> The links become available after the version is published to Maven Central.

## Public types

### Engine

| Type | Role |
|---|---|
| `io.github.krisner94.harpia.core.FlowItem` | Execution contract |
| `io.github.krisner94.harpia.core.Workflow` | Workflow base class |
| `io.github.krisner94.harpia.core.FlowBuilder` | Typed builder |
| `io.github.krisner94.harpia.core.FlowConfigurationException` | Configuration failure |

### Annotations

| Type | Role |
|---|---|
| `@HarpiaApplication` | Main class |
| `@Controller` | HTTP adapter |
| `@Service` | Business rules |
| `@Repository` | Persistence |
| `@Activity` | Workflow step |
| `@WorkflowDefinition` | Workflow |
| `@Component` | Generic component |

Package: `io.github.krisner94.harpia.annotation`.

## Source code

- [GitHub](https://github.com/Krisner94/Harpia-Arch)
- [Integration examples](https://github.com/Krisner94/Harpia-Arch/tree/main/harpia-integration-tests)
- [Benchmarks](https://github.com/Krisner94/Harpia-Arch/tree/main/harpia-benchmarks)
