# Configuração e build

O reator é um agregador Maven com dois parents e dois artefatos da extensão — runtime e
deployment. Entender essa divisão evita declarar dependências e plugins duplicados na aplicação.

## Requisitos

| Requisito | Versão |
|---|---|
| Java | 25 |
| Maven | 3.9+ (o wrapper fixa 3.9.16) |
| Quarkus | 3.40.1.1 |
| GraalVM | 25, apenas para Native Image |
| Node.js | 20+, apenas para o site de documentação |

Use o wrapper do repositório:

```bash
./mvnw -v
```

## Parent recomendado

A aplicação deve usar `harpia-quarkus`:

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

`packaging=quarkus` é obrigatório. O parent já fornece:

- Quarkus BOM e a versão do Quarkus;
- `quarkus-arc`, `quarkus-rest` e `quarkus-rest-jackson`;
- `microprofile-openapi-api` e `jakarta.validation-api`;
- `harpia-core`;
- JUnit e Rest Assured em escopo de teste;
- `quarkus-maven-plugin` com `extensions=true`;
- compiler, Surefire e Failsafe;
- perfil `native`;
- perfil `harpia-openapi-codegen`.

Não declare o `quarkus-maven-plugin` nem os plugins de compilação/teste novamente. A aplicação
declara apenas dependências e propriedades específicas.

## Usar `harpia-core` sem o parent

Se a aplicação já tem outro parent, importe o BOM do Quarkus e declare:

```xml
<dependency>
    <groupId>io.github.krisner94</groupId>
    <artifactId>harpia-core</artifactId>
    <version>1.0.0</version>
</dependency>
```

Declare também as extensões Quarkus usadas pela aplicação (`quarkus-rest`,
`quarkus-rest-jackson`, validação, persistência etc.) e configure o codegen OpenAPI no seu POM.

`harpia-core-deployment` não é declarado: o Quarkus resolve o deployment pelo descritor da
extensão.

## Propriedades do build

| Propriedade | Valor padrão | Papel |
|---|---|---|
| `maven.compiler.release` | `25` | Versão de compilação |
| `quarkus.version` | `3.40.1.1` | BOM e plugins Quarkus |
| `openapi-generator.version` | `7.4.0` | Gerador do contrato |
| `build-helper-maven-plugin.version` | `3.6.0` | Source root do código gerado |
| `surefire-plugin.version` | `3.5.6` | Testes e Failsafe |
| `harpia.openapi.spec` | `src/main/resources/openapi/openapi.yaml` | Contrato de entrada |
| `harpia.openapi.api-package` | `${project.groupId}.api` | Pacote das interfaces |
| `harpia.openapi.model-package` | `${harpia.openapi.api-package}.beans` | Pacote dos DTOs |
| `harpia.openapi.api-suffix` | `Api` | Sufixo das interfaces |
| `central.autoPublish` | `true` | Publicação no Central no perfil `release` |
| `central.waitUntil` | `published` | Espera do Central |
| `central.waitMaxTime` | `3600` | Tempo máximo de espera, em segundos |

## OpenAPI contract-first

O perfil `harpia-openapi-codegen` é ativado automaticamente quando existe
`src/main/resources/openapi`. Ele roda na fase `generate-sources` e usa o gerador `jaxrs-spec`
com:

- `interfaceOnly=true`;
- `useJakartaEe=true`;
- `useMicroProfileOpenAPIAnnotations=true`;
- `useBeanValidation=true`;
- `useSwaggerAnnotations=false`;
- `generateSupportingFiles=false`.

O diretório gerado é:

```text
target/generated-sources/openapi/src/main/java
```

O `build-helper-maven-plugin` o adiciona como source root antes do `javac`.

Consulte [Contract First](contract-first.md) para o exemplo completo de contrato e Controller.

## Configuração Quarkus

O módulo de integração usa H2 em memória:

```properties
quarkus.datasource.db-kind=h2
quarkus.datasource.username=sa
quarkus.datasource.password=
quarkus.datasource.jdbc.url=jdbc:h2:mem:customers;DB_CLOSE_DELAY=-1
quarkus.hibernate-orm.schema-management.strategy=drop-and-create
quarkus.hibernate-orm.log.sql=false
```

Em produção, troque o driver e use migrações. Consulte [Persistência](persistence.md).

## Perfis Maven

| Perfil | Ativação | Efeito |
|---|---|---|
| `harpia-openapi-codegen` | existe `src/main/resources/openapi` | Gera interfaces e DTOs |
| `native` | `-Dnative` | Habilita Native Image e Failsafe |
| `it` | `-Dit` | Executa `@QuarkusIntegrationTest` |
| `release` | `-Prelease` | Sources, Javadoc, GPG e Central |

## Comandos

```bash
./mvnw test                              # testes unitários
./mvnw -Dit verify                       # testes unitários + integração JVM
./mvnw -pl harpia-integration-tests verify -Dnative
./mvnw -Prelease verify -Dgpg.skip=true  # valida o pacote de release
./mvnw -Prelease deploy -DskipITs        # publica no Central
```

## CI e publicação

| Workflow | Gatilho | Resultado |
|---|---|---|
| `ci.yml` | push/PR em `main` e `beta` | build + testes em Java 25 |
| `docs.yml` | push em `website/**` | build Docusaurus + GitHub Pages |
| `beta.yml` | push em `beta` | `X.Y.Z-beta-SNAPSHOT` no repositório de snapshots |
| `release.yml` | push em `main` | valida, publica no Central, cria tag e GitHub Release |

A versão de desenvolvimento fica no `pom.xml` raiz. O workflow `release` verifica se a tag e a
versão já existem no Central antes de publicar. Consulte [Maven Central](maven-central.md) e
[`RELEASING.md`](https://github.com/Krisner94/Harpia-Arch/blob/main/RELEASING.md).

## Problemas comuns

| Sintoma | Causa provável |
|---|---|
| `packaging` inválido | Use `quarkus`, não `jar` |
| Código gerado não encontrado | O diretório `src/main/resources/openapi` não existe |
| Plugin declarado duas vezes | O parent já fornece o plugin |
| `401 Unauthorized` no deploy | Token do Central ausente/expirado ou Maven 3.10 |
| `@Activity ... must implement` | Contrato de execução incorreto |
| `@Repository ... must not depend on @Controller` | Dependência de camada invertida |

Consulte também [Erros e diagnóstico](../core/errors.md).
