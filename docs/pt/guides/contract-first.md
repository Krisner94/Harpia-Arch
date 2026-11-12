# Contract First com OpenAPI

O `harpia-quarkus` configura o `openapi-generator-maven-plugin` para gerar as interfaces JAX-RS e
os DTOs antes do `javac`.

```text
OpenAPI YAML
    -> openapi-generator
    -> interfaces JAX-RS + DTOs
    -> Controller
    -> Workflow
    -> Activities
    -> Repository
```

## Contrato

Por padrão, o contrato fica em:

```text
src/main/resources/openapi/openapi.yaml
```

O perfil `harpia-openapi-codegen` é ativado automaticamente quando esse diretório existe.

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

## Propriedades

| Propriedade | Default | Descrição |
|---|---|---|
| `harpia.openapi.spec` | `${project.basedir}/src/main/resources/openapi/openapi.yaml` | Arquivo de entrada |
| `harpia.openapi.api-package` | `${project.groupId}.api` | Pacote das interfaces |
| `harpia.openapi.model-package` | `${harpia.openapi.api-package}.beans` | Pacote dos DTOs |
| `harpia.openapi.api-suffix` | `Api` | Sufixo das interfaces |

Exemplo:

```xml
<properties>
    <harpia.openapi.api-package>com.acme.api</harpia.openapi.api-package>
    <harpia.openapi.api-suffix>Contract</harpia.openapi.api-suffix>
</properties>
```

## Código gerado

```text
target/generated-sources/openapi/src/main/java/com/acme/api/CustomersApi.java
target/generated-sources/openapi/src/main/java/com/acme/api/beans/*.java
```

O `build-helper-maven-plugin` adiciona o diretório como source root. O código é gerado antes do
`javac`, então o Controller pode implementar a interface normalmente.

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

Regras:

- a rota e os verbos vêm exclusivamente da interface gerada;
- o Controller não declara `@Path` nem verbos;
- o Controller não executa Activities diretamente; chama o Workflow.

## Retorno `Response`

O gerador pode ser configurado para retornar `Response` em vez dos DTOs:

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

Assim é possível controlar o status HTTP (por exemplo, `201 Created`) diretamente no Controller.

## IDE

O diretório gerado fica em:

```text
target/generated-sources/openapi/src/main/java
```

Depois de `mvn generate-sources`, recarregue o projeto Maven na IDE. Se necessário, marque a pasta
como **Generated Sources Root**.

## Usando outro parent Maven

Se a aplicação não usa `harpia-quarkus`, copie do `harpia-arch`:

- o `openapi-generator-maven-plugin`;
- o `build-helper-maven-plugin`;
- o perfil `harpia-openapi-codegen`.

Mantenha `packaging=quarkus` para o lifecycle correto.
