# Architectural decisions

## ADR-001 — Typed composition with closures

**Context:** chaining Activities with `Object` and casts would hide type errors.

**Decision:** `FlowBuilder` composes closures with the concrete input and output types. Values never
travel as `Object` and there are no casts on the execution path.

**Consequences:**

- incompatibilities fail at compile time;
- execution is a direct Java call;
- the builder is immutable and every `.step()` creates a new composition.

## ADR-002 — Workflow implements FlowItem

**Context:** a Workflow needed to be usable as a step of another Workflow.

**Decision:** `Workflow<I, C, O>` implements `FlowItem<I, C, O>`.

**Consequences:**

- hierarchical composition without an adapter when contracts match;
- different contexts require explicit `adaptContext`;
- different input/output types require `FlowItem.adapt`.

## ADR-003 — No Saga or observability in the core

**Context:** transactional compensation and observability had no defined requirements.

**Decision:** the core does not implement Saga, rollback, retry or listeners.

**Consequences:**

- smaller surface and less complexity;
- applications can implement compensation explicitly;
- future features can become separate modules.

## ADR-004 — Build-time validation

**Context:** architectural rules must be guaranteed before execution.

**Decision:** `harpia-core-deployment` validates annotations with Jandex during the Quarkus build.

**Consequences:**

- the build fails with clear messages;
- no reflection or runtime discovery;
- Quarkus resolves the deployment automatically.

## ADR-005 — Runtime/deployment split

**Context:** Quarkus extensions require a runtime and a deployment artifact.

**Decision:** `harpia-core` is the runtime; `harpia-core-deployment` contains build steps and
validation.

**Consequences:**

- users depend only on `harpia-core`;
- the deployment is resolved by the extension descriptor;
- modules have clear responsibilities.

## ADR-006 — `@WorkflowDefinition` name

**Context:** the original document used `@Workflow`, but Java does not allow an annotation and the
`Workflow` class with the same simple name in the same scope.

**Decision:** the annotation is named `@WorkflowDefinition`.

**Consequences:**

- code compiles with `@WorkflowDefinition` and `extends Workflow`;
- the name documents the annotation role.

## ADR-007 — Builder with class tokens

**Context:** `builder()` without arguments required type witnesses (`FlowBuilder.<I, C>builder()`).

**Decision:** `builder(Input.class, Context.class)` anchors the types and allows inference.

**Consequences:**

- the call is explicit;
- tokens do not perform runtime validation;
- safety remains a compile-time guarantee.

## ADR-008 — Lifecycle

**Context:** the composition must not be rebuilt per request.

**Decision:** `configure()` runs in `@PostConstruct`; `execute()` is `final`.

**Consequences:**

- single, safe construction;
- subclasses cannot ignore the composition;
- manually instantiated workflows initialize on first execution.

## ADR-009 — Internal modules outside distribution

**Context:** integration tests and benchmarks are not libraries.

**Decision:** `harpia-integration-tests` and `harpia-benchmarks` are not published.

**Consequences:**

- Maven Central receives only consumer artifacts;
- tests and benchmarks stay in the repository.
