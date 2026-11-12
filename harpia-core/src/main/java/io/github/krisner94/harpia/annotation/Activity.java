package io.github.krisner94.harpia.annotation;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Stereotype;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Identifies a single step of a workflow.
 *
 * <p>Every class annotated with {@code @Activity} must implement
 * {@code io.github.krisner94.harpia.core.FlowItem<I, C, O>}, must have a single
 * responsibility, must not expose HTTP endpoints and must not know the workflow
 * structure.</p>
 *
 * <p>It is an {@link ApplicationScoped} CDI stereotype.</p>
 */
@Stereotype
@ApplicationScoped
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface Activity {
}
