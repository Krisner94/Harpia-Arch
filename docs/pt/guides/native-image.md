# Native Image

O Harpia Arch é compatível com GraalVM Native Image. O fluxo não usa reflexão, proxies dinâmicos
ou descoberta de componentes em runtime.

## GraalVM

Use GraalVM 25 com `native-image`:

```bash
export GRAALVM_HOME=/caminho/para/graalvm
export JAVA_HOME=$GRAALVM_HOME
export PATH=$GRAALVM_HOME/bin:$PATH

native-image --version
```

## Build nativo

```bash
mvn -pl harpia-integration-tests verify -Dnative
```

O perfil `native` do `harpia-quarkus` configura:

- `quarkus.native.enabled=true`;
- `quarkus.package.jar.enabled=false`;
- `skipITs=false`;
- Failsafe com `@QuarkusIntegrationTest`.

## Teste

O `CustomerWorkflowIT` executa contra o executável nativo:

```java
@QuarkusIntegrationTest
class CustomerWorkflowIT {

    @Test
    void createsAndReadsCustomer() {
        // POST /customers e GET /customers/{id}
    }
}
```

Resultado de referência:

```text
Finished generating 'harpia-integration-tests-1.0.0-SNAPSHOT-runner' in 4m 27s

harpia-integration-tests native started in 0.033s
Installed features: [agroal, cdi, harpia-arch, hibernate-orm, jdbc-h2,
                     narayana-jta, rest, rest-jackson, ...]

Tests run: 1, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

## Por que funciona

- composição construída em build/startup;
- chamadas Java diretas;
- annotations com retenção de build/CLASS;
- nenhuma reflexão para descobrir Activities;
- Hibernate ORM e H2 com suporte nativo do Quarkus.
