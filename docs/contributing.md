# Contribuição

## Build

```bash
mvn clean install
```

O reactor contém:

- `harpia-core`;
- `harpia-core-deployment`;
- `harpia-quarkus`;
- `harpia-integration-tests`;
- `harpia-benchmarks`.

## Testes

```bash
mvn -pl harpia-core test
mvn -pl harpia-core-deployment test
mvn -pl harpia-integration-tests test
```

Integration test HTTP:

```bash
mvn -pl harpia-integration-tests verify -Dit
```

Native:

```bash
mvn -pl harpia-integration-tests verify -Dnative
```

## Benchmarks

```bash
mvn -pl harpia-benchmarks -am package
java -jar harpia-benchmarks/target/harpia-benchmarks.jar
```

## Documentação

```bash
pip install -r requirements-docs.txt
mkdocs serve
```

Build estrito:

```bash
mkdocs build --strict
```

## Estilo de código

- early return;
- evitar `else`;
- `switch` para seleção entre valores discretos;
- métodos pequenos e coesos;
- Javadoc em contratos públicos;
- sem comentários que repitam o código;
- sem reflexão no caminho crítico.

## Commits

Use Conventional Commits:

```text
feat(core): ...
fix(deployment): ...
docs: ...
test(integration): ...
build: ...
perf(benchmarks): ...
```

## Release

Consulte [Maven Central](guides/maven-central.md) e o
[`RELEASING.md`](https://github.com/Krisner94/Harpia-Arch/blob/main/RELEASING.md).
