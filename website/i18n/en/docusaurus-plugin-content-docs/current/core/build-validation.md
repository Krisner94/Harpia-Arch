# Build-time validation

`harpia-core` is a Quarkus extension. The `harpia-core-deployment` artifact is resolved
automatically by Quarkus and runs the validation during the build.

No configuration is required in the application POM.

## Rules

| Rule | Error |
|---|---|
| `@Activity` implements `FlowItem` or `SimpleFlowItem` | `@Activity ... must implement ...FlowItem or ...SimpleFlowItem` |
| `@WorkflowDefinition` extends `Workflow` or `SimpleWorkflow` | `@WorkflowDefinition ... must extend ...Workflow or ...SimpleWorkflow` |
| `@Controller` does not implement `FlowItem`/`SimpleFlowItem` | `@Controller ... must not implement ...FlowItem or ...SimpleFlowItem` |
| `@Controller` does not declare `@Path`/verbs | `@Controller ... must not declare @Path` |
| `@Activity` does not declare `@Path`/verbs | `@Activity ... must not declare GET` |
| `@Repository` does not depend on `@Controller` | `@Repository ... must not depend on @Controller` |
| `@Service` does not declare `@Path`/verbs | `@Service ... must not declare GET` |
| `@Component` does not declare `@Path`/verbs | `@Component ... must not declare GET` |
| A single `@HarpiaApplication` | `Only one @HarpiaApplication is allowed` |

## Failure example

An annotated Activity that implements neither `FlowItem` nor `SimpleFlowItem`:

```java
@Activity
public class InvalidActivity {
}
```

The build fails:

```text
[error]: Build step io.github.krisner94.harpia.deployment.HarpiaArchProcessor#validateArchitecture
threw an exception: java.lang.IllegalStateException:
@Activity com.acme.InvalidActivity must implement io.github.krisner94.harpia.core.FlowItem or io.github.krisner94.harpia.core.SimpleFlowItem
```

## How the validator works

`ArchitectureValidator` receives the Quarkus Jandex index and:

1. collects the annotated classes;
2. checks the interface and class hierarchy;
3. inspects `@Path` and HTTP verbs;
4. collects `@Repository` dependencies in fields and method parameters;
5. aggregates all violations and throws a single exception with the build.

The `@Activity` check accepts:

- `FlowItem<I, C, O>`;
- `SimpleFlowItem<I, O>`;
- any class that implements these contracts indirectly.

The `@WorkflowDefinition` check accepts:

- `Workflow<I, C, O>`;
- `SimpleWorkflow<I, O>`;
- indirect subclasses.

## Annotation registration

The role annotations are registered as bean-defining annotations:

```java
producer.produce(new BeanDefiningAnnotationBuildItem(
        DotName.createSimple(Activity.class.getName())));
```

This guarantees that ArC discovers annotated classes even when the library is not an explicit bean
archive. `@HarpiaApplication` is a marker and does not create a bean.

## Quarkus extension

```text
harpia-core
  META-INF/quarkus-extension.yaml
  META-INF/quarkus-extension.properties -> deployment artifact

harpia-core-deployment
  META-INF/quarkus-build-steps.list -> HarpiaArchProcessor
```

Quarkus loads the deployment during the build, runs the build steps and only generates the artifact
if all rules pass.

## Tests

The `harpia-core-deployment` module covers:

- valid/invalid activity (with and without a context);
- valid/invalid workflow (with and without a context);
- controller implementing `FlowItem`/`SimpleFlowItem`;
- controller with `@Path`/an HTTP verb;
- repository depending on a controller;
- multiple `@HarpiaApplication` classes;
- CDI initialization and actual execution of a workflow.

See [Testing](../guides/testing.md).

## Disabling validation

There is no disable flag. The proposal is that the architecture is guaranteed by the build; a
violation must be fixed, not ignored.

## Diagnostics

| Error | Fix |
|---|---|
| `must implement FlowItem` | Implement `FlowItem` or `SimpleFlowItem`, or remove `@Activity` |
| `must extend Workflow` | Extend `Workflow`/`SimpleWorkflow`, or remove `@WorkflowDefinition` |
| `must not implement FlowItem` | Move the step to an `@Activity` |
| `must not declare @Path` | Remove the route; use the interface generated from OpenAPI |
| `must not depend on @Controller` | Remove the inverted dependency from the Repository |
| `Only one @HarpiaApplication` | Keep only the main class |

See [Errors and diagnostics](errors.md).
