# Erros e diagnóstico

O Harpia Arch falha cedo: a maior parte dos erros aparece na compilação ou no build do Quarkus,
antes de a aplicação executar.

## Hierarquia de exceções

```java
public class FlowConfigurationException extends IllegalStateException {
}
```

Ela indica uma configuração inválida do fluxo ou do Workflow. Não há wrapping automático de
exceções de Activities.

## Erros de compilação

### Saída incompatível com a próxima entrada

```java
FlowBuilder.builder(String.class, CustomerContext.class)
        .step(lengthActivity)   // String -> Integer
        .step(lengthActivity);  // espera FlowItem<Integer, ...>
```

O compilador mostra algo como:

```text
required: FlowItem<Integer, CustomerContext, O>
found:    FlowItem<String, CustomerContext, Integer>
```

Corrija o tipo de entrada da próxima Activity ou use `FlowItem.adapt(...)` para converter
explicitamente.

### Contexto incompatível

```java
FlowBuilder.builder(String.class, CustomerContext.class)
        .step(new WorkflowComOutroContexto());
```

Um Workflow que exige outro contexto não compila. Use `adaptContext(...)`:

```java
.step(workflow.adaptContext(outro ->
        new CustomerContext(outro.correlationId())))
```

### Componente com contexto em fluxo sem contexto

```java
FlowBuilder.builder(String.class)
        .step(workflowQueExigeContexto); // não compila
```

Forneça o contexto explicitamente:

```java
FlowItem<String, Void, Customer> adapted =
        workflowQueExigeContexto.adaptContext((Void ignored) -> new CustomerContext("req-1"));
```

### Activity sem contexto em fluxo com contexto

Isso é permitido pelo overload `step(SimpleFlowItem)`:

```java
FlowBuilder.builder(String.class, CustomerContext.class)
        .step(activitySemContexto) // compila; o contexto é ignorado pela Activity
        .step(activityComContexto);
```

## `FlowConfigurationException`

Ela é lançada quando `configure()` não atribui `flowBuilder`:

```java
@WorkflowDefinition
public class BrokenWorkflow extends Workflow<String, Object, String> {

    @Override
    protected void configure() {
        // esqueceu de atribuir flowBuilder
    }
}
```

```text
BrokenWorkflow.configure() must assign flowBuilder
```

O `FlowBuilder` também lança essa exceção se `build()` for chamado sem nenhuma etapa. Na API
pública isso não acontece porque o `Starter` exige a primeira etapa.

## Validação arquitetural no build

O `harpia-core-deployment` interrompe o build com uma mensagem que contém a classe e a regra:

```text
@Activity com.acme.InvalidActivity must implement
io.github.krisner94.harpia.core.FlowItem or
io.github.krisner94.harpia.core.SimpleFlowItem
```

As regras e os exemplos completos estão em [Validação em build](build-validation.md).

## Exceções de Activities

A exceção original é propagada sem wrapping e as etapas seguintes não executam:

```java
FlowItem<String, TestTrace, String> failing = (input, context) -> {
    throw new IllegalStateException("boom");
};
```

Se a primeira etapa lançar `IllegalStateException`, o chamador recebe a mesma instância. O
Harpia Arch não faz retry, rollback ou conversão de exceção.

## Diagnóstico rápido

| Sintoma | Onde olhar |
|---|---|
| Erro de tipo em `.step()` | Entrada/saída declarada na Activity e no Workflow |
| Erro de contexto | Tipo do contexto no Workflow, `adaptContext` ou `SimpleFlowItem.from` |
| Build falha com `must implement` | A classe anotada implementa o contrato correto? |
| Build falha com `must extend` | O Workflow estende `Workflow` ou `SimpleWorkflow`? |
| Build falha com `@Path`/verbo | O Controller ou Activity está declarando rota? |
| `must not depend on @Controller` | O Repository recebeu um Controller por campo ou parâmetro? |
| `must assign flowBuilder` | O `configure()` atribui a composição? |
| Teste CDI não injeta o bean | A annotation é uma das stereotypes registradas? |

## Comandos úteis

```bash
./mvnw -pl harpia-core test
./mvnw -pl harpia-core-deployment test
./mvnw -pl harpia-integration-tests test
./mvnw -Dit verify
```

Para uma falha de build Quarkus, rode com mais detalhes:

```bash
./mvnw -pl harpia-integration-tests -Dit verify -X
```

## Limites

O núcleo não implementa:

- retry;
- compensação/Saga;
- rollback de transações;
- tratamento global de exceções;
- observabilidade automática.

Essas políticas pertencem à aplicação. Consulte [FAQ](../faq.md) e
[Persistência](../guides/persistence.md).
