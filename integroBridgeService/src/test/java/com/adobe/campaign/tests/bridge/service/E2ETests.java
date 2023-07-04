/*
 * Copyright 2022 Adobe
 * All Rights Reserved.
 *
 * NOTICE: Adobe permits you to use, modify, and distribute this file in
 * accordance with the terms of the Adobe license agreement accompanying
 * it.
 */
package com.adobe.campaign.tests.bridge.service;

import com.adobe.campaign.tests.bridge.testdata.one.EnvironmentVariableHandler;
import com.adobe.campaign.tests.bridge.testdata.one.SimpleStaticMethods;
import com.adobe.campaign.tests.bridge.testdata.two.StaticMethodsIntegrity;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.hamcrest.Matchers;
import org.testng.annotations.AfterGroups;
import org.testng.annotations.BeforeGroups;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import spark.Spark;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import static io.restassured.RestAssured.given;

public class E2ETests {
    public static final String EndPointURL = "http://localhost:8080/";
    private static final int port1 = 1111;
    ServerSocket serverSocket1 = null;

    @BeforeGroups(groups = "E2E")
    public void startUpService() throws IOException {
        IntegroAPI.startServices(8080);
        Spark.awaitInitialization();

        serverSocket1 = new ServerSocket(port1);
    }

    @BeforeMethod
    public void cleanCache() {
        ConfigValueHandlerIBS.resetAllValues();
    }

    @Test(groups = "E2E")
    public void testMainHelloWorld() {
        ConfigValueHandlerIBS.PRODUCT_VERSION.activate("101");

        given().when().get(EndPointURL + "test").then().assertThat().body(Matchers.startsWith("All systems up"))
                .body(Matchers.endsWith("101"));

        ConfigValueHandlerIBS.PRODUCT_USER_VERSION.activate("F");

        given().when().get(EndPointURL + "test").then().assertThat().body(Matchers.startsWith("All systems up"))
                .body(Matchers.endsWith("Product user version : F")).body(Matchers.containsString("101"));

    }

    @Test(groups = "E2E")
    public void testMainHelloWorld_negative() {
        given().when().get(EndPointURL + "hello").then().assertThat().statusCode(404);
    }

    @Test(groups = "E2E")
    public void testMainHelloWorldCall() {

        JavaCalls l_call = new JavaCalls();
        CallContent myContent = new CallContent();
        myContent.setClassName(SimpleStaticMethods.class.getTypeName());
        myContent.setMethodName("methodReturningString");
        myContent.setReturnType("java.lang.String");
        l_call.getCallContent().put("call1PL", myContent);

        given().body(l_call).post(EndPointURL + "call").then().assertThat().body("returnValues.call1PL",
                Matchers.equalTo(SimpleStaticMethods.SUCCESS_VAL));
    }

    @Test(groups = "E2E")
    public void testErrors() {
        JavaCallResults jcr = new JavaCallResults();

        given().body(jcr).post(EndPointURL + "call").then().statusCode(400).and().assertThat()
                .body(Matchers.startsWith(IntegroAPI.ERROR_JSON_TRANSFORMATION));
    }

    @Test(groups = "E2E")
    public void testTestAccess() {
        JavaCallResults jcr = new JavaCallResults();

        given().body(jcr).post(EndPointURL + "call").then().statusCode(400).and().assertThat()
                .body(Matchers.startsWith(IntegroAPI.ERROR_JSON_TRANSFORMATION));
    }

    @Test(groups = "E2E")
    public void testCheckConnectivity() {

        Map<String, String> urlsMap = new HashMap<>();
        urlsMap.put("url1", "not really a url");
        urlsMap.put("url2", "localhost:" + port1);

        given().body(urlsMap).post(EndPointURL + "service-check").then().assertThat().statusCode(200)
                .body("url1", Matchers.equalTo(false), "url2", Matchers.equalTo(true));

    }

    /**
     * Testing that we provide the correct error messages whenever the target method throws an error
     */
    @Test(groups = "E2E")
    public void testMainEror_Case1InvocationError() {

        JavaCalls l_call = new JavaCalls();
        CallContent myContent = new CallContent();
        myContent.setClassName(SimpleStaticMethods.class.getTypeName());
        myContent.setMethodName("methodThrowingException");
        myContent.setReturnType("java.lang.String");
        myContent.setArgs(
                new Object[] { 7, 7 });
        l_call.getCallContent().put("call1PL", myContent);

        given().body(l_call).post(EndPointURL + "call").then().assertThat().statusCode(400).body(
                Matchers.containsString("We do not allow numbers that are equal."));

    }

    /**
     * Testing that we provide the correct error messages whenever the target method throws an error
     */
    @Test(groups = "E2E")
    public void testMainEror_Case2AmbiguousMethodException() {

        JavaCalls l_call = new JavaCalls();
        CallContent myContent = new CallContent();
        myContent.setClassName("com.adobe.campaign.tests.bridge.testdata.one.SimpleStaticMethods");
        myContent.setMethodName("overLoadedMethod1Arg");
        myContent.setReturnType("java.lang.String");
        myContent.setArgs(
                new Object[] { "plop" });
        l_call.getCallContent().put("call1PL", myContent);

        given().body(l_call).post(EndPointURL + "call").then().assertThat().statusCode(400).body(
                Matchers.containsString(IntegroAPI.ERROR_JSON_TRANSFORMATION));
    }

    /**
     * Testing that we provide the correct error messages whenever the target method throws an error
     */
    @Test(groups = "E2E")
    public void testMainEror_Case3IBSConfigurationException1() {
        ConfigValueHandlerIBS.ENVIRONMENT_VARS_SETTER_CLASS.activate("a.b.c.NonExistingClass");

        JavaCalls l_call = new JavaCalls();
        CallContent myContent = new CallContent();
        myContent.setClassName("com.adobe.campaign.tests.bridgeservice.testdata.SimpleStaticMethods");
        myContent.setMethodName("overLoadedMethod1Arg");
        myContent.setReturnType("java.lang.String");
        myContent.setArgs(
                new Object[] { "plop" });
        l_call.getCallContent().put("call1PL", myContent);

        Properties l_envVars = new Properties();
        l_envVars.put("AC.UITEST.MAILING.PREFIX", "bada");
        l_envVars.put("AC.INTEGRO.MAILING.BASE", "boom.com");

        l_call.setEnvironmentVariables(l_envVars);

        given().body(l_call).post(EndPointURL + "call").then().assertThat().statusCode(400).body(
                Matchers.containsString(IntegroAPI.ERROR_IBS_CONFIG));

    }

    /**
     * Testing that we provide the correct error messages whenever the target method throws an error
     */
    @Test(groups = "E2E")
    public void testMainEror_Case4A_NonExistantJavaException() {

        JavaCalls l_call = new JavaCalls();
        CallContent myContent = new CallContent();
        myContent.setClassName("com.adobe.campaign.tests.bridgeservice.testdata.SimpleStaticMethodsNonExisting");
        myContent.setMethodName("methodReturningString");
        myContent.setReturnType("java.lang.String");
        l_call.getCallContent().put("call1PL", myContent);

        given().body(l_call).post(EndPointURL + "call").then().assertThat().statusCode(400).body(
                Matchers.containsString(IntegroAPI.ERROR_JAVA_OBJECT_NOT_FOUND));
    }

    /**
     * Testing that we provide the correct error messages whenever the target method throws an error
     */
    @Test(groups = "E2E")
    public void testMainEror_Case4B_NonExistantJavaException() {

        JavaCalls l_call = new JavaCalls();
        CallContent myContent = new CallContent();
        myContent.setClassName("com.adobe.campaign.tests.bridgeservice.testdata.SimpleStaticMethods");
        myContent.setMethodName("methodReturningStringNonExisting");
        myContent.setReturnType("java.lang.String");
        l_call.getCallContent().put("call1PL", myContent);

        given().body(l_call).post(EndPointURL + "call").then().assertThat().statusCode(400).body(
                Matchers.containsString(IntegroAPI.ERROR_JAVA_OBJECT_NOT_FOUND));
    }

    /**
     * Testing that we provide the correct error messages whenever the target method throws an error
     */
    @Test(groups = "E2E")
    public void testMainEror_passingNull() throws JsonProcessingException {
        ConfigValueHandlerIBS.ENVIRONMENT_VARS_SETTER_CLASS.activate("a.b.c.NonExistingClass");

        String l_jsonString =
                "{\n"
                        + "    \"callContent\": {\n"
                        + "        \"call1\": {\n"
                        + "            \"class\": \"com.adobe.campaign.tests.bridgeservice.testdata2.StaticMethodsIntegrity\",\n"
                        + "            \"method\": \"assembleBySystemValues\",\n"
                        + "            \"returnType\": \"java.lang.String\",\n"
                        + "            \"args\": []\n"
                        + "        }\n"
                        + "    },\n"
                        + "    \"environmentVariables\": {\n"
                        + "        \"AC.UITEST.MAILING.PREFIX\": \"tyrone\",\n"
                        + "        \"AC.INTEGRO.MAILING.BASE\" : null"
                        + "    }\n"
                        + "}";

        given().body(l_jsonString).post(EndPointURL + "call").then().assertThat().statusCode(400).body(
                Matchers.containsString(IntegroAPI.ERROR_JSON_TRANSFORMATION));

    }

    @Test(groups = "E2E")
    public void testIntegrity_pathsSet() {
        ConfigValueHandlerIBS.STATIC_INTEGRITY_PACKAGES.activate("com.adobe.campaign.tests.bridgeservice.testdata2");
        ConfigValueHandlerIBS.ENVIRONMENT_VARS_SETTER_CLASS.activate(EnvironmentVariableHandler.class.getTypeName());

        //Call 1
        JavaCalls l_myJavaCalls = new JavaCalls();

        CallContent l_cc = new CallContent();
        l_cc.setClassName(StaticMethodsIntegrity.class.getTypeName());
        l_cc.setMethodName("assembleBySystemValues");

        Properties l_envVars = new Properties();
        l_envVars.put("PREFIX", "bada");
        l_envVars.put("SUFFIX", "boom.com");

        l_myJavaCalls.setEnvironmentVariables(l_envVars);

        l_myJavaCalls.getCallContent().put("call1PL", l_cc);

        given().body(l_myJavaCalls).post(EndPointURL + "call").then().assertThat().statusCode(200).
                body("returnValues.call1PL", Matchers.endsWith("@boom.com"))
                .body("returnValues.call1PL", Matchers.startsWith("bada"));

        JavaCalls l_myJavaCallsB = new JavaCalls();
        CallContent l_ccB = new CallContent();
        l_ccB.setClassName(StaticMethodsIntegrity.class.getTypeName());
        l_ccB.setMethodName("assembleBySystemValues");
        l_myJavaCallsB.getCallContent().put("call2PL", l_ccB);

        given().body(l_myJavaCallsB).post(EndPointURL + "call").then().assertThat().statusCode(200).
                body("returnValues.call2PL", Matchers.not(Matchers.endsWith("@boom.com")))
                .body("returnValues.call2PL", Matchers.not(Matchers.startsWith("bada")));

    }

    /**
     * Issues:
     * <ul>
     *     <li>
     *         <a href="https://git.corp.adobe.com/AdobeCampaignQE/integroBridgeService/issues/41">#41 -  Make sure that the path for the systemvalue handler is included in the paths searched by the classloader</a>
     *     </li>
     *     <li>
     *         <a href="https://git.corp.adobe.com/AdobeCampaignQE/integroBridgeService/issues/47">#47 - Issue with Server=null when calling PushNotifications with IBS</a>
     *     </li>
     *     <li>
     *         <a href="https://git.corp.adobe.com/AdobeCampaignQE/integroBridgeService/issues/48">#48 - Dynamic management of IBS.CLASSLOADER.STATIC.INTEGRITY.PACKAGES</a>
     *     </li>
     * </ul>
     */
    @Test(groups = "E2E")
    public void testIntegrity_case2_pathsNotSet() {
        ConfigValueHandlerIBS.ENVIRONMENT_VARS_SETTER_CLASS.activate(EnvironmentVariableHandler.class.getTypeName());

        //Call 1
        JavaCalls l_myJavaCalls = new JavaCalls();
        //l_myJavaCalls.getLocalClassLoader().setPackagePaths(new HashSet<>());

        CallContent l_cc = new CallContent();
        l_cc.setClassName(StaticMethodsIntegrity.class.getTypeName());
        l_cc.setMethodName("assembleBySystemValues");

        Properties l_envVars = new Properties();
        l_envVars.put("PREFIX", "bada");
        l_envVars.put("SUFFIX", "boom.com");

        l_myJavaCalls.setEnvironmentVariables(l_envVars);

        l_myJavaCalls.getCallContent().put("call1PL", l_cc);

        given().body(l_myJavaCalls).post(EndPointURL + "call").then().assertThat().statusCode(200).
                body("returnValues.call1PL", Matchers.endsWith("@boom.com"))
                .body("returnValues.call1PL", Matchers.startsWith("bada"));

        //call 2 -- independant call. No access to the set environment variables
        JavaCalls l_myJavaCallsB = new JavaCalls();
        CallContent l_ccB = new CallContent();
        l_ccB.setClassName(StaticMethodsIntegrity.class.getTypeName());
        l_ccB.setMethodName("assembleBySystemValues");
        l_myJavaCallsB.getCallContent().put("call2PL", l_ccB);

        given().body(l_myJavaCallsB).post(EndPointURL + "call").then().assertThat().statusCode(200).
                body("returnValues.call2PL", Matchers.endsWith("null"))
                .body("returnValues.call2PL", Matchers.startsWith("null+c"));

    }

    @Test(groups = "E2E")
    public void testEnvironmentVars() {
        ConfigValueHandlerIBS.ENVIRONMENT_VARS_SETTER_CLASS.activate(EnvironmentVariableHandler.class.getTypeName());

        //Call 1
        JavaCalls l_myJavaCalls = new JavaCalls();

        CallContent l_cc = new CallContent();
        l_cc.setClassName(EnvironmentVariableHandler.class.getTypeName());
        l_cc.setMethodName("getCacheProperty");
        l_cc.setArgs(new Object[] { "ABC" });

        Properties l_authentication = new Properties();
        l_authentication.put("ABC", 123);
        l_myJavaCalls.setEnvironmentVariables(l_authentication);

        l_myJavaCalls.getCallContent().put("call1PL", l_cc);

        given().body(l_myJavaCalls).post(EndPointURL + "call").then().assertThat().statusCode(200).
                body("returnValues.call1PL", Matchers.equalTo("123"));
    }

    @Test(groups = "E2E")
    public void testTimeOutCalls() {
        String l_expectedDuration = "300";
        ConfigValueHandlerIBS.DEFAULT_CALL_TIMEOUT.activate(l_expectedDuration);
        Long l_sleepDuration = Long.parseLong(ConfigValueHandlerIBS.DEFAULT_CALL_TIMEOUT.fetchValue());

        JavaCalls jc = new JavaCalls();
        CallContent cc1 = new CallContent();
        cc1.setClassName(SimpleStaticMethods.class.getTypeName());
        cc1.setMethodName("methodWithTimeOut");
        cc1.setArgs(new Object[] { l_sleepDuration + 100 });
        jc.getCallContent().put("call1", cc1);

        given().body(jc).post(EndPointURL + "call").then().assertThat().statusCode(408).body(
                Matchers.containsString(IntegroAPI.ERROR_CALL_TIMEOUT));
    }

    @Test(groups = "E2E")
    public void testTimeOutCalls_overridePass() {
        String l_expectedDuration = "300";
        ConfigValueHandlerIBS.DEFAULT_CALL_TIMEOUT.activate(l_expectedDuration);
        Long l_sleepDuration = Long.parseLong(ConfigValueHandlerIBS.DEFAULT_CALL_TIMEOUT.fetchValue());

        JavaCalls jc = new JavaCalls();
        jc.setTimeout(450l);
        CallContent cc1 = new CallContent();
        cc1.setClassName(SimpleStaticMethods.class.getTypeName());
        cc1.setMethodName("methodWithTimeOut");
        cc1.setArgs(new Object[] { l_sleepDuration + 100 });
        jc.getCallContent().put("call1", cc1);

        given().body(jc).post(EndPointURL + "call").then().assertThat().statusCode(200).body(
                "callDurations.call1", Matchers.greaterThan(400)).body(
                "callDurations.call1", Matchers.lessThan(450));
    }

    @Test(groups = "E2E")
    public void testTimeOutCalls_overrideFAIL() {
        String l_expectedDuration = "300";
        ConfigValueHandlerIBS.DEFAULT_CALL_TIMEOUT.activate(l_expectedDuration);
        Long l_sleepDuration = Long.parseLong(ConfigValueHandlerIBS.DEFAULT_CALL_TIMEOUT.fetchValue());

        JavaCalls jc = new JavaCalls();
        jc.setTimeout(l_sleepDuration - 150);

        CallContent cc1 = new CallContent();
        cc1.setClassName(SimpleStaticMethods.class.getTypeName());
        cc1.setMethodName("methodWithTimeOut");
        cc1.setArgs(new Object[] { l_sleepDuration - 100 });
        jc.getCallContent().put("call1", cc1);

        given().body(jc).post(EndPointURL + "call").then().assertThat().statusCode(408).body(
                Matchers.containsString(IntegroAPI.ERROR_CALL_TIMEOUT));
    }


    @AfterGroups(groups = "E2E", alwaysRun = true)
    public void tearDown() throws IOException {
        ConfigValueHandlerIBS.resetAllValues();
        Spark.stop();
        serverSocket1.close();
    }
}
