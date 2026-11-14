# Contribuição

## Requisitos

- Java 25;
- Maven 3.9+ (use `./mvnw`);
- Node.js 20+ para o site;
- GraalVM 25 para Native Image.

## Build

```bash
./mvnw clean install
```

O reactor contém:

- `harpia-core`;
- `harpia-core-deployment`;
- `harpia-quarkus`;
- `harpia-integration-tests`;
- `harpia-benchmarks`.

## Testes

```bash
./mvnw -pl harpia-core test
./mvnw -pl harpia-core-deployment test
./mvnw -pl harpia-integration-tests test
```

Integração HTTP:

```bash
./mvnw -pl harpia-integration-tests verify -Dit
```

Native:

```bash
./mvnw -pl harpia-integration-tests verify -Dnative
```

A suíte cobre composição, contexto opcional, compilação negativa, pureza do núcleo, validação
Jandex, CDI, integração JVM e native. Consulte [Testes](guides/testing.md).

## Benchmarks

```bash
./mvnw -pl harpia-benchmarks -am package
java -jar harpia-benchmarks/target/harpia-benchmarks.jar
```

## Documentação

O site usa Docusaurus 3 e fica em `website/`:

```bash
cd website
npm install
npm start
```

Acesse `http://localhost:3000/Harpia-Arch/`.

Build de produção:

```bash
cd website
npm run build
npm run serve
```

Estrutura:

- `website/docs` — conteúdo PT (locale padrão);
- `website/i18n/en/...` — traduções EN;
- `website/sidebars.js` — navegação;
- `website/docusaurus.config.js` — configuração e i18n;
- `.github/workflows/docs.yml` — deploy no GitHub Pages.

Ao alterar um documento em PT, atualize a tradução correspondente em EN quando a mudança afetar
comportamento ou API.

## Estilo de código

- early return;
- evitar `else`;
- `switch` para seleção entre valores discretos;
- métodos pequenos e coesos;
- Javadoc em contratos públicos;
- sem comentários que repitam o código;
- sem reflexão, casts ou `Object` no caminho crítico;
- novos contratos devem preservar a compatibilidade quando possível.

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

Fluxo resumido:

- `beta` publica `X.Y.Z-beta-SNAPSHOT`;
- `main` publica `X.Y.Z` no Maven Central, cria tag e GitHub Release;
- a versão é sempre decidida no `pom.xml` raiz.

## Checklist de pull request

- [ ] build e testes passam com Java 25;
- [ ] regras de arquitetura continuam válidas;
- [ ] documentação PT/EN atualizada quando necessário;
- [ ] benchmark executado para mudanças no motor;
- [ ] nenhuma credencial ou chave commitada.
