# FAQ

## Qual a diferença entre `harpia-arch`, `harpia-quarkus` e `harpia-core`?

- `harpia-arch`: POM de build da biblioteca;
- `harpia-quarkus`: parent recomendado para aplicações;
- `harpia-core`: dependência de runtime com engine e annotations.

Detalhes em [Módulos](modules.md).

## Preciso declarar `harpia-core-deployment`?

Não. O Quarkus resolve o artefato de deployment automaticamente a partir do `harpia-core`.

## Preciso criar uma classe de contexto?

Não. O contexto é opcional. Você pode:

- usar um contexto real (`FlowItem<I, C, O>`);
- usar `Void` explícito (`FlowItem<I, Void, O>`);
- omitir o contexto (`SimpleFlowItem<I, O>`).

Consulte [Contexto opcional](core/context.md).

## Por que a annotation é `@WorkflowDefinition` e não `@Workflow`?

Java não permite uma annotation e a classe `Workflow<I, C, O>` com o mesmo nome simples no mesmo
escopo. `@WorkflowDefinition` representa o `@Workflow` das diretrizes.

## Posso usar um Workflow como etapa de outro?

Sim. `Workflow` implementa `FlowItem`, então `.step(subworkflow)` funciona quando os contratos de
entrada, contexto e saída são compatíveis. Um `SimpleWorkflow` também pode ser etapa de um fluxo
com ou sem contexto.

## Como usar contextos diferentes?

Com adaptação explícita:

```java
workflow.adaptContext(context -> new SubContext(context.correlationId()))
```

Para entrada e saída diferentes, use `FlowItem.adapt(...)`. Não há conversão implícita.

## Posso usar `Object` no contexto?

Tecnicamente sim, mas não é recomendado. O contexto faz parte do contrato e deve ter tipo próprio.
`SimpleFlowItem` é a forma correta de não usar contexto.

## Como inserir uma Activity `Void` em um fluxo com contexto?

Use a adaptação explícita:

```java
.step(SimpleFlowItem.from(voidActivity))
```

## Posso inserir um Workflow que exige contexto em um fluxo sem contexto?

Somente com um adaptador explícito que forneça o contexto:

```java
FlowItem<Input, Void, Output> adapted =
        workflow.adaptContext((Void ignored) -> new CustomerContext("req-1"));
```

## Existe compensação/Saga automática?

Não. O núcleo não implementa compensação, retry ou rollback automático. A aplicação pode compor
essas estratégias explicitamente.

## Como tratar erros?

A exceção original é propagada e as etapas seguintes não executam. Não há wrapping automático.
Consulte [Erros e diagnóstico](core/errors.md).

## Como desabilitar a validação arquitetural?

Não há flag. A validação é parte do contrato da biblioteca; uma violação deve ser corrigida.
Consulte [Validação em build](core/build-validation.md).

## Onde colocar o contrato OpenAPI?

Por padrão em `src/main/resources/openapi/openapi.yaml`. O perfil `harpia-openapi-codegen` é
ativado quando esse diretório existe.

## Qual Java é necessário?

Java 25. O projeto compila com `maven.compiler.release=25`.

## A biblioteca funciona sem Quarkus?

O núcleo é voltado ao Quarkus: usa CDI/ArC, stereotypes e uma extensão de deployment. Não é uma
biblioteca Java standalone.

## Como executar os testes?

```bash
./mvnw test
./mvnw -Dit verify
```

Native Image:

```bash
./mvnw -pl harpia-integration-tests verify -Dnative
```

Consulte [Testes](guides/testing.md).

## Como rodar o site de documentação?

```bash
cd website
npm install
npm start
```

Acesse `http://localhost:3000/Harpia-Arch/`. Consulte [Contribuição](contributing.md).

## O motor usa reflexão?

Não no caminho crítico. A composição é feita por closures tipadas e a validação de arquitetura usa
Jandex em build time. O ArC pode usar proxies CDI para beans `@ApplicationScoped`, mas o motor não
descobre Activities nem resolve contexto por reflexão.

## O Harpia Arch é compatível com Native Image?

Sim. O núcleo foi desenhado para evitar reflexão dinâmica, e o módulo de integração testa
Hibernate ORM, H2 e REST em native image. Consulte [Native Image](guides/native-image.md).

## Qual a diferença entre `Void` e `SimpleFlowItem`?

`Void` mantém o contrato de três parâmetros e passa `null`. `SimpleFlowItem` é um tipo próprio com
`execute(input)`, mas continua sendo um `FlowItem<I, Void, O>`.

## Posso misturar os contratos?

Sim, respeitando a matriz de compatibilidade:

- Activity sem contexto em fluxo com contexto: sim;
- Workflow sem contexto em fluxo com contexto: sim;
- contexto incompatível: não;
- componente que exige contexto em fluxo sem contexto: apenas com adaptador explícito.
