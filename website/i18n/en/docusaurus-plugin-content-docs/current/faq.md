# FAQ

## What is the difference between `harpia-arch`, `harpia-quarkus` and `harpia-core`?

- `harpia-arch`: library build POM;
- `harpia-quarkus`: recommended parent for applications;
- `harpia-core`: runtime dependency with the engine and annotations.

Details in [Modules](modules.md).

## Do I need to declare `harpia-core-deployment`?

No. Quarkus resolves the deployment artifact automatically from `harpia-core`.

## Do I need to create a context class?

No. The context is optional. You can:

- use a real context (`FlowItem<I, C, O>`);
- use explicit `Void` (`FlowItem<I, Void, O>`);
- omit the context (`SimpleFlowItem<I, O>`).

See [Optional context](core/context.md).

## Why is the annotation `@WorkflowDefinition` and not `@Workflow`?

Java does not allow an annotation and the `Workflow<I, C, O>` class to share the same simple name
in the same scope. `@WorkflowDefinition` represents the `@Workflow` from the guidelines.

## Can I use a Workflow as a step of another?

Yes. `Workflow` implements `FlowItem`, so `.step(subworkflow)` works when the input, context and
output contracts are compatible. A `SimpleWorkflow` can also be a step of a flow with or without a
context.

## How do I use different contexts?

With explicit adaptation:

```java
workflow.adaptContext(context -> new SubContext(context.correlationId()))
```

For different input and output, use `FlowItem.adapt(...)`. There is no implicit conversion.

## Can I use `Object` as the context?

Technically yes, but it is not recommended. The context is part of the contract and should have its
own type. `SimpleFlowItem` is the correct way to not use a context.

## How do I insert a `Void` Activity into a flow with a context?

Use the explicit adaptation:

```java
.step(SimpleFlowItem.from(voidActivity))
```

## Can I insert a Workflow that requires a context into a context-less flow?

Only with an explicit adapter that provides the context:

```java
FlowItem<Input, Void, Output> adapted =
        workflow.adaptContext((Void ignored) -> new CustomerContext("req-1"));
```

## Is there automatic compensation/Saga?

No. The core does not implement compensation, retry or automatic rollback. The application can
compose these strategies explicitly.

## How are errors handled?

The original exception is propagated and the following steps do not execute. There is no automatic
wrapping. See [Errors and diagnostics](core/errors.md).

## How do I disable architectural validation?

There is no flag. Validation is part of the library contract; a violation must be fixed.
See [Build-time validation](core/build-validation.md).

## Where should the OpenAPI contract live?

By default at `src/main/resources/openapi/openapi.yaml`. The `harpia-openapi-codegen` profile is
activated when that directory exists.

## Which Java version is required?

Java 25. The project compiles with `maven.compiler.release=25`.

## Does the library work without Quarkus?

The core targets Quarkus: it uses CDI/ArC, stereotypes and a deployment extension. It is not a
standalone Java library.

## How do I run the tests?

```bash
./mvnw test
./mvnw -Dit verify
```

Native Image:

```bash
./mvnw -pl harpia-integration-tests verify -Dnative
```

See [Testing](guides/testing.md).

## How do I run the documentation site?

```bash
cd website
npm install
npm start
```

Open `http://localhost:3000/Harpia-Arch/`. See [Contributing](contributing.md).

## Does the engine use reflection?

Not on the critical path. The composition is done with typed closures and architectural validation
uses Jandex at build time. ArC may use CDI proxies for `@ApplicationScoped` beans, but the engine
neither discovers Activities nor resolves the context through reflection.

## Is Harpia Arch compatible with Native Image?

Yes. The core was designed to avoid dynamic reflection, and the integration module tests Hibernate
ORM, H2 and REST in native image. See [Native Image](guides/native-image.md).

## What is the difference between `Void` and `SimpleFlowItem`?

`Void` keeps the three-parameter contract and passes `null`. `SimpleFlowItem` is its own type with
`execute(input)`, but it is still a `FlowItem<I, Void, O>`.

## Can I mix the contracts?

Yes, respecting the compatibility matrix:

- context-less Activity in a flow with a context: yes;
- context-less Workflow in a flow with a context: yes;
- incompatible context: no;
- component that requires a context in a context-less flow: only with an explicit adapter.
