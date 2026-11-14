# Annotations

As annotations ficam no pacote `io.github.krisner94.harpia.annotation` e são fornecidas pelo
`harpia-core`.

| Annotation | Papel | CDI |
|---|---|---|
| `@HarpiaApplication` | Classe principal da aplicação | Marker, não cria bean |
| `@Controller` | Adapter de entrada HTTP | `@Stereotype` + `@ApplicationScoped` |
| `@Service` | Regras de negócio | `@Stereotype` + `@ApplicationScoped` |
| `@Repository` | Persistência | `@Stereotype` + `@ApplicationScoped` |
| `@Activity` | Etapa de workflow | `@Stereotype` + `@ApplicationScoped` |
| `@WorkflowDefinition` | Workflow | `@Stereotype` + `@ApplicationScoped` |
| `@Component` | Componente genérico | `@Stereotype` + `@ApplicationScoped` |

Todas têm `@Retention(RUNTIME)`, `@Target(TYPE)` e `@Documented`. A retenção de runtime é usada
pelo CDI e pela documentação; a validação arquitetural do Harpia Arch usa Jandex em build time,
não reflexão em runtime.

## Tabela de dependências

| Origem | Pode depender de | Não pode depender de |
|---|---|---|
| `@Controller` | Workflow, Service | Repository (preferencialmente via Workflow) |
| `@WorkflowDefinition` | Activity, Workflow | HTTP/Controller |
| `@Activity` | Service, Repository | Controller |
| `@Service` | Repository, Service | Controller |
| `@Repository` | EntityManager, storage | Controller, Service, Activity |
| `@Component` | Dependências do seu papel | Controller, se for uma camada de domínio |

A validação em build cobre as regras explícitas de `@Repository` e a proibição de `@Path`/verbos
em `@Controller`, `@Activity`, `@Service` e `@Component`.

## @HarpiaApplication

Identifica a classe principal:

```java
@HarpiaApplication
@QuarkusMain
public class App {

    public static void main(String[] args) {
        Quarkus.run(args);
    }
}
```

É um marker: não cria bean. A aplicação deve ter no máximo uma classe com essa annotation.

## @Controller

Identifica o adapter HTTP:

```java
@Controller
public class CustomerController implements CustomersApi {

    @Override
    public Response createCustomer(CustomerInput input) {
        // ...
    }
}
```

Regras:

- implementa a interface gerada pelo contrato OpenAPI;
- não declara `@Path` nem verbos HTTP;
- não implementa `FlowItem` nem `SimpleFlowItem`.

## @Service

Identifica regras de negócio. Não expõe endpoints e não depende de controllers. Pode ser chamado
por Activities, outros Services ou diretamente pelo Workflow quando a regra não for uma etapa.

## @Repository

Identifica persistência. Não depende de controllers, services ou activities. É a única camada que
deve conhecer detalhes de storage.

## @Activity

Identifica uma etapa:

```java
@Activity
public class ValidateCustomerActivity
        implements FlowItem<CustomerInput, CustomerContext, ValidatedCustomer> {

    @Override
    public ValidatedCustomer execute(CustomerInput input, CustomerContext context) {
        return new ValidatedCustomer(input.name());
    }
}
```

Sem contexto:

```java
@Activity
public class TrimCustomerNameActivity
        implements SimpleFlowItem<CustomerInput, CustomerInput> {

    @Override
    public CustomerInput execute(CustomerInput input) {
        return new CustomerInput(input.name().trim());
    }
}
```

Regras:

- implementa `FlowItem<I, C, O>` ou `SimpleFlowItem<I, O>`;
- `Void` explícito também é aceito (`FlowItem<I, Void, O>`);
- responsabilidade única;
- não expõe endpoints;
- não conhece a estrutura do workflow.

## @WorkflowDefinition

Identifica um workflow:

```java
@WorkflowDefinition
public class CreateCustomerWorkflow
        extends Workflow<CustomerInput, CustomerContext, Customer> {

    @Override
    protected void configure() {
        flowBuilder = FlowBuilder.builder(CustomerInput.class, CustomerContext.class)
                .step(validate)
                .build();
    }
}
```

Sem contexto:

```java
@WorkflowDefinition
public class TrimCustomerWorkflow
        extends SimpleWorkflow<CustomerInput, CustomerInput> {

    @Override
    protected void configure() {
        flowBuilder = FlowBuilder.builder(CustomerInput.class)
                .step(validate)
                .build();
    }
}
```

Regras:

- estende `Workflow<I, C, O>` ou `SimpleWorkflow<I, O>`;
- apenas declara os steps em `configure()`.

> A annotation chama-se `@WorkflowDefinition` porque Java não permite uma annotation e a classe
> `Workflow<I, C, O>` com o mesmo nome simples no mesmo escopo. Ela representa o `@Workflow`
> previsto nas diretrizes.

## @Component

Uso genérico, quando nenhum papel específico se aplica. Não substitui uma camada sem
justificativa; a validação ainda impede exposição HTTP.

## Bean discovery

As annotations são registradas pelo `harpia-core-deployment` como *bean-defining annotations*.
Isso garante que classes anotadas sejam descobertas pelo ArC mesmo quando a biblioteca não é um
bean archive explícito.

## Validação

As regras de uso são verificadas em build time. Consulte
[Validação em build](build-validation.md).
