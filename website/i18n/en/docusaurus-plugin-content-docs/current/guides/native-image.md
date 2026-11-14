# Native Image

Harpia Arch is compatible with GraalVM Native Image. The engine does not use reflection to discover
Activities or to resolve the context type.

## GraalVM

Use GraalVM 25 with `native-image`:

```bash
export GRAALVM_HOME=/path/to/graalvm
export JAVA_HOME=$GRAALVM_HOME
export PATH=$GRAALVM_HOME/bin:$PATH

native-image --version
```

## Native build

```bash
./mvnw -pl harpia-integration-tests verify -Dnative
```

The `native` profile from `harpia-quarkus` configures:

- `quarkus.native.enabled=true`;
- `quarkus.package.jar.enabled=false`;
- `skipITs=false`;
- Failsafe with `@QuarkusIntegrationTest`.

## Test

`CustomerWorkflowIT` runs against the native executable:

```java
@QuarkusIntegrationTest
class CustomerWorkflowIT {

    @Test
    void createsAndReadsCustomer() {
        // POST /customers e GET /customers/{id}
    }
}
```

Reference result:

```text
Finished generating 'harpia-integration-tests-1.0.0-runner' in 4m 27s

harpia-integration-tests native started in 0.033s
Installed features: [agroal, cdi, harpia-arch, hibernate-orm, jdbc-h2,
                     narayana-jta, rest, rest-jackson, ...]

Tests run: 1, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

## Why it works

- composition built at build/startup time;
- direct Java calls on the execution path;
- architectural validation at build time with Jandex;
- no reflective discovery of Activities;
- no dynamic context resolution;
- Hibernate ORM and H2 with Quarkus native support.

The annotations have `RUNTIME` retention for CDI and documentation purposes, but Harpia Arch
validation does not read them through reflection at runtime; it uses the build-time Jandex index.
ArC may generate CDI proxies for `@ApplicationScoped` beans, but that is part of the container, not
the workflow engine.

## When to add native configuration

The core requires no reflection configuration of its own. The application may still need
configuration for:

- libraries that use reflection;
- DTO serialization;
- resources loaded by name;
- third-party integrations.

Generate the native agent report when needed:

```bash
./mvnw -pl harpia-integration-tests verify -Dnative \
  -Dquarkus.native.additional-build-args=-H:ReflectionConfigurationFiles=...
```

Prefer Quarkus extensions and `quarkus.native.resources.includes` before opening manual exceptions.

## Common problems

| Symptom | Likely cause |
|---|---|
| `native-image` not found | `GRAALVM_HOME`/`PATH` not configured |
| Very slow build | First native compilation; use a local cache |
| `ClassNotFoundException` at runtime | Library resource/reflection without native configuration |
| Native test does not run | `native` profile/Failsafe not activated |
| Insufficient memory | Increase the build process `-J-Xmx` |

## Limits

Native Image does not remove the need to test the packaged application. The native integration test
covers the main flow, but each application must validate its own dependencies and resources.
