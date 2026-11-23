# Harpia Core

Núcleo do Harpia Arch: engine de workflows, erros tipados e annotations arquiteturais.

- engine em `io.github.krisner94.harpia.core`;
- erros tipados em `io.github.krisner94.harpia.problem`;
- annotations em `io.github.krisner94.harpia.annotation`.

Tudo é fornecido automaticamente pelo parent `harpia-quarkus`.

## Engine

| Tipo | Papel |
|---|---|
| `FlowItem<I, O>` | Contrato de execução. Activities e Workflows implementam. |
| `Workflow<I, O>` | Composição imutável que também é um `FlowItem`. |
| `FlowBuilder` | Builder tipado; a única operação de composição é `.step()`. |
| `FlowConfigurationException` | Falha de configuração do workflow. |

```java
@Activity
public class ValidateCustomerActivity
        implements FlowItem<CustomerInput, ValidatedCustomer> {

    @Override
    public ValidatedCustomer execute(CustomerInput input) {
        if (input.name() == null || input.name().isBlank()) {
            throw new IllegalArgumentException("name is required");
        }
        return new ValidatedCustomer(input.name().trim());
    }
}
```

```java
@WorkflowDefinition
public class CreateCustomerWorkflow
        extends Workflow<CustomerInput, Customer> {

    private final ValidationWorkflow validation;
    private final PersistenceWorkflow persistence;

    public CreateCustomerWorkflow(ValidationWorkflow validation, PersistenceWorkflow persistence) {
        this.validation = validation;
        this.persistence = persistence;
    }

    @Override
    protected void configure() {
        flowBuilder = FlowBuilder.from(validation)
                .step(persistence)
                .build();
    }
}
```

### Composição hierárquica

`Workflow` implementa `FlowItem`, então um workflow é uma etapa de outro:

```java
flowBuilder = FlowBuilder.from(validationWorkflow)
        .step(enrichmentWorkflow)
        .step(persistenceWorkflow)
        .build();
```

Cada subworkflow tem seus próprios tipos de entrada e saída.

### Entrada e saída diferentes

A adaptação é explícita e tipada; não existe conversão implícita:

```java
FlowItem.adapt(item, inputAdapter, outputAdapter);
```

### Tipagem

O tipo de saída do builder muda a cada etapa. Uma cadeia incompatível não compila:

```java
FlowBuilder.builder(String.class)
        .step(lengthActivity)   // String -> Integer
        .step(lengthActivity);  // erro: espera FlowItem<Integer, ...>
```

A garantia é de compilação; por type erasure não há verificação em runtime. O encadeamento não usa
`Object`, casts ou `@SuppressWarnings`.

### Ciclo de vida

- `configure()` roda uma única vez no `@PostConstruct`, após a injeção CDI;
- `flowBuilder` é publicado como `volatile` e nunca é reconstruído;
- Workflows instanciados manualmente inicializam na primeira execução;
- não há estado de requisição em campos de beans;
- a execução não usa reflexão nem descoberta dinâmica de componentes; o ArC pode usar proxies
  CDI, mas o motor não depende deles.

## Erros tipados (RFC 9457)

| Tipo | Papel |
|---|---|
| `Result<O, E>` | Resultado selado (`Success`/`Failure`) de uma operação. |
| `ProblemDetails` | Corpo de erro RFC 9457. |
| `Problem` | Contrato de um erro de domínio. |
| `ProblemDetailsException` | Exceção que carrega um `ProblemDetails`. |
| `Results.orThrow(...)` | Desempacota um `Result` no Controller. |

```java
@Activity
public class ValidateCustomerActivity
        implements FlowItem<CustomerInput, Result<ValidatedCustomer, ValidationError>> {

    @Override
    public Result<ValidatedCustomer, ValidationError> execute(CustomerInput input) {
        if (input.name() == null || input.name().isBlank()) {
            return Result.failure(new ValidationError("name", "name is required"));
        }
        return Result.success(new ValidatedCustomer(input.name().trim()));
    }
}
```

No Controller:

```java
Customer customer = Results.orThrow(workflow.execute(input));
```

Os mappers JAX-RS (`ProblemDetailsExceptionMapper`, `ConstraintViolationExceptionMapper`) são
registrados automaticamente pelo Quarkus REST.

## Annotations

| Annotation | Papel | CDI |
|---|---|---|
| `@HarpiaApplication` | Identifica a classe principal da aplicação. | Marker, não cria bean |
| `@Controller` | Adapter de entrada HTTP. | `@Stereotype` + `@ApplicationScoped` |
| `@Service` | Regras de negócio. | `@Stereotype` + `@ApplicationScoped` |
| `@Repository` | Persistência. | `@Stereotype` + `@ApplicationScoped` |
| `@Activity` | Etapa de um workflow. | `@Stereotype` + `@ApplicationScoped` |
| `@WorkflowDefinition` | Workflow que declara a ordem das Activities. | `@Stereotype` + `@ApplicationScoped` |
| `@Component` | Componente genérico. | `@Stereotype` + `@ApplicationScoped` |

> A annotation de workflow chama-se `@WorkflowDefinition` porque Java não permite
> uma annotation e uma classe com o mesmo nome simples (`Workflow<I, O>`) no
> mesmo escopo. É a representação do `@Workflow` previsto nas diretrizes.

## Regras de uso

| Annotation | Regra |
|---|---|
| `@HarpiaApplication` | Uma única classe por aplicação, normalmente a `@QuarkusMain`. |
| `@Controller` | Implementa a interface gerada pelo contrato OpenAPI; não declara `@Path` nem verbos HTTP; não implementa `FlowItem`. |
| `@Service` | Não expõe endpoints e não depende de controllers. |
| `@Repository` | Não depende de controllers, services ou activities. |
| `@Activity` | Implementa `FlowItem<I, O>`; responsabilidade única; não expõe endpoints; não conhece a estrutura do workflow. |
| `@WorkflowDefinition` | Estende `Workflow<I, O>`; apenas declara os steps em `configure()`. |
| `@Component` | Uso genérico, quando nenhum papel específico se aplica. |

As regras são validadas em build time pelo módulo `harpia-core-deployment` (Jandex + `@BuildStep`).
Uma violação falha o build com a classe e a regra quebrada na mensagem.

## Vault de segredos (provider-agnóstico)

`VaultConfigSource` resolve chaves no formato `<AMBIENTE>_DB_<CAMPO>` (ex.: `HML_DB_URL`)
buscando o valor num cofre de segredos pelo secret `<prefixo>-<ambiente>-db-<campo>`. Assim o
`application.yaml` pode usar `${HML_DB_URL}` sem nunca expor URL, usuário ou senha.

O provedor é plugável via SPI (`SecretResolver`), descoberto por `ServiceLoader` e escolhido
por `harpia.vault.provider`. A configuração vive num `bootstrap.yaml` carregado por
`quarkus.config.locations=bootstrap.yaml`.

### Mapeamento de chave → secret (igual para todos os provedores)

| Placeholder no `application.yaml` | Nome do secret buscado |
|---|---|
| `${HML_DB_URL}` | `<secret-prefix>-hml-db-url` |
| `${HML_DB_USERNAME}` | `<secret-prefix>-hml-db-username` |
| `${HML_DB_PASSWORD}` | `<secret-prefix>-hml-db-password` |
| `${PRD_DB_URL}` | `<secret-prefix>-prd-db-url` |

Regra: `<AMBIENTE>_DB_<CAMPO>` → `<secret-prefix>-<ambiente>-db-<campo>` (tudo em minúsculas).

### Configuração genérica

| Propriedade | Padrão | Obrigatória | Uso |
|---|---|---|---|
| `harpia.vault.enabled` | `false` | não | ativa a busca (ou perfil `local`) |
| `harpia.vault.provider` | `oci` | não | qual provedor usar |
| `harpia.vault.secret-prefix` | `quarkus.application.name` | não | prefixo do nome dos secrets |

As demais propriedades são específicas de cada provedor, sob o prefixo
`harpia.vault.<provider>.*`.

---

### OCI Vault (Oracle) — `provider: oci` (implementado)

| Propriedade | Padrão | Obrigatória | Uso |
|---|---|---|---|
| `harpia.vault.oci.compartment-ocid` | — | **sim** | OCID do compartimento onde estão os secrets |
| `harpia.vault.oci.profile` | `DEFAULT` | não | perfil do `~/.oci/config` |
| `harpia.vault.oci.config-file` | local padrão do SDK | não | caminho do arquivo de config OCI |

```yaml
harpia:
  vault:
    provider: oci
    secret-prefix: user-info
    oci:
      compartment-ocid: ocid1.compartment.oc1..aaaa...
```

Autenticação: API key do `~/.oci/config` (user principal). Dependências (SDK OCI, opcional no
`harpia-core`): `oci-java-sdk-common`, `oci-java-sdk-vault`, `oci-java-sdk-secrets` e
`oci-java-sdk-common-httpclient-jersey3`.

---

### OpenBao — `provider: openbao` (planejado)

| Propriedade | Padrão | Obrigatória | Uso |
|---|---|---|---|
| `harpia.vault.openbao.address` | — | **sim** | URL do servidor (ex.: `https://bao.seudominio.com:8200`) |
| `harpia.vault.openbao.token` | — | uma das formas | autenticação por token |
| `harpia.vault.openbao.role-id` | — | uma das formas | AppRole: `role_id` |
| `harpia.vault.openbao.secret-id` | — | uma das formas | AppRole: `secret_id` |
| `harpia.vault.openbao.path` | `secret` | não | mount path do KV engine |
| `harpia.vault.openbao.kv-version` | `2` | não | versão do KV engine (`1` ou `2`) |
| `harpia.vault.openbao.namespace` | — | não | namespace (Enterprise) |

```yaml
harpia:
  vault:
    provider: openbao
    openbao:
      address: https://bao.seudominio.com:8200
      role-id: ${OPENBAO_ROLE_ID}
      secret-id: ${OPENBAO_SECRET_ID}
      path: secret
```

---

### HashiCorp Vault — `provider: vault` (planejado)

Mesma API do OpenBao; chaves sob `harpia.vault.vault.*`:

| Propriedade | Padrão | Obrigatória | Uso |
|---|---|---|---|
| `harpia.vault.vault.address` | — | **sim** | URL do servidor Vault |
| `harpia.vault.vault.token` | — | uma das formas | autenticação por token |
| `harpia.vault.vault.role-id` | — | uma das formas | AppRole: `role_id` |
| `harpia.vault.vault.secret-id` | — | uma das formas | AppRole: `secret_id` |
| `harpia.vault.vault.path` | `secret` | não | mount path do KV engine |
| `harpia.vault.vault.kv-version` | `2` | não | versão do KV engine |
| `harpia.vault.vault.namespace` | — | não | namespace (Enterprise) |

---

### Azure Key Vault — `provider: azure` (planejado)

| Propriedade | Padrão | Obrigatória | Uso |
|---|---|---|---|
| `harpia.vault.azure.key-vault-url` | — | **sim** | `https://<nome>.vault.azure.net` |
| `harpia.vault.azure.tenant-id` | — | sim (service principal) | tenant do Entra ID |
| `harpia.vault.azure.client-id` | — | sim (service principal) | client id |
| `harpia.vault.azure.client-secret` | — | sim (service principal) | client secret |

> Com managed identity, omita `tenant-id`/`client-id`/`client-secret` e use o
> `DefaultAzureCredential`.

```yaml
harpia:
  vault:
    provider: azure
    azure:
      key-vault-url: https://meu-vault.vault.azure.net
      tenant-id: ${AZURE_TENANT_ID}
      client-id: ${AZURE_CLIENT_ID}
      client-secret: ${AZURE_CLIENT_SECRET}
```

---

### AWS Secrets Manager — `provider: aws` (planejado)

| Propriedade | Padrão | Obrigatória | Uso |
|---|---|---|---|
| `harpia.vault.aws.region` | — | **sim** | região (ex.: `us-east-1`) |
| `harpia.vault.aws.access-key-id` | — | não | credencial (senão usa a default chain) |
| `harpia.vault.aws.secret-access-key` | — | não | credencial |
| `harpia.vault.aws.profile` | `default` | não | perfil do `~/.aws/credentials` |

```yaml
harpia:
  vault:
    provider: aws
    aws:
      region: us-east-1
```

---

### GCP Secret Manager — `provider: gcp` (planejado)

| Propriedade | Padrão | Obrigatória | Uso |
|---|---|---|---|
| `harpia.vault.gcp.project-id` | — | **sim** | projeto GCP |
| `harpia.vault.gcp.credentials` | — | não | JSON da service account (senão usa ADC) |

```yaml
harpia:
  vault:
    provider: gcp
    gcp:
      project-id: meu-projeto
```

---

### Adicionando um novo provedor

Implemente `SecretResolver` (lendo o próprio `harpia.vault.<provider>.*` no `configure()`) e
registre em `META-INF/services/io.github.krisner94.harpia.vault.SecretResolver`. O núcleo não
precisa ser alterado.
