# Benchmarks

O módulo `harpia-benchmarks` usa JMH para medir o custo real do motor.

## Executar

```bash
mvn -pl harpia-benchmarks -am package
java -jar harpia-benchmarks/target/harpia-benchmarks.jar
```

O `package` gera um JAR executável com o `org.openjdk.jmh.Main`.

### Alocações

```bash
java -jar harpia-benchmarks/target/harpia-benchmarks.jar -prof gc
```

### Execução rápida

```bash
java -jar harpia-benchmarks/target/harpia-benchmarks.jar \
  -wi 1 -i 1 -f 1 -w 200ms -r 200ms
```

## Benchmarks

| Benchmark | O que mede |
|---|---|
| `directCall` | chamada direta a `IntUnaryOperator` |
| `plainFlowItem` | chamada a um `FlowItem` simples |
| `singleActivity` | Activity única composta pelo builder |
| `workflowWithThreeSteps` | workflow com 3 etapas |
| `workflowWithSixteenSteps` | workflow com 16 etapas |
| `nestedWorkflows` | 3 subworkflows, 9 etapas no total |
| `buildComposition` | custo de construção de uma composição de 3 etapas |

## Resultado de referência

Ambiente: 3 warmups, 5 medições de 500 ms.

| Benchmark | Custo |
|---|---|
| Chamada direta | 0.44 ns/op |
| `FlowItem` simples | 0.44 ns/op |
| Activity única composta | 1.79 ns/op |
| Workflow com 3 etapas | 5.00 ns/op |
| Workflow com 16 etapas | 58.08 ns/op |
| 3 subworkflows (9 etapas) | 56.65 ns/op |
| Construção de composição (3 etapas) | 9.69 ns/op, 80 B/op |

Os números são específicos do ambiente. Compare sempre na mesma máquina, com o mesmo JDK e a
mesma configuração de JMH.
