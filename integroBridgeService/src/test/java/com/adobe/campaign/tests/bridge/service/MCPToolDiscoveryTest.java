/*
 * Copyright 2022 Adobe
 * All Rights Reserved.
 *
 * NOTICE: Adobe permits you to use, modify, and distribute this file in
 * accordance with the terms of the Adobe license agreement accompanying
 * it.
 */
package com.adobe.campaign.tests.bridge.service;

import com.adobe.campaign.tests.bridge.testdata.one.SimpleStaticMethods;
import org.testng.annotations.Test;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

public class MCPToolDiscoveryTest {

    private static final String TESTDATA_ONE_PACKAGE = "com.adobe.campaign.tests.bridge.testdata.one";

    // ---- discoverTools ----

    @Test
    public void testDiscoverTools_emptyPackages_returnsEmpty() {
        MCPToolDiscovery.DiscoveryResult result = MCPToolDiscovery.discoverTools("");
        assertThat(result.tools, is(empty()));
        assertThat(result.methodRegistry, is(anEmptyMap()));
    }

    @Test
    public void testDiscoverTools_nullPackages_returnsEmpty() {
        MCPToolDiscovery.DiscoveryResult result = MCPToolDiscovery.discoverTools(null);
        assertThat(result.tools, is(empty()));
        assertThat(result.methodRegistry, is(anEmptyMap()));
    }

    @Test
    public void testDiscoverTools_findsKnownStaticMethod() {
        MCPToolDiscovery.DiscoveryResult result = MCPToolDiscovery.discoverTools(TESTDATA_ONE_PACKAGE);

        // methodReturningString() is public static — must be discovered
        assertThat(result.methodRegistry.keySet(),
                hasItem("SimpleStaticMethods_methodReturningString"));
    }

    @Test
    public void testDiscoverTools_toolListMatchesRegistry() {
        MCPToolDiscovery.DiscoveryResult result = MCPToolDiscovery.discoverTools(TESTDATA_ONE_PACKAGE);

        // Every tool in the list must have a matching entry in the registry
        for (Map<String, Object> tool : result.tools) {
            String name = (String) tool.get("name");
            assertThat("Tool '" + name + "' missing from registry", result.methodRegistry, hasKey(name));
        }
        assertThat(result.tools.size(), equalTo(result.methodRegistry.size()));
    }

    @Test
    public void testDiscoverTools_toolHasRequiredFields() {
        MCPToolDiscovery.DiscoveryResult result = MCPToolDiscovery.discoverTools(TESTDATA_ONE_PACKAGE);

        Map<String, Object> tool = result.tools.stream()
                .filter(t -> "SimpleStaticMethods_methodReturningString".equals(t.get("name")))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Tool not found"));

        assertThat(tool, hasKey("name"));
        assertThat(tool, hasKey("description"));
        assertThat(tool, hasKey("inputSchema"));
        // Description is sourced from Javadoc — verify it reflects the actual comment, not the fallback
        assertThat((String) tool.get("description"), containsString("success string"));
    }

    @Test
    public void testDiscoverTools_nonStaticMethodExcluded() {
        MCPToolDiscovery.DiscoveryResult result = MCPToolDiscovery.discoverTools(TESTDATA_ONE_PACKAGE);

        // methodAcceptingStringAndArray is an instance method in SimpleStaticMethods (Issue #176)
        assertThat(result.methodRegistry.keySet(),
                not(hasItem("SimpleStaticMethods_methodAcceptingStringAndArray")));
    }

    @Test
    public void testDiscoverTools_ambiguousOverloadsSkipped() {
        MCPToolDiscovery.DiscoveryResult result = MCPToolDiscovery.discoverTools(TESTDATA_ONE_PACKAGE);

        // overLoadedMethod1Arg has two variants both with 1 parameter — must be skipped
        assertThat(result.methodRegistry.keySet(),
                not(hasItem("SimpleStaticMethods_overLoadedMethod1Arg_1")));
    }

    @Test
    public void testDiscoverTools_unambiguousOverloadsDisambiguated() {
        MCPToolDiscovery.DiscoveryResult result = MCPToolDiscovery.discoverTools(TESTDATA_ONE_PACKAGE);

        // methodThrowingException exists with 0 params (methodThrowsException is different name)
        // and with 2 params — these have different param counts so both should be registered
        // with suffix: SimpleStaticMethods_methodThrowingException_0 is NOT present because
        // methodThrowsException() is a distinct method name (0 params)
        // methodThrowingException(int, int) has 2 params and is the ONLY one with that count
        // So it should appear as SimpleStaticMethods_methodThrowingException_2 only if there
        // are multiple overloads by name. Let's check what we get:
        // - methodThrowsException() → unique by name → SimpleStaticMethods_methodThrowsException
        // - methodThrowingException(int, int) → unique by name → SimpleStaticMethods_methodThrowingException

        // Both are unique by name so they get simple names (no param count suffix)
        assertThat(result.methodRegistry.keySet(),
                hasItem("SimpleStaticMethods_methodThrowsException"));
        assertThat(result.methodRegistry.keySet(),
                hasItem("SimpleStaticMethods_methodThrowingException"));
    }

    @Test
    public void testDiscoverTools_trailingDotInPackageHandled() {
        // IBS config often uses trailing dots for package patterns
        MCPToolDiscovery.DiscoveryResult result =
                MCPToolDiscovery.discoverTools(TESTDATA_ONE_PACKAGE + ".");
        assertThat(result.methodRegistry.keySet(),
                hasItem("SimpleStaticMethods_methodReturningString"));
    }

    @Test
    public void testDiscoverTools_registryMethodMatchesClass() {
        MCPToolDiscovery.DiscoveryResult result = MCPToolDiscovery.discoverTools(TESTDATA_ONE_PACKAGE);

        Method m = result.methodRegistry.get("SimpleStaticMethods_methodReturningString");
        assertThat(m, notNullValue());
        assertThat(m.getDeclaringClass(), equalTo(SimpleStaticMethods.class));
        assertThat(m.getName(), equalTo("methodReturningString"));
    }

    // ---- buildInputSchema ----

    @Test
    public void testBuildInputSchema_noParams_emptyProperties() throws Exception {
        Method m = SimpleStaticMethods.class.getMethod("methodReturningString");
        Map<String, Object> schema = MCPToolDiscovery.buildInputSchema(m);

        assertThat(schema.get("type"), equalTo("object"));
        assertThat((Map<?, ?>) schema.get("properties"), is(anEmptyMap()));
        assertThat(schema, not(hasKey("required")));
    }

    @Test
    public void testBuildInputSchema_stringParam() throws Exception {
        Method m = SimpleStaticMethods.class.getMethod("methodAcceptingStringArgument", String.class);
        Map<String, Object> schema = MCPToolDiscovery.buildInputSchema(m);

        Map<?, ?> props = (Map<?, ?>) schema.get("properties");
        assertThat(props, hasKey("arg0"));
        assertThat(((Map<?, ?>) props.get("arg0")).get("type"), equalTo("string"));

        List<?> required = (List<?>) schema.get("required");
        assertThat(required, contains("arg0"));
    }

    @Test
    public void testBuildInputSchema_intParam() throws Exception {
        Method m = SimpleStaticMethods.class.getMethod("methodAcceptingIntArgument", int.class);
        Map<String, Object> schema = MCPToolDiscovery.buildInputSchema(m);

        Map<?, ?> props = (Map<?, ?>) schema.get("properties");
        assertThat(((Map<?, ?>) props.get("arg0")).get("type"), equalTo("integer"));
    }

    @Test
    public void testBuildInputSchema_twoParams_requiredListOrdered() throws Exception {
        Method m = SimpleStaticMethods.class.getMethod("methodAcceptingTwoArguments", String.class, String.class);
        Map<String, Object> schema = MCPToolDiscovery.buildInputSchema(m);

        List<?> required = (List<?>) schema.get("required");
        assertThat(required, contains("arg0", "arg1"));
    }

    @Test
    public void testBuildInputSchema_listParam() throws Exception {
        Method m = SimpleStaticMethods.class.getMethod("methodAcceptingListArguments", java.util.List.class);
        Map<String, Object> schema = MCPToolDiscovery.buildInputSchema(m);

        Map<?, ?> props = (Map<?, ?>) schema.get("properties");
        assertThat(((Map<?, ?>) props.get("arg0")).get("type"), equalTo("array"));
    }

    // ---- javaTypeToJsonSchemaType ----

    @Test
    public void testTypeMapping_primitives() {
        assertThat(MCPToolDiscovery.javaTypeToJsonSchemaType(String.class), equalTo("string"));
        assertThat(MCPToolDiscovery.javaTypeToJsonSchemaType(int.class), equalTo("integer"));
        assertThat(MCPToolDiscovery.javaTypeToJsonSchemaType(Integer.class), equalTo("integer"));
        assertThat(MCPToolDiscovery.javaTypeToJsonSchemaType(long.class), equalTo("integer"));
        assertThat(MCPToolDiscovery.javaTypeToJsonSchemaType(Long.class), equalTo("integer"));
        assertThat(MCPToolDiscovery.javaTypeToJsonSchemaType(double.class), equalTo("number"));
        assertThat(MCPToolDiscovery.javaTypeToJsonSchemaType(Double.class), equalTo("number"));
        assertThat(MCPToolDiscovery.javaTypeToJsonSchemaType(float.class), equalTo("number"));
        assertThat(MCPToolDiscovery.javaTypeToJsonSchemaType(boolean.class), equalTo("boolean"));
        assertThat(MCPToolDiscovery.javaTypeToJsonSchemaType(Boolean.class), equalTo("boolean"));
    }

    @Test
    public void testTypeMapping_collections() {
        assertThat(MCPToolDiscovery.javaTypeToJsonSchemaType(java.util.List.class), equalTo("array"));
        assertThat(MCPToolDiscovery.javaTypeToJsonSchemaType(String[].class), equalTo("array"));
    }

    @Test
    public void testTypeMapping_unknownType_returnsObject() {
        assertThat(MCPToolDiscovery.javaTypeToJsonSchemaType(java.io.File.class), equalTo("object"));
    }
}
