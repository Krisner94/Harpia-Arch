package io.github.krisner94.harpia.it;

import io.github.krisner94.harpia.annotation.Repository;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;

@Repository
public class CustomerRepository {

    @Inject
    EntityManager entityManager;

    public CustomerEntity save(CustomerEntity customer) {
        entityManager.persist(customer);
        return customer;
    }

    public CustomerEntity findById(Long id) {
        return entityManager.find(CustomerEntity.class, id);
    }
}
