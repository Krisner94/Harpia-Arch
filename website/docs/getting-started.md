# Primeiros passos

Este guia cria uma aplicação Quarkus com contrato OpenAPI, Activities, Workflow, Controller
opcional e testes.

## Requisitos

- Java 25;
- Maven 3.9+ (ou `./mvnw`);
- Quarkus 3.40+;
- GraalVM 25 apenas para Native Image.

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

O gerador cria `CustomersApi` e os DTOs. O pacote padrão é `${project.groupId}.api`.

## 3. Criar o contexto e as Activities

O contexto é opcional. Neste exemplo, use um contexto real para demonstrar correlação:

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

A segunda Activity transforma a entrada no DTO de saída. Persistência fica em uma Activity com
Repository, mostrada em [Persistência](guides/persistence.md).

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

Se o fluxo não precisar de contexto, use `SimpleFlowItem`:

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

## 4. Criar o Workflow

Com contexto:

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

Sem contexto:

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

O Controller não declara `@Path` nem verbos. A rota vem da interface gerada.

## 6. Executar

```bash
./mvnw quarkus:dev
```

O codegen roda antes do `javac`, a validação arquitetural roda no build e o Workflow é construído
uma única vez no `@PostConstruct`.

Teste:

```bash
curl -i -X POST http://localhost:8080/customers \
  -H 'Content-Type: application/json' \
  -d '{"name":"Ana"}'
```

## 7. Testar

```bash
./mvnw test
./mvnw -Dit verify
```

Para testar o executável nativo:

```bash
./mvnw -pl harpia-integration-tests verify -Dnative
```

## Próximos passos

- [Workflow engine](core/workflow-engine.md)
- [Contexto opcional](core/context.md)
- [Annotations](core/annotations.md)
- [Testes](guides/testing.md)
- [Contract First](guides/contract-first.md)
- [Persistência](guides/persistence.md)
- [Erros e diagnóstico](core/errors.md)
