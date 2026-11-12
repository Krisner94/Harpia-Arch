package io.github.krisner94.harpia.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Identifies the entry point of a Harpia Arch application.
 *
 * <p>It is a marker only: it does not create a CDI bean and does not change how
 * Quarkus boots the application. Its purpose is to make the main class explicit
 * and available to build-time validation.</p>
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface HarpiaApplication {
}
