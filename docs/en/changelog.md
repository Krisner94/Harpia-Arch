# Changelog

## 1.0.0

### Engine

- `FlowItem<I, C, O>`;
- `Workflow<I, C, O>` with immutable composition and CDI lifecycle;
- typed `FlowBuilder` with `builder(Class, Class)`, `.step()` and `.build()`;
- hierarchical workflow composition;
- explicit context adaptation with `adaptContext` and `FlowItem.adapt`;
- `FlowConfigurationException`;
- concurrent execution with isolated context.

### Annotations

- `@HarpiaApplication`;
- `@Controller`;
- `@Service`;
- `@Repository`;
- `@Activity`;
- `@WorkflowDefinition`;
- `@Component`.

### Validation

- Quarkus extension with runtime and deployment modules;
- annotations registered as bean-defining;
- Jandex validation at build time;
- CDI and architectural rule tests.

### Distribution

- application parent `harpia-quarkus`;
- build parent `harpia-arch`;
- Maven Central configuration;
- sources, Javadoc and GPG signatures;
- native image tested with Hibernate ORM and H2;
- JMH benchmarks.
