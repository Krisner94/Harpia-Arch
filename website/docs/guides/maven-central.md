# Maven Central e releases

## Coordenadas

```text
io.github.krisner94:harpia-arch:1.0.0
io.github.krisner94:harpia-quarkus:1.0.0
io.github.krisner94:harpia-core:1.0.0
io.github.krisner94:harpia-core-deployment:1.0.0
```

`harpia-integration-tests` e `harpia-benchmarks` não são publicados.

## Canais

| Branch | Canal | Versão | Destino | Definitivo? |
|---|---|---|---|---|
| `beta` | testes | `X.Y.Z-beta-SNAPSHOT` | Central snapshots | Não |
| `main` | produção | `X.Y.Z` | Maven Central | Sim |

O workflow de release verifica se a tag `vX.Y.Z` e a versão já existem no Central. Se existirem,
ele encerra como “nada a fazer”, sem falhar o build.

## Pré-requisitos

1. Conta no [Central Portal](https://central.sonatype.com/).
2. Namespace `io.github.krisner94` verificado com a conta GitHub `Krisner94`.
3. Chave GPG publicada em `keys.openpgp.org`.
4. Token do Central Portal configurado em `~/.m2/settings.xml`.
5. Canal de snapshots habilitado no namespace para a branch `beta`.

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

Use valores literais. `${env.VAR}` não é interpolado nesse ponto do deploy.

## Chave GPG

```bash
gpg --keyserver keys.openpgp.org --send-keys 3885F07BB8ADA7B3
```

Confirme:

```bash
curl -s -o /dev/null -w '%{http_code}\n' \
  https://keys.openpgp.org/vks/v1/by-fingerprint/3FBC24668ABF2EBD4CFC10AB3885F07BB8ADA7B3
```

## Publicação local

Valide o pacote:

```bash
./mvnw -Prelease verify -Dgpg.skip=true
```

Publique:

```bash
export MAVEN_GPG_PASSPHRASE='...'
./mvnw -Prelease deploy -DskipITs
```

O perfil `release` configura:

- `maven-source-plugin`;
- `maven-javadoc-plugin`;
- `maven-gpg-plugin`;
- `central-publishing-maven-plugin` com `autoPublish=true` e `waitUntil=published`.

Para revisar o bundle antes de publicar:

```bash
./mvnw -Prelease deploy -DskipITs -Dcentral.autoPublish=false
```

Nesse caso, publique manualmente em **Deployments → Publish**.

## Automação no GitHub Actions

| Workflow | Gatilho | Ação |
|---|---|---|
| `ci.yml` | push/PR em `main` e `beta` | build + testes |
| `beta.yml` | push em `beta` | publica `X.Y.Z-beta-SNAPSHOT` |
| `release.yml` | push em `main` ou manual | valida, publica, cria tag e release |
| `docs.yml` | push em `website/**` | publica o site |

### Secrets

```bash
gh secret set CENTRAL_USERNAME
gh secret set CENTRAL_PASSWORD
gh secret set GPG_PASSPHRASE
gpg --armor --export-secret-keys 3885F07BB8ADA7B3 | gh secret set GPG_PRIVATE_KEY
gh secret set GPG_KEY_ID --body 3885F07BB8ADA7B3
```

Ou execute:

```bash
./scripts/setup-release-env.sh
```

O script configura os secrets e o environment `production` com revisores obrigatórios.

## Fluxo de uma versão

```text
1. trabalhe e faça push na beta          -> 1.1.0-beta-SNAPSHOT
2. suba a versão no pom.xml para 1.1.0
3. abra PR de beta -> main e faça merge  -> 1.1.0 no Maven Central
4. o workflow cria a tag v1.1.0 e o GitHub Release
```

A versão vive no `pom.xml` raiz. `mvn versions:set` atualiza os módulos.

## Verificar

```bash
curl -I https://repo1.maven.org/maven2/io/github/krisner94/harpia-core/1.0.0/harpia-core-1.0.0.pom
```

A propagação pode levar alguns minutos.

## Problemas comuns

| Sintoma | Causa provável |
|---|---|
| `401 Unauthorized` | Token ausente/expirado ou Maven 3.10 |
| `Invalid signature` | GPG privada/passphrase incorretas |
| `Component already published` | Versão já existe no Central |
| `missing sources/javadoc` | Perfil `release` não usado |
| Publish esperando | Environment `production` sem aprovação |
| Beta `403 Forbidden` | Canal de snapshots não habilitado no namespace |

Consulte também [`RELEASING.md`](https://github.com/Krisner94/Harpia-Arch/blob/main/RELEASING.md).
