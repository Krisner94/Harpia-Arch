# Publicação

O projeto tem duas branches de publicação, ambas automatizadas por GitHub Actions:

| Branch | Canal | Versão | Destino | Definitivo? |
|---|---|---|---|---|
| `beta` | testes | `X.Y.Z-beta-SNAPSHOT` | [Central snapshots](https://central.sonatype.com/repository/maven-snapshots/) | não |
| `main` | produção | `X.Y.Z` | [Maven Central](https://repo1.maven.org/maven2/io/github/krisner94/) | sim |

Todo push na `beta` publica um snapshot. Todo push na `main` publica uma versão
definitiva — **se a versão do `pom.xml` ainda não existir no Central**. Se já existir
(ou a tag `vX.Y.Z` já existir), o workflow encerra como *nada a fazer* e fica verde.

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
4. Habilite o canal beta: em **Namespaces**, abra o menu do namespace `io.github.krisner94` e
   escolha **Enable SNAPSHOTs**. Sem isso o workflow Beta falha com `403 Forbidden`.

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

## Como lançar uma versão

```
1. trabalhe e faça push na beta          -> 1.1.0-beta-SNAPSHOT (snapshots)
2. quando estiver bom, suba a versão no pom.xml da beta para 1.1.0
3. abra PR de beta -> main e faça merge  -> 1.1.0 (Maven Central + tag v1.1.0)
```

1. Na branch `beta`, cada push publica `X.Y.Z-beta-SNAPSHOT` automaticamente.
2. Quando o beta estiver validado, altere a versão no `pom.xml` (na `beta`) para a versão
   final, sem sufixo: `1.1.0`.
3. Faça merge de `beta` na `main`. O push na `main` dispara o workflow **Release**.
4. O job **Build and test** roda; depois o **Publish** fica em *Waiting for approval*.
   Aprove em **Review deployments**.
5. O workflow então:
   - assina e publica os artefatos no Maven Central (`autoPublish=true`);
   - espera o Central concluir a publicação;
   - cria a tag `vX.Y.Z` e o GitHub Release com as notas geradas.

Em alguns minutos os artefatos ficam disponíveis em:

```
https://repo1.maven.org/maven2/io/github/krisner94/harpia-core/X.Y.Z/
```

### Disparo manual

**Actions → Release → Run workflow** permite publicar sem push (ex.: republicar após uma
falha). Os campos `version` (vazio = versão do `pom.xml`) e `dry_run` continuam disponíveis.

### Antes de publicar

- a `main` precisa estar verde no CI;
- a versão não pode existir no Central nem ter tag `vX.Y.Z` (se existir, o workflow apenas
  registra *nada a fazer* e encerra verde);
- para versões `X.Y.Z` o Maven Central é imutável: erros de metadata não podem ser
  corrigidos na mesma versão, só em uma nova;
- depois de publicar, faça merge da `main` de volta na `beta` para as branches não divergirem.

### Releases locais (sem GitHub Actions)

Use sempre o wrapper (`./mvnw`), que fixa o Maven 3.9.16. O Maven 3.10 não envia as
credenciais do `settings.xml` no deploy para o repositório de snapshots e o resultado é
`401 Unauthorized`.

```bash
# a chave tem passphrase: exporte-a para assinar
export MAVEN_GPG_PASSPHRASE='...'

./mvnw -Prelease verify -Dgpg.skip=true                        # valida o pacote
./mvnw -Prelease deploy -DskipITs                              # publica (precisa de ~/.m2/settings.xml)
```

`~/.m2/settings.xml` com o server `central`:

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

> Use os valores literais. `${env.VAR}` **não** é interpolado nesse ponto e o deploy falha
> com `401 Unauthorized`.

Para publicar sem auto-publish (revisar o bundle no portal antes):

```bash
./mvnw -Prelease deploy -DskipITs -Dcentral.autoPublish=false
```

---

## Como consumir o beta

Quem quiser testar o snapshot mais recente da branch `beta` adiciona o repositório de snapshots
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

O snapshot é sobrescrito a cada push na `beta`; ele não é uma versão estável e não deve ser
usado em produção.

---

## Fluxo de versões

A versão de desenvolvimento vive no `pom.xml` raiz: os módulos herdam via `<parent>` e a
dependência interna em `<dependencyManagement>` usa a versão literal (não `${project.version}`,
porque um parent publicado é interpolado no contexto do app consumidor). `mvn versions:set`
atualiza os três pontos de uma vez.

```
beta:  1.1.0  --push-->  1.1.0-beta-SNAPSHOT   (sobrescrito a cada push)
          |
          |  merge (com a versão final já no pom)
          v
main:  1.1.0  --push-->  1.1.0                  (Maven Central, imutável)
          |
          +--> tag v1.1.0 + GitHub Release
```

Depois de publicar, a `main` fica na versão já lançada. O próximo ciclo começa na `beta`
subindo a versão (ex.: `1.2.0` ou `1.1.1`). Não há bump automático: a versão é sempre uma
decisão explícita no `pom.xml`.

## Problemas comuns

| Sintoma | Causa provável |
|---|---|
| `401 Unauthorized` no deploy | secrets `CENTRAL_USERNAME`/`CENTRAL_PASSWORD` ausentes ou token expirado |
| `Invalid signature` / `gpg: signing failed` | `GPG_PRIVATE_KEY`/`GPG_PASSPHRASE` incorretos ou chave pública não publicada |
| `Component already published` | a versão já existe no Central; publique outra |
| `Deployment failed: missing sources/javadoc` | o perfil `-Prelease` não foi usado |
| Job de publish esperando para sempre | environment `production` sem revisor aprovado |
