# Harpia Arch

[![CI](https://github.com/Krisner94/Harpia-Arch/actions/workflows/ci.yml/badge.svg)](https://github.com/Krisner94/Harpia-Arch/actions/workflows/ci.yml)
[![Release](https://github.com/Krisner94/Harpia-Arch/actions/workflows/release.yml/badge.svg)](https://github.com/Krisner94/Harpia-Arch/actions/workflows/release.yml)
[![Maven Central](https://img.shields.io/maven-central/v/io.github.krisner94/harpia-core)](https://central.sonatype.com/artifact/io.github.krisner94/harpia-core)
[![Docs](https://img.shields.io/badge/docs-krisner94.github.io-blue)](https://krisner94.github.io/Harpia-Arch/)

Engine de workflows tipado e validação arquitetural em build time para aplicações Quarkus,
com OpenAPI contract-first. Sem reflexão, sem runtime mágico — o contrato é verificado durante
o build.

**Requisitos:** Java 25 e Quarkus 3.40+.

## Instalação

O jeito recomendado é usar `harpia-quarkus` como parent. Ele já traz o Quarkus BOM, as
dependências comuns, os plugins de build e o perfil `native`:

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

Se você já usa outro parent, adicione apenas o núcleo:

```xml
<dependency>
    <groupId>io.github.krisner94</groupId>
    <artifactId>harpia-core</artifactId>
    <version>1.0.0</version>
</dependency>
```

Também é possível herdar somente o POM de build, sem as dependências de aplicação:

```xml
<parent>
    <groupId>io.github.krisner94</groupId>
    <artifactId>harpia-arch</artifactId>
    <version>1.0.0</version>
</parent>
```

## Módulos

| Módulo | Papel | Publicado |
|---|---|---|
| `harpia-quarkus` | Parent das aplicações: Quarkus BOM, REST, OpenAPI, testes e plugin nativo. | sim |
| `harpia-arch` | Parent de build: versões, `openapi-generator-maven-plugin` e perfil de codegen. | sim |
| `harpia-core` | Núcleo: `FlowItem`, `Workflow`, `FlowBuilder`, annotations CDI e validações. | sim |
| `harpia-core-deployment` | Validação da arquitetura em build time (Jandex + `@BuildStep`). | sim |
| `harpia-integration-tests` | Aplicação Quarkus usada pelos testes de integração. | não |
| `harpia-benchmarks` | Benchmarks JMH do motor de workflows. | não |

## Links

- **Documentação:** https://krisner94.github.io/Harpia-Arch/
- **Maven Repository:** https://mvnrepository.com/artifact/io.github.krisner94/harpia-core
- **Maven Central:** https://central.sonatype.com/artifact/io.github.krisner94/harpia-core
- **Releases:** https://github.com/Krisner94/Harpia-Arch/releases
- **Canais de publicação (estável e beta):** [RELEASING.md](RELEASING.md)

## Licença

[Apache License 2.0](LICENSE)
