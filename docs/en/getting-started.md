# Getting started

## Requirements

- Java 25
- Maven 3.9+
- Quarkus 3.40+

## 1. Create the application

Use `harpia-quarkus` as the parent:

```xml
<parent>
    <groupId>io.github.krisner94</groupId>
    <artifactId>harpia-quarkus</artifactId>
    <version>1.0.0</version>
</parent>

<groupId>com.acme</groupId>
<artifactId>my-service</artifactId>
<version>1.0.0</version>
<packaging>quarkus</packaging>
```

The parent provides the Quarkus BOM, REST, Jackson, `harpia-core`, JUnit, Rest Assured, the
`native` profile and OpenAPI code generation.

> `packaging=quarkus` is required. Do not declare the `quarkus-maven-plugin` or the
> compiler/test plugins in the application POM.

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

The generator creates `CustomersApi` and the DTOs. The default package is
`${project.groupId}.api`.

## 3. Create the context and Activities

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
        if (input.getName() == null || input.getName().isBlank()) {
            throw new IllegalArgumentException("name is required");
        }
        return input;
    }
}
```

```java
@Activity
public class PersistCustomerActivity
        implements FlowItem<CustomerInput, CustomerContext, Customer> {

    @Override
    @Transactional
    public Customer execute(CustomerInput input, CustomerContext context) {
        Customer customer = new Customer();
        customer.setName(input.getName());
        return customer;
    }
}
```

## 4. Create the Workflow

```java
@WorkflowDefinition
public class CreateCustomerWorkflow
        extends Workflow<CustomerInput, CustomerContext, Customer> {

    private final ValidateCustomerActivity validate;
    private final PersistCustomerActivity persist;

    public CreateCustomerWorkflow(ValidateCustomerActivity validate,
            PersistCustomerActivity persist) {
        this.validate = validate;
        this.persist = persist;
    }

    @Override
    protected void configure() {
        flowBuilder = FlowBuilder.builder(CustomerInput.class, CustomerContext.class)
                .step(validate)
                .step(persist)
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

## 6. Run

```bash
mvn quarkus:dev
```

Code generation runs before `javac`, architectural validation runs during the build, and the
Workflow is built once in `@PostConstruct`.

## Next steps

- [Workflow engine](core/workflow-engine.md)
- [Annotations](core/annotations.md)
- [Contract First](guides/contract-first.md)
- [Persistence](guides/persistence.md)
