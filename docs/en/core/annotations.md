# Annotations

Annotations live in `io.github.krisner94.harpia.annotation` and are provided by `harpia-core`.

| Annotation | Role | CDI |
|---|---|---|
| `@HarpiaApplication` | Application main class | Marker, does not create a bean |
| `@Controller` | HTTP inbound adapter | `@Stereotype` + `@ApplicationScoped` |
| `@Service` | Business rules | `@Stereotype` + `@ApplicationScoped` |
| `@Repository` | Persistence | `@Stereotype` + `@ApplicationScoped` |
| `@Activity` | Workflow step | `@Stereotype` + `@ApplicationScoped` |
| `@WorkflowDefinition` | Workflow | `@Stereotype` + `@ApplicationScoped` |
| `@Component` | Generic component | `@Stereotype` + `@ApplicationScoped` |

## @HarpiaApplication

Identifies the main class:

```java
@HarpiaApplication
@QuarkusMain
public class App {

    public static void main(String[] args) {
        Quarkus.run(args);
    }
}
```

It is a marker: it does not create a bean. The application must have at most one class with this
annotation.

## @Controller

Identifies the HTTP adapter:

```java
@Controller
public class CustomerController implements CustomersApi {

    @Override
    public Response createCustomer(CustomerInput input) {
        // ...
    }
}
```

Rules:

- implements the interface generated from the OpenAPI contract;
- does not declare `@Path` or HTTP verbs;
- does not implement `FlowItem`.

## @Service

Identifies business rules. It does not expose endpoints and does not depend on controllers.

## @Repository

Identifies persistence. It does not depend on controllers, services or activities.

## @Activity

Identifies a step:

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

Rules:

- implements `FlowItem<I, C, O>`;
- single responsibility;
- does not expose endpoints;
- does not know the workflow structure.

## @WorkflowDefinition

Identifies a workflow:

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

Rules:

- extends `Workflow<I, C, O>`;
- only declares the steps in `configure()`.

> The annotation is named `@WorkflowDefinition` because Java does not allow an annotation and the
> class `Workflow<I, C, O>` with the same simple name in the same scope. It represents the
> `@Workflow` annotation from the original guidelines.

## @Component

Generic use, when no specific role applies.

## Bean discovery

The annotations are registered by `harpia-core-deployment` as bean-defining annotations. This
ensures annotated classes are discovered by ArC even when the library is not an explicit bean
archive.

## Validation

Usage rules are checked at build time. See [Build validation](build-validation.md).
