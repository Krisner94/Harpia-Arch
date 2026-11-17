# Harpia Core

Núcleo do Harpia Arch: engine de workflows e annotations arquiteturais.

- engine em `io.github.krisner94.harpia.core`;
- annotations em `io.github.krisner94.harpia.annotation`.

Tudo é fornecido automaticamente pelo parent `harpia-quarkus`.

## Engine

| Tipo | Papel |
|---|---|
| `FlowItem<I, C, O>` | Contrato de execução com contexto. Activities e Workflows implementam. |
| `SimpleFlowItem<I, O>` | Contrato de execução sem contexto. |
| `Workflow<I, C, O>` | Composição imutável que também é um `FlowItem`. |
| `SimpleWorkflow<I, O>` | Workflow sem declaração de contexto. |
| `FlowBuilder` | Builder tipado; a única operação de composição é `.step()`. |
| `SimpleFlowBuilder` | Builder tipado sem contexto, acionado por `builder(Class)`. |
| `FlowConfigurationException` | Falha de configuração do workflow. |

```java
@Activity
public class ValidateCustomerActivity
        implements FlowItem<CustomerInput, CustomerContext, ValidatedCustomer> {

    @Override
    public ValidatedCustomer execute(CustomerInput input, CustomerContext context) {
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
        extends Workflow<CustomerInput, CustomerContext, Customer> {

    private final ValidationWorkflow validation;
    private final PersistenceWorkflow persistence;

    public CreateCustomerWorkflow(ValidationWorkflow validation, PersistenceWorkflow persistence) {
        this.validation = validation;
        this.persistence = persistence;
    }

    @Override
    protected void configure() {
        flowBuilder = FlowBuilder.builder(CustomerInput.class, CustomerContext.class)
                .step(validation)
                .step(persistence)
                .build();
    }
}
```

### Composição hierárquica

`Workflow` implementa `FlowItem`, então um workflow é uma etapa de outro:

```java
flowBuilder = FlowBuilder.builder(CustomerInput.class, CustomerContext.class)
        .step(validationWorkflow)
        .step(enrichmentWorkflow)
        .step(persistenceWorkflow)
        .build();
```

Cada subworkflow tem seus próprios tipos de entrada, contexto e saída.

### Contexto opcional

O contexto pode ser omitido com `SimpleFlowItem`/`SimpleWorkflow`:

```java
@Activity
public class TrimCustomerNameActivity implements SimpleFlowItem<CustomerInput, CustomerInput> {

    @Override
    public CustomerInput execute(CustomerInput input) {
        return new CustomerInput(input.name().trim());
    }
}
```

```java
flowBuilder = FlowBuilder.builder(CustomerInput.class)
        .step(trim)
        .step(persist)
        .build();
```

Também é possível manter o contrato de três parâmetros com `Void`:

```java
FlowItem<CustomerInput, Void, CustomerInput>
Workflow<Void, Void, List<Customer>>
```

O contexto recebido é sempre `null`, sem criar objeto de contexto. `SimpleFlowItem` e
`SimpleWorkflow` existem porque Java não permite sobrecarregar `FlowItem`/`Workflow` pelo número de
parâmetros genéricos mantendo o mesmo nome.

Atividades sem contexto podem ser usadas em workflows com contexto; o contexto é ignorado pela
etapa e continua disponível para as seguintes. Um componente que exige contexto só entra em fluxo
sem contexto via `adaptContext(...)` explícito. Não há conversão implícita entre contextos nem
casts.

### Contextos diferentes

A adaptação é explícita e tipada; não existe conversão implícita:

```java
.step(enrichmentWorkflow.adaptContext(context ->
        new EnrichmentContext(context.correlationId())))
```

Para entradas e saídas diferentes:

```java
FlowItem.adapt(item, inputAdapter, contextAdapter, outputAdapter);
```

### Tipagem

O tipo de saída do builder muda a cada etapa. Uma cadeia incompatível não compila:

```java
FlowBuilder.builder(String.class, CustomerContext.class)
        .step(lengthActivity)   // String -> Integer
        .step(lengthActivity);  // erro: espera FlowItem<Integer, ...>
```

A garantia é de compilação; por type erasure não há verificação em runtime. O encadeamento não usa
`Object`, casts ou `@SuppressWarnings`.

### Ciclo de vida

- `configure()` roda uma única vez no `@PostConstruct`, após a injeção CDI;
- `flowBuilder` é publicado como `volatile` e nunca é reconstruído;
- Workflows instanciados manualmente inicializam na primeira execução;
- o contexto é sempre parâmetro de `execute`; não há estado de requisição em campos de beans;
- a execução não usa reflexão, descoberta dinâmica de componentes nem resolução dinâmica de
  contexto; o ArC pode usar proxies CDI, mas o motor não depende deles.

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
> uma annotation e uma classe com o mesmo nome simples (`Workflow<I, C, O>`) no
> mesmo escopo. É a representação do `@Workflow` previsto nas diretrizes.

## Regras de uso

| Annotation | Regra |
|---|---|
| `@HarpiaApplication` | Uma única classe por aplicação, normalmente a `@QuarkusMain`. |
| `@Controller` | Implementa a interface gerada pelo contrato OpenAPI; não declara `@Path` nem verbos HTTP; não implementa `FlowItem` nem `SimpleFlowItem`. |
| `@Service` | Não expõe endpoints e não depende de controllers. |
| `@Repository` | Não depende de controllers, services ou activities. |
| `@Activity` | Implementa `FlowItem<I, C, O>` ou `SimpleFlowItem<I, O>`; responsabilidade única; não expõe endpoints; não conhece a estrutura do workflow. |
| `@WorkflowDefinition` | Estende `Workflow<I, C, O>` ou `SimpleWorkflow<I, O>`; apenas declara os steps em `configure()`. |
| `@Component` | Uso genérico, quando nenhum papel específico se aplica. |

As regras são validadas em build time pelo módulo `harpia-core-deployment` (Jandex + `@BuildStep`).
Uma violação falha o build com a classe e a regra quebrada na mensagem.

## Vault de segredos (provider-agnóstico)

`VaultConfigSource` resolve chaves no formato `<AMBIENTE>_DB_<CAMPO>` (ex.: `HML_DB_URL`)
buscando o valor num cofre de segredos pelo secret `<prefixo>-<ambiente>-db-<campo>`. Assim o
`application.yaml` pode usar `${HML_DB_URL}` sem nunca expor URL, usuário ou senha.

O provedor é plugável via SPI (`SecretResolver`): OCI Vault, OpenBao/HashiCorp Vault, AWS
Secrets Manager, etc. Basta trocar a configuração — o provedor é descoberto por
`ServiceLoader` e escolhido por `harpia.vault.provider`.

Configuração (pode viver num `bootstrap.yaml` carregado via `quarkus.config.locations`):

| Propriedade | Padrão | Uso |
|---|---|---|
| `harpia.vault.enabled` | `false` | ativa a busca de segredos (ou perfil `local`) |
| `harpia.vault.provider` | `oci` | provedor a usar (`oci`, `openbao`, `aws`, ...) |
| `harpia.vault.secret-prefix` | `quarkus.application.name` | prefixo do nome dos secrets |
| `harpia.vault.oci.compartment-ocid` | — (obrigatória p/ OCI) | compartimento do Vault |
| `harpia.vault.oci.profile` | `DEFAULT` | perfil do `~/.oci/config` |
| `harpia.vault.oci.config-file` | local padrão do SDK | caminho do arquivo de config OCI |

Exemplo de `bootstrap.yaml`:

```yaml
harpia:
  vault:
    provider: oci
    secret-prefix: user-info
    oci:
      compartment-ocid: ocid1.compartment.oc1..aaaa...
```

O SDK OCI é **opcional**: aplicações que usam o provedor `oci` declaram
`com.oracle.oci.sdk:oci-java-sdk-common`, `oci-java-sdk-vault`, `oci-java-sdk-secrets` e
`oci-java-sdk-common-httpclient-jersey3`. Para criar um novo provedor, implemente
`SecretResolver` e registre em
`META-INF/services/io.github.krisner94.harpia.vault.SecretResolver`.
