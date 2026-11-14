# Architectural decisions

## ADR-001 — Typed composition with closures

**Context:** chaining Activities with `Object` and casts would hide type errors.

**Decision:** `FlowBuilder` composes closures with the concrete input and output types. The value
never flows as an `Object` and there is no cast on the execution path.

**Consequences:**

- incompatibilities fail at compile time;
- execution is a direct Java call;
- the builder is immutable and every `.step()` creates a new composition.

## ADR-002 — Workflow implements FlowItem

**Context:** it was necessary to allow a Workflow as a step of another.

**Decision:** `Workflow<I, C, O>` implements `FlowItem<I, C, O>`.

**Consequences:**

- hierarchical composition without an adapter when the contract is the same;
- different contexts require an explicit `adaptContext`;
- different input/output require `FlowItem.adapt`.

## ADR-003 — No Saga or observability in the core

**Context:** transactional compensation and observability had no defined requirements.

**Decision:** the core implements no Saga, rollback, retry or listeners.

**Consequences:**

- smaller surface and lower complexity;
- the application can implement compensation explicitly;
- future features can become separate modules.

## ADR-004 — Build-time validation

**Context:** architectural rules must be guaranteed before execution.

**Decision:** `harpia-core-deployment` validates the annotations with Jandex during the Quarkus build.

**Consequences:**

- the build fails with clear messages;
- no reflection or discovery at runtime;
- Quarkus resolves the deployment automatically.

## ADR-005 — Runtime/deployment separation

**Context:** Quarkus extensions require one runtime artifact and one deployment artifact.

**Decision:** `harpia-core` is the runtime; `harpia-core-deployment` holds the build steps and validation.

**Consequences:**

- the user depends only on `harpia-core`;
- the deployment is resolved by the extension descriptor;
- the modules have clear responsibilities.

## ADR-006 — Name `@WorkflowDefinition`

**Context:** the original document used `@Workflow`, but Java does not allow an annotation and the
`Workflow` class to share the same simple name in the same scope.

**Decision:** the annotation is named `@WorkflowDefinition`.

**Consequences:**

- the code compiles with `@WorkflowDefinition` and `extends Workflow`;
- the name documents the role of the annotation.

## ADR-007 — Builder with class tokens

**Context:** `builder()` without arguments required type witnesses (`FlowBuilder.<I, C>builder()`).

**Decision:** `builder(Input.class, Context.class)` anchors the types and allows inference.

**Consequences:**

- the call is explicit;
- the tokens do not perform runtime validation;
- safety remains a compile-time property.

## ADR-008 — Lifecycle

**Context:** the composition must not be rebuilt per request.

**Decision:** `configure()` runs in `@PostConstruct`; `execute()` is `final`.

**Consequences:**

- single construction and safe publication;
- subclasses cannot bypass the composition;
- manually instantiated workflows initialize on the first execution.

## ADR-009 — Internal modules outside the distribution

**Context:** integration tests and benchmarks are not libraries.

**Decision:** `harpia-integration-tests` and `harpia-benchmarks` are not published.

**Consequences:**

- Maven Central receives only the consumption artifacts;
- the tests and benchmarks remain in the repository.

## ADR-010 — Optional context with nominal contracts

**Context:** the context is not always necessary. Java does not allow declaring `FlowItem<I, O>` and
`FlowItem<I, C, O>` with the same name in the same package, and overloading `step` by context arity
would create ambiguity for lambdas.

**Decision:** keep `FlowItem<I, C, O>` and `Workflow<I, C, O>` for the full contract and add
`SimpleFlowItem<I, O>` (which extends `FlowItem<I, Void, O>`) and `SimpleWorkflow<I, O>` (which
extends `Workflow<I, Void, O>`) for the context-less form. `FlowBuilder.builder(Class)` creates
context-less flows and the `step(SimpleFlowItem)` overload allows a context-less unit to be used in
a flow with a context. Explicit `Void` remains a supported form of the three-parameter contract.

**Consequences:**

- the developer does not need to create a context class when it has no purpose;
- explicit `Void` remains available and no empty context is instantiated;
- compatibility is resolved at compile time, without reflection or casts;
- there is no implicit conversion between different contexts;
- `SimpleFlowItem.from(...)` is the explicit adapter for `Void` in a flow with a context;
- a component that requires a context only enters a context-less flow via explicit
  `adaptContext(...)`.

## ADR-011 — Negative compilation tests

**Context:** some Harpia Arch guarantees come from the compiler, not the runtime. A conventional
test cannot prove that an invalid composition does not compile.

**Decision:** `harpia-core` compiles deliberately invalid sources with
`ToolProvider.getSystemJavaCompiler()` and verifies that the compiler rejects Activity/Workflow
with an incompatible context.

**Consequences:**

- contract compatibility is tested at the same level where it is guaranteed;
- the test depends on a full JDK, not just the JRE;
- type errors are detected before any execution.

## ADR-012 — Documentation in the same repository

**Context:** the documentation needed to follow the API evolution and be published without depending
on a separate repository.

**Decision:** the Docusaurus site lives in `website/`, with PT as the default locale and EN in
`i18n/en`. The `docs.yml` workflow publishes to GitHub Pages on every push to `website/**`.

**Consequences:**

- the documentation is versioned together with the code;
- the site build is reproducible with `npm ci`;
- PT and EN are generated in the same build;
- the migration no longer depends on MkDocs and on the separate documentation clone.
