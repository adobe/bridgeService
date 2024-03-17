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
import com.adobe.campaign.tests.bridge.testdata.one.EnvironmentVariableHandler;
import com.adobe.campaign.tests.bridge.testdata.one.SimpleStaticMethods;
import com.adobe.campaign.tests.bridge.testdata.two.StaticMethodsIntegrity;
import com.fasterxml.jackson.databind.exc.UnrecognizedPropertyException;
import io.restassured.path.json.JsonPath;
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
import static org.hamcrest.MatcherAssert.assertThat;

public class E2ETests {
    public static final String EndPointURL = "http://localhost:8080/";
    protected static final boolean AUTOMATIC_FLAG = false;
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
        String bridgeServiceVersion = "101";
        ConfigValueHandlerIBS.PRODUCT_VERSION.activate(bridgeServiceVersion);

        String l_hostVersion = "F";
        ConfigValueHandlerIBS.PRODUCT_USER_VERSION.activate(l_hostVersion);

        given().when().get(EndPointURL + "test").then().assertThat()
                .body("overALLSystemState", Matchers.equalTo(IntegroAPI.SYSTEM_UP_MESSAGE))
                .body("deploymentMode", Matchers.equalTo(IntegroAPI.DeploymentMode.TEST.toString()))
                .body("bridgeServiceVersion", Matchers.equalTo(
                        bridgeServiceVersion))
                .body("hostVersion", Matchers.equalTo(l_hostVersion));

    }

    @Test(groups = "E2E")
    public void testWorkingWithNoProductVersion() {
        String bridgeServiceVersion = "101";
        ConfigValueHandlerIBS.PRODUCT_VERSION.activate(bridgeServiceVersion);

        given().when().get(EndPointURL + "test").then().assertThat()
                .body("overALLSystemState", Matchers.equalTo(IntegroAPI.SYSTEM_UP_MESSAGE))
                .body("deploymentMode", Matchers.equalTo(IntegroAPI.DeploymentMode.TEST.toString()))
                .body("bridgeServiceVersion", Matchers.equalTo(
                        bridgeServiceVersion));

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

        given().body(jcr).post(EndPointURL + "call").then().statusCode(404).and().assertThat()
                .body("title", Matchers.equalTo(IntegroAPI.ERROR_JSON_TRANSFORMATION))
                .body("detail", Matchers.startsWith(
                        "Unrecognized field \"callDurations\" (class com.adobe.campaign.tests.bridge.service.JavaCalls), not marked as ignorable"))
                .body("code", Matchers.equalTo(404))
                .body("bridgeServiceException", Matchers.equalTo(UnrecognizedPropertyException.class.getTypeName()))
                .body("originalException", Matchers.equalTo(ErrorObject.STD_NOT_APPLICABLE));
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
        final String l_calledMethod = "methodThrowingException";
        myContent.setMethodName(l_calledMethod);
        myContent.setReturnType("java.lang.String");
        myContent.setArgs(
                new Object[] { 7, 7 });
        l_call.getCallContent().put("call1PL", myContent);

        given().body(l_call).post(EndPointURL + "call").then().assertThat().statusCode(500)
                .contentType(IntegroAPI.ERROR_CONTENT_TYPE)
                .body("title", Matchers.equalTo(IntegroAPI.ERROR_CALLING_JAVA_METHOD))
                .body("detail", Matchers.containsString(
                        "We do not allow numbers that are equal."))
                .body("code", Matchers.equalTo(500))
                .body("bridgeServiceException", Matchers.equalTo(TargetJavaMethodCallException.class.getTypeName()))
                .body("originalException", Matchers.equalTo(IllegalArgumentException.class.getTypeName()))
                .body("stackTrace[0]",
                        Matchers.startsWith(SimpleStaticMethods.class.getTypeName() + "." + l_calledMethod));

    }

    /**
     * Testing that we provide the correct error messages whenever the target method throws an error
     */
    @Test(groups = "E2E")
    public void testMainError_Case2AmbiguousMethodException() {

        JavaCalls l_call = new JavaCalls();
        CallContent myContent = new CallContent();
        myContent.setClassName("com.adobe.campaign.tests.bridge.testdata.one.SimpleStaticMethods");
        myContent.setMethodName("overLoadedMethod1Arg");
        myContent.setReturnType("java.lang.String");
        myContent.setArgs(
                new Object[] { "plop" });
        l_call.getCallContent().put("call1PL", myContent);

        given().body(l_call).post(EndPointURL + "call").then().assertThat().statusCode(404)
                .body("title", Matchers.equalTo(IntegroAPI.ERROR_AMBIGUOUS_METHOD))
                .body("detail", Matchers.containsString(
                        "We could not find a unique method for"))
                .body("code", Matchers.equalTo(404))
                .body("bridgeServiceException", Matchers.equalTo(AmbiguousMethodException.class.getTypeName()))
                .body("originalException", Matchers.equalTo(ErrorObject.STD_NOT_APPLICABLE));
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

        given().body(l_call).post(EndPointURL + "call").then().assertThat().statusCode(500)
                .body("title", Matchers.equalTo(IntegroAPI.ERROR_IBS_CONFIG))
                .body("detail", Matchers.containsString(
                        "The given environment value handler"))
                .body("detail", Matchers.containsString(
                        "could not be found."))
                .body("code", Matchers.equalTo(500))
                .body("bridgeServiceException", Matchers.equalTo(IBSConfigurationException.class.getTypeName()))
                .body("originalException", Matchers.equalTo(NonExistentJavaObjectException.class.getTypeName()));
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

        given().body(l_call).post(EndPointURL + "call").then().assertThat().statusCode(404)
                .body("title", Matchers.equalTo(IntegroAPI.ERROR_JAVA_OBJECT_NOT_FOUND))
                .body("detail", Matchers.containsString(
                        "The given class com.adobe.campaign.tests.bridgeservice.testdata.SimpleStaticMethodsNonExisting could not be found."))
                .body("code", Matchers.equalTo(404))
                .body("bridgeServiceException", Matchers.equalTo(NonExistentJavaObjectException.class.getTypeName()))
                .body("originalException", Matchers.equalTo(ErrorObject.STD_NOT_APPLICABLE));

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

        given().body(l_call).post(EndPointURL + "call").then().assertThat().statusCode(404)
                .body("bridgeServiceException", Matchers.equalTo(NonExistentJavaObjectException.class.getTypeName()));
    }

    /**
     * Testing that we provide the correct error messages whenever the target method throws an error
     */
    @Test(groups = "E2E")
    public void testMainEror_passingNull() {
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

        given().body(l_jsonString).post(EndPointURL + "call").then().assertThat().statusCode(404)
                .body("title", Matchers.equalTo(IntegroAPI.ERROR_JSON_TRANSFORMATION));

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

        given().body(jc).post(EndPointURL + "call").then().assertThat().statusCode(408)
                .body("title", Matchers.equalTo(IntegroAPI.ERROR_CALL_TIMEOUT))
                .body("detail", Matchers.containsString(
                        "took longer than the set time limit of"))
                .body("code", Matchers.equalTo(408))
                .body("bridgeServiceException", Matchers.equalTo(IBSTimeOutException.class.getTypeName()))
                .body("originalException", Matchers.equalTo(ErrorObject.STD_NOT_APPLICABLE))
                .body("originalMessage", Matchers.equalTo(ErrorObject.STD_NOT_APPLICABLE));
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

        given().body(jc).post(EndPointURL + "call").then().assertThat().statusCode(408)
                .body("title", Matchers.equalTo(IntegroAPI.ERROR_CALL_TIMEOUT));
    }

    /**
     * Related to issue #34, where certain call combinations create a LinkageError
     */
    //@Test(groups = "E2E", invocationCount = 5001)
    @Test(groups = "E2E")
    public void testIssue34Manual() {
        //long before = MemoryTools.fetchMemory();
        ConfigValueHandlerIBS.INTEGRITY_PACKAGE_INJECTION_MODE.activate("manual");

        ConfigValueHandlerIBS.STATIC_INTEGRITY_PACKAGES.activate(
                "com.adobe.campaign.tests.bridge.testdata.issue34.pckg1");
        JavaCalls l_myJavaCalls = new JavaCalls();

        //Call 1
        CallContent l_cc1 = new CallContent();
        l_cc1.setClassName("com.adobe.campaign.tests.bridge.testdata.issue34.pckg1.CalledClass1");
        l_cc1.setMethodName("calledMethod");
        l_myJavaCalls.getCallContent().put("call1", l_cc1);

        //Call 2
        CallContent l_cc2 = new CallContent();
        l_cc2.setClassName("com.adobe.campaign.tests.bridge.testdata.issue34.pckg1.CalledClass2");
        l_cc2.setMethodName("calledMethod");
        l_myJavaCalls.getCallContent().put("call2", l_cc2);

        /* Problem disappears*/
        ConfigValueHandlerIBS.STATIC_INTEGRITY_PACKAGES.activate(
                "com.adobe.campaign.tests.bridge.testdata.issue34.pckg1.,com.adobe.campaign.tests.bridge.testdata.issue34.pckg2.");

        given().body(l_myJavaCalls).post(EndPointURL + "call").then().assertThat().statusCode(200).
                body("returnValues.call2", Matchers.equalTo("Whatever"));

        //long after = MemoryTools.fetchMemory();
        //System.out.println(before+";"+after+";"+(after-before));

    }

    @Test(groups = "E2E")
    public void testIssue34Automatic() {
        //long before = MemoryTools.fetchMemory();
        ConfigValueHandlerIBS.INTEGRITY_PACKAGE_INJECTION_MODE.activate("automatic");
        JavaCalls l_myJavaCalls = new JavaCalls();

        //Call 1
        CallContent l_cc1 = new CallContent();
        l_cc1.setClassName("com.adobe.campaign.tests.bridge.testdata.issue34.pckg1.CalledClass1");
        l_cc1.setMethodName("calledMethod");
        l_myJavaCalls.getCallContent().put("call1", l_cc1);

        //Call 2
        CallContent l_cc2 = new CallContent();
        l_cc2.setClassName("com.adobe.campaign.tests.bridge.testdata.issue34.pckg1.CalledClass2");
        l_cc2.setMethodName("calledMethod");
        l_myJavaCalls.getCallContent().put("call2", l_cc2);

        /* Problem disappears*/
        ConfigValueHandlerIBS.STATIC_INTEGRITY_PACKAGES.activate(
                "com.adobe.campaign.tests.bridge.testdata.issue34.pckg1.,com.adobe.campaign.tests.bridge.testdata.issue34.pckg2.");

        given().body(l_myJavaCalls).post(EndPointURL + "call").then().assertThat().statusCode(200).
                body("returnValues.call2", Matchers.equalTo("Whatever"));

        //long after = MemoryTools.fetchMemory();
        //System.out.println(before+";"+after+";"+(after-before));
    }

    /**
     * This exception is a little tricky as the message changes depending on the number of executions in a session. The
     * first time, you get: <br/>
     * <i>loader constraint violation: loader 'app' </i>
     * <br/> The second time you get: <br/>
     * <i>loader constraint violation: when resolving method</i>
     * <br/> This is prevalent only when you run all tests in one go.
     */
    @Test(groups = "E2E")
    public void testIssue34Manual_Negative() {
        //long before = MemoryTools.fetchMemory();
        ConfigValueHandlerIBS.INTEGRITY_PACKAGE_INJECTION_MODE.activate("manual");
        ConfigValueHandlerIBS.STATIC_INTEGRITY_PACKAGES.activate(
                "com.adobe.campaign.tests.bridge.testdata.issue34.pckg1");
        JavaCalls l_myJavaCalls = new JavaCalls();

        //Call 1
        CallContent l_cc1 = new CallContent();
        l_cc1.setClassName("com.adobe.campaign.tests.bridge.testdata.issue34.pckg1.CalledClass1");
        l_cc1.setMethodName("calledMethod");
        l_myJavaCalls.getCallContent().put("call1", l_cc1);

        //Call 2
        CallContent l_cc2 = new CallContent();
        l_cc2.setClassName("com.adobe.campaign.tests.bridge.testdata.issue34.pckg1.CalledClass2");
        l_cc2.setMethodName("calledMethod");
        l_myJavaCalls.getCallContent().put("call2", l_cc2);

        given().body(l_myJavaCalls).post(EndPointURL + "call").then().assertThat().statusCode(500).
                body("title", Matchers.equalTo(IntegroAPI.ERROR_IBS_CONFIG))
                .body("code", Matchers.equalTo(500))
                .body("detail", Matchers.startsWith("Linkage Error detected."))
                .body("bridgeServiceException", Matchers.equalTo(IBSConfigurationException.class.getTypeName()))
                .body("originalException", Matchers.equalTo(LinkageError.class.getTypeName()))
                .body("originalMessage", Matchers.startsWith("loader constraint violation:"));

    }

    @Test(groups = "E2E")
    public void test_issue35_callToClassWithNoModifiers() {
        JavaCalls jc = new JavaCalls();
        CallContent l_cc = new CallContent();
        l_cc.setClassName("com.adobe.campaign.tests.bridge.testdata.one.ClassWithNoModifiers");
        l_cc.setMethodName("hello");
        jc.getCallContent().put("one", l_cc);

        given().body(jc).post(EndPointURL + "call").then().assertThat().statusCode(500)
                .body("title", Matchers.equalTo(IntegroAPI.ERROR_IBS_RUNTIME))
                .body("detail", Matchers.startsWith(
                        "java.lang.RuntimeException: We do not have the right to execute the given class."));
    }

    @Test(groups = "E2E", enabled = false)
    public void testIntegroIssue() {
        ConfigValueHandlerIBS.DEFAULT_CALL_TIMEOUT.activate("0");
        JavaCalls l_myJavaCalls = new JavaCalls();

        CallContent l_cc = new CallContent();
        l_cc.setClassName("com.adobe.campaign.tests.integro.tools.RandomManager");
        l_cc.setMethodName("fetchRandomCountry");

        l_myJavaCalls.getCallContent().put("countries", l_cc);

        System.out.println(given().body(l_myJavaCalls).post(EndPointURL + "call").then().extract().asPrettyString());

        given().body(l_myJavaCalls).post(EndPointURL + "call").then().assertThat().statusCode(500)
                .body("originalException", Matchers.equalTo(
                        "java.lang.IllegalAccessError")).body("originalMessage", Matchers.startsWith(
                        "class jdk.internal.reflect.ConstructorAccessorImpl loaded by com.adobe.campaign.tests.bridge.service.IntegroBridgeClassLoader"))
                .body("originalMessage", Matchers.endsWith(
                        " cannot access jdk/internal/reflect superclass jdk.internal.reflect.MagicAccessorImpl"));
    }

    @Test(groups = "E2E")
    public void testIssueWithInternalError() {
        JavaCalls l_myJavaCalls = new JavaCalls();

        CallContent l_cc = new CallContent();

        l_cc.setClassName("com.adobe.campaign.tests.bridge.testdata.one.SimpleStaticMethods");
        l_cc.setMethodName("returnClassWithGet");
        l_myJavaCalls.getCallContent().put("call1", l_cc);

        given().body(l_myJavaCalls).post(EndPointURL + "call").then().assertThat().statusCode(200)
                .body("returnValues.call1.this", Matchers.equalTo(
                        "5"));
    }

    @Test(groups = "E2E")
    public void testInternalErrorCall() {

        ConfigValueHandlerIBS.TEMP_INTERNAL_ERROR_MODE.activate("active");

        JavaCalls l_call = new JavaCalls();
        CallContent myContent = new CallContent();
        myContent.setClassName("com.adobe.campaign.tests.bridge.testdata.one.SimpleStaticMethods");
        myContent.setMethodName("methodReturningMap");
        myContent.setReturnType("java.lang.String");
        l_call.getCallContent().put("call1PL", myContent);

        given().body(l_call).post(EndPointURL + "call").then().assertThat().statusCode(500).body("title",
                Matchers.equalTo(
                        "Internal IBS error. Please file a bug report with the project and provide this JSON in the report."));

    }

    /**
     * We run two paralle threads (as much as possible), and make sure the failed step is correct
     */
    @Test(groups = "E2E")
    public void testExternalErrorCall() {

        JavaCalls l_call = new JavaCalls();
        CallContent myContent = new CallContent();
        myContent.setClassName("com.adobe.campaign.tests.bridge.testdata.one.SimpleStaticMethods");
        myContent.setMethodName("methodCallingMethodThrowingExceptionAndPackingIt");
        myContent.setReturnType("java.lang.String");
        l_call.getCallContent().put("call1PL", myContent);

        given().body(l_call).post(EndPointURL + "call").then().assertThat().statusCode(500)
                .body("title",
                        Matchers.equalTo(
                                "Error during call of target Java Class and Method."))
                .body("originalException", Matchers.equalTo("java.lang.IllegalArgumentException"))
                .body("originalMessage", Matchers.equalTo("Will always throw this"))
                .body("stackTrace[0]", Matchers.equalTo(
                        "com.adobe.campaign.tests.bridge.testdata.one.SimpleStaticMethods.methodThrowsException(SimpleStaticMethods.java:65)"));

    }

    //Testing Error Step detection
    @Test(groups = "E2E")
    public void correctlyDetectErrorSteps() {
        //Method 1   throws exception
        CallContent l_cc1 = new CallContent();
        l_cc1.setClassName("com.adobe.campaign.tests.bridge.testdata.one.ClassWithNoModifiers");
        l_cc1.setMethodName("hello");

        //Method 2
        CallContent l_cc2 = new CallContent();
        l_cc2.setClassName(SimpleStaticMethods.class.getTypeName());
        l_cc2.setMethodName("methodAcceptingStringArgument");
        l_cc2.setArgs(new Object[] { "A" });

        JavaCalls l_call1 = new JavaCalls();
        l_call1.getCallContent().put("step1", l_cc1);
        l_call1.getCallContent().put("step2", l_cc2);

        JavaCalls l_call2 = new JavaCalls();
        l_call2.getCallContent().put("step1", l_cc2);
        l_call2.getCallContent().put("step2", l_cc1);

        final JsonPath[] l_call1Result = new JsonPath[1];
        final JsonPath[] l_call2Result = new JsonPath[1];

        //Define two threads to execute in parallel
        Thread t1 = new Thread(new Runnable() {
            @Override
            public void run() {

                l_call1Result[0] = given().body(l_call1).post(EndPointURL + "call").getBody().jsonPath();
            }
        });

        Thread t2 = new Thread(new Runnable() {
            @Override
            public void run() {

                l_call2Result[0] = given().body(l_call2).post(EndPointURL + "call").getBody().jsonPath();
            }
        });

        //Start threads and wait till they finish
        t1.start();
        t2.start();

        try {
            t1.join();
            t2.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        assertThat("We should be able to get the body", l_call1Result[0].get("title"), Matchers.equalTo(IntegroAPI.ERROR_IBS_RUNTIME));
        assertThat("We should be able to get the body", l_call1Result[0].get("failureAtStep"), Matchers.equalTo("step1"));
        assertThat("We should be able to get the body", l_call2Result[0].get("title"), Matchers.equalTo(IntegroAPI.ERROR_IBS_RUNTIME));
        assertThat("We should be able to get the body", l_call2Result[0].get("failureAtStep"), Matchers.equalTo("step2"));
    }

    @AfterGroups(groups = "E2E", alwaysRun = true)
    public void tearDown() throws IOException {
        ConfigValueHandlerIBS.resetAllValues();
        Spark.stop();
        serverSocket1.close();
    }
}
