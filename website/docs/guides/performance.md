# Performance e concorrência

O Harpia Arch foi desenhado para que a execução de um fluxo seja próxima de uma sequência de
chamadas Java diretas. O custo relevante está na construção da composição e no trabalho real das
Activities, não em um motor interpretativo.

## Princípios

- **construção única** — `configure()` roda no `@PostConstruct`;
- **composição imutável** — cada `.step()` devolve um novo builder; o fluxo final não muda;
- **chamadas diretas** — as etapas são closures `Stage`, não `Method.invoke`;
- **sem `Object` no encadeamento** — os tipos concretos permanecem no código gerado;
- **sem casts** — a compatibilidade é resolvida pelo compilador;
- **contexto opcional sem alocação** — `SimpleFlowItem` não cria contexto vazio;
- **sem resolução dinâmica de contexto** — nenhuma reflexão ou inspeção de tipo em runtime.

## Modelo de custo

Para um fluxo com `n` etapas:

- a construção cria `n` composições intermediárias e o fluxo final;
- a execução percorre `n` chamadas de função;
- não há alocação por etapa no motor, exceto o que a própria Activity alocar;
- um fluxo sem contexto não passa contexto entre as etapas;
- um Workflow aninhado adiciona uma chamada de `execute` por nível.

O `FlowBuilder` é imutável: cada `.step()` cria uma nova `Stage` que referencia a anterior. Isso
troca um pequeno custo de construção por uma execução simples e segura.

## Benchmarks

```bash
./mvnw -pl harpia-benchmarks -am package
java -jar harpia-benchmarks/target/harpia-benchmarks.jar
```

Para inspecionar alocações:

```bash
java -jar harpia-benchmarks/target/harpia-benchmarks.jar -prof gc
```

Para uma execução rápida:

```bash
java -jar harpia-benchmarks/target/harpia-benchmarks.jar \
  -wi 1 -i 1 -f 1 -w 200ms -r 200ms
```

### Benchmarks disponíveis

| Benchmark | O que mede |
|---|---|
| `directCall` | chamada direta a `IntUnaryOperator` |
| `plainFlowItem` | chamada a um `FlowItem` simples |
| `singleActivity` | Activity única composta pelo builder |
| `workflowWithThreeSteps` | Workflow com 3 etapas |
| `workflowWithSixteenSteps` | Workflow com 16 etapas |
| `nestedWorkflows` | 3 subworkflows, 9 etapas |
| `buildComposition` | construção de uma composição de 3 etapas |
| `contextFreeActivity` | Activity única sem contexto |
| `contextFreeWorkflowWithSixteenSteps` | Workflow sem contexto com 16 etapas |
| `contextFreeNestedWorkflows` | 3 subworkflows sem contexto |
| `buildContextFreeComposition` | construção de uma composição sem contexto |

### Referência

Ambiente: 3 warmups, 5 medições de 500 ms, nesta máquina.

| Benchmark | Custo |
|---|---|
| Chamada direta | 0.44 ns/op |
| `FlowItem` simples | 0.44 ns/op |
| Activity única composta | 1.79 ns/op |
| Workflow com 3 etapas | 5.00 ns/op |
| Workflow com 16 etapas | 58.08 ns/op |
| 3 subworkflows (9 etapas) | 56.65 ns/op |
| Construção de composição (3 etapas) | 9.69 ns/op, 80 B/op |

Números de benchmark são específicos de hardware, JDK, governor de CPU e configuração do JMH.
Compare sempre na mesma máquina e com a mesma carga.

## Contexto e alocação

Há três caminhos:

| Caminho | Contexto por execução | Alocação do motor |
|---|---|---|
| `FlowItem` com contexto real | objeto fornecido pelo chamador | nenhuma |
| `SimpleFlowItem` | nenhum | nenhuma |
| `Void` explícito | `null` | nenhuma |

O overload `step(SimpleFlowItem)` chama `execute(input)` diretamente. Ele não cria um `Void`,
não cria um contexto vazio e não faz `instanceof`.

## Concorrência

O Workflow é seguro para execuções concorrentes quando o contexto fornecido também é:

- a composição é publicada uma vez em um campo `volatile`;
- `execute()` não altera o `flowBuilder`;
- não há `ThreadLocal` no motor;
- o contexto é sempre parâmetro de execução;
- cada execução usa sua própria instância de contexto.

Se o contexto for mutável e compartilhado entre threads, a segurança passa a depender da
aplicação. Prefira contextos imutáveis ou coleções concorrentes quando necessário.

## Native Image

O motor não usa reflexão para descobrir Activities nem para resolver contexto. Isso reduz a
necessidade de configuração nativa no núcleo. As dependências da aplicação, como Hibernate ORM e
REST, continuam sujeitas ao suporte nativo do Quarkus.

Consulte [Native Image](native-image.md).

## Como medir uma mudança

1. meça a linha de base com o mesmo JMH e a mesma máquina;
2. altere uma variável por vez;
3. rode `-prof gc` para detectar alocações inesperadas;
4. compare mediana/erro, não apenas o melhor resultado;
5. valide o caminho nativo quando a mudança afetar inicialização ou reflexão.

## Limites

O motor não implementa pool de fluxos, cache de execução, paralelismo de etapas ou retry. Essas
decisões pertencem ao caso de uso e podem ser implementadas em Activities, Workflows ou módulos
separados.
