# Changelog

## 1.0.0

### Engine

- `FlowItem<I, C, O>`;
- `SimpleFlowItem<I, O>` for Activities without a context;
- `Workflow<I, C, O>` with immutable composition and CDI lifecycle;
- `SimpleWorkflow<I, O>` for Workflows without a context;
- typed `FlowBuilder` with `builder(Class, Class)`, `.step()` and `.build()`;
- `SimpleFlowBuilder` triggered by `builder(Class)`;
- `step(SimpleFlowItem)` overload to compose context-free units in flows that have a context;
- `step(FlowItem<..., Void, ...>)` overload for flows without a context;
- hierarchical composition of workflows;
- explicit context adaptation with `adaptContext`, `ignoringContext` and `FlowItem.adapt`;
- `SimpleFlowItem.from(...)` to adapt `Void` units;
- `FlowConfigurationException`;
- concurrent execution with an isolated context.

### Optional context

- explicit `Void` supported without creating a context object;
- absence of a context through `SimpleFlowItem`/`SimpleWorkflow`;
- Activity without a context in a Workflow with a context;
- Workflow without a context in a Workflow with a context;
- incompatible context rejected at compile time;
- a component that requires a context in a flow without a context requires `adaptContext(...)`.

### Annotations

- `@HarpiaApplication`;
- `@Controller`;
- `@Service`;
- `@Repository`;
- `@Activity`;
- `@WorkflowDefinition`;
- `@Component`.

### Validation

- Quarkus extension with runtime and deployment;
- registration of the annotations as bean-defining;
- Jandex validation at build time;
- support for `SimpleFlowItem` and `SimpleWorkflow` in the validation;
- CDI and architectural rule tests.

### Tests

- composition and lifecycle tests;
- optional context tests;
- negative compilation with `javax.tools`;
- core purity guard (no reflection/casts);
- CDI test with a Workflow with and without a context;
- JVM and Native Image integration tests;
- 44 tests in total.

### Distribution

- `harpia-quarkus` application parent;
- `harpia-arch` build parent;
- Maven Central configuration;
- sources, Javadoc and GPG signature;
- native image tested with Hibernate ORM and H2;
- JMH benchmarks;
- CI on Java 25;
- beta publication to snapshots and automated release on `main`.

### Documentation

- Docusaurus 3 site in PT/EN;
- migration from MkDocs to Docusaurus;
- GitHub Pages deployment via `.github/workflows/docs.yml`;
- local search, Mermaid and light/dark theme;
- new pages for architecture, context, errors, testing, configuration, performance,
  compatibility and glossary.
