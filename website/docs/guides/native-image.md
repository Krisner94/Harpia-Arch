# Native Image

O Harpia Arch é compatível com GraalVM Native Image. O motor não usa reflexão para descobrir
Activities nem para resolver o tipo de contexto.

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
./mvnw -pl harpia-integration-tests verify -Dnative
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
Finished generating 'harpia-integration-tests-1.0.0-runner' in 4m 27s

harpia-integration-tests native started in 0.033s
Installed features: [agroal, cdi, harpia-arch, hibernate-orm, jdbc-h2,
                     narayana-jta, rest, rest-jackson, ...]

Tests run: 1, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

## Por que funciona

- composição construída em build/startup;
- chamadas Java diretas no caminho de execução;
- validação arquitetural em build time com Jandex;
- nenhuma descoberta reflexiva de Activities;
- nenhuma resolução dinâmica de contexto;
- Hibernate ORM e H2 com suporte nativo do Quarkus.

As annotations têm retenção `RUNTIME` para uso CDI e documentação, mas a validação do Harpia Arch
não as lê por reflexão em runtime; ela usa o índice Jandex do build. O ArC pode gerar proxies CDI
para beans `@ApplicationScoped`, mas isso é parte do container, não do motor de workflows.

## Quando adicionar configuração nativa

O núcleo não exige reflection config própria. A aplicação ainda pode precisar de configuração
para:

- bibliotecas que usam reflexão;
- serialização de DTOs;
- recursos carregados por nome;
- integrações de terceiros.

Gere o relatório de agente nativo quando necessário:

```bash
./mvnw -pl harpia-integration-tests verify -Dnative \
  -Dquarkus.native.additional-build-args=-H:ReflectionConfigurationFiles=...
```

Prefira as extensões Quarkus e o `quarkus.native.resources.includes` antes de abrir exceções
manuais.

## Problemas comuns

| Sintoma | Causa provável |
|---|---|
| `native-image` não encontrado | `GRAALVM_HOME`/`PATH` não configurados |
| Build muito lento | Primeira compilação nativa; use cache local |
| `ClassNotFoundException` em runtime | Recurso/reflexão de biblioteca sem configuração nativa |
| Teste nativo não roda | Perfil `native`/Failsafe não ativado |
| Memória insuficiente | Aumente `-J-Xmx` do processo de build |

## Limites

Native Image não elimina a necessidade de testar a aplicação empacotada. O teste de integração
nativo cobre o fluxo principal, mas cada aplicação deve validar suas próprias dependências e
recursos.
