# Contexto opcional

O contexto existe para compartilhar informações durante a execução — correlação, usuário,
tenant, trace ou qualquer estado do caso de uso. Ele **não é obrigatório**.

O Harpia Arch suporta duas formas equivalentes:

1. **`Void` explícito** — mantém o contrato de três parâmetros;
2. **sem declaração de contexto** — usa os contratos nominais `SimpleFlowItem`/`SimpleWorkflow`.

## Por que existem dois nomes?

Java não permite declarar ao mesmo tempo:

```java
FlowItem<I, O>
FlowItem<I, C, O>
```

Interfaces não podem ser sobrecarregadas pelo número de parâmetros genéricos mantendo o mesmo
nome no mesmo pacote. O Harpia Arch não simula essa sobrecarga. A forma sem contexto usa tipos
nominais próprios:

```java
SimpleFlowItem<I, O>
SimpleWorkflow<I, O>
```

## Contrato sem contexto

```java
@FunctionalInterface
public interface SimpleFlowItem<I, O> extends FlowItem<I, Void, O> {

    O execute(I input);

    @Override
    default O execute(I input, Void context) {
        return execute(input);
    }
}
```

O método de dois parâmetros é uma ponte: recebe `null`, ignora o valor e chama `execute(input)`.
Nenhum contexto vazio é instanciado.

```java
public abstract class SimpleWorkflow<I, O>
        extends Workflow<I, Void, O>
        implements SimpleFlowItem<I, O> {

    @Override
    public final O execute(I input) {
        return execute(input, null);
    }
}
```

## `Void` explícito

Quando o time prefere manter o contrato completo:

```java
FlowItem<Cliente, Void, Cliente>
Workflow<Void, Void, List<Cliente>>
```

O contexto recebido é sempre `null`. O `Workflow` continua com os três parâmetros genéricos e
pode ser executado com `execute(input, null)`.

## Activity com contexto

```java
@Activity
public class ValidateCustomerActivity
        implements FlowItem<CustomerInput, CustomerContext, ValidatedCustomer> {

    @Override
    public ValidatedCustomer execute(CustomerInput input, CustomerContext context) {
        return new ValidatedCustomer(input.name().trim());
    }
}
```

## Activity sem contexto

```java
@Activity
public class TrimCustomerNameActivity
        implements SimpleFlowItem<CustomerInput, CustomerInput> {

    @Override
    public CustomerInput execute(CustomerInput input) {
        return new CustomerInput(input.name().trim());
    }
}
```

## Workflow sem contexto

```java
@WorkflowDefinition
public class TrimCustomerWorkflow extends SimpleWorkflow<CustomerInput, CustomerInput> {

    @Override
    protected void configure() {
        flowBuilder = FlowBuilder.builder(CustomerInput.class)
                .step(new TrimCustomerNameActivity())
                .build();
    }
}
```

## Builder

Com contexto:

```java
FlowBuilder.builder(CustomerInput.class, CustomerContext.class)
        .step(validateCustomer)
        .step(saveCustomer)
        .build();
```

Sem contexto:

```java
FlowBuilder.builder(CustomerInput.class)
        .step(validateCustomer)
        .step(saveCustomer)
        .build();
```

O overload `step(SimpleFlowItem<OUT, O>)` permite usar uma unidade sem contexto dentro de um
fluxo com contexto. O contexto continua disponível para as etapas seguintes.

## Matriz de compatibilidade

| Cenário | Compila? | Comportamento |
|---|---|---|
| Activity `SimpleFlowItem` em Workflow com contexto | Sim | O contexto é ignorado pela etapa |
| Activity `FlowItem<..., Void, ...>` em fluxo sem contexto | Sim | Recebe `null` |
| Activity `FlowItem<..., Void, ...>` em fluxo com contexto | Com `SimpleFlowItem.from(...)` | Adaptação explícita, sem criar contexto |
| Activity com contexto em fluxo com o mesmo contexto | Sim | Contexto compartilhado |
| Activity com outro contexto | Não | Não há conversão implícita |
| Workflow `SimpleWorkflow` em Workflow com contexto | Sim | O contexto externo é ignorado pelo subworkflow |
| Workflow `SimpleWorkflow` em Workflow sem contexto | Sim | Composição direta |
| Workflow que exige contexto em fluxo sem contexto | Não | Exige `adaptContext(...)` explícito |

## Adaptações explícitas

### Ignorar o contexto de um fluxo

```java
FlowItem<CustomerInput, CustomerContext, CustomerOutput> step =
        contextFreeActivity.ignoringContext();
```

### Adaptar um item `Void` para um fluxo com contexto

```java
FlowItem<CustomerInput, Void, CustomerOutput> voidActivity = ...;

flowBuilder = FlowBuilder.builder(CustomerInput.class, CustomerContext.class)
        .step(SimpleFlowItem.from(voidActivity))
        .build();
```

`SimpleFlowItem.from(...)` apenas adapta o contrato. Ele não cria contexto.

### Fornecer contexto para um componente que o exige

```java
FlowItem<CustomerInput, Void, CustomerOutput> adapted =
        contextualWorkflow.adaptContext((Void ignored) -> new CustomerContext("req-1"));
```

O `adaptContext` recebe a função que produz o contexto real. A criação é explícita e fica a cargo
da aplicação. Para entrada e saída diferentes, use `FlowItem.adapt(...)`.

## O que não fazer

```java
// Não use cast para "resolver" o contexto.
FlowItem<CustomerInput, CustomerContext, CustomerOutput> step =
        (FlowItem<CustomerInput, CustomerContext, CustomerOutput>) simpleItem;
```

```java
// Não use Object como contexto para evitar o contrato.
FlowItem<CustomerInput, Object, CustomerOutput> step = (input, context) -> ...;
```

O primeiro caso é um cast inseguro. O segundo abre mão da tipagem e não é recomendado.

## Desempenho

- o fluxo sem contexto não recebe um parâmetro de contexto;
- o overload de composição chama diretamente `execute(input)`;
- nenhum objeto de contexto vazio é alocado;
- não há reflexão para descobrir se a Activity usa contexto;
- não há resolução dinâmica do tipo de contexto;
- a compatibilidade é resolvida pelo compilador.

## Testes

Os testes do `harpia-core` cobrem:

- Activity com `Void`;
- Activity sem declaração de contexto;
- Workflow com `Void`;
- Workflow sem contexto;
- Workflow com contexto real;
- Activity sem contexto dentro de Workflow com contexto;
- Workflow sem contexto dentro de Workflow com contexto;
- tentativa de inserir componente que exige contexto em fluxo sem contexto (falha de compilação);
- ausência de casts e reflexão no caminho crítico.

Consulte [Testes](../guides/testing.md).
