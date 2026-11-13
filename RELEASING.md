# Publicação

O projeto tem dois canais de distribuição, ambos automatizados por GitHub Actions:

| Canal | Ambiente | Versão | Destino | Definitivo? |
|---|---|---|---|---|
| **Beta** | testes | `X.Y.Z-beta-SNAPSHOT` | [Central snapshots](https://central.sonatype.com/repository/maven-snapshots/) | não |
| **Release** | produção | `X.Y.Z` | [Maven Central](https://repo1.maven.org/maven2/io/github/krisner94/) | sim |

O canal beta é disparado automaticamente a cada push na `main` (workflow `Beta (snapshot)`).
O canal de produção é disparado manualmente (workflow `Release`) e exige aprovação no
environment `production` antes de qualquer coisa sair do GitHub.

## Artefatos publicados

| Artefato | Tipo |
|---|---|
| `io.github.krisner94:harpia-arch` | POM pai |
| `io.github.krisner94:harpia-quarkus` | POM pai de aplicações |
| `io.github.krisner94:harpia-core` | Extensão Quarkus (runtime) |
| `io.github.krisner94:harpia-core-deployment` | Extensão Quarkus (deployment) |

`harpia-integration-tests` e `harpia-benchmarks` nunca são publicados
(`maven.deploy.skip=true`).

---

## Configuração inicial (uma vez)

### 1. Namespace no Central Portal

1. Crie a conta em <https://central.sonatype.com/>.
2. Verifique o namespace `io.github.krisner94`. Para `io.github.*`, a verificação é feita
   vinculando a conta do GitHub `Krisner94`.
3. Em **Account → Generate User Token**, gere o token (usuário + senha).

### 2. Chave GPG

A chave usada hoje é:

```
3885F07BB8ADA7B3  Rhama Krisner <rhamakrisner@gmail.com>
```

A chave pública precisa estar publicada:

```bash
gpg --keyserver keys.openpgp.org --send-keys 3885F07BB8ADA7B3
```

O Central exige que os artefatos sejam assinados; o workflow importa a chave privada dos
secrets e assina com `MAVEN_GPG_PASSPHRASE`.

### 3. Secrets do repositório

```bash
gh secret set CENTRAL_USERNAME   # usuário do token do Central Portal
gh secret set CENTRAL_PASSWORD   # senha do token
gh secret set GPG_PASSPHRASE     # passphrase da chave (vazio se não houver)

# chave privada exportada em ASCII armor
gpg --armor --export-secret-keys 3885F07BB8ADA7B3 | gh secret set GPG_PRIVATE_KEY

# opcional: força a chave correta se houver mais de uma no keyring
gh secret set GPG_KEY_ID --body 3885F07BB8ADA7B3
```

Ou rode o script que faz tudo de uma vez:

```bash
./scripts/setup-release-env.sh
```

### 4. Environment `production` com aprovação obrigatória

O job de publicação usa o environment `production`. Para exigir aprovação manual:

```bash
./scripts/setup-release-env.sh
```

ou manualmente em **Settings → Environments → New environment → `production`** e marque
**Required reviewers** com o seu usuário.

Sem revisores configurados, o job roda direto após o disparo.

---

## Como lançar uma versão (produção)

1. GitHub → **Actions** → **Release** → **Run workflow**:
   - `version`: a versão a publicar, ex.: `1.0.0` (sem `-SNAPSHOT`);
   - `next_version`: opcional; por padrão o workflow sobe o patch para `X.Y.(Z+1)-SNAPSHOT`;
   - `dry_run`: marque para apenas buildar e testar.
2. O job **Build and test** roda antes de qualquer aprovação.
3. O job **Publish** fica em *Waiting for approval*. Aprove em **Review deployments**.
4. O workflow então:
   - assina e publica os artefatos no Maven Central (`autoPublish=true`);
   - espera o Central terminar a publicação;
   - cria a tag `vX.Y.Z` e o GitHub Release com as notas geradas;
   - commita a próxima versão de desenvolvimento (`chore: start X.Y.Z-SNAPSHOT ...`).

Em alguns minutos os artefatos ficam disponíveis em:

```
https://repo1.maven.org/maven2/io/github/krisner94/harpia-core/X.Y.Z/
```

### Antes de disparar

- a `main` precisa estar verde no CI;
- a versão não pode já existir (o workflow falha se a tag `vX.Y.Z` existir);
- para versões `X.Y.Z`, o Maven Central é imutável: erros de metadata não podem ser
  corrigidos na mesma versão, só em uma nova.

### Releases locais (sem GitHub Actions)

```bash
# a chave tem passphrase: exporte-a para assinar
export MAVEN_GPG_PASSPHRASE='...'

mvn -Prelease verify -Dgpg.skip=true                        # valida o pacote
mvn -Prelease deploy -DskipITs                              # publica (precisa de ~/.m2/settings.xml)
```

`~/.m2/settings.xml` com o server `central`:

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

Para publicar sem auto-publish (revisar o bundle no portal antes):

```bash
mvn -Prelease deploy -DskipITs -Dcentral.autoPublish=false
```

---

## Como consumir o beta

Quem quiser testar o snapshot mais recente da `main` adiciona o repositório de snapshots
e usa a versão `X.Y.Z-beta-SNAPSHOT`:

```xml
<repositories>
    <repository>
        <id>central-snapshots</id>
        <url>https://central.sonatype.com/repository/maven-snapshots/</url>
        <snapshots>
            <enabled>true</enabled>
        </snapshots>
    </repository>
</repositories>

<parent>
    <groupId>io.github.krisner94</groupId>
    <artifactId>harpia-quarkus</artifactId>
    <version>1.0.0-beta-SNAPSHOT</version>
</parent>
```

Para atualizar o snapshot local: `mvn -U clean verify`.

O snapshot é sobrescrito a cada push na `main`; ele não é uma versão estável e não deve ser
usado em produção.

---

## Fluxo de versões

A versão de desenvolvimento vive apenas no `<version>` do `pom.xml` raiz — os módulos herdam
via `<parent>` e a dependência interna usa `${project.version}`.

```
1.0.1-SNAPSHOT  --push na main-->  1.0.1-beta-SNAPSHOT  (canal beta)
1.0.1-SNAPSHOT  --Release 1.0.1--> 1.0.1               (Maven Central)
                                     |
                                     +--> bump automático para 1.0.2-SNAPSHOT
```

Para uma versão minor/major, informe `next_version` no disparo (ex.: `1.1.0-SNAPSHOT`).

## Problemas comuns

| Sintoma | Causa provável |
|---|---|
| `401 Unauthorized` no deploy | secrets `CENTRAL_USERNAME`/`CENTRAL_PASSWORD` ausentes ou token expirado |
| `Invalid signature` / `gpg: signing failed` | `GPG_PRIVATE_KEY`/`GPG_PASSPHRASE` incorretos ou chave pública não publicada |
| `Component already published` | a versão já existe no Central; publique outra |
| `Deployment failed: missing sources/javadoc` | o perfil `-Prelease` não foi usado |
| Job de publish esperando para sempre | environment `production` sem revisor aprovado |
