# Maven Central

## Coordenadas

```text
io.github.krisner94:harpia-arch:1.0.0
io.github.krisner94:harpia-quarkus:1.0.0
io.github.krisner94:harpia-core:1.0.0
io.github.krisner94:harpia-core-deployment:1.0.0
```

`harpia-integration-tests` e `harpia-benchmarks` não são publicados.

## Pré-requisitos

1. Conta no [Central Portal](https://central.sonatype.com/).
2. Namespace `io.github.krisner94` verificado com a conta GitHub `Krisner94`.
3. Chave GPG publicada no `keys.openpgp.org`.
4. Token do Central Portal configurado em `~/.m2/settings.xml`.

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

O `<id>` precisa ser `central`, pois é o `publishingServerId` configurado no POM.

## Chave GPG

```bash
gpg --keyserver keys.openpgp.org --send-keys 3885F07BB8ADA7B3
```

Confirme:

```bash
curl -s -o /dev/null -w '%{http_code}\n' \
  https://keys.openpgp.org/vks/v1/by-fingerprint/3FBC24668ABF2EBD4CFC10AB3885F07BB8ADA7B3
```

## Validar o pacote

```bash
mvn -Prelease -DskipTests -Dgpg.skip=true package
```

Gera sources e Javadoc sem assinar.

## Assinar e publicar

```bash
export MAVEN_GPG_PASSPHRASE='...'

mvn -Prelease -DskipTests deploy \
  -pl .,harpia-core,harpia-core-deployment,harpia-quarkus
```

O `autoPublish` está `false`: o bundle é enviado e validado, mas a publicação é manual.

No Central Portal:

```text
Deployments -> selecionar o bundle -> Publish
```

## Verificar

```bash
curl -I https://repo1.maven.org/maven2/io/github/krisner94/harpia-core/1.0.0/harpia-core-1.0.0.pom
```

A propagação pode levar alguns minutos.

## Nova versão

Atualize:

1. `<version>` no `pom.xml` raiz;
2. `<harpia.version>`;
3. a versão do parent em cada módulo.

Depois repita o deploy.
