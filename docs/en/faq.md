# FAQ

## What is the difference between `harpia-arch`, `harpia-quarkus` and `harpia-core`?

- `harpia-arch`: library build POM;
- `harpia-quarkus`: recommended parent for applications;
- `harpia-core`: runtime dependency with the engine and annotations.

See [Modules](modules.md).

## Do I need to declare `harpia-core-deployment`?

No. Quarkus resolves the deployment artifact automatically from `harpia-core`.

## Why is the annotation `@WorkflowDefinition` instead of `@Workflow`?

Java does not allow an annotation and the class `Workflow<I, C, O>` with the same simple name in
the same scope. `@WorkflowDefinition` represents the original `@Workflow`.

## Can one Workflow be a step of another?

Yes. `Workflow` implements `FlowItem`, so `.step(subworkflow)` works when input, context and output
contracts are compatible.

## How do I use different contexts?

With explicit adaptation:

```java
workflow.adaptContext(context -> new SubContext(context.correlationId()))
```

For different input/output types, use `FlowItem.adapt(...)`.

## Can I use `Object` as the context?

Technically yes, but it is not recommended. The context is part of the contract and should have its
own type.

## Is there automatic compensation/Saga?

No. The core does not implement compensation, retry or automatic rollback. Applications can compose
those strategies explicitly.

## How are errors handled?

The original exception is propagated and subsequent steps do not execute. There is no automatic
wrapping.

## How do I disable architectural validation?

There is no flag. Validation is part of the library contract; a violation should be fixed.

## Where should the OpenAPI contract live?

By default at `src/main/resources/openapi/openapi.yaml`.

## Which Java version is required?

Java 25. The project compiles with `maven.compiler.release=25`.

## Does the library work without Quarkus?

The core targets Quarkus: it uses CDI/ArC, stereotypes and a deployment extension. It is not a
standalone Java library.

## How do I run native tests?

```bash
mvn -pl harpia-integration-tests verify -Dnative
```

Requires GraalVM with `native-image`.
