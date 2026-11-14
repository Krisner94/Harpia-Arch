# Compatibility

## Versions

| Component | Version |
|---|---|
| Harpia Arch | 1.0.0 |
| Java | 25 |
| Maven | 3.9+ (wrapper 3.9.16) |
| Quarkus | 3.40.1.1 |
| GraalVM | 25, for Native Image |
| OpenAPI Generator | 7.4.0 |
| JUnit Jupiter | managed by the Quarkus BOM |
| Node.js | 20+, only for the documentation site |

The project compiles with `maven.compiler.release=25`. There is no declared support for earlier
versions of Java.

## Artifacts

| Coordinate | Type | Published |
|---|---|---|
| `io.github.krisner94:harpia-arch:1.0.0` | Parent POM | Yes |
| `io.github.krisner94:harpia-quarkus:1.0.0` | Parent POM | Yes |
| `io.github.krisner94:harpia-core:1.0.0` | Runtime JAR | Yes |
| `io.github.krisner94:harpia-core-deployment:1.0.0` | Deployment JAR | Yes |
| `io.github.krisner94:harpia-integration-tests:1.0.0` | Test application | No |
| `io.github.krisner94:harpia-benchmarks:1.0.0` | Benchmarks | No |

`harpia-core-deployment` is resolved by Quarkus from the `harpia-core` descriptor. The application
does not need to declare it.

## Execution contracts

| Contract | Use | Context |
|---|---|---|
| `FlowItem<I, C, O>` | Activity with context | Received as a parameter |
| `SimpleFlowItem<I, O>` | Activity without context | None; the bridge receives `null` |
| `Workflow<I, C, O>` | Workflow with context | Received as a parameter |
| `SimpleWorkflow<I, O>` | Workflow without context | None; executed with one argument |
| `FlowItem<I, Void, O>` | Activity with explicit `Void` | `null` |

## Compatibility between contexts

| Origin | Destination | Result |
|---|---|---|
| `FlowItem<I, C, O>` | flow with the same `C` | Direct |
| `SimpleFlowItem<I, O>` | flow with any `C` | Direct; context ignored |
| `SimpleWorkflow<I, O>` | flow with any `C` | Direct; context ignored |
| `FlowItem<I, Void, O>` | flow without context | Direct; receives `null` |
| `FlowItem<I, Void, O>` | flow with a real `C` | `SimpleFlowItem.from(...)` |
| `FlowItem<I, C1, O>` | flow with a different `C2` | `adaptContext(...)` |
| `SimpleWorkflow<I, O>` | flow without context | Direct |

There is no implicit conversion between `C1` and `C2`, no cast between contracts and no dynamic
resolution.

## Compatibility with the previous version

Version 1.0.0 keeps the existing `FlowItem`, `Workflow` and `FlowBuilder` contracts. The addition of
`SimpleFlowItem`, `SimpleWorkflow`, `SimpleFlowBuilder` and the `step`/`builder` overloads is
additive:

- code that uses `FlowItem<I, C, O>` still compiles;
- code that uses `Workflow<I, C, O>` still compiles;
- `FlowBuilder.builder(Class, Class)` remains unchanged;
- explicit `Void` remains supported;
- there are no deprecations in 1.0.0.

## Native Image

The core does not use reflection to discover Activities or to resolve context. The application still
depends on the native support of the libraries it uses. The integration module tests Hibernate ORM,
H2 and REST in native image. See [Native Image](../guides/native-image.md).

## Documentation

The site is built with Docusaurus 3 and published on GitHub Pages at:

```text
https://krisner94.github.io/Harpia-Arch/
```

The documentation source code lives in `website/`. The PT and EN versions are generated in the same
build.

## Evolution policy

- contract changes must be discussed in an ADR;
- additions must preserve binary and source compatibility where possible;
- architectural validation may become stricter only with a justification and a migration;
- releases are immutable on Maven Central;
- snapshots from the `beta` branch are not stable versions.
