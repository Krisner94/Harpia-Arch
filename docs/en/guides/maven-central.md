# Maven Central

## Coordinates

```text
io.github.krisner94:harpia-arch:1.0.0
io.github.krisner94:harpia-quarkus:1.0.0
io.github.krisner94:harpia-core:1.0.0
io.github.krisner94:harpia-core-deployment:1.0.0
```

`harpia-integration-tests` and `harpia-benchmarks` are not published.

## Requirements

1. A [Central Portal](https://central.sonatype.com/) account.
2. The `io.github.krisner94` namespace verified with the `Krisner94` GitHub account.
3. A GPG key published on `keys.openpgp.org`.
4. A Central Portal token configured in `~/.m2/settings.xml`.

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

The `<id>` must be `central`, because it is the `publishingServerId` configured in the POM.

## GPG key

```bash
gpg --keyserver keys.openpgp.org --send-keys 3885F07BB8ADA7B3
```

Verify:

```bash
curl -s -o /dev/null -w '%{http_code}\n' \
  https://keys.openpgp.org/vks/v1/by-fingerprint/3FBC24668ABF2EBD4CFC10AB3885F07BB8ADA7B3
```

## Validate the package

```bash
mvn -Prelease -DskipTests -Dgpg.skip=true package
```

This generates sources and Javadoc without signing.

## Sign and publish

```bash
export MAVEN_GPG_PASSPHRASE='...'

mvn -Prelease -DskipTests deploy \
  -pl .,harpia-core,harpia-core-deployment,harpia-quarkus
```

`autoPublish` is `false`: the bundle is uploaded and validated, but publishing is manual.

In the Central Portal:

```text
Deployments -> select the bundle -> Publish
```

## Verify

```bash
curl -I https://repo1.maven.org/maven2/io/github/krisner94/harpia-core/1.0.0/harpia-core-1.0.0.pom
```

Propagation may take a few minutes.

## New version

Update:

1. `<version>` in the root `pom.xml`;
2. `<harpia.version>`;
3. the parent version in every module.

Then repeat the deploy.
