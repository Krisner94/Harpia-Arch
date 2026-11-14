# Workflow engine

## FlowItem

`FlowItem<I, C, O>` é o contrato de toda unidade executável com contexto:

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

## Contexto opcional

O contexto não é obrigatório. O desenvolvedor pode escolher entre duas formas equivalentes de
declaração. A referência completa, com a matriz de compatibilidade e os adaptadores explícitos,
está em [Contexto opcional](context.md).

### 1. `Void` explícito

Quando a preferência for manter o contrato de três parâmetros, declare `Void`:

```java
FlowItem<Cliente, Void, Cliente>

Workflow<Void, Void, List<Cliente>>
```

O contexto recebido por uma unidade `Void` é sempre `null`. Nenhuma instância de contexto é
criada. Um `Workflow` ou `FlowBuilder` com `Void` continua usando o contrato completo de três
parâmetros genéricos.

### 2. Sem declaração de Context

Também é possível omitir o contexto:

```java
SimpleFlowItem<Cliente, Cliente>

SimpleWorkflow<Void, List<Cliente>>
```

`SimpleFlowItem` e `SimpleWorkflow` são os nomes dos contratos sem contexto. Eles não são uma
segunda declaração de `FlowItem`/`Workflow`: Java não permite sobrecarregar interfaces pelo número
de parâmetros genéricos mantendo o mesmo nome no mesmo pacote. A sintaxe sem contexto usa,
portanto, tipos nominais próprios.

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

```java
public abstract class SimpleWorkflow<I, O> extends Workflow<I, Void, O>
        implements SimpleFlowItem<I, O> {

    @Override
    public final O execute(I input) {
        return execute(input, null);
    }
}
```

Como `SimpleFlowItem` estende `FlowItem<I, Void, O>`, a unidade continua compatível com o
contrato de três parâmetros onde ele for necessário. A ponte passa `null`; não há alocação de um
objeto de contexto vazio.

## Activity

Uma Activity pode usar contexto:

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

Ou pode não declarar contexto:

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

Uma Activity com `Void` explícito também é válida:

```java
@Activity
public class TrimCustomerNameActivity
        implements FlowItem<CustomerInput, Void, CustomerInput> {

    @Override
    public CustomerInput execute(CustomerInput input, Void context) {
        return new CustomerInput(input.name().trim());
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

Sem contexto:

```java
@WorkflowDefinition
public class CreateCustomerWorkflow
        extends SimpleWorkflow<CustomerInput, Customer> {

    private final TrimCustomerNameActivity trim;
    private final PersistCustomerActivity persist;

    public CreateCustomerWorkflow(TrimCustomerNameActivity trim,
            PersistCustomerActivity persist) {
        this.trim = trim;
        this.persist = persist;
    }

    @Override
    protected void configure() {
        flowBuilder = FlowBuilder.builder(CustomerInput.class)
                .step(trim)
                .step(persist)
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

`SimpleWorkflow<I, O>` herda esse ciclo de vida e adiciona `execute(I)`.

## FlowBuilder

A única operação para adicionar etapas é `.step()`.

Com contexto:

```java
flowBuilder = FlowBuilder.builder(CustomerInput.class, CustomerContext.class)
        .step(validateCustomer)
        .step(enrichCustomer)
        .step(saveCustomer)
        .build();
```

Sem contexto:

```java
flowBuilder = FlowBuilder.builder(CustomerInput.class)
        .step(validateCustomer)
        .step(enrichCustomer)
        .step(saveCustomer)
        .build();
```

- `builder(Class)` ancora a entrada de um fluxo sem contexto e devolve `SimpleFlowBuilder`;
- `builder(Class, Class)` ancora entrada e contexto;
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

### Composição entre contratos

| Cenário | Resultado | Mecanismo |
|---|---|---|
| Activity sem contexto em Workflow com contexto | suportado | overload `step(SimpleFlowItem)`, ignora o contexto |
| Activity com `Void` em fluxo sem contexto | suportado | overload `step(FlowItem<..., Void, ...>)` |
| Activity que exige contexto em fluxo com o mesmo contexto | suportado | `step(FlowItem<..., C, ...>)` |
| Activity que exige outro contexto | não compila | não existe conversão implícita |
| Workflow sem contexto dentro de Workflow sem contexto | suportado | `SimpleWorkflow` é `SimpleFlowItem` |
| Workflow sem contexto dentro de Workflow com contexto | suportado | overload `step(SimpleFlowItem)` |
| Workflow que exige contexto em fluxo com o mesmo contexto | suportado | `step(FlowItem<..., C, ...>)` |
| Workflow que exige contexto em fluxo sem contexto | não compila | exige adaptador explícito |
| Contexto `Void` explícito em fluxo com contexto | adaptação explícita | `SimpleFlowItem.from(...)` |

A ausência de contexto não impede a composição. O contexto é simplesmente ignorado pela unidade
sem contexto; ele continua disponível para as etapas seguintes.

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

Para inserir um componente que exige contexto em um fluxo sem contexto, forneça o contexto
explicitamente:

```java
FlowItem<CustomerInput, Void, Customer> adapted = contextualWorkflow
        .adaptContext((Void ignored) -> new CustomerContext("req-1"));

SimpleFlowItem<CustomerInput, Customer> contextless = SimpleFlowItem.from(adapted);
```

Para um componente com `Void` explícito participar de um fluxo com contexto:

```java
flowBuilder = FlowBuilder.builder(CustomerInput.class, CustomerContext.class)
        .step(SimpleFlowItem.from(voidActivity))
        .step(validateCustomer)
        .build();
```

`SimpleFlowItem.from(...)` apenas adapta o contrato; não cria contexto.

Para entrada e saída diferentes:

```java
FlowItem<I, C, O> adapted = FlowItem.adapt(
        item,
        inputAdapter,
        contextAdapter,
        outputAdapter);
```

Não existe conversão implícita de contexto e não há cast entre contratos.

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
- nenhum contexto vazio criado automaticamente;
- nenhuma resolução dinâmica do tipo de contexto;
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
