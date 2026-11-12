# Changelog

## 1.0.0

### Engine

- `FlowItem<I, C, O>`;
- `Workflow<I, C, O>` com composição imutável e ciclo de vida CDI;
- `FlowBuilder` tipado com `builder(Class, Class)`, `.step()` e `.build()`;
- composição hierárquica de workflows;
- adaptação explícita de contexto com `adaptContext` e `FlowItem.adapt`;
- `FlowConfigurationException`;
- execução concorrente com contexto isolado.

### Annotations

- `@HarpiaApplication`;
- `@Controller`;
- `@Service`;
- `@Repository`;
- `@Activity`;
- `@WorkflowDefinition`;
- `@Component`.

### Validação

- extensão Quarkus com runtime e deployment;
- registro das annotations como bean-defining;
- validação Jandex em build time;
- testes de CDI e de regras arquiteturais.

### Distribuição

- parent de aplicação `harpia-quarkus`;
- parent de build `harpia-arch`;
- configuração de Maven Central;
- sources, Javadoc e assinatura GPG;
- native image testado com Hibernate ORM e H2;
- benchmarks JMH.
