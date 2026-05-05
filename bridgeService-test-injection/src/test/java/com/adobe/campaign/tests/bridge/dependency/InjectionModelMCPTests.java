/*
 * Copyright 2022 Adobe
 * All Rights Reserved.
 *
 * NOTICE: Adobe permits you to use, modify, and distribute this file in
 * accordance with the terms of the Adobe license agreement accompanying
 * it.
 */
package com.adobe.campaign.tests.bridge.dependency;

import com.adobe.campaign.tests.bridge.dependency.factory.DepFactory;
import com.adobe.campaign.tests.bridge.service.CallContent;
import com.adobe.campaign.tests.bridge.service.ConfigValueHandlerIBS;
import com.adobe.campaign.tests.bridge.service.IntegroAPI;
import com.adobe.campaign.tests.bridge.service.JavaCalls;
import com.adobe.campaign.tests.bridge.testdata.one.SimpleStaticMethods;
import io.javalin.Javalin;
import org.hamcrest.Matchers;
import org.testng.annotations.AfterGroups;
import org.testng.annotations.BeforeGroups;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import static io.restassured.RestAssured.given;

/**
 * Verifies that when BridgeService is started from bridgeService-test-injection's classpath,
 * methods from both this module and bridgeService-data are reachable via REST (/call)
 * and via MCP (tools/list).
 *
 * The server is started once with MCP enabled and both modules' packages in
 * STATIC_INTEGRITY_PACKAGES so that tool discovery and class loading cover both.
 */
public class InjectionModelMCPTests {

    private static final String REST_ENDPOINT = "http://localhost:8080/";
    private static final String MCP_ENDPOINT = "http://localhost:8080/mcp";
    private static final String CONTENT_TYPE_JSON = "application/json";

    /** Package from bridgeService-data whose Javadoc is embedded at compile time. */
    private static final String BRIDGE_DATA_PACKAGE =
            "com.adobe.campaign.tests.bridge.testdata.one";

    /** Package from this module whose Javadoc is embedded via therapi at compile time. */
    private static final String FACTORY_PACKAGE =
            "com.adobe.campaign.tests.bridge.dependency.factory.";

    private static final String BOTH_PACKAGES = BRIDGE_DATA_PACKAGE + "," + FACTORY_PACKAGE;

    private Javalin app;

    @BeforeGroups(groups = "INJECTION_MCP")
    public void startMCPService() {
        ConfigValueHandlerIBS.STATIC_INTEGRITY_PACKAGES.activate(BOTH_PACKAGES);
        ConfigValueHandlerIBS.MCP_ENABLED.activate("true");
        app = IntegroAPI.startServices(8080);
    }

    @BeforeMethod
    public void resetConfig() {
        ConfigValueHandlerIBS.STATIC_INTEGRITY_PACKAGES.activate(BOTH_PACKAGES);
    }

    @AfterGroups(groups = "INJECTION_MCP", alwaysRun = true)
    public void tearDown() {
        ConfigValueHandlerIBS.resetAllValues();
        app.stop();
    }

    // ---- REST access ----

    /**
     * Calls SimpleStaticMethods.methodReturningString() from bridgeService-data via REST.
     * Proves that dependency JAR methods are callable through the /call endpoint when
     * their package is listed in STATIC_INTEGRITY_PACKAGES.
     */
    @Test(groups = "INJECTION_MCP")
    public void testRESTCall_bridgeDataMethod() {
        JavaCalls l_calls = new JavaCalls();
        CallContent l_cc = new CallContent();
        l_cc.setClassName(SimpleStaticMethods.class.getTypeName());
        l_cc.setMethodName("methodReturningString");
        l_calls.getCallContent().put("call1", l_cc);

        given().body(l_calls).post(REST_ENDPOINT + "call").then()
                .assertThat().statusCode(200)
                .body("returnValues.call1", Matchers.notNullValue());
    }

    /**
     * Calls DepFactory.makeMiddleMan() from this module (bridgeService-test-injection) via REST.
     * Proves that methods defined in the module that starts the service are also callable
     * through the /call endpoint.
     */
    @Test(groups = "INJECTION_MCP")
    public void testRESTCall_testInjectionMethod() {
        JavaCalls l_calls = new JavaCalls();
        CallContent l_cc = new CallContent();
        l_cc.setClassName(DepFactory.class.getTypeName());
        l_cc.setMethodName("makeMiddleMan");
        l_calls.getCallContent().put("call1", l_cc);

        given().body(l_calls).post(REST_ENDPOINT + "call").then()
                .assertThat().statusCode(200);
    }

    // ---- MCP tools/list access ----

    /**
     * Verifies tools/list returns SimpleStaticMethods_methodReturningString from bridgeService-data.
     * Proves that MCP tool discovery scans dependency JARs on the classpath when their package
     * is listed in STATIC_INTEGRITY_PACKAGES at server startup.
     */
    @Test(groups = "INJECTION_MCP")
    public void testMCPToolDiscovery_findsBridgeDataMethod() {
        given().contentType(CONTENT_TYPE_JSON)
                .body("{\"jsonrpc\":\"2.0\",\"id\":1,\"method\":\"tools/list\",\"params\":{}}")
                .post(MCP_ENDPOINT)
                .then()
                .assertThat().statusCode(200)
                .body("result.tools.name",
                        Matchers.hasItem("SimpleStaticMethods_methodReturningString"));
    }

    /**
     * Verifies tools/list returns DepFactory_makeMiddleMan from this module.
     * Proves that MCP tool discovery also covers classes compiled into the module that
     * starts the service — not just classes from external dependency JARs.
     */
    @Test(groups = "INJECTION_MCP")
    public void testMCPToolDiscovery_findsTestInjectionMethod() {
        given().contentType(CONTENT_TYPE_JSON)
                .body("{\"jsonrpc\":\"2.0\",\"id\":2,\"method\":\"tools/list\",\"params\":{}}")
                .post(MCP_ENDPOINT)
                .then()
                .assertThat().statusCode(200)
                .body("result.tools.name", Matchers.hasItem("DepFactory_makeMiddleMan"));
    }
}
