package io.github.krisner94.harpia.annotation;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Stereotype;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Identifies an inbound adapter: the class that exposes HTTP endpoints.
 *
 * <p>With the contract-first model, a controller implements an interface generated
 * from the OpenAPI contract and never declares {@code @Path}, HTTP verbs or query
 * parameters. The generated interface is the only source of routing.</p>
 *
 * <p>It is an {@link ApplicationScoped} CDI stereotype.</p>
 */
@Stereotype
@ApplicationScoped
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface Controller {
}
