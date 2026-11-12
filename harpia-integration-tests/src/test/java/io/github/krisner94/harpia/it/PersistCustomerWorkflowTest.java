package io.github.krisner94.harpia.it;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.Test;

@QuarkusTest
class PersistCustomerWorkflowTest {

    @Inject
    PersistCustomerWorkflow workflow;

    @Inject
    CustomerRepository repository;

    @Test
    @Transactional
    void persistsCustomerThroughTheWorkflow() {
        CustomerEntity customer = workflow.execute("Ana", new CustomerContext());

        assertNotNull(customer.getId());
        assertEquals("Ana", repository.findById(customer.getId()).getName());
    }
}
