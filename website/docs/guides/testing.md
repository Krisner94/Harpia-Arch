# Testes e qualidade

A estratégia de testes do Harpia Arch combina testes de comportamento, testes de compilação e
testes de arquitetura. O objetivo não é apenas cobrir caminhos felizes, mas provar que a
composição é segura **antes** da execução.

## Pirâmide de testes

| Nível | Módulo | O que prova |
|---|---|---|
| Unitário | `harpia-core` | builder, Workflow, composição, contexto opcional e concorrência |
| Compilação negativa | `harpia-core` | contratos incompatíveis não compilam |
| Pureza do núcleo | `harpia-core` | sem reflexão e sem casts no caminho crítico |
| Validação Jandex | `harpia-core-deployment` | regras arquiteturais falham o build |
| CDI | `harpia-core-deployment` | beans, `@PostConstruct` e execução real via ArC |
| Integração JVM | `harpia-integration-tests` | REST + Hibernate ORM + H2 |
| Integração nativa | `harpia-integration-tests` | o mesmo fluxo no executável nativo |
| Benchmarks | `harpia-benchmarks` | custo e alocações do motor |

## Suítes existentes

### `harpia-core`

| Classe | Cobertura |
|---|---|
| `FlowBuilderTest` | ordem das etapas, fluxo de uma etapa, contexto compartilhado |
| `WorkflowTest` | construção única, `flow()`, falha quando `configure()` não atribui |
| `WorkflowCompositionTest` | subworkflows, adaptação de contexto, isolamento concorrente, exceções |
| `OptionalContextTest` | `Void`, `SimpleFlowItem`, `SimpleWorkflow`, composição mista |
| `ContextlessCompileSafetyTest` | compilação negativa com `javax.tools` |
| `CorePurityTest` | ausência de reflexão e casts no código do núcleo |
| `ArchitecturalAnnotationsTest` | stereotypes e marker `@HarpiaApplication` |

### `harpia-core-deployment`

| Classe | Cobertura |
|---|---|
| `ArchitectureValidatorTest` | regras de Activity, Workflow, Controller, Repository e aplicação |
| `CdiWorkflowTest` | inicialização CDI e execução de Workflow com e sem contexto |

### `harpia-integration-tests`

| Classe | Cobertura |
|---|---|
| `PersistCustomerWorkflowTest` | Workflow + Repository + Hibernate ORM em `@QuarkusTest` |
| `CustomerWorkflowIT` | `POST /customers` e `GET /customers/{id}` em `@QuarkusIntegrationTest` |

## Comandos

```bash
# Toda a suíte
./mvnw test

# Módulos isolados
./mvnw -pl harpia-core test
./mvnw -pl harpia-core-deployment test
./mvnw -pl harpia-integration-tests test

# Integração JVM
./mvnw -pl harpia-integration-tests verify -Dit

# Native Image
./mvnw -pl harpia-integration-tests verify -Dnative
```

O CI executa `./mvnw -B -ntp -Dit verify` em Java 25.

## Como escrever um teste de composição

Use tipos reais e um contexto simples. Não é necessário mockar o engine:

```java
class CreateCustomerTest {

    record TraceContext(List<String> steps) {
    }

    @Test
    void composesActivitiesInOrder() {
        TraceContext context = new TraceContext(new ArrayList<>());

        FlowItem<String, TraceContext, String> trim = (input, trace) -> {
            trace.steps().add("trim");
            return input.trim();
        };

        FlowItem<String, TraceContext, String> upper = (input, trace) -> {
            trace.steps().add("upper");
            return input.toUpperCase(Locale.ROOT);
        };

        FlowItem<String, TraceContext, String> flow = FlowBuilder
                .builder(String.class, TraceContext.class)
                .step(trim)
                .step(upper)
                .build();

        assertEquals("ANA", flow.execute(" ana ", context));
        assertEquals(List.of("trim", "upper"), context.steps());
    }
}
```

Prefira testar o contrato público: `execute`, `FlowBuilder` e o Workflow. Não teste detalhes
internos do `Stage`.

## Como testar contexto opcional

```java
SimpleFlowItem<String, String> trim = input -> input.trim();

FlowItem<String, TraceContext, String> flow = FlowBuilder
        .builder(String.class, TraceContext.class)
        .step(trim)
        .step((input, context) -> {
            context.steps().add("after-trim");
            return input;
        })
        .build();
```

O teste deve verificar que a Activity sem contexto funciona e que o contexto continua disponível
para a etapa seguinte.

## Compilação negativa

O `ContextlessCompileSafetyTest` compila fontes inválidas em tempo de teste com
`ToolProvider.getSystemJavaCompiler()` e verifica que o compilador rejeita:

- Activity que exige contexto em fluxo sem contexto;
- Workflow que exige contexto em fluxo sem contexto;
- contexto de tipo incompatível.

Esse padrão é útil quando a garantia é do compilador, não do runtime.

## Pureza do caminho crítico

`CorePurityTest` varre o código-fonte do pacote `core` e falha se encontrar:

- `java.lang.reflect`;
- `Class.forName`;
- `getDeclaredMethod`/`getMethod`;
- `isAssignableFrom`;
- `instanceof` dos contratos de fluxo;
- casts para `FlowItem`, `Workflow`, `SimpleFlowItem` ou `SimpleWorkflow`;
- cast para `Object`.

O teste é uma guarda de arquitetura. Ele não substitui revisão de código, mas impede regressões
óbvias.

## Integração com Quarkus

`PersistCustomerWorkflowTest` usa `@QuarkusTest` e valida o caminho completo até o banco H2.
`CustomerWorkflowIT` usa `@QuarkusIntegrationTest` e valida o artefato empacotado via HTTP.

Para um novo caso de uso, teste:

1. o Workflow isoladamente com um contexto fake;
2. a Activity com as dependências reais ou dublês;
3. o Controller com Rest Assured;
4. o build nativo quando a aplicação usar recursos sensíveis a reflexão.

## Concorrência

`WorkflowCompositionTest.isolatesConcurrentExecutions` executa 128 chamadas em um pool de 8
threads com contextos independentes. O teste prova que:

- a composição é compartilhada;
- o contexto é isolado por execução;
- o Workflow não guarda estado de requisição.

## Benchmark

```bash
./mvnw -pl harpia-benchmarks -am package
java -jar harpia-benchmarks/target/harpia-benchmarks.jar
```

Consulte [Benchmarks](benchmarks.md) e [Performance](performance.md).

## Cobertura

O build atual não configura um plugin de cobertura. A qualidade é medida por testes de
comportamento, compilação, arquitetura, CDI, integração e native. Se a cobertura for adicionada,
ela deve complementar esses testes, não substituí-los.
