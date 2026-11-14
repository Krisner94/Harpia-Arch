# Decisões arquiteturais

## ADR-001 — Composição tipada com closures

**Contexto:** encadear Activities com `Object` e casts esconderia erros de tipo.

**Decisão:** o `FlowBuilder` compõe closures com os tipos concretos de entrada e saída. O valor
nunca circula como `Object` e não há cast no caminho de execução.

**Consequências:**

- incompatibilidades falham na compilação;
- a execução é uma chamada Java direta;
- o builder é imutável e cada `.step()` cria uma nova composição.

## ADR-002 — Workflow implementa FlowItem

**Contexto:** era necessário permitir um Workflow como etapa de outro.

**Decisão:** `Workflow<I, C, O>` implementa `FlowItem<I, C, O>`.

**Consequências:**

- composição hierárquica sem adaptador para o caso de mesmo contrato;
- contextos diferentes exigem `adaptContext` explícito;
- entrada/saída diferentes exigem `FlowItem.adapt`.

## ADR-003 — Sem Saga nem observabilidade no núcleo

**Contexto:** compensação transacional e observabilidade não tinham requisitos definidos.

**Decisão:** o núcleo não implementa Saga, rollback, retry ou listeners.

**Consequências:**

- menor superfície e menor complexidade;
- a aplicação pode implementar compensação explicitamente;
- funcionalidades futuras podem virar módulos separados.

## ADR-004 — Validação em build time

**Contexto:** regras arquiteturais precisam ser garantidas antes da execução.

**Decisão:** o `harpia-core-deployment` valida as annotations com Jandex durante o build do Quarkus.

**Consequências:**

- o build falha com mensagens claras;
- nenhuma reflexão ou descoberta em runtime;
- o Quarkus resolve o deployment automaticamente.

## ADR-005 — Separação runtime/deployment

**Contexto:** extensões Quarkus exigem um artefato de runtime e outro de deployment.

**Decisão:** `harpia-core` é o runtime; `harpia-core-deployment` contém build steps e validação.

**Consequências:**

- o usuário depende apenas de `harpia-core`;
- o deployment é resolvido pelo descritor da extensão;
- os módulos têm responsabilidades claras.

## ADR-006 — Nome `@WorkflowDefinition`

**Contexto:** o documento original usava `@Workflow`, mas Java não permite uma annotation e a
classe `Workflow` com o mesmo nome simples no mesmo escopo.

**Decisão:** a annotation chama-se `@WorkflowDefinition`.

**Consequências:**

- o código compila com `@WorkflowDefinition` e `extends Workflow`;
- o nome documenta o papel da annotation.

## ADR-007 — Builder com class tokens

**Contexto:** `builder()` sem argumentos exigia type witnesses (`FlowBuilder.<I, C>builder()`).

**Decisão:** `builder(Input.class, Context.class)` ancora os tipos e permite inferência.

**Consequências:**

- a chamada fica explícita;
- os tokens não fazem validação de runtime;
- a segurança continua sendo de compilação.

## ADR-008 — Ciclo de vida

**Contexto:** a composição não pode ser reconstruída por requisição.

**Decisão:** `configure()` roda no `@PostConstruct`; `execute()` é `final`.

**Consequências:**

- construção única e publicação segura;
- subclasses não conseguem ignorar a composição;
- workflows instanciados manualmente inicializam na primeira execução.

## ADR-009 — Módulos internos fora da distribuição

**Contexto:** testes de integração e benchmarks não são bibliotecas.

**Decisão:** `harpia-integration-tests` e `harpia-benchmarks` não são publicados.

**Consequências:**

- o Maven Central recebe apenas os artefatos de consumo;
- os testes e benchmarks continuam no repositório.

## ADR-010 — Contexto opcional com contratos nominais

**Contexto:** o contexto nem sempre é necessário. Java não permite declarar `FlowItem<I, O>` e
`FlowItem<I, C, O>` com o mesmo nome no mesmo pacote, e sobrecarregar `step` por aridade de
contexto criaria ambiguidade para lambdas.

**Decisão:** manter `FlowItem<I, C, O>` e `Workflow<I, C, O>` para o contrato completo e adicionar
`SimpleFlowItem<I, O>` (que estende `FlowItem<I, Void, O>`) e `SimpleWorkflow<I, O>` (que estende
`Workflow<I, Void, O>`) para a forma sem contexto. `FlowBuilder.builder(Class)` cria fluxos sem
contexto e o overload `step(SimpleFlowItem)` permite usar uma unidade sem contexto em um fluxo com
contexto. `Void` explícito continua sendo uma forma suportada do contrato de três parâmetros.

**Consequências:**

- o desenvolvedor não precisa criar uma classe de contexto quando ela não tem função;
- `Void` explícito continua disponível e nenhum contexto vazio é instanciado;
- a compatibilidade é resolvida em compilação, sem reflexão nem casts;
- não há conversão implícita entre contextos diferentes;
- `SimpleFlowItem.from(...)` é o adaptador explícito para `Void` em fluxo com contexto;
- um componente que exige contexto só entra em fluxo sem contexto via `adaptContext(...)`
  explícito.

## ADR-011 — Testes de compilação negativa

**Contexto:** algumas garantias do Harpia Arch são do compilador, não do runtime. Um teste
convencional não consegue provar que uma composição inválida não compila.

**Decisão:** o `harpia-core` compila fontes propositalmente inválidas com
`ToolProvider.getSystemJavaCompiler()` e verifica que o compilador rejeita Activity/Workflow com
contexto incompatível.

**Consequências:**

- a compatibilidade de contratos é testada no mesmo nível em que é garantida;
- o teste depende de um JDK completo, não apenas do JRE;
- erros de tipo são detectados antes de qualquer execução.

## ADR-012 — Documentação no mesmo repositório

**Contexto:** a documentação precisava acompanhar a evolução da API e ser publicada sem depender
de um repositório separado.

**Decisão:** o site Docusaurus vive em `website/`, com PT como locale padrão e EN em `i18n/en`. O
workflow `docs.yml` publica no GitHub Pages a cada push em `website/**`.

**Consequências:**

- a documentação é versionada junto com o código;
- o build do site é reproduzível com `npm ci`;
- PT e EN são gerados no mesmo build;
- a migração deixou de depender de MkDocs e do clone separado de documentação.
