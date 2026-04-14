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
                .body("result.tools", not(empty()))
                .body("result.tools.name", hasItem("SimpleStaticMethods_methodReturningString"))
                .body("result.tools.name", hasItem("java_call"));
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
        // The description for methodReturningString should be sourced from its Javadoc,
        // not the fallback "Calls com.example.MyClass.methodName()" string.
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
        Response resp = given()
                .contentType(CONTENT_TYPE_JSON)
                .body("{\"jsonrpc\":\"2.0\",\"id\":4,\"method\":\"tools/list\",\"params\":{}}")
        .when()
                .post(MCP_ENDPOINT)
        .then()
                .statusCode(200)
                .extract().response();

        // methodReturningString takes no args — inputSchema.properties should be empty
        int idx = resp.path("result.tools.name").toString()
                .indexOf("SimpleStaticMethods_methodReturningString");
        assertThat(idx, greaterThanOrEqualTo(0));
    }

    @Test(groups = "MCP")
    public void testToolsList_undocumentedMethodExcluded() {
        // EnvironmentVariableHandler methods have no Javadoc — must be absent with default REQUIRE_JAVADOC=true
        given()
                .contentType(CONTENT_TYPE_JSON)
                .body("{\"jsonrpc\":\"2.0\",\"id\":13,\"method\":\"tools/list\",\"params\":{}}")
        .when()
                .post(MCP_ENDPOINT)
        .then()
                .statusCode(200)
                .body("result.tools.name", not(hasItem("EnvironmentVariableHandler_getCacheProperty")))
                .body("result.tools.name", not(hasItem("EnvironmentVariableHandler_setIntegroCache")));
    }

    // ---- tools/call (discovered tools) ----

    @Test(groups = "MCP")
    public void testToolsCall_noArgMethod_returnsResult() {
        given()
                .contentType(CONTENT_TYPE_JSON)
                .body("{\"jsonrpc\":\"2.0\",\"id\":5,\"method\":\"tools/call\","
                        + "\"params\":{\"name\":\"SimpleStaticMethods_methodReturningString\","
                        + "\"arguments\":{}}}")
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
                        + "\"params\":{\"name\":\"SimpleStaticMethods_methodAcceptingStringArgument\","
                        + "\"arguments\":{\"arg0\":\"hello\"}}}")
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
                        + "\"params\":{\"name\":\"SimpleStaticMethods_methodThrowsException\","
                        + "\"arguments\":{}}}")
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
                        + "\"params\":{\"name\":\"SimpleStaticMethods_methodWithTimeOut\","
                        + "\"arguments\":{\"arg0\":5000}}}")
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
        // while the actual tool result ("result") must be present.
        ConfigValueHandlerIBS.MCP_PRECHAIN.activate(
                "{\"ibs_pre\":{\"class\":\"com.adobe.campaign.tests.bridge.testdata.one.SimpleStaticMethods\","
                + "\"method\":\"methodReturningString\",\"args\":[]}}");

        given()
                .contentType(CONTENT_TYPE_JSON)
                .body("{\"jsonrpc\":\"2.0\",\"id\":20,\"method\":\"tools/call\","
                        + "\"params\":{\"name\":\"SimpleStaticMethods_methodReturningString\","
                        + "\"arguments\":{}}}")
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
                        + "\"params\":{\"name\":\"SimpleStaticMethods_methodReturningString\","
                        + "\"arguments\":{}}}")
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
                        + "\"params\":{\"name\":\"SimpleStaticMethods_methodReturningString\","
                        + "\"arguments\":{}}}")
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
                        + "\"params\":{\"name\":\"SimpleStaticMethods_methodReturningString\","
                        + "\"arguments\":{}}}")
        .when()
                .post(MCP_ENDPOINT)
        .then()
                .statusCode(200)
                .body("result.isError", equalTo(false))
                .body("result.content[0].text", containsString("_Success"));

        ConfigValueHandlerIBS.MCP_PRECHAIN.reset();
    }

    @Test(groups = "MCP")
    public void testPrechain_notAppliedToJavaCall() {
        // java_call must be unaffected even when IBS.MCP.PRECHAIN is set to an invalid entry
        // that would cause an error if executed.
        ConfigValueHandlerIBS.MCP_PRECHAIN.activate(
                "{\"ibs_pre\":{\"class\":\"com.example.NonExistentClass\","
                + "\"method\":\"nonExistentMethod\",\"args\":[]}}");

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
                .body("result.content[0].text", containsString("_Success"));

        ConfigValueHandlerIBS.MCP_PRECHAIN.reset();
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
}
