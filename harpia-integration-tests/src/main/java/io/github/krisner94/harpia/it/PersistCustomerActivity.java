package io.github.krisner94.harpia.it;

import io.github.krisner94.harpia.annotation.Activity;
import io.github.krisner94.harpia.core.FlowItem;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

@Activity
public class PersistCustomerActivity implements FlowItem<String, CustomerContext, CustomerEntity> {

    @Inject
    CustomerRepository repository;

    @Override
    @Transactional
    public CustomerEntity execute(String input, CustomerContext context) {
        return repository.save(new CustomerEntity(input));
    }
}
