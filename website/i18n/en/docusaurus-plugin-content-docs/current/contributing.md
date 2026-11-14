# Contributing

## Requirements

- Java 25;
- Maven 3.9+ (use `./mvnw`);
- Node.js 20+ for the site;
- GraalVM 25 for Native Image.

## Build

```bash
./mvnw clean install
```

The reactor contains:

- `harpia-core`;
- `harpia-core-deployment`;
- `harpia-quarkus`;
- `harpia-integration-tests`;
- `harpia-benchmarks`.

## Tests

```bash
./mvnw -pl harpia-core test
./mvnw -pl harpia-core-deployment test
./mvnw -pl harpia-integration-tests test
```

HTTP integration:

```bash
./mvnw -pl harpia-integration-tests verify -Dit
```

Native:

```bash
./mvnw -pl harpia-integration-tests verify -Dnative
```

The suite covers composition, optional context, negative compilation, core purity, Jandex validation,
CDI, JVM integration and native integration. See [Testing](guides/testing.md).

## Benchmarks

```bash
./mvnw -pl harpia-benchmarks -am package
java -jar harpia-benchmarks/target/harpia-benchmarks.jar
```

## Documentation

The site uses Docusaurus 3 and lives in `website/`:

```bash
cd website
npm install
npm start
```

Open `http://localhost:3000/Harpia-Arch/`.

Production build:

```bash
cd website
npm run build
npm run serve
```

Structure:

- `website/docs` — PT content (default locale);
- `website/i18n/en/...` — EN translations;
- `website/sidebars.js` — navigation;
- `website/docusaurus.config.js` — configuration and i18n;
- `.github/workflows/docs.yml` — GitHub Pages deployment.

When you change a document in PT, update the corresponding EN translation whenever the change
affects behavior or the API.

## Code style

- early return;
- avoid `else`;
- `switch` for selecting between discrete values;
- small, cohesive methods;
- Javadoc on public contracts;
- no comments that repeat the code;
- no reflection, casts or `Object` on the critical path;
- new contracts must preserve compatibility whenever possible.

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

Summary of the flow:

- `beta` publishes `X.Y.Z-beta-SNAPSHOT`;
- `main` publishes `X.Y.Z` to Maven Central, creates the tag and the GitHub Release;
- the version is always decided in the root `pom.xml`.

## Pull request checklist

- [ ] build and tests pass with Java 25;
- [ ] architectural rules are still valid;
- [ ] PT/EN documentation updated when necessary;
- [ ] benchmark run for engine changes;
- [ ] no credentials or keys committed.
