/*
 * Copyright 2022 Adobe
 * All Rights Reserved.
 *
 * NOTICE: Adobe permits you to use, modify, and distribute this file in
 * accordance with the terms of the Adobe license agreement accompanying
 * it.
 */
package com.adobe.campaign.tests.bridge.dependency;

import com.adobe.campaign.tests.bridge.service.CallContent;
import com.adobe.campaign.tests.bridge.service.ConfigValueHandlerIBS;
import com.adobe.campaign.tests.bridge.service.IntegroAPI;
import com.adobe.campaign.tests.bridge.service.JavaCalls;
import com.adobe.campaign.tests.bridge.service.exceptions.IBSConfigurationException;
import com.adobe.campaign.tests.bridge.dependency.caller.DepCaller;
import com.adobe.campaign.tests.bridge.dependency.factory.DepFactory;
import io.javalin.Javalin;
import org.hamcrest.Matchers;
import org.testng.annotations.AfterGroups;
import org.testng.annotations.BeforeGroups;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import static io.restassured.RestAssured.given;

/**
 * E2E tests for the injection model (IBS.CLASSLOADER.STATIC.INTEGRITY.PACKAGES).
 * Demonstrates that dependency library packages can and must be listed alongside
 * project packages to avoid LinkageError when the library's factory returns a type
 * that is also loaded by the IBS classloader.
 */
public class InjectionModelE2ETests {

    private static final String END_POINT_URL = "http://localhost:8080/";

    private static final String CALLER_PACKAGE =
            "com.adobe.campaign.tests.bridge.dependency.caller.";
    private static final String MODEL_PACKAGE =
            "com.adobe.campaign.tests.bridge.dependency.model.";
    private static final String FACTORY_PACKAGE =
            "com.adobe.campaign.tests.bridge.dependency.factory.";

    private Javalin app;

    @BeforeGroups(groups = "E2E")
    public void startUpService() {
        app = IntegroAPI.startServices(8080);
    }

    @BeforeMethod
    public void cleanCache() {
        ConfigValueHandlerIBS.resetAllValues();
    }

    @AfterGroups(groups = "E2E", alwaysRun = true)
    public void tearDown() {
        ConfigValueHandlerIBS.resetAllValues();
        app.stop();
    }

    /**
     * Negative test: when DepResult's package IS in STATIC_INTEGRITY_PACKAGES but
     * DepFactory's package is NOT, the IBS classloader and the parent classloader both
     * load DepResult independently, producing a LinkageError.
     */
    @Test(groups = "E2E")
    public void testInjectionConflict_missingFactoryPackage() {
        ConfigValueHandlerIBS.INTEGRITY_PACKAGE_INJECTION_MODE.activate("manual");
        ConfigValueHandlerIBS.STATIC_INTEGRITY_PACKAGES.activate(
                MODEL_PACKAGE + "," + CALLER_PACKAGE);

        JavaCalls l_myJavaCalls = new JavaCalls();
        CallContent l_cc = new CallContent();
        l_cc.setClassName(DepCaller.class.getTypeName());
        l_cc.setMethodName("doSomething");
        l_myJavaCalls.getCallContent().put("call1", l_cc);

        given().body(l_myJavaCalls).post(END_POINT_URL + "call").then()
                .assertThat().statusCode(500)
                .body("title", Matchers.equalTo(
                        "The provided class and method for setting environment variables is not valid."))
                .body("code", Matchers.equalTo(500))
                .body("detail", Matchers.startsWith("Linkage Error detected"))
                .body("bridgeServiceException",
                        Matchers.equalTo(IBSConfigurationException.class.getTypeName()))
                .body("originalException",
                        Matchers.equalTo(LinkageError.class.getTypeName()));
    }

    /**
     * Positive test: adding DepFactory's package to STATIC_INTEGRITY_PACKAGES ensures all
     * three packages are loaded by the same IBS classloader, eliminating the type mismatch.
     */
    @Test(groups = "E2E")
    public void testInjectionConflict_allPackagesPresent() {
        ConfigValueHandlerIBS.INTEGRITY_PACKAGE_INJECTION_MODE.activate("manual");
        ConfigValueHandlerIBS.STATIC_INTEGRITY_PACKAGES.activate(
                MODEL_PACKAGE + "," + CALLER_PACKAGE + "," + FACTORY_PACKAGE);

        JavaCalls l_myJavaCalls = new JavaCalls();
        CallContent l_cc = new CallContent();
        l_cc.setClassName(DepCaller.class.getTypeName());
        l_cc.setMethodName("doSomething");
        l_myJavaCalls.getCallContent().put("call1", l_cc);

        given().body(l_myJavaCalls).post(END_POINT_URL + "call").then()
                .assertThat().statusCode(200)
                .body("returnValues.call1", Matchers.equalTo("initial"));
    }

    /**
     * Cross-module type test: DepFactory.makeMiddleMan() returns a MiddleMan from bridgeService-data.
     * When the factory package is in STATIC_INTEGRITY_PACKAGES, the call succeeds and the
     * MiddleMan object is serialised correctly by the BridgeService response layer.
     */
    @Test(groups = "E2E")
    public void testDepFactoryReturnsBridgeDataType() {
        ConfigValueHandlerIBS.INTEGRITY_PACKAGE_INJECTION_MODE.activate("manual");
        ConfigValueHandlerIBS.STATIC_INTEGRITY_PACKAGES.activate(FACTORY_PACKAGE);

        JavaCalls l_myJavaCalls = new JavaCalls();
        CallContent l_cc = new CallContent();
        l_cc.setClassName(DepFactory.class.getTypeName());
        l_cc.setMethodName("makeMiddleMan");
        l_myJavaCalls.getCallContent().put("call1", l_cc);

        given().body(l_myJavaCalls).post(END_POINT_URL + "call").then()
                .assertThat().statusCode(200);
    }
}
