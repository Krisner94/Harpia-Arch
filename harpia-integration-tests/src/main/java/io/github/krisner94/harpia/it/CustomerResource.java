package io.github.krisner94.harpia.it;

import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@Path("/customers")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class CustomerResource {

    @Inject
    PersistCustomerWorkflow workflow;

    @Inject
    CustomerRepository repository;

    @POST
    public CustomerResponse create(CustomerRequest request) {
        CustomerEntity customer = workflow.execute(request.name(), new CustomerContext());
        return new CustomerResponse(customer.getId(), customer.getName());
    }

    @GET
    @Path("/{id}")
    public CustomerResponse find(@PathParam("id") Long id) {
        CustomerEntity customer = repository.findById(id);
        if (customer == null) {
            throw new NotFoundException();
        }
        return new CustomerResponse(customer.getId(), customer.getName());
    }

    public record CustomerRequest(String name) {
    }

    public record CustomerResponse(Long id, String name) {
    }
}
