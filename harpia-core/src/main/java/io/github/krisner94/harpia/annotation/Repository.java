package io.github.krisner94.harpia.annotation;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Stereotype;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Identifies a persistence component.
 *
 * <p>A repository isolates database access and must not depend on controllers,
 * services or activities. It is the only layer allowed to know storage details.</p>
 *
 * <p>It is an {@link ApplicationScoped} CDI stereotype.</p>
 */
@Stereotype
@ApplicationScoped
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface Repository {
}
