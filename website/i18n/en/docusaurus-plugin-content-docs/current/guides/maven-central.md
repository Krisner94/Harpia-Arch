# Maven Central and releases

## Coordinates

```text
io.github.krisner94:harpia-arch:1.0.0
io.github.krisner94:harpia-quarkus:1.0.0
io.github.krisner94:harpia-core:1.0.0
io.github.krisner94:harpia-core-deployment:1.0.0
```

`harpia-integration-tests` and `harpia-benchmarks` are not published.

## Channels

| Branch | Channel | Version | Destination | Final? |
|---|---|---|---|---|
| `beta` | testing | `X.Y.Z-beta-SNAPSHOT` | Central snapshots | No |
| `main` | production | `X.Y.Z` | Maven Central | Yes |

The release workflow checks whether the `vX.Y.Z` tag and the version already exist on Central. If
they do, it ends as "nothing to do", without failing the build.

## Prerequisites

1. A [Central Portal](https://central.sonatype.com/) account.
2. The `io.github.krisner94` namespace verified with the `Krisner94` GitHub account.
3. A GPG key published on `keys.openpgp.org`.
4. A Central Portal token configured in `~/.m2/settings.xml`.
5. The snapshot channel enabled on the namespace for the `beta` branch.

```xml
<settings>
    <servers>
        <server>
            <id>central</id>
            <username>SEU_USUARIO_DO_TOKEN</username>
            <password>SUA_SENHA_DO_TOKEN</password>
        </server>
    </servers>
</settings>
```

Use literal values. `${env.VAR}` is not interpolated at that point of the deploy.

## GPG key

```bash
gpg --keyserver keys.openpgp.org --send-keys 3885F07BB8ADA7B3
```

Confirm:

```bash
curl -s -o /dev/null -w '%{http_code}\n' \
  https://keys.openpgp.org/vks/v1/by-fingerprint/3FBC24668ABF2EBD4CFC10AB3885F07BB8ADA7B3
```

## Local publication

Validate the package:

```bash
./mvnw -Prelease verify -Dgpg.skip=true
```

Publish:

```bash
export MAVEN_GPG_PASSPHRASE='...'
./mvnw -Prelease deploy -DskipITs
```

The `release` profile configures:

- `maven-source-plugin`;
- `maven-javadoc-plugin`;
- `maven-gpg-plugin`;
- `central-publishing-maven-plugin` with `autoPublish=true` and `waitUntil=published`.

To review the bundle before publishing:

```bash
./mvnw -Prelease deploy -DskipITs -Dcentral.autoPublish=false
```

In this case, publish manually under **Deployments → Publish**.

## GitHub Actions automation

| Workflow | Trigger | Action |
|---|---|---|
| `ci.yml` | push/PR on `main` and `beta` | build + tests |
| `beta.yml` | push on `beta` | publishes `X.Y.Z-beta-SNAPSHOT` |
| `release.yml` | push on `main` or manual | validates, publishes, creates the tag and the release |
| `docs.yml` | push on `website/**` | publishes the site |

### Secrets

```bash
gh secret set CENTRAL_USERNAME
gh secret set CENTRAL_PASSWORD
gh secret set GPG_PASSPHRASE
gpg --armor --export-secret-keys 3885F07BB8ADA7B3 | gh secret set GPG_PRIVATE_KEY
gh secret set GPG_KEY_ID --body 3885F07BB8ADA7B3
```

Or run:

```bash
./scripts/setup-release-env.sh
```

The script sets up the secrets and the `production` environment with required reviewers.

## Flow of a version

```text
1. work and push to beta                 -> 1.1.0-beta-SNAPSHOT
2. bump the version in pom.xml to 1.1.0
3. open a PR from beta -> main and merge -> 1.1.0 on Maven Central
4. the workflow creates the tag v1.1.0 and the GitHub Release
```

The version lives in the root `pom.xml`. `mvn versions:set` updates the modules.

## Verify

```bash
curl -I https://repo1.maven.org/maven2/io/github/krisner94/harpia-core/1.0.0/harpia-core-1.0.0.pom
```

Propagation may take a few minutes.

## Common issues

| Symptom | Likely cause |
|---|---|
| `401 Unauthorized` | Missing/expired token or Maven 3.10 |
| `Invalid signature` | Wrong private GPG key/passphrase |
| `Component already published` | Version already exists on Central |
| `missing sources/javadoc` | `release` profile not used |
| Publish pending | `production` environment without approval |
| Beta `403 Forbidden` | Snapshot channel not enabled on the namespace |

See also [`RELEASING.md`](https://github.com/Krisner94/Harpia-Arch/blob/main/RELEASING.md).
