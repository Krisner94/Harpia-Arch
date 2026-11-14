# Changelog

## 1.0.0

### Engine

- `FlowItem<I, C, O>`;
- `SimpleFlowItem<I, O>` para Activities sem contexto;
- `Workflow<I, C, O>` com composição imutável e ciclo de vida CDI;
- `SimpleWorkflow<I, O>` para Workflows sem contexto;
- `FlowBuilder` tipado com `builder(Class, Class)`, `.step()` e `.build()`;
- `SimpleFlowBuilder` acionado por `builder(Class)`;
- overload `step(SimpleFlowItem)` para compor unidades sem contexto em fluxos com contexto;
- overload `step(FlowItem<..., Void, ...>)` para fluxos sem contexto;
- composição hierárquica de workflows;
- adaptação explícita de contexto com `adaptContext`, `ignoringContext` e `FlowItem.adapt`;
- `SimpleFlowItem.from(...)` para adaptar unidades `Void`;
- `FlowConfigurationException`;
- execução concorrente com contexto isolado.

### Contexto opcional

- `Void` explícito suportado sem criar objeto de contexto;
- ausência de contexto por `SimpleFlowItem`/`SimpleWorkflow`;
- Activity sem contexto em Workflow com contexto;
- Workflow sem contexto em Workflow com contexto;
- contexto incompatível rejeitado em compilação;
- componente que exige contexto em fluxo sem contexto exige `adaptContext(...)`.

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
- suporte a `SimpleFlowItem` e `SimpleWorkflow` na validação;
- testes de CDI e de regras arquiteturais.

### Testes

- testes de composição e ciclo de vida;
- testes de contexto opcional;
- compilação negativa com `javax.tools`;
- guarda de pureza do núcleo (sem reflexão/casts);
- teste CDI com Workflow com e sem contexto;
- testes de integração JVM e Native Image;
- 44 testes no total.

### Distribuição

- parent de aplicação `harpia-quarkus`;
- parent de build `harpia-arch`;
- configuração de Maven Central;
- sources, Javadoc e assinatura GPG;
- native image testado com Hibernate ORM e H2;
- benchmarks JMH;
- CI em Java 25;
- publicação beta em snapshots e release automatizado na `main`.

### Documentação

- site Docusaurus 3 em PT/EN;
- migração do MkDocs para Docusaurus;
- deploy no GitHub Pages via `.github/workflows/docs.yml`;
- busca local, Mermaid e tema claro/escuro;
- novas páginas de arquitetura, contexto, erros, testes, configuração, performance,
  compatibilidade e glossário.
