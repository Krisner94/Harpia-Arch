# Validação em build

O `harpia-core` é uma extensão Quarkus. O artefato `harpia-core-deployment` é resolvido
automaticamente pelo Quarkus e executa a validação durante o build.

Nenhuma configuração é necessária no POM da aplicação.

## Regras

| Regra | Erro |
|---|---|
| `@Activity` implementa `FlowItem` ou `SimpleFlowItem` | `@Activity ... must implement ...FlowItem or ...SimpleFlowItem` |
| `@WorkflowDefinition` estende `Workflow` ou `SimpleWorkflow` | `@WorkflowDefinition ... must extend ...Workflow or ...SimpleWorkflow` |
| `@Controller` não implementa `FlowItem`/`SimpleFlowItem` | `@Controller ... must not implement ...FlowItem or ...SimpleFlowItem` |
| `@Controller` não declara `@Path`/verbos | `@Controller ... must not declare @Path` |
| `@Activity` não declara `@Path`/verbos | `@Activity ... must not declare GET` |
| `@Repository` não depende de `@Controller` | `@Repository ... must not depend on @Controller` |
| `@Service` não declara `@Path`/verbos | `@Service ... must not declare GET` |
| `@Component` não declara `@Path`/verbos | `@Component ... must not declare GET` |
| Uma única `@HarpiaApplication` | `Only one @HarpiaApplication is allowed` |

## Exemplo de falha

Uma Activity anotada que não implementa `FlowItem` nem `SimpleFlowItem`:

```java
@Activity
public class InvalidActivity {
}
```

O build falha:

```text
[error]: Build step io.github.krisner94.harpia.deployment.HarpiaArchProcessor#validateArchitecture
threw an exception: java.lang.IllegalStateException:
@Activity com.acme.InvalidActivity must implement io.github.krisner94.harpia.core.FlowItem or io.github.krisner94.harpia.core.SimpleFlowItem
```

## Como o validador funciona

O `ArchitectureValidator` recebe o índice Jandex do Quarkus e:

1. coleta as classes anotadas;
2. verifica a hierarquia de interfaces e classes;
3. inspeciona `@Path` e verbos HTTP;
4. coleta dependências de `@Repository` em campos e parâmetros de métodos;
5. agrega todas as violações e lança uma única exceção com o build.

A verificação de `@Activity` aceita:

- `FlowItem<I, C, O>`;
- `SimpleFlowItem<I, O>`;
- qualquer classe que implemente esses contratos indiretamente.

A verificação de `@WorkflowDefinition` aceita:

- `Workflow<I, C, O>`;
- `SimpleWorkflow<I, O>`;
- subclasses indiretas.

## Registro das annotations

As annotations de papel são registradas como bean-defining annotations:

```java
producer.produce(new BeanDefiningAnnotationBuildItem(
        DotName.createSimple(Activity.class.getName())));
```

Isso garante que o ArC descubra classes anotadas mesmo quando a biblioteca não é um bean archive
explícito. O `@HarpiaApplication` é um marker e não cria bean.

## Extensão Quarkus

```text
harpia-core
  META-INF/quarkus-extension.yaml
  META-INF/quarkus-extension.properties -> deployment artifact

harpia-core-deployment
  META-INF/quarkus-build-steps.list -> HarpiaArchProcessor
```

O Quarkus carrega o deployment durante o build, executa os build steps e só gera o artefato se
todas as regras passarem.

## Testes

O módulo `harpia-core-deployment` cobre:

- activity válida/inválida (com e sem contexto);
- workflow válido/inválido (com e sem contexto);
- controller implementando `FlowItem`/`SimpleFlowItem`;
- controller com `@Path`/verbo HTTP;
- repository dependendo de controller;
- múltiplas `@HarpiaApplication`;
- inicialização CDI e execução real de um workflow.

Consulte [Testes](../guides/testing.md).

## Desativar a validação

Não existe flag de desativação. A proposta é que a arquitetura seja garantida pelo build; uma
violação deve ser corrigida, não ignorada.

## Diagnóstico

| Erro | Correção |
|---|---|
| `must implement FlowItem` | Implemente `FlowItem` ou `SimpleFlowItem`, ou remova `@Activity` |
| `must extend Workflow` | Estenda `Workflow`/`SimpleWorkflow`, ou remova `@WorkflowDefinition` |
| `must not implement FlowItem` | Mova a etapa para uma `@Activity` |
| `must not declare @Path` | Remova a rota; use a interface gerada pelo OpenAPI |
| `must not depend on @Controller` | Remova a dependência invertida do Repository |
| `Only one @HarpiaApplication` | Mantenha apenas a classe principal |

Consulte [Erros e diagnóstico](errors.md).
