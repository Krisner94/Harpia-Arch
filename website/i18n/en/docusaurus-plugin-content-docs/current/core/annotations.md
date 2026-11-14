# Annotations

The annotations live in the `io.github.krisner94.harpia.annotation` package and are provided by
`harpia-core`.

| Annotation | Role | CDI |
|---|---|---|
| `@HarpiaApplication` | Application main class | Marker, does not create a bean |
| `@Controller` | HTTP inbound adapter | `@Stereotype` + `@ApplicationScoped` |
| `@Service` | Business rules | `@Stereotype` + `@ApplicationScoped` |
| `@Repository` | Persistence | `@Stereotype` + `@ApplicationScoped` |
| `@Activity` | Workflow step | `@Stereotype` + `@ApplicationScoped` |
| `@WorkflowDefinition` | Workflow | `@Stereotype` + `@ApplicationScoped` |
| `@Component` | Generic component | `@Stereotype` + `@ApplicationScoped` |

They all have `@Retention(RUNTIME)`, `@Target(TYPE)` and `@Documented`. Runtime retention is used
by CDI and by the documentation; Harpia Arch architectural validation uses Jandex at build time,
not runtime reflection.

## Dependency table

| Source | May depend on | Must not depend on |
|---|---|---|
| `@Controller` | Workflow, Service | Repository (preferably via Workflow) |
| `@WorkflowDefinition` | Activity, Workflow | HTTP/Controller |
| `@Activity` | Service, Repository | Controller |
| `@Service` | Repository, Service | Controller |
| `@Repository` | EntityManager, storage | Controller, Service, Activity |
| `@Component` | Dependencies of its role | Controller, if it is a domain layer |

Build validation covers the explicit `@Repository` rules and the prohibition of `@Path`/verbs on
`@Controller`, `@Activity`, `@Service` and `@Component`.

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
- does not implement `FlowItem` or `SimpleFlowItem`.

## @Service

Identifies business rules. It does not expose endpoints and does not depend on controllers. It can be
called by Activities, other Services, or directly by the Workflow when the rule is not a step.

## @Repository

Identifies persistence. It does not depend on controllers, services or activities. It is the only
layer that should know storage details.

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

Without a context:

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

Rules:

- implements `FlowItem<I, C, O>` or `SimpleFlowItem<I, O>`;
- explicit `Void` is also accepted (`FlowItem<I, Void, O>`);
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

Without a context:

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

Rules:

- extends `Workflow<I, C, O>` or `SimpleWorkflow<I, O>`;
- only declares the steps in `configure()`.

> The annotation is named `@WorkflowDefinition` because Java does not allow an annotation and the
> `Workflow<I, C, O>` class to share the same simple name in the same scope. It represents the
> `@Workflow` foreseen in the guidelines.

## @Component

Generic use, when no specific role applies. It does not replace a layer without justification; the
validation still prevents HTTP exposure.

## Bean discovery

The annotations are registered by `harpia-core-deployment` as *bean-defining annotations*.
This guarantees that annotated classes are discovered by ArC even when the library is not an
explicit bean archive.

## Validation

The usage rules are checked at build time. See
[Build-time validation](build-validation.md).
