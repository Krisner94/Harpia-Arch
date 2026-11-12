# Build validation

`harpia-core` is a Quarkus extension. The `harpia-core-deployment` artifact is resolved
automatically by Quarkus and runs validation during the build.

No application POM configuration is required.

## Rules

| Rule | Error |
|---|---|
| `@Activity` implements `FlowItem` | `@Activity ... must implement ...FlowItem` |
| `@WorkflowDefinition` extends `Workflow` | `@WorkflowDefinition ... must extend ...Workflow` |
| `@Controller` does not implement `FlowItem` | `@Controller ... must not implement ...FlowItem` |
| `@Controller` does not declare `@Path`/verbs | `@Controller ... must not declare @Path` |
| `@Activity` does not declare `@Path`/verbs | `@Activity ... must not declare GET` |
| `@Repository` does not depend on `@Controller` | `@Repository ... must not depend on @Controller` |
| A single `@HarpiaApplication` | `Only one @HarpiaApplication is allowed` |

## Failure example

An annotated Activity that does not implement `FlowItem`:

```java
@Activity
public class InvalidActivity {
}
```

The build fails:

```text
[error]: Build step io.github.krisner94.harpia.deployment.HarpiaArchProcessor#validateArchitecture
threw an exception: java.lang.IllegalStateException:
@Activity com.acme.InvalidActivity must implement io.github.krisner94.harpia.core.FlowItem
```

## How it works

1. `harpia-core` publishes `META-INF/quarkus-extension.properties` pointing to
   `harpia-core-deployment`.
2. Quarkus loads the deployment build step.
3. `HarpiaArchProcessor` registers the annotations as bean-defining and validates the Jandex index.
4. If there are violations, the build stops before producing the artifact.

The Jandex index is the one Quarkus already built; no class parsing happens at runtime.

## Tests

The `harpia-core-deployment` module covers:

- valid/invalid activity;
- valid/invalid workflow;
- controller implementing `FlowItem`;
- controller with `@Path`/HTTP verb;
- repository depending on a controller;
- multiple `@HarpiaApplication`;
- CDI initialization and actual workflow execution.

## Disabling validation

There is no disable flag. The architecture is meant to be guaranteed by the build; a violation
should be fixed, not ignored.
