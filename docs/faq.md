# FAQ

## Qual a diferença entre `harpia-arch`, `harpia-quarkus` e `harpia-core`?

- `harpia-arch`: POM de build da biblioteca;
- `harpia-quarkus`: parent recomendado para aplicações;
- `harpia-core`: dependência de runtime com engine e annotations.

Detalhes em [Módulos](modules.md).

## Preciso declarar `harpia-core-deployment`?

Não. O Quarkus resolve o artefato de deployment automaticamente a partir do `harpia-core`.

## Por que a annotation é `@WorkflowDefinition` e não `@Workflow`?

Java não permite uma annotation e a classe `Workflow<I, C, O>` com o mesmo nome simples no mesmo
escopo. `@WorkflowDefinition` representa o `@Workflow` das diretrizes.

## Posso usar um Workflow como etapa de outro?

Sim. `Workflow` implementa `FlowItem`, então `.step(subworkflow)` funciona quando os contratos de
entrada, contexto e saída são compatíveis.

## Como usar contextos diferentes?

Com adaptação explícita:

```java
workflow.adaptContext(context -> new SubContext(context.correlationId()))
```

Para entrada/saída diferentes, use `FlowItem.adapt(...)`.

## Posso usar `Object` no contexto?

Tecnicamente sim, mas não é recomendado. O contexto faz parte do contrato e deve ter tipo próprio.

## Existe compensação/Saga automática?

Não. O núcleo não implementa compensação, retry ou rollback automático. A aplicação pode compor
essas estratégias explicitamente.

## Como tratar erros?

A exceção original é propagada e as etapas seguintes não executam. Não há wrapping automático.

## Como desabilitar a validação arquitetural?

Não há flag. A validação é parte do contrato da biblioteca; uma violação deve ser corrigida.

## Onde colocar o contrato OpenAPI?

Por padrão em `src/main/resources/openapi/openapi.yaml`.

## Qual Java é necessário?

Java 25. O projeto compila com `maven.compiler.release=25`.

## A biblioteca funciona sem Quarkus?

O núcleo é voltado ao Quarkus: usa CDI/ArC, stereotypes e uma extensão de deployment. Não é uma
biblioteca Java standalone.

## Como executar os testes nativos?

```bash
mvn -pl harpia-integration-tests verify -Dnative
```

Requer GraalVM com `native-image`.
