package io.github.krisner94.harpia.it;

import io.github.krisner94.harpia.annotation.WorkflowDefinition;
import io.github.krisner94.harpia.core.FlowBuilder;
import io.github.krisner94.harpia.core.Workflow;
import jakarta.inject.Inject;

@WorkflowDefinition
public class PersistCustomerWorkflow extends Workflow<String, CustomerContext, CustomerEntity> {

    @Inject
    PersistCustomerActivity activity;

    @Override
    protected void configure() {
        flowBuilder = FlowBuilder.builder(String.class, CustomerContext.class)
                .step(activity)
                .build();
    }
}
