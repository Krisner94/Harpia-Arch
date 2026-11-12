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
- não implementa `FlowItem`.

## @Service

Identifica regras de negócio. Não expõe endpoints e não depende de controllers.

## @Repository

Identifica persistência. Não depende de controllers, services ou activities.

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

Regras:

- implementa `FlowItem<I, C, O>`;
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

Regras:

- estende `Workflow<I, C, O>`;
- apenas declara os steps em `configure()`.

> A annotation chama-se `@WorkflowDefinition` porque Java não permite uma annotation e a classe
> `Workflow<I, C, O>` com o mesmo nome simples no mesmo escopo. Ela representa o `@Workflow`
> previsto nas diretrizes.

## @Component

Uso genérico, quando nenhum papel específico se aplica.

## Bean discovery

As annotations são registradas pelo `harpia-core-deployment` como *bean-defining annotations*.
Isso garante que classes anotadas sejam descobertas pelo ArC mesmo quando a biblioteca não é um
bean archive explícito.

## Validação

As regras de uso são verificadas em build time. Consulte
[Validação em build](build-validation.md).
