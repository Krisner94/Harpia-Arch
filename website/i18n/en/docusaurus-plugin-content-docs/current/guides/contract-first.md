# Contract First with OpenAPI

`harpia-quarkus` configures `openapi-generator-maven-plugin` to generate JAX-RS interfaces and
DTOs before `javac`.

```text
OpenAPI YAML
    -> openapi-generator
    -> JAX-RS interfaces + DTOs
    -> Controller
    -> Workflow
    -> Activities
    -> Repository
```

## Contract

By default, the contract lives at:

```text
src/main/resources/openapi/openapi.yaml
```

The `harpia-openapi-codegen` profile activates automatically when this directory exists.

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

## Properties

| Property | Default | Description |
|---|---|---|
| `harpia.openapi.spec` | `${project.basedir}/src/main/resources/openapi/openapi.yaml` | Input file |
| `harpia.openapi.api-package` | `${project.groupId}.api` | Interface package |
| `harpia.openapi.model-package` | `${harpia.openapi.api-package}.beans` | DTO package |
| `harpia.openapi.api-suffix` | `Api` | Interface suffix |

Example:

```xml
<properties>
    <harpia.openapi.api-package>com.acme.api</harpia.openapi.api-package>
    <harpia.openapi.api-suffix>Contract</harpia.openapi.api-suffix>
</properties>
```

## Generated code

```text
target/generated-sources/openapi/src/main/java/com/acme/api/CustomersApi.java
target/generated-sources/openapi/src/main/java/com/acme/api/beans/*.java
```

`build-helper-maven-plugin` adds the directory as a source root. Code is generated before
`javac`, so the Controller can implement the interface normally.

## Controller

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

Rules:

- routes and verbs come exclusively from the generated interface;
- the Controller does not declare `@Path` or verbs;
- the Controller does not execute Activities directly; it calls the Workflow.

## `Response` return type

The generator can return `Response` instead of DTOs:

```xml
<plugin>
    <groupId>org.openapitools</groupId>
    <artifactId>openapi-generator-maven-plugin</artifactId>
    <executions>
        <execution>
            <id>harpia-generate-code-from-openapi</id>
            <configuration>
                <configOptions>
                    <returnResponse>true</returnResponse>
                </configOptions>
            </configuration>
        </execution>
    </executions>
</plugin>
```

This allows the Controller to control the HTTP status (for example, `201 Created`).

## IDE

The generated directory is:

```text
target/generated-sources/openapi/src/main/java
```

After `mvn generate-sources`, reload the Maven project in the IDE. If needed, mark the folder as
**Generated Sources Root**.

## Using another Maven parent

If the application does not use `harpia-quarkus`, copy from `harpia-arch`:

- `openapi-generator-maven-plugin`;
- `build-helper-maven-plugin`;
- the `harpia-openapi-codegen` profile.

Keep `packaging=quarkus` for the correct lifecycle.
