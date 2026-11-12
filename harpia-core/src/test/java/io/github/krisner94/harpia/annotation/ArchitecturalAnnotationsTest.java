package io.github.krisner94.harpia.annotation;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Stereotype;
import java.util.List;
import org.junit.jupiter.api.Test;

class ArchitecturalAnnotationsTest {

    private static final List<Class<?>> STEREOTYPES = List.of(
            Controller.class,
            Service.class,
            Repository.class,
            Activity.class,
            WorkflowDefinition.class,
            Component.class);

    @Test
    void rolesAreApplicationScopedStereotypes() {
        for (Class<?> role : STEREOTYPES) {
            assertTrue(role.isAnnotationPresent(Stereotype.class), role.getSimpleName() + " must be a stereotype");
            assertTrue(role.isAnnotationPresent(ApplicationScoped.class),
                    role.getSimpleName() + " must be application scoped");
        }
    }

    @Test
    void applicationMarkerDoesNotCreateABean() {
        assertFalse(HarpiaApplication.class.isAnnotationPresent(Stereotype.class));
        assertFalse(HarpiaApplication.class.isAnnotationPresent(ApplicationScoped.class));
    }
}
