package io.github.krisner94.harpia.annotation;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Stereotype;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Identifies a workflow: the component that declares the order of activities.
 *
 * <p>Every class annotated with {@code @WorkflowDefinition} must extend
 * {@code io.github.krisner94.harpia.core.Workflow<I, C, O>}. The annotation is
 * named {@code WorkflowDefinition} because Java does not allow an annotation and
 * a class with the same simple name in the same scope.</p>
 *
 * <p>It is an {@link ApplicationScoped} CDI stereotype.</p>
 */
@Stereotype
@ApplicationScoped
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface WorkflowDefinition {
}
