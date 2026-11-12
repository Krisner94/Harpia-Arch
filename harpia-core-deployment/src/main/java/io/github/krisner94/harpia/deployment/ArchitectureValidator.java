package io.github.krisner94.harpia.deployment;

import io.github.krisner94.harpia.annotation.Activity;
import io.github.krisner94.harpia.annotation.Component;
import io.github.krisner94.harpia.annotation.Controller;
import io.github.krisner94.harpia.annotation.HarpiaApplication;
import io.github.krisner94.harpia.annotation.Repository;
import io.github.krisner94.harpia.annotation.Service;
import io.github.krisner94.harpia.annotation.WorkflowDefinition;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.FieldInfo;
import org.jboss.jandex.IndexView;
import org.jboss.jandex.MethodInfo;
import org.jboss.jandex.Type;
import org.jboss.jandex.WildcardType;

/**
 * Validates the architectural rules declared by the Harpia annotations.
 *
 * <p>The rules are checked against the Jandex index during the Quarkus build, so
 * an invalid application never produces an artifact.</p>
 */
final class ArchitectureValidator {

    private static final DotName HARPIA_APPLICATION = DotName.createSimple(HarpiaApplication.class.getName());
    private static final DotName CONTROLLER = DotName.createSimple(Controller.class.getName());
    private static final DotName SERVICE = DotName.createSimple(Service.class.getName());
    private static final DotName ACTIVITY = DotName.createSimple(Activity.class.getName());
    private static final DotName WORKFLOW_DEFINITION = DotName.createSimple(WorkflowDefinition.class.getName());
    private static final DotName REPOSITORY = DotName.createSimple(Repository.class.getName());
    private static final DotName COMPONENT = DotName.createSimple(Component.class.getName());

    private static final DotName FLOW_ITEM =
            DotName.createSimple("io.github.krisner94.harpia.core.FlowItem");
    private static final DotName WORKFLOW =
            DotName.createSimple("io.github.krisner94.harpia.core.Workflow");

    private static final DotName PATH = DotName.createSimple("jakarta.ws.rs.Path");
    private static final Set<DotName> HTTP_METHODS = Set.of(
            DotName.createSimple("jakarta.ws.rs.GET"),
            DotName.createSimple("jakarta.ws.rs.POST"),
            DotName.createSimple("jakarta.ws.rs.PUT"),
            DotName.createSimple("jakarta.ws.rs.DELETE"),
            DotName.createSimple("jakarta.ws.rs.PATCH"),
            DotName.createSimple("jakarta.ws.rs.HEAD"),
            DotName.createSimple("jakarta.ws.rs.OPTIONS"));

    private static final DotName OBJECT = DotName.createSimple(Object.class.getName());

    /**
     * Checks every annotated class in the index.
     *
     * @param index application index
     * @return all violations found, empty when the architecture is valid
     */
    List<String> validate(IndexView index) {
        List<String> errors = new ArrayList<>();

        Set<DotName> controllers = new HashSet<>();
        for (ClassInfo clazz : annotatedClasses(index, CONTROLLER)) {
            controllers.add(clazz.name());
            if (implementsInterface(index, clazz, FLOW_ITEM)) {
                errors.add("@Controller " + clazz.name()
                        + " must not implement " + FLOW_ITEM + "; move the step to an @Activity");
            }
            checkNoJaxrs(clazz, "@Controller", errors);
        }

        for (ClassInfo clazz : annotatedClasses(index, ACTIVITY)) {
            if (!implementsInterface(index, clazz, FLOW_ITEM)) {
                errors.add("@Activity " + clazz.name() + " must implement " + FLOW_ITEM);
            }
            checkNoJaxrs(clazz, "@Activity", errors);
        }

        for (ClassInfo clazz : annotatedClasses(index, WORKFLOW_DEFINITION)) {
            if (!extendsClass(index, clazz, WORKFLOW)) {
                errors.add("@WorkflowDefinition " + clazz.name() + " must extend " + WORKFLOW);
            }
        }

        for (ClassInfo clazz : annotatedClasses(index, REPOSITORY)) {
            checkNoControllerDependency(index, clazz, controllers, errors);
        }

        for (ClassInfo clazz : annotatedClasses(index, SERVICE)) {
            checkNoJaxrs(clazz, "@Service", errors);
        }

        for (ClassInfo clazz : annotatedClasses(index, COMPONENT)) {
            checkNoJaxrs(clazz, "@Component", errors);
        }

        List<ClassInfo> applications = annotatedClasses(index, HARPIA_APPLICATION);
        if (applications.size() > 1) {
            errors.add("Only one @HarpiaApplication is allowed, found " + applications.size());
        }

        return errors;
    }

    private static List<ClassInfo> annotatedClasses(IndexView index, DotName annotation) {
        List<ClassInfo> classes = new ArrayList<>();
        for (AnnotationInstance instance : index.getAnnotations(annotation)) {
            if (instance.target().kind() == AnnotationTarget.Kind.CLASS) {
                classes.add(instance.target().asClass());
            }
        }
        return classes;
    }

    private static void checkNoJaxrs(ClassInfo clazz, String role, List<String> errors) {
        if (clazz.hasDeclaredAnnotation(PATH)) {
            errors.add(role + " " + clazz.name() + " must not declare @Path");
        }
        for (MethodInfo method : clazz.methods()) {
            if (method.hasAnnotation(PATH)) {
                errors.add(role + " " + clazz.name() + "#" + method.name() + " must not declare @Path");
            }
            for (DotName httpMethod : HTTP_METHODS) {
                if (method.hasAnnotation(httpMethod)) {
                    errors.add(role + " " + clazz.name() + "#" + method.name()
                            + " must not declare " + simpleName(httpMethod));
                }
            }
        }
    }

    private static String simpleName(DotName name) {
        String value = name.toString();
        int separator = value.lastIndexOf('.');
        return separator < 0 ? value : value.substring(separator + 1);
    }

    private static void checkNoControllerDependency(IndexView index, ClassInfo clazz,
            Set<DotName> controllers, List<String> errors) {
        Set<DotName> dependencies = new HashSet<>();
        for (FieldInfo field : clazz.fields()) {
            collectTypes(field.type(), dependencies);
        }
        for (MethodInfo method : clazz.methods()) {
            for (Type parameter : method.parameterTypes()) {
                collectTypes(parameter, dependencies);
            }
        }
        for (DotName dependency : dependencies) {
            if (controllers.contains(dependency)) {
                errors.add("@Repository " + clazz.name() + " must not depend on @Controller " + dependency);
            }
        }
    }

    private static void collectTypes(Type type, Set<DotName> types) {
        switch (type.kind()) {
            case CLASS -> types.add(type.name());
            case ARRAY -> collectTypes(type.asArrayType().component(), types);
            case PARAMETERIZED_TYPE -> {
                types.add(type.name());
                for (Type argument : type.asParameterizedType().arguments()) {
                    collectTypes(argument, types);
                }
            }
            case WILDCARD_TYPE -> {
                WildcardType wildcard = type.asWildcardType();
                if (wildcard.extendsBound() != null) {
                    collectTypes(wildcard.extendsBound(), types);
                }
                if (wildcard.superBound() != null) {
                    collectTypes(wildcard.superBound(), types);
                }
            }
            default -> {
            }
        }
    }

    private static boolean implementsInterface(IndexView index, ClassInfo clazz, DotName target) {
        for (DotName name : clazz.interfaceNames()) {
            if (name.equals(target)) {
                return true;
            }
            ClassInfo contract = index.getClassByName(name);
            if (contract != null && implementsInterface(index, contract, target)) {
                return true;
            }
        }
        DotName superName = clazz.superName();
        if (superName != null && !superName.equals(OBJECT)) {
            ClassInfo superClass = index.getClassByName(superName);
            if (superClass != null) {
                return implementsInterface(index, superClass, target);
            }
        }
        return false;
    }

    private static boolean extendsClass(IndexView index, ClassInfo clazz, DotName target) {
        DotName superName = clazz.superName();
        while (superName != null && !superName.equals(OBJECT)) {
            if (superName.equals(target)) {
                return true;
            }
            ClassInfo superClass = index.getClassByName(superName);
            if (superClass == null) {
                return false;
            }
            superName = superClass.superName();
        }
        return false;
    }
}
