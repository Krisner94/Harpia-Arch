package io.github.krisner94.harpia.annotation;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Stereotype;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Identifies a generic application component that does not fit a more specific
 * architectural role.
 *
 * <p>It is an {@link ApplicationScoped} CDI stereotype.</p>
 */
@Stereotype
@ApplicationScoped
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface Component {
}
