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
| `io.github.krisner94.harpia.core.FlowItem` | Contrato de execução |
| `io.github.krisner94.harpia.core.Workflow` | Base de workflows |
| `io.github.krisner94.harpia.core.FlowBuilder` | Builder tipado |
| `io.github.krisner94.harpia.core.FlowConfigurationException` | Falha de configuração |

### Annotations

| Tipo | Papel |
|---|---|
| `@HarpiaApplication` | Classe principal |
| `@Controller` | Adapter HTTP |
| `@Service` | Regras de negócio |
| `@Repository` | Persistência |
| `@Activity` | Etapa de workflow |
| `@WorkflowDefinition` | Workflow |
| `@Component` | Componente genérico |

Pacote: `io.github.krisner94.harpia.annotation`.

## Código-fonte

- [GitHub](https://github.com/Krisner94/Harpia-Arch)
- [Exemplos de integração](https://github.com/Krisner94/Harpia-Arch/tree/main/harpia-integration-tests)
- [Benchmarks](https://github.com/Krisner94/Harpia-Arch/tree/main/harpia-benchmarks)
