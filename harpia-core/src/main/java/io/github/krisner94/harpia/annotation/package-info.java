/**
 * Architectural annotations and CDI stereotypes of Harpia Arch.
 *
 * <p>Each annotation defines a role and is composed with {@code @ApplicationScoped}
 * through {@code @Stereotype}, so a single annotation registers the component in
 * the CDI container and documents its architectural responsibility.</p>
 *
 * <ul>
 *     <li>{@link io.github.krisner94.harpia.annotation.HarpiaApplication} - main class</li>
 *     <li>{@link io.github.krisner94.harpia.annotation.Controller} - inbound HTTP adapter</li>
 *     <li>{@link io.github.krisner94.harpia.annotation.Service} - business rules</li>
 *     <li>{@link io.github.krisner94.harpia.annotation.Repository} - persistence</li>
 *     <li>{@link io.github.krisner94.harpia.annotation.Activity} - workflow step</li>
 *     <li>{@link io.github.krisner94.harpia.annotation.WorkflowDefinition} - workflow</li>
 *     <li>{@link io.github.krisner94.harpia.annotation.Component} - generic component</li>
 * </ul>
 */
package io.github.krisner94.harpia.annotation;
