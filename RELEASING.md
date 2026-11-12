# Publicação no Maven Central

Os artefatos publicados são:

| Artefato | Tipo |
|---|---|
| `io.github.krisner94:harpia-arch` | POM pai |
| `io.github.krisner94:harpia-quarkus` | POM pai de aplicações |
| `io.github.krisner94:harpia-core` | Extensão Quarkus (runtime) |
| `io.github.krisner94:harpia-core-deployment` | Extensão Quarkus (deployment) |

`harpia-integration-tests` e `harpia-benchmarks` não são publicados
(`maven.deploy.skip=true`).

## Pré-requisitos

1. Conta no [Central Portal](https://central.sonatype.com/).
2. Namespace `io.github.krisner94` verificado. Para `io.github.*`, a verificação é feita
   vinculando a conta do GitHub `Krisner94`.
3. Chave GPG com chave pública publicada em `keys.openpgp.org`.
4. Token do Central Portal configurado no `~/.m2/settings.xml`.

Exemplo de `~/.m2/settings.xml`:

```xml
<settings>
    <servers>
        <server>
            <id>central</id>
            <username>${env.CENTRAL_USERNAME}</username>
            <password>${env.CENTRAL_PASSWORD}</password>
        </server>
    </servers>
</settings>
```

O usuário e a senha são o **token** gerado em Central Portal → Account → Generate User Token.

## Chave GPG

A chave local é:

```
3885F07BB8ADA7B3  Rhama Krisner <rhamakrisner@gmail.com>
```

Publique a chave pública:

```bash
gpg --keyserver keys.openpgp.org --send-keys 3885F07BB8ADA7B3
```

Se a chave tiver passphrase, deixe o `gpg-agent` cachear ou use:

```bash
mvn -Prelease -Dgpg.passphrase='...' ...
```

## Validar o pacote antes de publicar

```bash
mvn -Prelease -DskipTests -Dgpg.skip=true package
```

Isso gera os JARs de sources e Javadoc sem assinar.

## Publicar

```bash
mvn -Prelease -DskipTests deploy \
  -pl .,harpia-core,harpia-core-deployment,harpia-quarkus
```

O `autoPublish` está como `false`: o bundle é enviado para o Central Portal e precisa ser
publicado manualmente em **Deployments → Publish**.

Depois da publicação, os artefatos ficam disponíveis em:

```
https://repo1.maven.org/maven2/io/github/krisner94/
```

## Próxima versão

O projeto está em `1.0.0`. Para uma nova release:

1. atualize `<version>` no `pom.xml` raiz;
2. atualize `<harpia.version>`;
3. atualize a versão do parent nos módulos;
4. rode novamente o comando de publicação.
