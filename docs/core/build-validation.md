# Validação em build

O `harpia-core` é uma extensão Quarkus. O artefato `harpia-core-deployment` é resolvido
automaticamente pelo Quarkus e executa a validação durante o build.

Nenhuma configuração é necessária no POM da aplicação.

## Regras

| Regra | Erro |
|---|---|
| `@Activity` implementa `FlowItem` | `@Activity ... must implement ...FlowItem` |
| `@WorkflowDefinition` estende `Workflow` | `@WorkflowDefinition ... must extend ...Workflow` |
| `@Controller` não implementa `FlowItem` | `@Controller ... must not implement ...FlowItem` |
| `@Controller` não declara `@Path`/verbos | `@Controller ... must not declare @Path` |
| `@Activity` não declara `@Path`/verbos | `@Activity ... must not declare GET` |
| `@Repository` não depende de `@Controller` | `@Repository ... must not depend on @Controller` |
| Uma única `@HarpiaApplication` | `Only one @HarpiaApplication is allowed` |

## Exemplo de falha

Uma Activity anotada que não implementa `FlowItem`:

```java
@Activity
public class InvalidActivity {
}
```

O build falha:

```text
[error]: Build step io.github.krisner94.harpia.deployment.HarpiaArchProcessor#validateArchitecture
threw an exception: java.lang.IllegalStateException:
@Activity com.acme.InvalidActivity must implement io.github.krisner94.harpia.core.FlowItem
```

## Como funciona

1. O `harpia-core` publica `META-INF/quarkus-extension.properties` apontando para
   `harpia-core-deployment`.
2. O Quarkus carrega o build step do deployment.
3. O `HarpiaArchProcessor` registra as annotations como bean-defining e valida o índice Jandex.
4. Se houver violações, o build é interrompido antes de gerar o artefato.

O índice Jandex é o mesmo já construído pelo Quarkus; não há parsing de classes em runtime.

## Testes

O módulo `harpia-core-deployment` cobre:

- activity válida/inválida;
- workflow válido/inválido;
- controller implementando `FlowItem`;
- controller com `@Path`/verbo HTTP;
- repository dependendo de controller;
- múltiplas `@HarpiaApplication`;
- inicialização CDI e execução real de um workflow.

## Desativar a validação

Não existe flag de desativação. A proposta é que a arquitetura seja garantida pelo build; uma
violação deve ser corrigida, não ignorada.
