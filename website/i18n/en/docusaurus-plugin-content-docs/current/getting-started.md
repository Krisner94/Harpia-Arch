# Getting started

This guide creates a Quarkus application with an OpenAPI contract, Activities, a Workflow, an
optional Controller and tests.

## Requirements

- Java 25;
- Maven 3.9+ (or `./mvnw`);
- Quarkus 3.40+;
- GraalVM 25 only for Native Image.

## 1. Create the application

Use `harpia-quarkus` as the parent:

```xml
<parent>
    <groupId>io.github.krisner94</groupId>
    <artifactId>harpia-quarkus</artifactId>
    <version>1.0.0</version>
</parent>

<groupId>com.acme</groupId>
<artifactId>meu-servico</artifactId>
<version>1.0.0</version>
<packaging>quarkus</packaging>
```

The parent provides the Quarkus BOM, REST, Jackson, `harpia-core`, JUnit, Rest Assured, the `native`
profile and the OpenAPI codegen.

> `packaging=quarkus` is mandatory. Do not declare the `quarkus-maven-plugin` or the
> compilation/test plugins in the application POM.

## 2. Define the OpenAPI contract

Create `src/main/resources/openapi/openapi.yaml`:

```yaml
openapi: 3.0.3
info:
  title: Customers API
  version: "1.0"
paths:
  /customers:
    post:
      operationId: createCustomer
      requestBody:
        required: true
        content:
          application/json:
            schema:
              $ref: '#/components/schemas/CustomerInput'
      responses:
        "201":
          description: Customer created
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/Customer'
components:
  schemas:
    CustomerInput:
      type: object
      required: [name]
      properties:
        name:
          type: string
    Customer:
      type: object
      properties:
        id:
          type: integer
          format: int64
        name:
          type: string
```

The generator creates `CustomersApi` and the DTOs. The default package is `${project.groupId}.api`.

## 3. Create the context and the Activities

The context is optional. In this example, use a real context to demonstrate correlation:

```java
public record CustomerContext(String correlationId) {
}
```

```java
@Activity
public class ValidateCustomerActivity
        implements FlowItem<CustomerInput, CustomerContext, CustomerInput> {

    @Override
    public CustomerInput execute(CustomerInput input, CustomerContext context) {
        if (input.name() == null || input.name().isBlank()) {
            throw new IllegalArgumentException("name is required");
        }
        return input;
    }
}
```

The second Activity transforms the input into the output DTO. Persistence lives in an Activity with
a Repository, shown in [Persistence](guides/persistence.md).

```java
@Activity
public class CreateCustomerActivity
        implements FlowItem<CustomerInput, CustomerContext, Customer> {

    @Override
    public Customer execute(CustomerInput input, CustomerContext context) {
        Customer customer = new Customer();
        customer.setName(input.name());
        return customer;
    }
}
```

If the flow does not need a context, use `SimpleFlowItem`:

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

## 4. Create the Workflow

With a context:

```java
@WorkflowDefinition
public class CreateCustomerWorkflow
        extends Workflow<CustomerInput, CustomerContext, Customer> {

    private final ValidateCustomerActivity validate;
    private final CreateCustomerActivity create;

    public CreateCustomerWorkflow(ValidateCustomerActivity validate,
            CreateCustomerActivity create) {
        this.validate = validate;
        this.create = create;
    }

    @Override
    protected void configure() {
        flowBuilder = FlowBuilder.builder(CustomerInput.class, CustomerContext.class)
                .step(validate)
                .step(create)
                .build();
    }
}
```

Without a context:

```java
@WorkflowDefinition
public class TrimCustomerWorkflow extends SimpleWorkflow<CustomerInput, CustomerInput> {

    private final TrimCustomerNameActivity trim;

    public TrimCustomerWorkflow(TrimCustomerNameActivity trim) {
        this.trim = trim;
    }

    @Override
    protected void configure() {
        flowBuilder = FlowBuilder.builder(CustomerInput.class)
                .step(trim)
                .build();
    }
}
```

## 5. Implement the Controller

```java
@Controller
public class CustomerController implements CustomersApi {

    private final CreateCustomerWorkflow workflow;

    public CustomerController(CreateCustomerWorkflow workflow) {
        this.workflow = workflow;
    }

    @Override
    public Response createCustomer(CustomerInput input) {
        Customer customer = workflow.execute(input, new CustomerContext("req-1"));
        return Response.status(Response.Status.CREATED).entity(customer).build();
    }
}
```

The Controller declares neither `@Path` nor verbs. The route comes from the generated interface.

## 6. Run

```bash
./mvnw quarkus:dev
```

The codegen runs before `javac`, the architectural validation runs during the build and the Workflow
is built only once in `@PostConstruct`.

Test:

```bash
curl -i -X POST http://localhost:8080/customers \
  -H 'Content-Type: application/json' \
  -d '{"name":"Ana"}'
```

## 7. Test

```bash
./mvnw test
./mvnw -Dit verify
```

To test the native executable:

```bash
./mvnw -pl harpia-integration-tests verify -Dnative
```

## Next steps

- [Workflow engine](core/workflow-engine.md)
- [Optional context](core/context.md)
- [Annotations](core/annotations.md)
- [Testing](guides/testing.md)
- [Contract First](guides/contract-first.md)
- [Persistence](guides/persistence.md)
- [Errors and diagnostics](core/errors.md)
