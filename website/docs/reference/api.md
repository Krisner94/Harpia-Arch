# API Java

A documentação Javadoc é publicada junto com os artefatos.

## Javadoc

- [`harpia-core`](https://javadoc.io/doc/io.github.krisner94/harpia-core/1.0.0/)
- [`harpia-core-deployment`](https://javadoc.io/doc/io.github.krisner94/harpia-core-deployment/1.0.0/)

> Os links ficam disponíveis após a publicação da versão no Maven Central.

## Tipos públicos

### Engine

| Tipo | Papel |
|---|---|
| `io.github.krisner94.harpia.core.FlowItem` | Contrato de execução com contexto |
| `io.github.krisner94.harpia.core.SimpleFlowItem` | Contrato de execução sem contexto |
| `io.github.krisner94.harpia.core.Workflow` | Base de workflows com contexto |
| `io.github.krisner94.harpia.core.SimpleWorkflow` | Base de workflows sem contexto |
| `io.github.krisner94.harpia.core.FlowBuilder` | Builder tipado com contexto |
| `io.github.krisner94.harpia.core.SimpleFlowBuilder` | Builder tipado sem contexto |
| `io.github.krisner94.harpia.core.FlowConfigurationException` | Falha de configuração |

`Stage` é interno ao pacote e não faz parte da API pública.

## `FlowItem<I, C, O>`

```java
@FunctionalInterface
public interface FlowItem<I, C, O> {

    O execute(I input, C context);

    default <C2> FlowItem<I, C2, O> adaptContext(Function<C2, C> adapter);

    static <I, C, O, SI, SC, SO> FlowItem<I, C, O> adapt(
            FlowItem<SI, SC, SO> item,
            Function<I, SI> inputAdapter,
            Function<C, SC> contextAdapter,
            Function<SO, O> outputAdapter);
}
```

| Membro | Semântica |
|---|---|
| `execute(input, context)` | Executa a unidade e devolve a saída |
| `adaptContext(adapter)` | Adapta o contexto externo para o contexto interno |
| `FlowItem.adapt(...)` | Adapta entrada, contexto e saída explicitamente |

`adaptContext` e `adapt` são adaptações explícitas. Não há conversão implícita.

## `SimpleFlowItem<I, O>`

```java
@FunctionalInterface
public interface SimpleFlowItem<I, O> extends FlowItem<I, Void, O> {

    O execute(I input);

    @Override
    default O execute(I input, Void context) {
        return execute(input);
    }

    default <C> FlowItem<I, C, O> ignoringContext();

    static <I, O> SimpleFlowItem<I, O> from(FlowItem<I, Void, O> item);
}
```

| Membro | Semântica |
|---|---|
| `execute(input)` | Executa sem contexto |
| `execute(input, null)` | Ponte que ignora o contexto e chama `execute(input)` |
| `ignoringContext()` | Visão explícita para um fluxo com contexto |
| `from(item)` | Adapta um `FlowItem<I, Void, O>` para o contrato sem contexto |

## `Workflow<I, C, O>`

```java
public abstract class Workflow<I, C, O> implements FlowItem<I, C, O> {

    protected volatile FlowItem<I, C, O> flowBuilder;

    protected abstract void configure();

    @PostConstruct
    final void initialize();

    @Override
    public final O execute(I input, C context);

    protected final FlowItem<I, C, O> flow();
}
```

| Membro | Semântica |
|---|---|
| `flowBuilder` | Composição construída por `configure()` |
| `configure()` | Declara a composição; deve atribuir `flowBuilder` |
| `initialize()` | Executa `configure()` uma única vez no `@PostConstruct` |
| `execute(input, context)` | Executa a composição, inicializando se necessário |
| `flow()` | Retorna a composição construída |

`execute()` é `final` para que subclasses não ignorem a composição.

## `SimpleWorkflow<I, O>`

```java
public abstract class SimpleWorkflow<I, O>
        extends Workflow<I, Void, O>
        implements SimpleFlowItem<I, O> {

    @Override
    public final O execute(I input);
}
```

Herda o ciclo de vida de `Workflow` e adiciona `execute(input)`.

## `FlowBuilder<I, C, OUT>`

```java
public final class FlowBuilder<I, C, OUT> {

    public static <I, C> Starter<I, C> builder(Class<I> inputType, Class<C> contextType);

    public static <I> SimpleFlowBuilder.Starter<I> builder(Class<I> inputType);

    public <O> FlowBuilder<I, C, O> step(FlowItem<OUT, C, O> next);

    public <O> FlowBuilder<I, C, O> step(SimpleFlowItem<OUT, O> next);

    public FlowItem<I, C, OUT> build();
}
```

O overload `builder(Class)` delega para `SimpleFlowBuilder`. O overload `step(SimpleFlowItem)`
ignora o contexto do fluxo.

## `SimpleFlowBuilder<I, OUT>`

```java
public final class SimpleFlowBuilder<I, OUT> {

    public <O> SimpleFlowBuilder<I, O> step(SimpleFlowItem<OUT, O> next);

    public <O> SimpleFlowBuilder<I, O> step(FlowItem<OUT, Void, O> next);

    public SimpleFlowItem<I, OUT> build();
}
```

O overload `step(FlowItem<OUT, Void, O>)` aceita unidades com `Void` explícito e passa `null`.

## `FlowConfigurationException`

```java
public class FlowConfigurationException extends IllegalStateException {
}
```

Usada quando `configure()` não atribui `flowBuilder` ou quando uma composição vazia é construída.

## Matriz de builders

| Fluxo | Entrada | Etapas aceitas | Saída de `build()` |
|---|---|---|---|
| Com contexto | `builder(Input.class, C.class)` | `FlowItem<OUT, C, O>`, `SimpleFlowItem<OUT, O>` | `FlowItem<I, C, OUT>` |
| Sem contexto | `builder(Input.class)` | `SimpleFlowItem<OUT, O>`, `FlowItem<OUT, Void, O>` | `SimpleFlowItem<I, OUT>` |

## Annotations

| Tipo | Papel |
|---|---|
| `@HarpiaApplication` | Classe principal; marker sem CDI |
| `@Controller` | Adapter HTTP |
| `@Service` | Regras de negócio |
| `@Repository` | Persistência |
| `@Activity` | Etapa de workflow |
| `@WorkflowDefinition` | Workflow |
| `@Component` | Componente genérico |

Pacote: `io.github.krisner94.harpia.annotation`.

Todas as annotations de papel usam `@Stereotype` + `@ApplicationScoped`, exceto
`@HarpiaApplication`, que é apenas um marker com retenção de runtime.

## Código-fonte

- [GitHub](https://github.com/Krisner94/Harpia-Arch)
- [Exemplos de integração](https://github.com/Krisner94/Harpia-Arch/tree/main/harpia-integration-tests)
- [Benchmarks](https://github.com/Krisner94/Harpia-Arch/tree/main/harpia-benchmarks)
