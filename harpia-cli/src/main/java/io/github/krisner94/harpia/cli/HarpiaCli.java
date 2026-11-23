package io.github.krisner94.harpia.cli;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Entry point of the Harpia Arch scaffolding CLI.
 *
 * <p>Generates a Quarkus service (POM, OpenAPI contract, typed Activities,
 * Workflows and a Controller skeleton) from an OpenAPI contract.</p>
 *
 * <p>Usage:</p>
 * <pre>
 * java -jar harpia-cli.jar \
 *     --name customers \
 *     --group com.acme \
 *     --package com.acme \
 *     --openapi openapi.yaml \
 *     --output ./customers
 * </pre>
 */
public final class HarpiaCli {

    private HarpiaCli() {
    }

    public static void main(String[] args) throws Exception {
        Options options = parse(args);

        Path spec = Path.of(options.openapi());
        if (!Files.isRegularFile(spec)) {
            throw new IllegalArgumentException("OpenAPI contract not found: " + spec);
        }

        new ScaffoldGenerator(options).generate(spec);

        System.out.println("Service generated in " + options.output());
    }

    private static Options parse(String[] args) {
        Map<String, String> values = new LinkedHashMap<>();
        for (int i = 0; i < args.length; i++) {
            String arg = args[i];
            if (!arg.startsWith("--")) {
                throw new IllegalArgumentException("unexpected argument: " + arg);
            }
            if (i + 1 >= args.length) {
                throw new IllegalArgumentException("missing value for " + arg);
            }
            values.put(arg.substring(2), args[++i]);
        }

        String name = values.get("name");
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("--name is required");
        }

        String group = values.getOrDefault("group", "com.example");
        String packageName = values.getOrDefault("package", group);
        String openapi = values.get("openapi");
        if (openapi == null || openapi.isBlank()) {
            throw new IllegalArgumentException("--openapi is required");
        }
        String output = values.getOrDefault("output", "./" + name);

        return new Options(name, group, packageName, openapi, output);
    }

    record Options(String name, String group, String packageName, String openapi, String output) {
    }
}
