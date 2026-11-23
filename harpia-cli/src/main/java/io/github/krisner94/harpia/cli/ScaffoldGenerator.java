package io.github.krisner94.harpia.cli;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.yaml.snakeyaml.Yaml;

/**
 * Generates a Harpia Arch service from an OpenAPI 3 contract.
 */
final class ScaffoldGenerator {

    private static final Set<String> HTTP_METHODS =
            Set.of("get", "post", "put", "patch", "delete", "head", "options");

    private final HarpiaCli.Options options;
    private final String dtoPackage;

    ScaffoldGenerator(HarpiaCli.Options options) {
        this.options = options;
        this.dtoPackage = options.group() + ".api.beans";
    }

    void generate(Path spec) throws IOException {
        List<Operation> operations = parse(spec);

        Path root = Path.of(options.output());
        Files.createDirectories(root);

        write(root.resolve("pom.xml"), pom());
        write(root.resolve("README.md"), readme(operations));

        Path contractDir = root.resolve("src/main/resources/openapi");
        Files.createDirectories(contractDir);
        try (InputStream in = Files.newInputStream(spec)) {
            Files.copy(in, contractDir.resolve("openapi.yaml"), StandardCopyOption.REPLACE_EXISTING);
        }

        String workflowPkg = options.packageName() + ".workflow";
        for (Operation op : operations) {
            String className = toClassName(op.operationId());
            write(sourceFile(root, workflowPkg, className + "Activity"),
                    activity(workflowPkg, className, op));
            write(sourceFile(root, workflowPkg, className + "Workflow"),
                    workflow(workflowPkg, className, op));
        }

        if (!operations.isEmpty()) {
            String httpPkg = options.packageName() + ".http";
            write(sourceFile(root, httpPkg, toClassName(options.name()) + "Controller"),
                    controller(httpPkg, workflowPkg, operations));
        }
    }

    @SuppressWarnings("unchecked")
    private List<Operation> parse(Path spec) throws IOException {
        Map<String, Object> document = new Yaml().load(Files.newInputStream(spec));
        List<Operation> operations = new ArrayList<>();

        Object paths = document.get("paths");
        if (!(paths instanceof Map<?, ?> pathsMap)) {
            return operations;
        }

        for (Map.Entry<?, ?> pathEntry : pathsMap.entrySet()) {
            String path = pathEntry.getKey().toString();
            if (!(pathEntry.getValue() instanceof Map<?, ?> methods)) {
                continue;
            }
            for (Map.Entry<?, ?> methodEntry : methods.entrySet()) {
                String method = methodEntry.getKey().toString();
                if (!HTTP_METHODS.contains(method) || !(methodEntry.getValue() instanceof Map<?, ?> body)) {
                    continue;
                }
                Map<String, Object> operation = (Map<String, Object>) body;
                String operationId = (String) operation.get("operationId");
                if (operationId == null || operationId.isBlank()) {
                    operationId = method + path.replaceAll("[^a-zA-Z0-9]", "");
                }
                String input = requestType(operation);
                String output = responseType(operation);
                operations.add(new Operation(operationId, method, path, input, output));
            }
        }
        return operations;
    }

    @SuppressWarnings("unchecked")
    private String requestType(Map<String, Object> operation) {
        Object body = operation.get("requestBody");
        if (!(body instanceof Map<?, ?> requestBody)) {
            return "Void";
        }
        return schemaRef(((Map<String, Object>) requestBody).get("content"));
    }

    @SuppressWarnings("unchecked")
    private String responseType(Map<String, Object> operation) {
        Object responses = operation.get("responses");
        if (!(responses instanceof Map<?, ?> responsesMap)) {
            return "Void";
        }
        for (Object key : responsesMap.keySet()) {
            if (key.toString().startsWith("2")) {
                Object response = responsesMap.get(key);
                if (response instanceof Map<?, ?> responseMap) {
                    String type = schemaRef(responseMap.get("content"));
                    if (!"Void".equals(type)) {
                        return type;
                    }
                }
            }
        }
        return "Void";
    }

    @SuppressWarnings("unchecked")
    private String schemaRef(Object content) {
        if (!(content instanceof Map<?, ?> contentMap)) {
            return "Void";
        }
        Object json = contentMap.get("application/json");
        if (!(json instanceof Map<?, ?> jsonMap)) {
            return "Void";
        }
        Object schema = jsonMap.get("schema");
        if (!(schema instanceof Map<?, ?> schemaMap)) {
            return "Void";
        }
        Object ref = schemaMap.get("$ref");
        if (ref instanceof String reference && reference.startsWith("#/components/schemas/")) {
            return reference.substring("#/components/schemas/".length());
        }
        return "Void";
    }

    private String pom() {
        return """
                <?xml version="1.0" encoding="UTF-8"?>
                <project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
                    <modelVersion>4.0.0</modelVersion>

                    <parent>
                        <groupId>io.github.krisner94</groupId>
                        <artifactId>harpia-quarkus</artifactId>
                        <version>1.0.0</version>
                    </parent>

                    <groupId>%s</groupId>
                    <artifactId>%s</artifactId>
                    <version>1.0.0</version>
                    <packaging>quarkus</packaging>

                    <name>%s</name>
                </project>
                """.formatted(options.group(), options.name(), options.name());
    }

    private String activity(String pkg, String className, Operation op) {
        String imports = dtoImports(op.input(), op.output());
        return """
                package %s;

                %simport io.github.krisner94.harpia.annotation.Activity;
                import io.github.krisner94.harpia.core.FlowItem;

                /**
                 * Step generated from operation {@code %s}.
                 */
                @Activity
                public class %sActivity implements FlowItem<%s, %s> {

                    @Override
                    public %s execute(%s input) {
                        // TODO: implement operation %s (%s %s)
                        throw new UnsupportedOperationException("%s is not implemented");
                    }
                }
                """.formatted(pkg, imports, op.operationId(), className, op.input(), op.output(),
                op.output(), op.input(), op.operationId(), op.httpMethod().toUpperCase(), op.path(),
                op.operationId());
    }

    private String workflow(String pkg, String className, Operation op) {
        String imports = dtoImports(op.input(), op.output());
        return """
                package %s;

                %simport io.github.krisner94.harpia.annotation.WorkflowDefinition;
                import io.github.krisner94.harpia.core.FlowBuilder;
                import io.github.krisner94.harpia.core.Workflow;
                import jakarta.inject.Inject;

                /**
                 * Workflow generated from operation {@code %s}.
                 */
                @WorkflowDefinition
                public class %sWorkflow extends Workflow<%s, %s> {

                    private final %sActivity activity;

                    @Inject
                    public %sWorkflow(%sActivity activity) {
                        this.activity = activity;
                    }

                    @Override
                    protected void configure() {
                        flowBuilder = FlowBuilder.from(activity)
                                .build();
                    }
                }
                """.formatted(pkg, imports, op.operationId(), className, op.input(), op.output(),
                className, className, className);
    }

    private String controller(String httpPkg, String workflowPkg, List<Operation> operations) {
        StringBuilder fields = new StringBuilder();
        StringBuilder ctorParams = new StringBuilder();
        StringBuilder ctorAssign = new StringBuilder();
        StringBuilder methods = new StringBuilder();
        StringBuilder imports = new StringBuilder();

        for (Operation op : operations) {
            String className = toClassName(op.operationId());
            String field = lowerFirst(className) + "Workflow";
            fields.append("    private final ").append(className).append("Workflow ").append(field).append(";\n");
            ctorParams.append(className).append("Workflow ").append(field).append(", ");
            ctorAssign.append("        this.").append(field).append(" = ").append(field).append(";\n");
            imports.append("import ").append(workflowPkg).append('.').append(className).append("Workflow;\n");
            if (!"Void".equals(op.input())) {
                imports.append("import ").append(dtoPackage).append('.').append(op.input()).append(";\n");
            }
            if (!"Void".equals(op.output())) {
                imports.append("import ").append(dtoPackage).append('.').append(op.output()).append(";\n");
            }
            methods.append("""

                        // operationId: %s (%s %s)
                        public %s %s(%s input) {
                            return %s.execute(input);
                        }
                    """.formatted(op.operationId(), op.httpMethod().toUpperCase(), op.path(),
                    op.output(), op.operationId(), op.input(), field));
        }

        if (ctorParams.length() > 0) {
            ctorParams.setLength(ctorParams.length() - 2);
        }
        String controllerName = toClassName(options.name()) + "Controller";

        return """
                package %s;

                %simport io.github.krisner94.harpia.annotation.Controller;
                import jakarta.inject.Inject;

                /**
                 * HTTP adapter generated from the OpenAPI contract.
                 *
                 * <p>After the first {@code mvn generate-sources}, implement the generated
                 * {@code *Api} interface and add {@code @Override} to each method below. The
                 * route and the verbs come exclusively from that interface.</p>
                 */
                @Controller
                public class %s {

                %s
                    @Inject
                    public %s(%s) {
                %s    }
                %s}
                """.formatted(httpPkg, imports, controllerName, fields, controllerName, ctorParams,
                ctorAssign, methods);
    }

    private String readme(List<Operation> operations) {
        StringBuilder steps = new StringBuilder();
        for (Operation op : operations) {
            steps.append("- `")
                    .append(op.httpMethod().toUpperCase())
                    .append(' ')
                    .append(op.path())
                    .append("` → `")
                    .append(toClassName(op.operationId()))
                    .append("Workflow`\n");
        }
        return """
                # %s

                Serviço gerado pelo Harpia CLI a partir do contrato OpenAPI.

                ## Operações

                %s
                ## Build

                ```bash
                ./mvnw quarkus:dev
                ```

                O codegen OpenAPI roda antes do `javac`; o primeiro build gera a interface `*Api` e
                os DTOs em `%s` e `%s`.
                """.formatted(options.name(), steps, options.group() + ".api", dtoPackage);
    }

    private String dtoImports(String... types) {
        StringBuilder sb = new StringBuilder();
        for (String type : types) {
            if (!"Void".equals(type) && sb.indexOf("import " + dtoPackage + "." + type + ";") < 0) {
                sb.append("import ").append(dtoPackage).append('.').append(type).append(";\n");
            }
        }
        return sb.toString();
    }

    private static Path sourceFile(Path root, String pkg, String className) throws IOException {
        Path dir = root.resolve("src/main/java").resolve(pkg.replace('.', '/'));
        Files.createDirectories(dir);
        return dir.resolve(className + ".java");
    }

    private static void write(Path file, String content) throws IOException {
        Files.createDirectories(file.getParent());
        Files.writeString(file, content);
    }

    private static String toClassName(String id) {
        StringBuilder sb = new StringBuilder();
        boolean upper = true;
        for (char c : id.toCharArray()) {
            if (Character.isLetterOrDigit(c)) {
                sb.append(upper ? Character.toUpperCase(c) : c);
                upper = false;
            } else {
                upper = true;
            }
        }
        return sb.toString();
    }

    private static String lowerFirst(String s) {
        return s.isEmpty() ? s : Character.toLowerCase(s.charAt(0)) + s.substring(1);
    }

    record Operation(String operationId, String httpMethod, String path, String input, String output) {
    }
}
