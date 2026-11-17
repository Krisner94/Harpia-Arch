# Changelog

## 1.2.0

### Secrets vault (provider-agnostic)

- `SecretResolver` SPI — plug any secret backend (OCI Vault, OpenBao, HashiCorp Vault, Azure, AWS, GCP);
- `VaultConfigSource` resolves `<ENV>_DB_<FIELD>` into `<prefix>-<env>-db-<field>`;
- provider selected via `harpia.vault.provider`;
- bootstrap configuration in `bootstrap.yaml` (loaded by `quarkus.config.locations`);
- OCI SDK kept as an optional dependency (only the `oci` provider requires it).

## 1.1.0

### OCI Vault

- `OciVaultConfigSource` + `OciVaultConfigSourceFactory`;
- resolves database credentials from OCI Vault at boot;
- activated by `harpia.vault.enabled` or the `local` profile.

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
