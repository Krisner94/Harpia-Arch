# Native Image

Harpia Arch is compatible with GraalVM Native Image. The flow does not use reflection, dynamic
proxies or runtime component discovery.

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
mvn -pl harpia-integration-tests verify -Dnative
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
        // POST /customers and GET /customers/{id}
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
- direct Java calls;
- build/CLASS-retained annotations;
- no reflection to discover Activities;
- Hibernate ORM and H2 with native Quarkus support.
