package io.github.krisner94.harpia.it;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.is;

import io.quarkus.test.junit.QuarkusIntegrationTest;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;

@QuarkusIntegrationTest
class CustomerWorkflowIT {

    @Test
    void createsAndReadsCustomer() {
        Long id = given()
                .contentType(ContentType.JSON)
                .body("{\"name\":\"Ana\"}")
                .post("/customers")
                .then()
                .statusCode(200)
                .extract()
                .jsonPath()
                .getLong("id");

        given()
                .get("/customers/" + id)
                .then()
                .statusCode(200)
                .body("name", is("Ana"));
    }
}
