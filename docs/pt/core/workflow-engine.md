# Workflow engine

## FlowItem

`FlowItem<I, C, O>` é o contrato de toda unidade executável:

```java
@FunctionalInterface
public interface FlowItem<I, C, O> {

    O execute(I input, C context);
}
```

| Tipo | Significado |
|---|---|
| `I` | entrada da unidade |
| `C` | contexto compartilhado |
| `O` | saída produzida |

O `Workflow` também implementa `FlowItem`, por isso pode ser usado como etapa de outro workflow.

## Activity

Uma Activity implementa `FlowItem`:

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

## Workflow

```java
@WorkflowDefinition
public class CreateCustomerWorkflow
        extends Workflow<CustomerInput, CustomerContext, Customer> {

    private final ValidationWorkflow validation;
    private final PersistenceWorkflow persistence;

    public CreateCustomerWorkflow(ValidationWorkflow validation,
            PersistenceWorkflow persistence) {
        this.validation = validation;
        this.persistence = persistence;
    }

    @Override
    protected void configure() {
        flowBuilder = FlowBuilder.builder(CustomerInput.class, CustomerContext.class)
                .step(validation)
                .step(persistence)
                .build();
    }
}
```

### Ciclo de vida

- `configure()` é chamado uma única vez no `@PostConstruct`.
- O fluxo construído é atribuído a `flowBuilder`.
- `execute()` é `final` e usa a composição construída.
- Workflows instanciados manualmente inicializam na primeira execução.
- Uma configuração inválida lança `FlowConfigurationException`.

```java
public abstract class Workflow<I, C, O> implements FlowItem<I, C, O> {

    protected volatile FlowItem<I, C, O> flowBuilder;

    protected abstract void configure();

    @PostConstruct
    final void initialize() { ... }

    @Override
    public final O execute(I input, C context) { ... }
}
```

## FlowBuilder

A única operação para adicionar etapas é `.step()`:

```java
flowBuilder = FlowBuilder.builder(CustomerInput.class, CustomerContext.class)
        .step(validateCustomer)
        .step(enrichCustomer)
        .step(saveCustomer)
        .build();
```

- `builder(Class, Class)` ancora os tipos de entrada e contexto;
- `.step()` infere a saída a partir da Activity;
- `.build()` devolve a composição imutável;
- o builder é imutável: cada `.step()` devolve um novo builder.

### Segurança de tipos

O tipo de saída do builder muda a cada etapa. Uma cadeia incompatível não compila:

```java
FlowBuilder.builder(String.class, CustomerContext.class)
        .step(lengthActivity)   // String -> Integer
        .step(lengthActivity);  // erro: espera FlowItem<Integer, ...>
```

```text
required: FlowItem<Integer, CustomerContext, O>
found:    FlowItem<String, CustomerContext, Integer>
```

A garantia é de compilação. Por type erasure não há verificação em runtime; os class tokens de
`builder()` são âncoras de inferência, não validação reflexiva.

## Composição hierárquica

Como `Workflow` implementa `FlowItem`, um workflow pode ser etapa de outro:

```java
flowBuilder = FlowBuilder.builder(CustomerInput.class, CustomerContext.class)
        .step(validationWorkflow)
        .step(enrichmentWorkflow)
        .step(persistenceWorkflow)
        .build();
```

Cada subworkflow possui seus próprios tipos de entrada, contexto e saída.

### Contextos diferentes

Adapte explicitamente:

```java
flowBuilder = FlowBuilder.builder(CustomerInput.class, CustomerContext.class)
        .step(validationWorkflow)
        .step(enrichmentWorkflow.adaptContext(context ->
                new EnrichmentContext(context.correlationId())))
        .step(persistenceWorkflow)
        .build();
```

Para entrada e saída diferentes:

```java
FlowItem<I, C, O> adapted = FlowItem.adapt(
        item,
        inputAdapter,
        contextAdapter,
        outputAdapter);
```

Não existe conversão implícita de contexto.

## Concorrência

- a composição é imutável após a inicialização;
- o contexto é sempre parâmetro de `execute`;
- não há `ThreadLocal`, estado global ou campos de bean com dados de requisição;
- a mesma instância de Workflow pode atender execuções concorrentes;
- os testes cobrem 128 execuções concorrentes com contextos isolados.

## Desempenho

- construção uma única vez;
- execução por chamadas Java diretas, sem reflexão;
- nenhum `Object` entre Activities;
- nenhum cast no caminho de execução;
- apenas um objeto de estado por execução quando necessário;
- compatível com GraalVM Native Image.

Consulte os [benchmarks](../guides/benchmarks.md).

## O que não faz parte do núcleo

- compensação/Saga automática;
- retry automático;
- observabilidade embutida;
- descoberta dinâmica de Activities.

Esses comportamentos não foram especificados e podem ser implementados na aplicação ou em módulos
futuros.
