# Compatibilidade

## Versões

| Componente | Versão |
|---|---|
| Harpia Arch | 1.0.0 |
| Java | 25 |
| Maven | 3.9+ (wrapper 3.9.16) |
| Quarkus | 3.40.1.1 |
| GraalVM | 25, para Native Image |
| OpenAPI Generator | 7.4.0 |
| JUnit Jupiter | gerenciado pelo Quarkus BOM |
| Node.js | 20+, apenas para o site de documentação |

O projeto compila com `maven.compiler.release=25`. Não há suporte declarado para versões anteriores
de Java.

## Artefatos

| Coordenada | Tipo | Publicado |
|---|---|---|
| `io.github.krisner94:harpia-arch:1.0.0` | POM pai | Sim |
| `io.github.krisner94:harpia-quarkus:1.0.0` | POM pai | Sim |
| `io.github.krisner94:harpia-core:1.0.0` | JAR de runtime | Sim |
| `io.github.krisner94:harpia-core-deployment:1.0.0` | JAR de deployment | Sim |
| `io.github.krisner94:harpia-integration-tests:1.0.0` | Aplicação de teste | Não |
| `io.github.krisner94:harpia-benchmarks:1.0.0` | Benchmarks | Não |

O `harpia-core-deployment` é resolvido pelo Quarkus a partir do descritor do `harpia-core`. A
aplicação não precisa declará-lo.

## Contratos de execução

| Contrato | Uso | Contexto |
|---|---|---|
| `FlowItem<I, C, O>` | Activity com contexto | Recebido por parâmetro |
| `SimpleFlowItem<I, O>` | Activity sem contexto | Nenhum; ponte recebe `null` |
| `Workflow<I, C, O>` | Workflow com contexto | Recebido por parâmetro |
| `SimpleWorkflow<I, O>` | Workflow sem contexto | Nenhum; execução com um argumento |
| `FlowItem<I, Void, O>` | Activity com `Void` explícito | `null` |

## Compatibilidade entre contextos

| Origem | Destino | Resultado |
|---|---|---|
| `FlowItem<I, C, O>` | fluxo com o mesmo `C` | Direto |
| `SimpleFlowItem<I, O>` | fluxo com qualquer `C` | Direto; contexto ignorado |
| `SimpleWorkflow<I, O>` | fluxo com qualquer `C` | Direto; contexto ignorado |
| `FlowItem<I, Void, O>` | fluxo sem contexto | Direto; recebe `null` |
| `FlowItem<I, Void, O>` | fluxo com `C` real | `SimpleFlowItem.from(...)` |
| `FlowItem<I, C1, O>` | fluxo com `C2` diferente | `adaptContext(...)` |
| `SimpleWorkflow<I, O>` | fluxo sem contexto | Direto |

Não há conversão implícita entre `C1` e `C2`, cast entre contratos ou resolução dinâmica.

## Compatibilidade com a versão anterior

A versão 1.0.0 mantém os contratos `FlowItem`, `Workflow` e `FlowBuilder` existentes. A adição de
`SimpleFlowItem`, `SimpleWorkflow`, `SimpleFlowBuilder` e dos overloads de `step`/`builder` é
aditiva:

- código que usa `FlowItem<I, C, O>` continua compilando;
- código que usa `Workflow<I, C, O>` continua compilando;
- `FlowBuilder.builder(Class, Class)` continua igual;
- `Void` explícito continua suportado;
- não há depreciações em 1.0.0.

## Native Image

O núcleo não usa reflexão para descobrir Activities nem para resolver contexto. A aplicação ainda
depende do suporte nativo das bibliotecas que usa. O módulo de integração testa Hibernate ORM,
H2 e REST em native image. Consulte [Native Image](../guides/native-image.md).

## Documentação

O site é construído com Docusaurus 3 e publicado no GitHub Pages em:

```text
https://krisner94.github.io/Harpia-Arch/
```

O código-fonte da documentação fica em `website/`. As versões PT e EN são geradas no mesmo build.

## Política de evolução

- mudanças de contrato devem ser discutidas em uma ADR;
- adições devem preservar a compatibilidade binária e de fonte quando possível;
- a validação arquitetural pode ficar mais rígida apenas com uma justificativa e migração;
- releases são imutáveis no Maven Central;
- snapshots da branch `beta` não são versões estáveis.
