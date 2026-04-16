/*
 * Copyright 2022 Adobe
 * All Rights Reserved.
 *
 * NOTICE: Adobe permits you to use, modify, and distribute this file in
 * accordance with the terms of the Adobe license agreement accompanying
 * it.
 */
package com.adobe.campaign.tests.bridge.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import spark.Request;
import spark.Response;

import java.lang.reflect.Method;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Handles JSON-RPC 2.0 requests arriving at the POST /mcp endpoint.
 *
 * Implements the MCP (Model Context Protocol) Streamable HTTP transport for tool access:
 * - initialize    : MCP handshake
 * - tools/list    : returns a single {@code java_call} tool whose description embeds a
 *                   catalog of auto-discovered methods
 * - tools/call    : invokes the {@code java_call} tool, which accepts arbitrary BridgeService
 *                   call chains; auto-discovered methods are not directly callable as
 *                   separate MCP tools — the catalog in the description tells the LLM which
 *                   class and method names to place in the callContent payload
 *
 * Tool discovery is performed once at construction time using IBS.CLASSLOADER.STATIC.INTEGRITY.PACKAGES.
 */
public class MCPRequestHandler {

    private static final Logger log = LogManager.getLogger();
    private static final String JSONRPC_VERSION = "2.0";
    private static final String MCP_PROTOCOL_VERSION = "2024-11-05";
    private static final String JAVA_CALL_TOOL_NAME = "java_call";

    private static final String JAVA_CALL_TOOL_SCHEMA = "{"
            + "\"type\":\"object\","
            + "\"required\":[\"callContent\"],"
            + "\"properties\":{"
            + "\"callContent\":{"
            + "\"type\":\"object\","
            + "\"description\":\"Map of call IDs to call definitions. A string arg matching a prior call ID is substituted with that call's result.\","
            + "\"additionalProperties\":{"
            + "\"type\":\"object\","
            + "\"required\":[\"class\",\"method\"],"
            + "\"properties\":{"
            + "\"class\":{\"type\":\"string\",\"description\":\"Fully qualified Java class name\"},"
            + "\"method\":{\"type\":\"string\",\"description\":\"Method name\"},"
            + "\"args\":{\"type\":\"array\",\"description\":\"Method arguments\"},"
            + "\"returnType\":{\"type\":\"string\",\"description\":\"Optional expected return type\"}"
            + "}}},"
            + "\"environmentVariables\":{\"type\":\"object\",\"description\":\"Key-value pairs injected before execution\"},"
            + "\"timeout\":{\"type\":\"integer\",\"description\":\"Timeout in milliseconds (0=unlimited, default 10000)\"}"
            + "}}";

    private final ObjectMapper mapper = new ObjectMapper();
    private final List<Map<String, Object>> toolList;

    /**
     * Constructs the handler, performs tool discovery from IBS.CLASSLOADER.STATIC.INTEGRITY.PACKAGES,
     * and builds a single {@code java_call} tool whose description embeds a catalog of all
     * discovered methods.
     */
    public MCPRequestHandler() {
        MCPToolDiscovery.DiscoveryResult discovery = MCPToolDiscovery.discoverTools(
                ConfigValueHandlerIBS.STATIC_INTEGRITY_PACKAGES.fetchValue());
        String catalog = buildCatalog(discovery.tools, discovery.methodRegistry);

        List<Map<String, Object>> tools = new ArrayList<>();
        try {
            Map<String, Object> javaCallTool = new LinkedHashMap<>();
            javaCallTool.put("name", JAVA_CALL_TOOL_NAME);
            javaCallTool.put("description", buildJavaCallDescription(catalog));
            javaCallTool.put("inputSchema", mapper.readValue(JAVA_CALL_TOOL_SCHEMA, Map.class));
            tools.add(javaCallTool);
        } catch (JsonProcessingException e) {
            log.error("Failed to parse java_call tool schema — the tool will not be available.", e);
        }
        this.toolList = Collections.unmodifiableList(tools);
        log.info("MCPRequestHandler ready: {} method(s) in catalog via java_call.", discovery.methodRegistry.size());
    }

    /**
     * Spark route handler. Parses the incoming JSON-RPC 2.0 request and dispatches
     * to the appropriate handler. All exceptions are caught and returned as MCP errors
     * rather than propagating to Spark's HTTP exception handlers.
     */
    public Object handle(Request req, Response res) {
        res.type("application/json");

        Map<String, Object> body;
        try {
            body = mapper.readValue(req.body(), Map.class);
        } catch (Exception e) {
            res.status(400);
            return buildError(null, -32700, "Parse error: " + e.getMessage());
        }

        Object id = body.get("id");
        String method = (String) body.get("method");

        // Notifications have no id — acknowledge with 202 and no body
        if (id == null && method != null && !method.equals("initialize")) {
            res.status(202);
            return "";
        }

        if (method == null) {
            return buildError(id, -32600, "Invalid Request: missing method field");
        }

        try {
            switch (method) {
                case "initialize":
                    return buildResult(id, buildInitializeResult());

                case "tools/list":
                    return buildResult(id, Collections.singletonMap("tools", toolList));

                case "tools/call":
                    @SuppressWarnings("unchecked")
                    Map<String, Object> params = (Map<String, Object>) body.getOrDefault("params", Collections.emptyMap());
                    Map<String, String> headers = req.headers().stream()
                            .collect(Collectors.toMap(k -> k, req::headers));
                    return handleToolCall(id, params, headers);

                default:
                    return buildError(id, -32601, "Method not found: " + method);
            }
        } catch (Exception e) {
            log.error("Unexpected error handling MCP method '{}': {}", method, e.getMessage(), e);
            return buildError(id, -32603, "Internal error: " + e.getMessage());
        }
    }

    private Map<String, Object> buildInitializeResult() {
        Map<String, Object> serverInfo = new LinkedHashMap<>();
        serverInfo.put("name", "bridgeService");
        serverInfo.put("version", ConfigValueHandlerIBS.PRODUCT_VERSION.fetchValue());

        Map<String, Object> capabilities = new LinkedHashMap<>();
        capabilities.put("tools", new LinkedHashMap<>());

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("protocolVersion", MCP_PROTOCOL_VERSION);
        result.put("serverInfo", serverInfo);
        result.put("capabilities", capabilities);
        return result;
    }

    @SuppressWarnings("unchecked")
    private String handleToolCall(Object id, Map<String, Object> params, Map<String, String> headers) {
        String toolName = (String) params.get("name");
        Map<String, Object> arguments = (Map<String, Object>) params.getOrDefault("arguments",
                Collections.emptyMap());

        if (toolName == null) {
            return buildError(id, -32602, "Invalid params: missing tool name");
        }

        if (JAVA_CALL_TOOL_NAME.equals(toolName)) {
            return handleJavaCall(id, arguments, headers);
        }

        return buildCallToolResult(id, "Unknown tool: " + toolName
                + ". The only executable tool is java_call — use the class and method from its description catalog.",
                true);
    }

    /**
     * Builds the catalog text embedded in the java_call tool description. Each discovered
     * method is listed with its fully qualified class name, method name, Javadoc description,
     * and argument list so the LLM can construct the correct callContent payload.
     */
    private String buildCatalog(List<Map<String, Object>> tools, Map<String, Method> methodRegistry) {
        if (methodRegistry.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        sb.append("Discovered methods (use class/method values in callContent for java_call):\n\n");
        for (Map.Entry<String, Method> entry : methodRegistry.entrySet()) {
            String toolName = entry.getKey();
            Method method = entry.getValue();

            sb.append(toolName).append("\n");
            sb.append("  class:  ").append(method.getDeclaringClass().getName()).append("\n");
            sb.append("  method: ").append(method.getName()).append("\n");

            Map<String, Object> toolDef = tools.stream()
                    .filter(t -> toolName.equals(t.get("name")))
                    .findFirst()
                    .orElse(null);

            if (toolDef != null) {
                String desc = (String) toolDef.get("description");
                if (desc != null && !desc.isEmpty()) {
                    sb.append("  ").append(desc).append("\n");
                }

                @SuppressWarnings("unchecked")
                Map<String, Object> schema = (Map<String, Object>) toolDef.get("inputSchema");
                if (schema != null) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> props = (Map<String, Object>) schema.get("properties");
                    if (props == null || props.isEmpty()) {
                        sb.append("  args: (none)\n");
                    } else {
                        for (Map.Entry<String, Object> propEntry : props.entrySet()) {
                            @SuppressWarnings("unchecked")
                            Map<String, Object> propSchema = (Map<String, Object>) propEntry.getValue();
                            String type = (String) propSchema.get("type");
                            String propDesc = (String) propSchema.get("description");
                            sb.append("  ").append(propEntry.getKey())
                                    .append(" (").append(type).append("): ")
                                    .append(propDesc).append("\n");
                        }
                    }
                }
            }
            sb.append("\n");
        }
        return sb.toString().trim();
    }

    /**
     * Assembles the full java_call tool description, combining the base usage guidance with
     * the auto-discovered method catalog (when methods are found in the configured packages).
     */
    private String buildJavaCallDescription(String catalog) {
        String base = "Generic BridgeService call. Accepts the full /call payload including call chaining, "
                + "instance methods, environment variables, and timeout. "
                + "Bundle all operations into one callContent chain so they share a single isolated "
                + "execution context. State (including authentication) does not persist between "
                + "separate tool calls.";
        if (catalog.isEmpty()) {
            return base;
        }
        return base + "\n\n" + catalog;
    }

    /**
     * Parses the IBS.MCP.PRECHAIN JSON string into an ordered map of CallContent entries.
     * Returns an empty map if the value is null, blank, or malformed (logs a warning in
     * the malformed case without printing the raw value to avoid leaking credentials).
     *
     * @param prechainJson the raw JSON string from IBS.MCP.PRECHAIN
     * @return ordered map of prechain call entries, never null
     */
    private Map<String, CallContent> parsePrechainJson(String prechainJson) {
        if (prechainJson == null || prechainJson.isBlank()) {
            return Collections.emptyMap();
        }
        try {
            return mapper.readValue(prechainJson,
                    new TypeReference<LinkedHashMap<String, CallContent>>() {});
        } catch (JsonProcessingException e) {
            log.warn("IBS.MCP.PRECHAIN could not be parsed — pre-chain skipped. Check the JSON syntax.");
            return Collections.emptyMap();
        }
    }

    @SuppressWarnings("unchecked")
    private String handleJavaCall(Object id, Map<String, Object> arguments, Map<String, String> headers) {
        try {
            // MCP clients may serialise complex object arguments as JSON strings rather than
            // nested objects. Unwrap callContent if it arrived as a pre-serialised string.
            Object cc = arguments.get("callContent");
            if (cc instanceof String) {
                arguments.put("callContent", mapper.readValue((String) cc, Object.class));
            }
            // Prepend PRECHAIN entries to callContent so java_call chains share the same
            // server-level setup (e.g. auth) as the rest of the catalog. PRECHAIN keys are
            // stripped from the result.
            Map<String, CallContent> prechain = parsePrechainJson(ConfigValueHandlerIBS.MCP_PRECHAIN.fetchValue());
            if (!prechain.isEmpty()) {
                Map<String, Object> callContent = (Map<String, Object>) arguments
                        .computeIfAbsent("callContent", k -> new LinkedHashMap<>());
                // Build a new map with prechain entries first, then the user's entries
                Map<String, Object> merged = new LinkedHashMap<>();
                for (Map.Entry<String, CallContent> e : prechain.entrySet()) {
                    merged.put(e.getKey(), mapper.convertValue(e.getValue(), Map.class));
                }
                merged.putAll(callContent);
                arguments.put("callContent", merged);
            }
            String json = mapper.writeValueAsString(arguments);
            JavaCalls calls = BridgeServiceFactory.createJavaCalls(json);
            calls.addHeaders(headers);
            JavaCallResults results = calls.submitCalls();
            prechain.keySet().forEach(k -> {
                results.getReturnValues().remove(k);
                results.getCallDurations().remove(k);
            });
            String resultJson = mapper.writeValueAsString(results);
            return buildCallToolResult(id, resultJson, false);
        } catch (Exception e) {
            log.debug("java_call tool failed: {}", e.getMessage());
            return buildCallToolResult(id, exceptionToErrorPayload(e), true);
        }
    }

    private String buildResult(Object id, Object result) {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("jsonrpc", JSONRPC_VERSION);
        response.put("id", id);
        response.put("result", result);
        return toJson(response);
    }

    private String buildError(Object id, int code, String message) {
        Map<String, Object> error = new LinkedHashMap<>();
        error.put("code", code);
        error.put("message", message);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("jsonrpc", JSONRPC_VERSION);
        response.put("id", id);
        response.put("error", error);
        return toJson(response);
    }

    private String buildCallToolResult(Object id, String text, boolean isError) {
        Map<String, Object> content = new LinkedHashMap<>();
        content.put("type", "text");
        content.put("text", text);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("content", Collections.singletonList(content));
        result.put("isError", isError);

        return buildResult(id, result);
    }

    /**
     * Converts an exception into a serialized ErrorObject payload, mirroring the error
     * structure returned by the /call endpoint so MCP clients receive the same level of
     * detail (originalException, originalMessage, failureAtStep, stackTrace, etc.).
     */
    private String exceptionToErrorPayload(Exception e) {
        return BridgeServiceFactory.createExceptionPayLoad(e);
    }

    private String toJson(Object obj) {
        try {
            return mapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialise MCP response", e);
            return "{\"jsonrpc\":\"2.0\",\"error\":{\"code\":-32603,\"message\":\"Internal serialisation error\"}}";
        }
    }
}
