package io.github.krisner94.harpia.cli;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ScaffoldGeneratorTest {

    @Test
    void generatesAServiceFromAnOpenApiContract(@TempDir Path temp) throws Exception {
        Path spec = temp.resolve("openapi.yaml");
        Files.writeString(spec, """
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
                          description: Created
                          content:
                            application/json:
                              schema:
                                $ref: '#/components/schemas/Customer'
                components:
                  schemas:
                    CustomerInput:
                      type: object
                      properties:
                        name:
                          type: string
                    Customer:
                      type: object
                      properties:
                        id:
                          type: integer
                          format: int64
                """);

        Path output = temp.resolve("customers");
        HarpiaCli.Options options = new HarpiaCli.Options(
                "customers", "com.acme", "com.acme", spec.toString(), output.toString());

        new ScaffoldGenerator(options).generate(spec);

        String pom = Files.readString(output.resolve("pom.xml"));
        assertTrue(pom.contains("<artifactId>harpia-quarkus</artifactId>"), "pom usa o parent harpia-quarkus");

        assertTrue(Files.exists(output.resolve("src/main/resources/openapi/openapi.yaml")),
                "contrato é copiado");

        String activity = Files.readString(
                output.resolve("src/main/java/com/acme/workflow/CreateCustomerActivity.java"));
        assertTrue(activity.contains("implements FlowItem<CustomerInput, Customer>"),
                "activity tipada com entrada e saída");

        assertTrue(Files.exists(
                        output.resolve("src/main/java/com/acme/workflow/CreateCustomerWorkflow.java")),
                "workflow é gerado");

        String controller = Files.readString(
                output.resolve("src/main/java/com/acme/http/CustomersController.java"));
        assertTrue(controller.contains("public Customer createCustomer(CustomerInput input)"),
                "controller delega para o workflow");

        assertTrue(Files.exists(output.resolve("README.md")), "readme é gerado");
    }
}
