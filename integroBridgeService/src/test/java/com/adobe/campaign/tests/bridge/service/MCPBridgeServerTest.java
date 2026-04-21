/*
 * Copyright 2022 Adobe
 * All Rights Reserved.
 *
 * NOTICE: Adobe permits you to use, modify, and distribute this file in
 * accordance with the terms of the Adobe license agreement accompanying
 * it.
 */
package com.adobe.campaign.tests.bridge.service;

import io.restassured.response.Response;
import org.hamcrest.Matchers;
import org.testng.annotations.AfterGroups;
import org.testng.annotations.BeforeGroups;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import spark.Spark;

import static io.restassured.RestAssured.given;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

/**
 * Integration tests for the MCP endpoint (POST /mcp).
 *
 * Follows the same in-process server lifecycle as E2ETests: @BeforeGroups starts Spark
 * with MCP enabled, REST-assured sends raw JSON-RPC 2.0 requests to /mcp, @AfterGroups
 * stops Spark. Tests run within the "MCP" group.
 */
public class MCPBridgeServerTest {

    public static final String MCP_ENDPOINT = "http://localhost:8080/mcp";
    private static final String TESTDATA_ONE_PACKAGE = "com.adobe.campaign.tests.bridge.testdata.one";
    private static final String CONTENT_TYPE_JSON = "application/json";

    @BeforeGroups(groups = "MCP")
    public void startMCPService() {
        ConfigValueHandlerIBS.STATIC_INTEGRITY_PACKAGES.activate(TESTDATA_ONE_PACKAGE);
        ConfigValueHandlerIBS.MCP_ENABLED.activate("true");
        IntegroAPI.startServices(8080);
        Spark.awaitInitialization();
    }

    @BeforeMethod
    public void resetConfigBetweenTests() {
        // Re-apply packages so each tools/call can load classes via the custom class loader.
        // The MCP server was started with these packages; we re-activate them after each
        // BeforeMethod reset so call execution continues to work.
        ConfigValueHandlerIBS.STATIC_INTEGRITY_PACKAGES.activate(TESTDATA_ONE_PACKAGE);
    }

    @AfterGroups(groups = "MCP", alwaysRun = true)
    public void stopMCPService() {
        ConfigValueHandlerIBS.resetAllValues();
        Spark.stop();
    }

    // ---- initialize handshake ----

    @Test(groups = "MCP")
    public void testInitialize_returnsProtocolVersion() {
        given()
                .contentType(CONTENT_TYPE_JSON)
                .body("{\"jsonrpc\":\"2.0\",\"id\":1,\"method\":\"initialize\","
                        + "\"params\":{\"protocolVersion\":\"2024-11-05\","
                        + "\"clientInfo\":{\"name\":\"test\",\"version\":\"1.0\"},"
                        + "\"capabilities\":{}}}")
        .when()
                .post(MCP_ENDPOINT)
        .then()
                .statusCode(200)
                .body("jsonrpc", equalTo("2.0"))
                .body("result.protocolVersion", equalTo("2024-11-05"))
                .body("result.serverInfo.name", equalTo("bridgeService"))
                .body("result.capabilities.tools", notNullValue());
    }

    // ---- tools/list ----

    @Test(groups = "MCP")
    public void testToolsList_returnsDiscoveredTools() {
        given()
                .contentType(CONTENT_TYPE_JSON)
                .body("{\"jsonrpc\":\"2.0\",\"id\":2,\"method\":\"tools/list\",\"params\":{}}")
        .when()
                .post(MCP_ENDPOINT)
        .then()
                .statusCode(200)
                .body("result.tools.name", hasItems("java_call", "ibs_diagnostics"))
                .body("result.tools.name", hasItem("SimpleStaticMethods_methodReturningString"));
    }

    @Test(groups = "MCP")
    public void testToolsList_eachToolHasRequiredFields() {
        given()
                .contentType(CONTENT_TYPE_JSON)
                .body("{\"jsonrpc\":\"2.0\",\"id\":3,\"method\":\"tools/list\",\"params\":{}}")
        .when()
                .post(MCP_ENDPOINT)
        .then()
                .statusCode(200)
                .body("result.tools[0].name", notNullValue())
                .body("result.tools[0].description", notNullValue())
                .body("result.tools[0].inputSchema", notNullValue());
    }

    @Test(groups = "MCP")
    public void testToolsList_descriptionComesFromJavadoc() {
        // The individual tool for methodReturningString uses its Javadoc text, not the
        // fallback "Calls com.example.MyClass.methodName()" string.
        given()
                .contentType(CONTENT_TYPE_JSON)
                .body("{\"jsonrpc\":\"2.0\",\"id\":12,\"method\":\"tools/list\",\"params\":{}}")
        .when()
                .post(MCP_ENDPOINT)
        .then()
                .statusCode(200)
                .body("result.tools.find { it.name == 'SimpleStaticMethods_methodReturningString' }.description",
                        containsString("success string"));
    }

    @Test(groups = "MCP")
    public void testToolsList_noArgToolHasEmptyProperties() {
        given()
                .contentType(CONTENT_TYPE_JSON)
                .body("{\"jsonrpc\":\"2.0\",\"id\":4,\"method\":\"tools/list\",\"params\":{}}")
        .when()
                .post(MCP_ENDPOINT)
        .then()
                .statusCode(200)
                .body("result.tools.name", hasItem("SimpleStaticMethods_methodReturningString"))
                .body("result.tools.find { it.name == 'SimpleStaticMethods_methodReturningString' }.inputSchema.required",
                        nullValue());
    }

    @Test(groups = "MCP")
    public void testToolsList_undocumentedMethodExcluded() {
        // EnvironmentVariableHandler methods have no Javadoc — must be absent from the
        // tools list (IBS.MCP.REQUIRE_JAVADOC defaults to true).
        given()
                .contentType(CONTENT_TYPE_JSON)
                .body("{\"jsonrpc\":\"2.0\",\"id\":13,\"method\":\"tools/list\",\"params\":{}}")
        .when()
                .post(MCP_ENDPOINT)
        .then()
                .statusCode(200)
                .body("result.tools.name", hasItems("java_call", "ibs_diagnostics"))
                .body("result.tools.name", not(hasItem("EnvironmentVariableHandler_getCacheProperty")))
                .body("result.tools.name", not(hasItem("EnvironmentVariableHandler_setIntegroCache")));
    }

    // ---- ibs_diagnostics tool ----

    @Test(groups = "MCP")
    public void testToolsList_includesDiagnosticsTool() {
        given()
                .contentType(CONTENT_TYPE_JSON)
                .body("{\"jsonrpc\":\"2.0\",\"id\":50,\"method\":\"tools/list\",\"params\":{}}")
        .when()
                .post(MCP_ENDPOINT)
        .then()
                .statusCode(200)
                .body("result.tools.name", hasItems("java_call", "ibs_diagnostics"))
                .body("result.tools.find { it.name == 'ibs_diagnostics' }.description", notNullValue())
                .body("result.tools.find { it.name == 'ibs_diagnostics' }.inputSchema", notNullValue());
    }

    @Test(groups = "MCP")
    public void testDiagnosticsTool_basicCall_returnsExpectedFields() {
        given()
                .contentType(CONTENT_TYPE_JSON)
                .body("{\"jsonrpc\":\"2.0\",\"id\":51,\"method\":\"tools/call\","
                        + "\"params\":{\"name\":\"ibs_diagnostics\",\"arguments\":{}}}")
        .when()
                .post(MCP_ENDPOINT)
        .then()
                .statusCode(200)
                .body("result.isError", equalTo(false))
                .body("result.content[0].type", equalTo("text"))
                .body("result.content[0].text", containsString("ibsVersion"))
                .body("result.content[0].text", containsString("deploymentMode"))
                .body("result.content[0].text", containsString("mcpConfig"))
                .body("result.content[0].text", containsString("headers"))
                .body("result.content[0].text", containsString("discoveredToolCount"));
    }

    @Test(groups = "MCP")
    public void testDiagnosticsTool_mcpConfigReflectsCurrentState() {
        Response resp = given()
                .contentType(CONTENT_TYPE_JSON)
                .body("{\"jsonrpc\":\"2.0\",\"id\":52,\"method\":\"tools/call\","
                        + "\"params\":{\"name\":\"ibs_diagnostics\",\"arguments\":{}}}")
        .when()
                .post(MCP_ENDPOINT)
        .then()
                .statusCode(200)
                .extract().response();

        String text = resp.path("result.content[0].text");
        assertThat(text, containsString("packagesConfigured"));
        assertThat(text, containsString(TESTDATA_ONE_PACKAGE));
        assertThat(text, containsString("\"prechainActive\":false"));
        assertThat(text, containsString("\"javadocRequired\":true"));
    }

    @Test(groups = "MCP")
    public void testDiagnosticsTool_secretHeaders_keysReportedValuesAbsent() {
        given()
                .contentType(CONTENT_TYPE_JSON)
                .header("ibs-secret-api-key", "MY_SECRET_VALUE")
                .header("ibs-secret-token", "ANOTHER_SECRET")
                .body("{\"jsonrpc\":\"2.0\",\"id\":53,\"method\":\"tools/call\","
                        + "\"params\":{\"name\":\"ibs_diagnostics\",\"arguments\":{}}}")
        .when()
                .post(MCP_ENDPOINT)
        .then()
                .statusCode(200)
                .body("result.isError", equalTo(false))
                .body("result.content[0].text", containsString("ibs-secret-api-key"))
                .body("result.content[0].text", containsString("ibs-secret-token"))
                .body("result.content[0].text", not(containsString("MY_SECRET_VALUE")))
                .body("result.content[0].text", not(containsString("ANOTHER_SECRET")));
    }

    @Test(groups = "MCP")
    public void testDiagnosticsTool_envVarHeaders_decodedNamesAndValuesReported() {
        given()
                .contentType(CONTENT_TYPE_JSON)
                .header("ibs-env-AC.UITEST.HOST", "example.com")
                .header("ibs-env-DEPLOY_ENV", "stage")
                .body("{\"jsonrpc\":\"2.0\",\"id\":54,\"method\":\"tools/call\","
                        + "\"params\":{\"name\":\"ibs_diagnostics\",\"arguments\":{}}}")
        .when()
                .post(MCP_ENDPOINT)
        .then()
                .statusCode(200)
                .body("result.isError", equalTo(false))
                .body("result.content[0].text", containsString("AC.UITEST.HOST"))
                .body("result.content[0].text", containsString("DEPLOY_ENV"))
                .body("result.content[0].text", containsString("example.com"))
                .body("result.content[0].text", containsString("stage"));
    }

    @Test(groups = "MCP")
    public void testDiagnosticsTool_noSpecialHeaders_emptyLists() {
        given()
                .contentType(CONTENT_TYPE_JSON)
                .body("{\"jsonrpc\":\"2.0\",\"id\":55,\"method\":\"tools/call\","
                        + "\"params\":{\"name\":\"ibs_diagnostics\",\"arguments\":{}}}")
        .when()
                .post(MCP_ENDPOINT)
        .then()
                .statusCode(200)
                .body("result.isError", equalTo(false))
                .body("result.content[0].text", containsString("\"secretHeaderKeys\":[]"))
                .body("result.content[0].text", containsString("\"envVarHeaders\":{}"));
    }

    @Test(groups = "MCP")
    public void testDiagnosticsTool_discoveredToolCount_isPositive() {
        Response resp = given()
                .contentType(CONTENT_TYPE_JSON)
                .body("{\"jsonrpc\":\"2.0\",\"id\":56,\"method\":\"tools/call\","
                        + "\"params\":{\"name\":\"ibs_diagnostics\",\"arguments\":{}}}")
        .when()
                .post(MCP_ENDPOINT)
        .then()
                .statusCode(200)
                .body("result.isError", equalTo(false))
                .extract().response();

        String text = resp.path("result.content[0].text");
        assertThat(text, containsString("\"discoveredToolCount\""));
        assertThat(text, not(containsString("\"discoveredToolCount\":0")));
    }

    @Test(groups = "MCP")
    public void testDiagnosticsTool_prechainActive_reflectsConfig() {
        ConfigValueHandlerIBS.MCP_PRECHAIN.activate(
                "{\"ibs_pre\":{\"class\":\"com.adobe.campaign.tests.bridge.testdata.one.SimpleStaticMethods\","
                + "\"method\":\"methodReturningString\",\"args\":[]}}");
        try {
            given()
                    .contentType(CONTENT_TYPE_JSON)
                    .body("{\"jsonrpc\":\"2.0\",\"id\":57,\"method\":\"tools/call\","
                            + "\"params\":{\"name\":\"ibs_diagnostics\",\"arguments\":{}}}")
            .when()
                    .post(MCP_ENDPOINT)
            .then()
                    .statusCode(200)
                    .body("result.isError", equalTo(false))
                    .body("result.content[0].text", containsString("\"prechainActive\":true"));
        } finally {
            ConfigValueHandlerIBS.MCP_PRECHAIN.reset();
        }
    }

    @Test(groups = "MCP")
    public void testDiagnosticsTool_prechainActive_trueWhenProvidedViaHeader() {
        given()
                .contentType(CONTENT_TYPE_JSON)
                .header(MCPRequestHandler.PRECHAIN_HEADER,
                        "{\"ibs_auth\":{\"class\":\"utils.CampaignUtils\","
                        + "\"method\":\"setCurrentAuthenticationToLocal\","
                        + "\"args\":[\"ibs-secret-url\",\"ibs-secret-login\",\"ibs-secret-pass\"]}}")
                .body("{\"jsonrpc\":\"2.0\",\"id\":58,\"method\":\"tools/call\","
                        + "\"params\":{\"name\":\"ibs_diagnostics\",\"arguments\":{}}}")
        .when()
                .post(MCP_ENDPOINT)
        .then()
                .statusCode(200)
                .body("result.isError", equalTo(false))
                .body("result.content[0].text", containsString("\"prechainActive\":true"));
    }

    // ---- type handling ----

    @Test(groups = "MCP")
    public void testJavaCall_intArgument_jsonIntegerSucceeds() {
        // A JSON integer (42) must be accepted by a method expecting int.
        // Jackson deserialises it as Integer; Java reflection widens Integer → int.
        // Expected return: 42 * 3 = 126.
        given()
                .contentType(CONTENT_TYPE_JSON)
                .body("{\"jsonrpc\":\"2.0\",\"id\":70,\"method\":\"tools/call\","
                        + "\"params\":{\"name\":\"java_call\","
                        + "\"arguments\":{"
                        + "\"callContent\":{"
                        + "\"result\":{"
                        + "\"class\":\"com.adobe.campaign.tests.bridge.testdata.one.SimpleStaticMethods\","
                        + "\"method\":\"methodAcceptingIntArgument\","
                        + "\"args\":[42]}}}}}")
        .when()
                .post(MCP_ENDPOINT)
        .then()
                .statusCode(200)
                .body("result.isError", equalTo(false))
                .body("result.content[0].text", containsString("126"));
    }

    @Test(groups = "MCP")
    public void testJavaCall_intArgument_jsonStringCoerced() {
        // A JSON string "42" is coerced to int by castArgs before invocation.
        // Expected return: 42 * 3 = 126.
        given()
                .contentType(CONTENT_TYPE_JSON)
                .body("{\"jsonrpc\":\"2.0\",\"id\":71,\"method\":\"tools/call\","
                        + "\"params\":{\"name\":\"java_call\","
                        + "\"arguments\":{"
                        + "\"callContent\":{"
                        + "\"result\":{"
                        + "\"class\":\"com.adobe.campaign.tests.bridge.testdata.one.SimpleStaticMethods\","
                        + "\"method\":\"methodAcceptingIntArgument\","
                        + "\"args\":[\"42\"]}}}}}")
        .when()
                .post(MCP_ENDPOINT)
        .then()
                .statusCode(200)
                .body("result.isError", equalTo(false))
                .body("result.content[0].text", containsString("126"));
    }

    @Test(groups = "MCP")
    public void testJavaCall_intArgument_unparsableStringFails() {
        // A JSON string that cannot be parsed as int ("hello") → isError with a structured
        // ErrorObject that names the problematic value and target type.
        given()
                .contentType(CONTENT_TYPE_JSON)
                .body("{\"jsonrpc\":\"2.0\",\"id\":72,\"method\":\"tools/call\","
                        + "\"params\":{\"name\":\"java_call\","
                        + "\"arguments\":{"
                        + "\"callContent\":{"
                        + "\"result\":{"
                        + "\"class\":\"com.adobe.campaign.tests.bridge.testdata.one.SimpleStaticMethods\","
                        + "\"method\":\"methodAcceptingIntArgument\","
                        + "\"args\":[\"hello\"]}}}}}")
        .when()
                .post(MCP_ENDPOINT)
        .then()
                .statusCode(200)
                .body("result.isError", equalTo(true))
                .body("result.content[0].text", containsString("\"title\""))
                .body("result.content[0].text", containsString("hello"));
    }

    // ---- tools/call (via java_call) ----

    @Test(groups = "MCP")
    public void testToolsCall_noArgMethod_returnsResult() {
        given()
                .contentType(CONTENT_TYPE_JSON)
                .body("{\"jsonrpc\":\"2.0\",\"id\":5,\"method\":\"tools/call\","
                        + "\"params\":{\"name\":\"java_call\","
                        + "\"arguments\":{"
                        + "\"callContent\":{"
                        + "\"result\":{"
                        + "\"class\":\"com.adobe.campaign.tests.bridge.testdata.one.SimpleStaticMethods\","
                        + "\"method\":\"methodReturningString\","
                        + "\"args\":[]}}}}}")
        .when()
                .post(MCP_ENDPOINT)
        .then()
                .statusCode(200)
                .body("result.isError", equalTo(false))
                .body("result.content[0].type", equalTo("text"))
                .body("result.content[0].text", containsString("_Success"));
    }

    @Test(groups = "MCP")
    public void testToolsCall_withStringArg_returnsResult() {
        given()
                .contentType(CONTENT_TYPE_JSON)
                .body("{\"jsonrpc\":\"2.0\",\"id\":6,\"method\":\"tools/call\","
                        + "\"params\":{\"name\":\"java_call\","
                        + "\"arguments\":{"
                        + "\"callContent\":{"
                        + "\"result\":{"
                        + "\"class\":\"com.adobe.campaign.tests.bridge.testdata.one.SimpleStaticMethods\","
                        + "\"method\":\"methodAcceptingStringArgument\","
                        + "\"args\":[\"hello\"]}}}}}")
        .when()
                .post(MCP_ENDPOINT)
        .then()
                .statusCode(200)
                .body("result.isError", equalTo(false))
                .body("result.content[0].text", containsString("hello"))
                .body("result.content[0].text", containsString("_Success"));
    }

    @Test(groups = "MCP")
    public void testToolsCall_unknownTool_returnsIsError() {
        given()
                .contentType(CONTENT_TYPE_JSON)
                .body("{\"jsonrpc\":\"2.0\",\"id\":7,\"method\":\"tools/call\","
                        + "\"params\":{\"name\":\"NonExistent_toolName\","
                        + "\"arguments\":{}}}")
        .when()
                .post(MCP_ENDPOINT)
        .then()
                .statusCode(200)
                .body("result.isError", equalTo(true))
                .body("result.content[0].text", containsString("NonExistent_toolName"));
    }

    @Test(groups = "MCP")
    public void testToolsCall_methodThrowsException_returnsIsError() {
        given()
                .contentType(CONTENT_TYPE_JSON)
                .body("{\"jsonrpc\":\"2.0\",\"id\":8,\"method\":\"tools/call\","
                        + "\"params\":{\"name\":\"java_call\","
                        + "\"arguments\":{"
                        + "\"callContent\":{"
                        + "\"result\":{"
                        + "\"class\":\"com.adobe.campaign.tests.bridge.testdata.one.SimpleStaticMethods\","
                        + "\"method\":\"methodThrowsException\","
                        + "\"args\":[]}}}}}")
        .when()
                .post(MCP_ENDPOINT)
        .then()
                .statusCode(200)
                .body("result.isError", equalTo(true))
                // Error text must be a full ErrorObject JSON, not a bare exception string
                .body("result.content[0].text", containsString("\"title\""))
                .body("result.content[0].text", containsString("\"detail\""))
                .body("result.content[0].text", containsString("\"originalException\""));
    }

    @Test(groups = "MCP")
    public void testToolsCall_timeout_returnsIsError() {
        // methodWithTimeOut sleeps for the given ms; set a short IBS timeout
        ConfigValueHandlerIBS.DEFAULT_CALL_TIMEOUT.activate("500");

        given()
                .contentType(CONTENT_TYPE_JSON)
                .body("{\"jsonrpc\":\"2.0\",\"id\":9,\"method\":\"tools/call\","
                        + "\"params\":{\"name\":\"java_call\","
                        + "\"arguments\":{"
                        + "\"callContent\":{"
                        + "\"result\":{"
                        + "\"class\":\"com.adobe.campaign.tests.bridge.testdata.one.SimpleStaticMethods\","
                        + "\"method\":\"methodWithTimeOut\","
                        + "\"args\":[5000]}}}}}")
        .when()
                .post(MCP_ENDPOINT)
        .then()
                .statusCode(200)
                .body("result.isError", equalTo(true))
                .body("result.content[0].text", containsString("\"title\""))
                .body("result.content[0].text", containsString("timeout"));
    }

    // ---- tools/call (java_call fallback) ----

    @Test(groups = "MCP")
    public void testJavaCallTool_basicCall_returnsResult() {
        String payload = "{\"jsonrpc\":\"2.0\",\"id\":10,\"method\":\"tools/call\","
                + "\"params\":{\"name\":\"java_call\","
                + "\"arguments\":{"
                + "\"callContent\":{"
                + "\"result\":{"
                + "\"class\":\"com.adobe.campaign.tests.bridge.testdata.one.SimpleStaticMethods\","
                + "\"method\":\"methodReturningString\","
                + "\"args\":[]"
                + "}}}}}";

        given()
                .contentType(CONTENT_TYPE_JSON)
                .body(payload)
        .when()
                .post(MCP_ENDPOINT)
        .then()
                .statusCode(200)
                .body("result.isError", equalTo(false))
                .body("result.content[0].text", containsString("_Success"));
    }

    @Test(groups = "MCP")
    public void testJavaCallTool_callContentAsString_isUnwrapped() {
        // MCP clients may serialise the callContent object as a JSON string rather than a
        // nested object. The handler must unwrap it and execute the call normally.
        String callContentJson = "{\\\"result\\\":{\\\"class\\\":\\\"com.adobe.campaign.tests.bridge.testdata.one.SimpleStaticMethods\\\","
                + "\\\"method\\\":\\\"methodReturningString\\\",\\\"args\\\":[]}}";
        String payload = "{\"jsonrpc\":\"2.0\",\"id\":30,\"method\":\"tools/call\","
                + "\"params\":{\"name\":\"java_call\","
                + "\"arguments\":{\"callContent\":\"" + callContentJson + "\"}}}";

        given()
                .contentType(CONTENT_TYPE_JSON)
                .body(payload)
        .when()
                .post(MCP_ENDPOINT)
        .then()
                .statusCode(200)
                .body("result.isError", equalTo(false))
                .body("result.content[0].text", containsString("_Success"));
    }

    // ---- unknown JSON-RPC method ----

    @Test(groups = "MCP")
    public void testUnknownMethod_returnsJsonRpcError() {
        given()
                .contentType(CONTENT_TYPE_JSON)
                .body("{\"jsonrpc\":\"2.0\",\"id\":11,\"method\":\"unknown/method\",\"params\":{}}")
        .when()
                .post(MCP_ENDPOINT)
        .then()
                .statusCode(200)
                .body("error.code", equalTo(-32601))
                .body("error.message", containsString("unknown/method"));
    }

    // ---- regression: existing /call endpoint unaffected ----

    @Test(groups = "MCP")
    public void testExistingCallEndpoint_stillWorks() {
        String payload = "{\"callContent\":{\"step1\":{"
                + "\"class\":\"com.adobe.campaign.tests.bridge.testdata.one.SimpleStaticMethods\","
                + "\"method\":\"methodReturningString\",\"args\":[]}}}";

        given()
                .contentType(CONTENT_TYPE_JSON)
                .body(payload)
        .when()
                .post("http://localhost:8080/call")
        .then()
                .statusCode(200)
                .body("returnValues.step1", Matchers.equalTo("_Success"));
    }

    // ---- IBS.MCP.PRECHAIN ----

    @Test(groups = "MCP")
    public void testPrechain_isExecutedAndResultStripped() {
        // Pre-chain runs a no-arg method; its key must be absent from returnValues,
        // while the actual call result ("result") must be present.
        ConfigValueHandlerIBS.MCP_PRECHAIN.activate(
                "{\"ibs_pre\":{\"class\":\"com.adobe.campaign.tests.bridge.testdata.one.SimpleStaticMethods\","
                + "\"method\":\"methodReturningString\",\"args\":[]}}");

        given()
                .contentType(CONTENT_TYPE_JSON)
                .body("{\"jsonrpc\":\"2.0\",\"id\":20,\"method\":\"tools/call\","
                        + "\"params\":{\"name\":\"java_call\","
                        + "\"arguments\":{"
                        + "\"callContent\":{"
                        + "\"result\":{"
                        + "\"class\":\"com.adobe.campaign.tests.bridge.testdata.one.SimpleStaticMethods\","
                        + "\"method\":\"methodReturningString\","
                        + "\"args\":[]}}}}}}")
        .when()
                .post(MCP_ENDPOINT)
        .then()
                .statusCode(200)
                .body("result.isError", equalTo(false))
                .body("result.content[0].text", not(containsString("\"ibs_pre\"")))
                .body("result.content[0].text", containsString("\"result\""));

        ConfigValueHandlerIBS.MCP_PRECHAIN.reset();
    }

    @Test(groups = "MCP")
    public void testPrechain_dependencyResolutionBetweenPrechainSteps() {
        // Second pre-chain entry references the first by key — both must execute without error.
        ConfigValueHandlerIBS.MCP_PRECHAIN.activate(
                "{\"ibs_pre1\":{\"class\":\"com.adobe.campaign.tests.bridge.testdata.one.SimpleStaticMethods\","
                + "\"method\":\"methodReturningString\",\"args\":[]},"
                + "\"ibs_pre2\":{\"class\":\"com.adobe.campaign.tests.bridge.testdata.one.SimpleStaticMethods\","
                + "\"method\":\"methodAcceptingStringArgument\",\"args\":[\"ibs_pre1\"]}}");

        given()
                .contentType(CONTENT_TYPE_JSON)
                .body("{\"jsonrpc\":\"2.0\",\"id\":21,\"method\":\"tools/call\","
                        + "\"params\":{\"name\":\"java_call\","
                        + "\"arguments\":{"
                        + "\"callContent\":{"
                        + "\"result\":{"
                        + "\"class\":\"com.adobe.campaign.tests.bridge.testdata.one.SimpleStaticMethods\","
                        + "\"method\":\"methodReturningString\","
                        + "\"args\":[]}}}}}}")
        .when()
                .post(MCP_ENDPOINT)
        .then()
                .statusCode(200)
                .body("result.isError", equalTo(false));

        ConfigValueHandlerIBS.MCP_PRECHAIN.reset();
    }

    @Test(groups = "MCP")
    public void testPrechain_secretHeaderArgIsResolved() {
        // A prechain arg that matches an ibs-secret-* request header is resolved to the
        // header value via the existing expandArgs mechanism (no error expected).
        ConfigValueHandlerIBS.MCP_PRECHAIN.activate(
                "{\"ibs_pre\":{\"class\":\"com.adobe.campaign.tests.bridge.testdata.one.SimpleStaticMethods\","
                + "\"method\":\"methodAcceptingStringArgument\",\"args\":[\"ibs-secret-test-val\"]}}");

        given()
                .contentType(CONTENT_TYPE_JSON)
                .header("ibs-secret-test-val", "RESOLVED")
                .body("{\"jsonrpc\":\"2.0\",\"id\":22,\"method\":\"tools/call\","
                        + "\"params\":{\"name\":\"java_call\","
                        + "\"arguments\":{"
                        + "\"callContent\":{"
                        + "\"result\":{"
                        + "\"class\":\"com.adobe.campaign.tests.bridge.testdata.one.SimpleStaticMethods\","
                        + "\"method\":\"methodReturningString\","
                        + "\"args\":[]}}}}}}")
        .when()
                .post(MCP_ENDPOINT)
        .then()
                .statusCode(200)
                .body("result.isError", equalTo(false))
                .body("result.content[0].text", not(containsString("ibs-secret-test-val")));

        ConfigValueHandlerIBS.MCP_PRECHAIN.reset();
    }

    @Test(groups = "MCP")
    public void testPrechain_malformedJsonIsSkippedGracefully() {
        ConfigValueHandlerIBS.MCP_PRECHAIN.activate("not valid json {{{");

        given()
                .contentType(CONTENT_TYPE_JSON)
                .body("{\"jsonrpc\":\"2.0\",\"id\":23,\"method\":\"tools/call\","
                        + "\"params\":{\"name\":\"java_call\","
                        + "\"arguments\":{"
                        + "\"callContent\":{"
                        + "\"result\":{"
                        + "\"class\":\"com.adobe.campaign.tests.bridge.testdata.one.SimpleStaticMethods\","
                        + "\"method\":\"methodReturningString\","
                        + "\"args\":[]}}}}}}")
        .when()
                .post(MCP_ENDPOINT)
        .then()
                .statusCode(200)
                .body("result.isError", equalTo(false))
                .body("result.content[0].text", containsString("_Success"));

        ConfigValueHandlerIBS.MCP_PRECHAIN.reset();
    }

    @Test(groups = "MCP")
    public void testPrechain_appliedToJavaCall_andResultStripped() {
        // PRECHAIN must run before the user's java_call chain, and its keys must be
        // stripped from the result — consistent with auto-discovered tool behaviour.
        ConfigValueHandlerIBS.MCP_PRECHAIN.activate(
                "{\"ibs_pre\":{\"class\":\"com.adobe.campaign.tests.bridge.testdata.one.SimpleStaticMethods\","
                + "\"method\":\"methodReturningString\",\"args\":[]}}");

        String payload = "{\"jsonrpc\":\"2.0\",\"id\":24,\"method\":\"tools/call\","
                + "\"params\":{\"name\":\"java_call\","
                + "\"arguments\":{"
                + "\"callContent\":{"
                + "\"result\":{"
                + "\"class\":\"com.adobe.campaign.tests.bridge.testdata.one.SimpleStaticMethods\","
                + "\"method\":\"methodReturningString\","
                + "\"args\":[]"
                + "}}}}}";

        given()
                .contentType(CONTENT_TYPE_JSON)
                .body(payload)
        .when()
                .post(MCP_ENDPOINT)
        .then()
                .statusCode(200)
                .body("result.isError", equalTo(false))
                .body("result.content[0].text", not(containsString("\"ibs_pre\"")))
                .body("result.content[0].text", containsString("\"result\""));

        ConfigValueHandlerIBS.MCP_PRECHAIN.reset();
    }

    @Test(groups = "MCP")
    public void testPrechain_javaCallCanReferenceToPrechain() {
        // A java_call step can reference a PRECHAIN step's result by key — enabling
        // the auth-object pattern where PRECHAIN establishes auth and the chain passes it along.
        ConfigValueHandlerIBS.MCP_PRECHAIN.activate(
                "{\"ibs_pre\":{\"class\":\"com.adobe.campaign.tests.bridge.testdata.one.SimpleStaticMethods\","
                + "\"method\":\"methodReturningString\",\"args\":[]}}");

        // The user's step references "ibs_pre" — BridgeService substitutes the return value.
        String payload = "{\"jsonrpc\":\"2.0\",\"id\":26,\"method\":\"tools/call\","
                + "\"params\":{\"name\":\"java_call\","
                + "\"arguments\":{"
                + "\"callContent\":{"
                + "\"result\":{"
                + "\"class\":\"com.adobe.campaign.tests.bridge.testdata.one.SimpleStaticMethods\","
                + "\"method\":\"methodAcceptingStringArgument\","
                + "\"args\":[\"ibs_pre\"]"
                + "}}}}}";

        given()
                .contentType(CONTENT_TYPE_JSON)
                .body(payload)
        .when()
                .post(MCP_ENDPOINT)
        .then()
                .statusCode(200)
                .body("result.isError", equalTo(false))
                // "ibs_pre" resolved to "_Success"; methodAcceptingStringArgument appends "_Success"
                .body("result.content[0].text", containsString("_Success_Success"));

        ConfigValueHandlerIBS.MCP_PRECHAIN.reset();
    }

    @Test(groups = "MCP")
    public void testJavaCallTool_callChaining_complexObjectPassedByReference() {
        // Proves that java_call call chaining works end-to-end through the MCP HTTP layer:
        // step A returns a List<MimeMessage> (a complex, non-JSON-serializable object);
        // step B receives it by reference inside the same classloader context and extracts subjects.
        // This mirrors the chainingComplexCalls unit test in TestFetchCalls but exercises
        // the full JSON-RPC 2.0 → MCPRequestHandler → JavaCalls.submitCalls() path.
        String payload = "{\"jsonrpc\":\"2.0\",\"id\":50,\"method\":\"tools/call\","
                + "\"params\":{\"name\":\"java_call\","
                + "\"arguments\":{"
                + "\"callContent\":{"
                + "\"fetchMessages\":{"
                + "\"class\":\"com.adobe.campaign.tests.bridge.testdata.one.MimeMessageMethods\","
                + "\"method\":\"fetchMessages\","
                + "\"args\":[\"mcpChain\",4]"
                + "},"
                + "\"fetchSubjects\":{"
                + "\"class\":\"com.adobe.campaign.tests.bridge.testdata.one.MimeMessageMethods\","
                + "\"method\":\"fetchMessageSubjects\","
                + "\"args\":[\"fetchMessages\"]"
                + "}}}}}";

        given()
                .contentType(CONTENT_TYPE_JSON)
                .body(payload)
        .when()
                .post(MCP_ENDPOINT)
        .then()
                .statusCode(200)
                .body("result.isError", equalTo(false))
                .body("result.content[0].text", containsString("fetchSubjects"))
                .body("result.content[0].text", containsString("mcpChain_3"));
    }

    @Test(groups = "MCP")
    public void testJavaCall_descriptionMentionsCallChaining() {
        // The java_call tool description must contain guidance about call chaining.
        given()
                .contentType(CONTENT_TYPE_JSON)
                .body("{\"jsonrpc\":\"2.0\",\"id\":25,\"method\":\"tools/list\",\"params\":{}}")
        .when()
                .post(MCP_ENDPOINT)
        .then()
                .statusCode(200)
                .body("result.tools.find { it.name == 'java_call' }.description",
                        containsString("isolated"));
    }

    // ---- ibs-env-* header injection ----

    @Test(groups = "MCP")
    public void testToolsCall_envHeadersInjected_discoveredTool() {
        // Configure the env-var setter to the in-package EnvironmentVariableHandler so the
        // test can verify that ENVVAR1 and ENVVAR2 were injected into the call context.
        ConfigValueHandlerIBS.ENVIRONMENT_VARS_SETTER_CLASS.activate(
                "com.adobe.campaign.tests.bridge.testdata.one.EnvironmentVariableHandler");
        ConfigValueHandlerIBS.ENVIRONMENT_VARS_SETTER_METHOD.activate("setIntegroCache");

        given()
                .contentType(CONTENT_TYPE_JSON)
                .header("ibs-env-ENVVAR1", "hello")
                .header("ibs-env-ENVVAR2", "world")
                .body("{\"jsonrpc\":\"2.0\",\"id\":40,\"method\":\"tools/call\","
                        + "\"params\":{\"name\":\"java_call\","
                        + "\"arguments\":{"
                        + "\"callContent\":{"
                        + "\"result\":{"
                        + "\"class\":\"com.adobe.campaign.tests.bridge.testdata.one.SimpleStaticMethods\","
                        + "\"method\":\"usesEnvironmentVariables\","
                        + "\"args\":[]}}}}}}")
        .when()
                .post(MCP_ENDPOINT)
        .then()
                .statusCode(200)
                .body("result.isError", equalTo(false))
                .body("result.content[0].text", containsString("hello_world"));

        ConfigValueHandlerIBS.ENVIRONMENT_VARS_SETTER_CLASS.reset();
        ConfigValueHandlerIBS.ENVIRONMENT_VARS_SETTER_METHOD.reset();
    }

    @Test(groups = "MCP")
    public void testToolsCall_envHeadersInjected_javaCallTool() {
        // Same verification for the java_call path.
        ConfigValueHandlerIBS.ENVIRONMENT_VARS_SETTER_CLASS.activate(
                "com.adobe.campaign.tests.bridge.testdata.one.EnvironmentVariableHandler");
        ConfigValueHandlerIBS.ENVIRONMENT_VARS_SETTER_METHOD.activate("setIntegroCache");

        String payload = "{\"jsonrpc\":\"2.0\",\"id\":41,\"method\":\"tools/call\","
                + "\"params\":{\"name\":\"java_call\","
                + "\"arguments\":{"
                + "\"callContent\":{"
                + "\"result\":{"
                + "\"class\":\"com.adobe.campaign.tests.bridge.testdata.one.SimpleStaticMethods\","
                + "\"method\":\"usesEnvironmentVariables\","
                + "\"args\":[]"
                + "}}}}}";

        given()
                .contentType(CONTENT_TYPE_JSON)
                .header("ibs-env-ENVVAR1", "foo")
                .header("ibs-env-ENVVAR2", "bar")
                .body(payload)
        .when()
                .post(MCP_ENDPOINT)
        .then()
                .statusCode(200)
                .body("result.isError", equalTo(false))
                .body("result.content[0].text", containsString("foo_bar"));

        ConfigValueHandlerIBS.ENVIRONMENT_VARS_SETTER_CLASS.reset();
        ConfigValueHandlerIBS.ENVIRONMENT_VARS_SETTER_METHOD.reset();
    }

    @Test(groups = "MCP")
    public void testToolsCall_envHeadersDoNotLeakToNonEnvHeaders() {
        // Headers without the ibs-env- prefix must not be treated as env vars.
        // If no ibs-env-* headers are sent, result relies on unset cache — no crash expected.
        given()
                .contentType(CONTENT_TYPE_JSON)
                .header("x-custom-header", "somevalue")
                .body("{\"jsonrpc\":\"2.0\",\"id\":42,\"method\":\"tools/call\","
                        + "\"params\":{\"name\":\"java_call\","
                        + "\"arguments\":{"
                        + "\"callContent\":{"
                        + "\"result\":{"
                        + "\"class\":\"com.adobe.campaign.tests.bridge.testdata.one.SimpleStaticMethods\","
                        + "\"method\":\"methodReturningString\","
                        + "\"args\":[]}}}}}}")
        .when()
                .post(MCP_ENDPOINT)
        .then()
                .statusCode(200)
                .body("result.isError", equalTo(false))
                .body("result.content[0].text", containsString("_Success"));
    }

    // ---- notification handling ----

    @Test(groups = "MCP")
    public void testNotification_returns202() {
        given()
                .contentType(CONTENT_TYPE_JSON)
                .body("{\"jsonrpc\":\"2.0\",\"method\":\"notifications/initialized\"}")
        .when()
                .post(MCP_ENDPOINT)
        .then()
                .statusCode(202);
    }

    // ---- individual tool routing ----

    @Test(groups = "MCP")
    public void testToolsList_exposesIndividualDiscoveredTools() {
        given()
                .contentType(CONTENT_TYPE_JSON)
                .body("{\"jsonrpc\":\"2.0\",\"id\":100,\"method\":\"tools/list\",\"params\":{}}")
        .when()
                .post(MCP_ENDPOINT)
        .then()
                .statusCode(200)
                .body("result.tools.name", hasItem("SimpleStaticMethods_methodReturningString"))
                .body("result.tools.name", hasItem("SimpleStaticMethods_methodAcceptingStringArgument"))
                .body("result.tools.find { it.name == 'SimpleStaticMethods_methodReturningString' }.inputSchema",
                        notNullValue())
                .body("result.tools.find { it.name == 'SimpleStaticMethods_methodAcceptingStringArgument' }.inputSchema.required",
                        hasItem("arg0"));
    }

    @Test(groups = "MCP")
    public void testIndividualTool_noArgMethod_returnsResult() {
        given()
                .contentType(CONTENT_TYPE_JSON)
                .body("{\"jsonrpc\":\"2.0\",\"id\":101,\"method\":\"tools/call\","
                        + "\"params\":{\"name\":\"SimpleStaticMethods_methodReturningString\","
                        + "\"arguments\":{}}}")
        .when()
                .post(MCP_ENDPOINT)
        .then()
                .statusCode(200)
                .body("result.isError", equalTo(false))
                .body("result.content[0].text", containsString("_Success"));
    }

    @Test(groups = "MCP")
    public void testIndividualTool_stringArgMethod_returnsResult() {
        given()
                .contentType(CONTENT_TYPE_JSON)
                .body("{\"jsonrpc\":\"2.0\",\"id\":102,\"method\":\"tools/call\","
                        + "\"params\":{\"name\":\"SimpleStaticMethods_methodAcceptingStringArgument\","
                        + "\"arguments\":{\"arg0\":\"hello\"}}}")
        .when()
                .post(MCP_ENDPOINT)
        .then()
                .statusCode(200)
                .body("result.isError", equalTo(false))
                .body("result.content[0].text", containsString("hello_Success"));
    }

    @Test(groups = "MCP")
    public void testIndividualTool_intArgMethod_returnsResult() {
        given()
                .contentType(CONTENT_TYPE_JSON)
                .body("{\"jsonrpc\":\"2.0\",\"id\":103,\"method\":\"tools/call\","
                        + "\"params\":{\"name\":\"SimpleStaticMethods_methodAcceptingIntArgument\","
                        + "\"arguments\":{\"arg0\":42}}}")
        .when()
                .post(MCP_ENDPOINT)
        .then()
                .statusCode(200)
                .body("result.isError", equalTo(false))
                .body("result.content[0].text", containsString("126"));
    }

    @Test(groups = "MCP")
    public void testIndividualTool_twoArgMethod_argOrderPreserved() {
        given()
                .contentType(CONTENT_TYPE_JSON)
                .body("{\"jsonrpc\":\"2.0\",\"id\":104,\"method\":\"tools/call\","
                        + "\"params\":{\"name\":\"SimpleStaticMethods_methodAcceptingTwoArguments\","
                        + "\"arguments\":{\"arg0\":\"foo\",\"arg1\":\"bar\"}}}")
        .when()
                .post(MCP_ENDPOINT)
        .then()
                .statusCode(200)
                .body("result.isError", equalTo(false))
                .body("result.content[0].text", containsString("foo+bar_Success"));
    }

    @Test(groups = "MCP")
    public void testIndividualTool_methodThrowsException_returnsIsError() {
        given()
                .contentType(CONTENT_TYPE_JSON)
                .body("{\"jsonrpc\":\"2.0\",\"id\":105,\"method\":\"tools/call\","
                        + "\"params\":{\"name\":\"SimpleStaticMethods_methodThrowsException\","
                        + "\"arguments\":{}}}")
        .when()
                .post(MCP_ENDPOINT)
        .then()
                .statusCode(200)
                .body("result.isError", equalTo(true))
                .body("result.content[0].text", containsString("\"title\""))
                .body("result.content[0].text", containsString("\"originalException\""));
    }

    @Test(groups = "MCP")
    public void testJavaCallDescription_doesNotContainCatalog() {
        given()
                .contentType(CONTENT_TYPE_JSON)
                .body("{\"jsonrpc\":\"2.0\",\"id\":106,\"method\":\"tools/list\",\"params\":{}}")
        .when()
                .post(MCP_ENDPOINT)
        .then()
                .statusCode(200)
                .body("result.tools.find { it.name == 'java_call' }.description",
                        not(containsString("Discovered methods")));
    }

    @Test(groups = "MCP")
    public void testIndividualTool_prechainIsApplied() {
        ConfigValueHandlerIBS.MCP_PRECHAIN.activate(
                "{\"ibs_pre\":{\"class\":\"com.adobe.campaign.tests.bridge.testdata.one.SimpleStaticMethods\","
                + "\"method\":\"methodReturningString\",\"args\":[]}}");
        try {
            given()
                    .contentType(CONTENT_TYPE_JSON)
                    .body("{\"jsonrpc\":\"2.0\",\"id\":107,\"method\":\"tools/call\","
                            + "\"params\":{\"name\":\"SimpleStaticMethods_methodReturningString\","
                            + "\"arguments\":{}}}")
            .when()
                    .post(MCP_ENDPOINT)
            .then()
                    .statusCode(200)
                    .body("result.isError", equalTo(false))
                    .body("result.content[0].text", not(containsString("\"ibs_pre\"")))
                    .body("result.content[0].text", containsString("\"result\""));
        } finally {
            ConfigValueHandlerIBS.MCP_PRECHAIN.reset();
        }
    }

    // ---- constructor and complex object chaining ----

    @Test(groups = "MCP")
    public void testToolsList_constructorsNotExposedAsIndividualTools() {
        given()
                .contentType(CONTENT_TYPE_JSON)
                .body("{\"jsonrpc\":\"2.0\",\"id\":108,\"method\":\"tools/list\",\"params\":{}}")
        .when()
                .post(MCP_ENDPOINT)
        .then()
                .statusCode(200)
                .body("result.tools.name", not(hasItem("Instantiable_Instantiable")));
    }

    @Test(groups = "MCP")
    public void testJavaCall_constructorChain_instantiableThenStaticMethod() {
        // Constructors are not available as individual tools — use java_call to instantiate
        // and then pass the result object by reference to a static method in the same chain.
        String payload = "{\"jsonrpc\":\"2.0\",\"id\":109,\"method\":\"tools/call\","
                + "\"params\":{\"name\":\"java_call\","
                + "\"arguments\":{"
                + "\"callContent\":{"
                + "\"obj\":{"
                + "\"class\":\"com.adobe.campaign.tests.bridge.testdata.one.Instantiable\","
                + "\"method\":\"Instantiable\","
                + "\"args\":[\"hello\"]"
                + "},"
                + "\"fetch\":{"
                + "\"class\":\"com.adobe.campaign.tests.bridge.testdata.one.StaticType\","
                + "\"method\":\"fetchInstantiableStringValue\","
                + "\"args\":[\"obj\"]"
                + "}}}}}";

        given()
                .contentType(CONTENT_TYPE_JSON)
                .body(payload)
        .when()
                .post(MCP_ENDPOINT)
        .then()
                .statusCode(200)
                .body("result.isError", equalTo(false))
                .body("result.content[0].text", containsString("hello"));
    }
}
