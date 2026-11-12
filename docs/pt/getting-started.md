# Primeiros passos

## Requisitos

- Java 25
- Maven 3.9+
- Quarkus 3.40+

## 1. Criar a aplicação

Use o `harpia-quarkus` como parent:

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

O parent fornece Quarkus BOM, REST, Jackson, `harpia-core`, JUnit, Rest Assured, perfil `native` e
o codegen OpenAPI.

> `packaging=quarkus` é obrigatório. Não declare o `quarkus-maven-plugin` nem os plugins de
> compilação/teste no POM da aplicação.

## 2. Definir o contrato OpenAPI

Crie `src/main/resources/openapi/openapi.yaml`:

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
          description: Cliente criado
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

O gerador cria `CustomersApi` e os DTOs automaticamente. O pacote padrão é
`${project.groupId}.api`.

## 3. Criar o contexto e as Activities

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

## 4. Criar o Workflow

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

## 5. Implementar o Controller

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

## 6. Executar

```bash
mvn quarkus:dev
```

O codegen roda antes do `javac`, a validação arquitetural roda no build e o Workflow é construído
uma única vez no `@PostConstruct`.

## Próximos passos

- [Workflow engine](core/workflow-engine.md)
- [Annotations](core/annotations.md)
- [Contract First](guides/contract-first.md)
- [Persistência](guides/persistence.md)
