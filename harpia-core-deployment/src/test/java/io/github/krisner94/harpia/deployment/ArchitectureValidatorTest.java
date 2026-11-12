package io.github.krisner94.harpia.deployment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.krisner94.harpia.annotation.Activity;
import io.github.krisner94.harpia.annotation.Controller;
import io.github.krisner94.harpia.annotation.HarpiaApplication;
import io.github.krisner94.harpia.annotation.Repository;
import io.github.krisner94.harpia.annotation.WorkflowDefinition;
import io.github.krisner94.harpia.core.FlowBuilder;
import io.github.krisner94.harpia.core.FlowItem;
import io.github.krisner94.harpia.core.Workflow;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import java.io.IOException;
import java.util.List;
import org.jboss.jandex.Index;
import org.jboss.jandex.IndexView;
import org.junit.jupiter.api.Test;

class ArchitectureValidatorTest {

    private final ArchitectureValidator validator = new ArchitectureValidator();

    @Test
    void acceptsAValidApplication() throws IOException {
        List<String> errors = validate(ValidActivity.class, ValidWorkflow.class,
                ValidController.class, ValidRepository.class, App.class);

        assertTrue(errors.isEmpty(), () -> String.join(System.lineSeparator(), errors));
    }

    @Test
    void activityMustImplementFlowItem() throws IOException {
        List<String> errors = validate(InvalidActivity.class);

        assertEquals(1, errors.size());
        assertTrue(errors.get(0).contains("@Activity"));
        assertTrue(errors.get(0).contains("must implement"));
    }

    @Test
    void workflowMustExtendWorkflow() throws IOException {
        List<String> errors = validate(InvalidWorkflow.class);

        assertEquals(1, errors.size());
        assertTrue(errors.get(0).contains("@WorkflowDefinition"));
        assertTrue(errors.get(0).contains("must extend"));
    }

    @Test
    void controllerMustNotImplementFlowItem() throws IOException {
        List<String> errors = validate(ControllerActivity.class);

        assertEquals(1, errors.size());
        assertTrue(errors.get(0).contains("@Controller"));
        assertTrue(errors.get(0).contains("must not implement"));
    }

    @Test
    void controllerMustNotDeclareRoutes() throws IOException {
        List<String> errors = validate(ControllerWithPath.class, ControllerWithVerb.class);

        assertEquals(2, errors.size(), () -> String.join(System.lineSeparator(), errors));
        assertTrue(errors.stream().anyMatch(error -> error.contains("must not declare @Path")),
                () -> String.join(System.lineSeparator(), errors));
        assertTrue(errors.stream().anyMatch(error -> error.contains("must not declare GET")),
                () -> String.join(System.lineSeparator(), errors));
    }

    @Test
    void repositoryMustNotDependOnController() throws IOException {
        List<String> errors = validate(ValidController.class, InvalidRepository.class);

        assertEquals(1, errors.size());
        assertTrue(errors.get(0).contains("@Repository"));
        assertTrue(errors.get(0).contains("must not depend on @Controller"));
    }

    @Test
    void allowsOnlyOneApplicationClass() throws IOException {
        List<String> errors = validate(App.class, SecondApp.class);

        assertEquals(1, errors.size());
        assertTrue(errors.get(0).contains("Only one @HarpiaApplication"));
    }

    private List<String> validate(Class<?>... classes) throws IOException {
        IndexView index = Index.of(classes);
        return validator.validate(index);
    }

    @HarpiaApplication
    static final class App {
    }

    @HarpiaApplication
    static final class SecondApp {
    }

    @Activity
    static final class ValidActivity implements FlowItem<String, Object, String> {

        @Override
        public String execute(String input, Object context) {
            return input;
        }
    }

    @Activity
    static final class InvalidActivity {
    }

    @WorkflowDefinition
    static final class ValidWorkflow extends Workflow<String, Object, String> {

        @Override
        protected void configure() {
            flowBuilder = FlowBuilder.builder(String.class, Object.class)
                    .step((input, context) -> input)
                    .build();
        }
    }

    @WorkflowDefinition
    static final class InvalidWorkflow {
    }

    @Controller
    static final class ValidController implements PetsApi {
    }

    @Controller
    @Path("/invalid")
    static final class ControllerWithPath {
    }

    @Controller
    static final class ControllerWithVerb {

        @GET
        public String route() {
            return "";
        }
    }

    @Controller
    static final class ControllerActivity implements FlowItem<String, Object, String> {

        @Override
        public String execute(String input, Object context) {
            return input;
        }
    }

    @Repository
    static final class ValidRepository {
    }

    @Repository
    static final class InvalidRepository {

        private ValidController controller;
    }

    @Path("/pets")
    interface PetsApi {
    }
}
