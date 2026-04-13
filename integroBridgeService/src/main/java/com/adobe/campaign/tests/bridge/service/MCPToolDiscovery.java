/*
 * Copyright 2022 Adobe
 * All Rights Reserved.
 *
 * NOTICE: Adobe permits you to use, modify, and distribute this file in
 * accordance with the terms of the Adobe license agreement accompanying
 * it.
 */
package com.adobe.campaign.tests.bridge.service;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.reflections.Reflections;
import org.reflections.scanners.Scanners;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Discovers Java methods in configured packages and converts them into MCP tool definitions.
 * Only public static methods are exposed. Overloaded methods with the same parameter count
 * are skipped (they are accessible via the generic java_call tool instead).
 */
public class MCPToolDiscovery {

    private static final Logger log = LogManager.getLogger();

    /**
     * Holds the results of a tool discovery scan.
     */
    public static class DiscoveryResult {
        /** MCP tool definitions ready for serialisation in a tools/list response. */
        public final List<Map<String, Object>> tools;
        /** Maps each tool name to the Java Method it represents, for dispatch in tools/call. */
        public final Map<String, Method> methodRegistry;

        public DiscoveryResult(List<Map<String, Object>> tools, Map<String, Method> methodRegistry) {
            this.tools = Collections.unmodifiableList(tools);
            this.methodRegistry = Collections.unmodifiableMap(methodRegistry);
        }
    }

    /**
     * Scans the given comma-separated package prefixes and builds MCP tool definitions for
     * every discoverable public static method.
     *
     * @param packagesCsv the value of IBS.CLASSLOADER.STATIC.INTEGRITY.PACKAGES
     * @return a DiscoveryResult containing the tool list and dispatch registry
     */
    public static DiscoveryResult discoverTools(String packagesCsv) {
        List<Map<String, Object>> tools = new ArrayList<>();
        Map<String, Method> registry = new LinkedHashMap<>();

        if (packagesCsv == null || packagesCsv.trim().isEmpty()) {
            log.warn("IBS.CLASSLOADER.STATIC.INTEGRITY.PACKAGES is not set — no tools will be discovered for MCP. "
                    + "Set this property to enable tool discovery.");
            return new DiscoveryResult(tools, registry);
        }

        // Strip trailing dots that IBS uses as package separators (e.g. "com.example.")
        String[] packages = Arrays.stream(packagesCsv.split(","))
                .map(String::trim)
                .map(p -> p.endsWith(".") ? p.substring(0, p.length() - 1) : p)
                .filter(p -> !p.isEmpty())
                .toArray(String[]::new);

        Set<Class<?>> allClasses = new LinkedHashSet<>();
        for (String pkg : packages) {
            try {
                Reflections reflections = new Reflections(pkg, Scanners.SubTypes.filterResultsBy(s -> true));
                allClasses.addAll(reflections.getSubTypesOf(Object.class));
            } catch (Exception e) {
                log.warn("Failed to scan package '{}' for MCP tools: {}", pkg, e.getMessage());
            }
        }

        for (Class<?> clazz : allClasses) {
            // Only inspect methods declared directly on this class — not inherited statics
            Map<String, List<Method>> byName = Arrays.stream(clazz.getMethods())
                    .filter(m -> m.getDeclaringClass().equals(clazz))
                    .filter(m -> Modifier.isStatic(m.getModifiers()))
                    .collect(Collectors.groupingBy(Method::getName));

            for (Map.Entry<String, List<Method>> entry : byName.entrySet()) {
                String methodName = entry.getKey();
                List<Method> overloads = entry.getValue();

                if (overloads.size() == 1) {
                    // Unique method name on this class — use simple tool name
                    String toolName = clazz.getSimpleName() + "_" + methodName;
                    registerTool(tools, registry, toolName, overloads.get(0));
                } else {
                    // Multiple overloads — disambiguate by parameter count
                    Map<Integer, List<Method>> byParamCount = overloads.stream()
                            .collect(Collectors.groupingBy(Method::getParameterCount));

                    for (Map.Entry<Integer, List<Method>> countEntry : byParamCount.entrySet()) {
                        if (countEntry.getValue().size() > 1) {
                            log.warn("Skipping ambiguous overloads for {}.{}({} param(s)) — "
                                    + "use the java_call tool to invoke them directly.",
                                    clazz.getName(), methodName, countEntry.getKey());
                        } else {
                            String toolName = clazz.getSimpleName() + "_" + methodName + "_" + countEntry.getKey();
                            registerTool(tools, registry, toolName, countEntry.getValue().get(0));
                        }
                    }
                }
            }
        }

        log.info("MCP tool discovery complete: {} tool(s) registered from {} class(es).",
                tools.size(), allClasses.size());
        return new DiscoveryResult(tools, registry);
    }

    private static void registerTool(List<Map<String, Object>> tools, Map<String, Method> registry,
            String toolName, Method method) {
        if (registry.containsKey(toolName)) {
            log.warn("Tool name collision for '{}' — skipping duplicate from {}. "
                    + "Consider using fully qualified class names.",
                    toolName, method.getDeclaringClass().getName());
            return;
        }
        Map<String, Object> tool = new LinkedHashMap<>();
        tool.put("name", toolName);
        tool.put("description",
                "Calls " + method.getDeclaringClass().getName() + "." + method.getName() + "()");
        tool.put("inputSchema", buildInputSchema(method));

        tools.add(tool);
        registry.put(toolName, method);
        log.debug("Registered MCP tool '{}'", toolName);
    }

    /**
     * Builds a JSON Schema object describing the input parameters of a method.
     * Parameter names are generated as arg0, arg1, ... since Java reflection
     * does not expose source-level parameter names unless compiled with -parameters.
     */
    static Map<String, Object> buildInputSchema(Method method) {
        Map<String, Object> schema = new LinkedHashMap<>();
        schema.put("type", "object");

        Class<?>[] paramTypes = method.getParameterTypes();
        if (paramTypes.length == 0) {
            schema.put("properties", new LinkedHashMap<>());
            return schema;
        }

        Map<String, Object> properties = new LinkedHashMap<>();
        List<String> required = new ArrayList<>();

        for (int i = 0; i < paramTypes.length; i++) {
            String paramName = "arg" + i;
            Map<String, Object> paramSchema = new LinkedHashMap<>();
            paramSchema.put("type", javaTypeToJsonSchemaType(paramTypes[i]));
            paramSchema.put("description", paramTypes[i].getSimpleName());
            properties.put(paramName, paramSchema);
            required.add(paramName);
        }

        schema.put("properties", properties);
        schema.put("required", required);
        return schema;
    }

    /**
     * Maps a Java type to its closest JSON Schema primitive type.
     */
    static String javaTypeToJsonSchemaType(Class<?> type) {
        if (type == String.class) return "string";
        if (type == int.class || type == Integer.class
                || type == long.class || type == Long.class) return "integer";
        if (type == double.class || type == Double.class
                || type == float.class || type == Float.class) return "number";
        if (type == boolean.class || type == Boolean.class) return "boolean";
        if (type.isArray() || List.class.isAssignableFrom(type)) return "array";
        return "object";
    }
}
