# Glossário

| Termo | Definição |
|---|---|
| **Activity** | Unidade executável com uma responsabilidade coesa. Implementa `FlowItem` ou `SimpleFlowItem` e é anotada com `@Activity`. |
| **ArC** | Container CDI do Quarkus. Descobre beans e stereotypes em build time. |
| **Builder** | Objeto imutável que acumula etapas e devolve uma composição. `FlowBuilder` e `SimpleFlowBuilder` são os builders públicos. |
| **CDI** | Contexts and Dependency Injection. O Harpia Arch usa stereotypes `@ApplicationScoped` para registrar beans. |
| **Composição** | Encadeamento imutável de etapas. Cada `.step()` cria uma nova composição tipada. |
| **Contexto** | Objeto compartilhado entre etapas de uma execução. Pode ser real, `Void` ou ausente. |
| **Contract First** | Abordagem em que o contrato OpenAPI gera as interfaces HTTP e os DTOs antes do Controller. |
| **Deployment** | Artefato Quarkus carregado em build time. No Harpia Arch, `harpia-core-deployment` contém validação e build steps. |
| **FlowItem** | Contrato de execução `execute(input, context)`. |
| **Flow** | Pipeline imutável criado por um builder e executado por um Workflow ou diretamente. |
| **Jandex** | Índice de classes usado pelo Quarkus para validação em build time. |
| **JAX-RS** | API Jakarta REST usada pelas interfaces geradas pelo OpenAPI. |
| **Native Image** | Compilação AOT do GraalVM que gera um executável nativo. |
| **Repository** | Componente de persistência anotado com `@Repository`. Não depende de Controller, Service ou Activity. |
| **Service** | Componente de regras de negócio anotado com `@Service`. Não expõe HTTP. |
| **SimpleFlowItem** | Contrato de Activity sem contexto; estende `FlowItem<I, Void, O>`. |
| **SimpleWorkflow** | Workflow sem contexto; estende `Workflow<I, Void, O>`. |
| **Stage** | Closure interna tipada usada pelo builder para compor etapas. Não é API pública. |
| **Stereotype** | Annotation CDI que agrupa outras annotations, como `@ApplicationScoped`. |
| **Void** | Tipo usado para declarar explicitamente que não há contexto. O valor recebido é `null`. |
| **Workflow** | Composição declarativa de Activities que também é um `FlowItem`. |
