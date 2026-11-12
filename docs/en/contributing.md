# Contributing

## Build

```bash
mvn clean install
```

The reactor contains:

- `harpia-core`;
- `harpia-core-deployment`;
- `harpia-quarkus`;
- `harpia-integration-tests`;
- `harpia-benchmarks`.

## Tests

```bash
mvn -pl harpia-core test
mvn -pl harpia-core-deployment test
mvn -pl harpia-integration-tests test
```

HTTP integration test:

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

## Documentation

```bash
pip install -r requirements-docs.txt
mkdocs serve
```

Strict build:

```bash
mkdocs build --strict
```

## Code style

- early return;
- avoid `else`;
- `switch` for discrete values;
- small, cohesive methods;
- Javadoc for public contracts;
- no comments that repeat the code;
- no reflection on the hot path.

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

See [Maven Central](guides/maven-central.md) and
[`RELEASING.md`](https://github.com/Krisner94/Harpia-Arch/blob/main/RELEASING.md).
