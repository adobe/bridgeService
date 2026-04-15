/*
 * Copyright 2022 Adobe
 * All Rights Reserved.
 *
 * NOTICE: Adobe permits you to use, modify, and distribute this file in
 * accordance with the terms of the Adobe license agreement accompanying
 * it.
 */
package com.adobe.campaign.tests.bridge.service;

import com.adobe.campaign.tests.bridge.service.exceptions.*;
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
 * - initialize       : MCP handshake
 * - tools/list       : returns all discovered public static methods as MCP tools
 * - tools/call       : invokes a discovered tool or the generic java_call fallback
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
    private final Map<String, Method> methodRegistry;

    /**
     * Constructs the handler and performs tool discovery from IBS.CLASSLOADER.STATIC.INTEGRITY.PACKAGES.
     */
    public MCPRequestHandler() {
        MCPToolDiscovery.DiscoveryResult discovery = MCPToolDiscovery.discoverTools(
                ConfigValueHandlerIBS.STATIC_INTEGRITY_PACKAGES.fetchValue());
        this.toolList = new ArrayList<>(discovery.tools);
        this.methodRegistry = new LinkedHashMap<>(discovery.methodRegistry);

        // Add the generic java_call fallback tool
        try {
            Map<String, Object> javaCallTool = new LinkedHashMap<>();
            javaCallTool.put("name", JAVA_CALL_TOOL_NAME);
            javaCallTool.put("description",
                    "Generic BridgeService call. Accepts the full /call payload including call chaining, "
                    + "instance methods, environment variables, and timeout. Use for scenarios not covered "
                    + "by the auto-discovered tools. "
                    + "Prefer this tool over individual auto-discovered tools for multi-step scenarios: "
                    + "bundle all operations into one callContent chain so they share a single isolated "
                    + "execution context. State (including authentication) does not persist between "
                    + "separate tool calls.");
            javaCallTool.put("inputSchema", mapper.readValue(JAVA_CALL_TOOL_SCHEMA, Map.class));
            this.toolList.add(javaCallTool);
        } catch (JsonProcessingException e) {
            log.error("Failed to parse java_call tool schema — the tool will not be available.", e);
        }

        log.info("MCPRequestHandler ready: {} auto-discovered tool(s) + java_call.", this.methodRegistry.size());
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

        Method method = methodRegistry.get(toolName);
        if (method == null) {
            return buildCallToolResult(id, "Unknown tool: " + toolName
                    + ". Use tools/list to see available tools.", true);
        }

        return invokeDiscoveredTool(id, method, arguments, headers);
    }

    private String invokeDiscoveredTool(Object id, Method method, Map<String, Object> arguments,
            Map<String, String> headers) {
        int paramCount = method.getParameterCount();
        Object[] args = new Object[paramCount];
        for (int i = 0; i < paramCount; i++) {
            args[i] = arguments.get("arg" + i);
        }

        JavaCalls calls = new JavaCalls();
        calls.addHeaders(headers);
        extractEnvHeaders(headers).forEach((k, v) -> calls.getEnvironmentVariables().setProperty(k, v));

        Map<String, CallContent> prechain = parsePrechainJson(ConfigValueHandlerIBS.MCP_PRECHAIN.fetchValue());
        Set<String> prechainKeys = new LinkedHashSet<>(prechain.keySet());
        calls.getCallContent().putAll(prechain);

        CallContent cc = new CallContent();
        cc.setClassName(method.getDeclaringClass().getName());
        cc.setMethodName(method.getName());
        cc.setArgs(args);
        calls.getCallContent().put("result", cc);

        try {
            JavaCallResults results = calls.submitCalls();
            prechainKeys.forEach(k -> {
                results.getReturnValues().remove(k);
                results.getCallDurations().remove(k);
            });
            return buildCallToolResult(id, mapper.writeValueAsString(results), false);
        } catch (Exception e) {
            log.debug("Tool call failed for {}.{}: {}", method.getDeclaringClass().getSimpleName(),
                    method.getName(), e.getMessage());
            return buildCallToolResult(id, exceptionToErrorPayload(e), true);
        }
    }

    /**
     * Extracts environment variable entries from the request headers using the configured
     * {@code IBS.MCP.ENV.HEADER.PREFIX} (default {@code ibs-env-}). The prefix is stripped
     * from each matching header name to produce the variable name.
     *
     * @param headers all request headers
     * @return ordered map of variable name → value, never null
     */
    private Map<String, String> extractEnvHeaders(Map<String, String> headers) {
        String prefix = ConfigValueHandlerIBS.MCP_ENV_HEADER_PREFIX.fetchValue();
        if (prefix == null || prefix.isBlank()) {
            return Collections.emptyMap();
        }
        String lowerPrefix = prefix.toLowerCase(java.util.Locale.ROOT);
        Map<String, String> result = new LinkedHashMap<>();
        headers.entrySet().stream()
                .filter(e -> e.getKey().toLowerCase(java.util.Locale.ROOT).startsWith(lowerPrefix))
                .forEach(e -> result.put(
                        e.getKey().substring(prefix.length()).toUpperCase(java.util.Locale.ROOT),
                        e.getValue()));
        return result;
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
            // Merge ibs-env-* headers into the environmentVariables map before serialising
            Map<String, String> envFromHeaders = extractEnvHeaders(headers);
            if (!envFromHeaders.isEmpty()) {
                Map<String, Object> envVars = (Map<String, Object>) arguments
                        .computeIfAbsent("environmentVariables", k -> new LinkedHashMap<>());
                envVars.putAll(envFromHeaders);
            }
            String json = mapper.writeValueAsString(arguments);
            JavaCalls calls = BridgeServiceFactory.createJavaCalls(json);
            JavaCallResults results = calls.submitCalls();
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
        String title;
        int code;
        boolean includeStackTrace = true;

        if (e instanceof IBSTimeOutException) {
            title = IntegroAPI.ERROR_CALL_TIMEOUT;
            code = 408;
            includeStackTrace = false;
        } else if (e instanceof NonExistentJavaObjectException) {
            title = IntegroAPI.ERROR_JAVA_OBJECT_NOT_FOUND;
            code = 404;
            includeStackTrace = false;
        } else if (e instanceof AmbiguousMethodException) {
            title = IntegroAPI.ERROR_AMBIGUOUS_METHOD;
            code = 404;
            includeStackTrace = false;
        } else if (e instanceof IBSConfigurationException) {
            title = IntegroAPI.ERROR_IBS_CONFIG;
            code = 500;
        } else if (e instanceof IBSRunTimeException) {
            title = IntegroAPI.ERROR_IBS_RUNTIME;
            code = 500;
        } else if (e instanceof TargetJavaMethodCallException) {
            title = IntegroAPI.ERROR_CALLING_JAVA_METHOD;
            code = 500;
        } else if (e instanceof JavaObjectInaccessibleException) {
            title = IntegroAPI.ERROR_JAVA_OBJECT_NOT_ACCESSIBLE;
            code = 404;
            includeStackTrace = false;
        } else {
            title = IntegroAPI.ERROR_IBS_INTERNAL;
            code = 500;
        }

        return BridgeServiceFactory.createExceptionPayLoad(new ErrorObject(e, title, code, includeStackTrace));
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
