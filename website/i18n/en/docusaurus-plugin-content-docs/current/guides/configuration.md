# Configuration and build

The reactor is a Maven aggregator with two parents and two extension artifacts — runtime and deployment. Understanding this
split avoids declaring duplicate dependencies and plugins in the application.

## Requirements

| Requirement | Version |
|---|---|
| Java | 25 |
| Maven | 3.9+ (the wrapper pins 3.9.16) |
| Quarkus | 3.40.1.1 |
| GraalVM | 25, only for Native Image |
| Node.js | 20+, only for the documentation site |

Use the repository wrapper:

```bash
./mvnw -v
```

## Recommended parent

The application should use `harpia-quarkus`:

```xml
<parent>
    <groupId>io.github.krisner94</groupId>
    <artifactId>harpia-quarkus</artifactId>
    <version>1.0.0</version>
</parent>

<groupId>com.acme</groupId>
<artifactId>meu-servico</artifactId>
<version>1.0.0</version>
<packaging>quarkus</packaging>
```

`packaging=quarkus` is mandatory. The parent already provides:

- the Quarkus BOM and the Quarkus version;
- `quarkus-arc`, `quarkus-rest` and `quarkus-rest-jackson`;
- `microprofile-openapi-api` and `jakarta.validation-api`;
- `harpia-core`;
- JUnit and Rest Assured in test scope;
- `quarkus-maven-plugin` with `extensions=true`;
- compiler, Surefire and Failsafe;
- the `native` profile;
- the `harpia-openapi-codegen` profile.

Do not declare the `quarkus-maven-plugin` or the compilation/test plugins again. The application
declares only its own dependencies and properties.

## Using `harpia-core` without the parent

If the application already has another parent, import the Quarkus BOM and declare:

```xml
<dependency>
    <groupId>io.github.krisner94</groupId>
    <artifactId>harpia-core</artifactId>
    <version>1.0.0</version>
</dependency>
```

Also declare the Quarkus extensions used by the application (`quarkus-rest`,
`quarkus-rest-jackson`, validation, persistence, etc.) and configure the OpenAPI codegen in your POM.

`harpia-core-deployment` is not declared: Quarkus resolves the deployment through the extension
descriptor.

## Build properties

| Property | Default value | Role |
|---|---|---|
| `maven.compiler.release` | `25` | Compilation version |
| `quarkus.version` | `3.40.1.1` | Quarkus BOM and plugins |
| `openapi-generator.version` | `7.4.0` | Contract generator |
| `build-helper-maven-plugin.version` | `3.6.0` | Source root for generated code |
| `surefire-plugin.version` | `3.5.6` | Tests and Failsafe |
| `harpia.openapi.spec` | `src/main/resources/openapi/openapi.yaml` | Input contract |
| `harpia.openapi.api-package` | `${project.groupId}.api` | Package for the interfaces |
| `harpia.openapi.model-package` | `${harpia.openapi.api-package}.beans` | Package for the DTOs |
| `harpia.openapi.api-suffix` | `Api` | Interface suffix |
| `central.autoPublish` | `true` | Publication to Central in the `release` profile |
| `central.waitUntil` | `published` | Central wait condition |
| `central.waitMaxTime` | `3600` | Maximum wait time, in seconds |

## OpenAPI contract-first

The `harpia-openapi-codegen` profile is activated automatically when
`src/main/resources/openapi` exists. It runs in the `generate-sources` phase and uses the `jaxrs-spec`
generator with:

- `interfaceOnly=true`;
- `useJakartaEe=true`;
- `useMicroProfileOpenAPIAnnotations=true`;
- `useBeanValidation=true`;
- `useSwaggerAnnotations=false`;
- `generateSupportingFiles=false`.

The generated directory is:

```text
target/generated-sources/openapi/src/main/java
```

The `build-helper-maven-plugin` adds it as a source root before `javac`.

See [Contract First](contract-first.md) for the complete contract and Controller example.

## Quarkus configuration

The integration module uses an in-memory H2 database:

```properties
quarkus.datasource.db-kind=h2
quarkus.datasource.username=sa
quarkus.datasource.password=
quarkus.datasource.jdbc.url=jdbc:h2:mem:customers;DB_CLOSE_DELAY=-1
quarkus.hibernate-orm.schema-management.strategy=drop-and-create
quarkus.hibernate-orm.log.sql=false
```

In production, change the driver and use migrations. See [Persistence](persistence.md).

## Maven profiles

| Profile | Activation | Effect |
|---|---|---|
| `harpia-openapi-codegen` | `src/main/resources/openapi` exists | Generates interfaces and DTOs |
| `native` | `-Dnative` | Enables Native Image and Failsafe |
| `it` | `-Dit` | Runs `@QuarkusIntegrationTest` |
| `release` | `-Prelease` | Sources, Javadoc, GPG and Central |

## Commands

```bash
./mvnw test                              # unit tests
./mvnw -Dit verify                       # unit tests + JVM integration tests
./mvnw -pl harpia-integration-tests verify -Dnative
./mvnw -Prelease verify -Dgpg.skip=true  # validate the release package
./mvnw -Prelease deploy -DskipITs        # publish to Central
```

## CI and publication

| Workflow | Trigger | Result |
|---|---|---|
| `ci.yml` | push/PR on `main` and `beta` | build + tests on Java 25 |
| `docs.yml` | push on `website/**` | Docusaurus build + GitHub Pages |
| `beta.yml` | push on `beta` | `X.Y.Z-beta-SNAPSHOT` in the snapshot repository |
| `release.yml` | push on `main` | validates, publishes to Central, creates the tag and GitHub Release |

The development version lives in the root `pom.xml`. The `release` workflow checks whether the tag
and version already exist in Central before publishing. See [Maven Central](maven-central.md) and
[`RELEASING.md`](https://github.com/Krisner94/Harpia-Arch/blob/main/RELEASING.md).

## Common problems

| Symptom | Likely cause |
|---|---|
| invalid `packaging` | Use `quarkus`, not `jar` |
| Generated code not found | The `src/main/resources/openapi` directory does not exist |
| Plugin declared twice | The parent already provides the plugin |
| `401 Unauthorized` on deploy | Missing/expired Central token or Maven 3.10 |
| `@Activity ... must implement` | Incorrect execution contract |
| `@Repository ... must not depend on @Controller` | Inverted layer dependency |

See also [Errors and diagnostics](../core/errors.md).
