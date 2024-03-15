/*
 * Copyright 2022 Adobe
 * All Rights Reserved.
 *
 * NOTICE: Adobe permits you to use, modify, and distribute this file in
 * accordance with the terms of the Adobe license agreement accompanying
 * it.
 */
package com.adobe.campaign.tests.bridge.service;

import com.adobe.campaign.tests.bridge.service.exceptions.AmbiguousMethodException;
import com.adobe.campaign.tests.bridge.service.exceptions.NonExistentJavaObjectException;
import com.adobe.campaign.tests.bridge.service.exceptions.TargetJavaMethodCallException;
import com.adobe.campaign.tests.bridge.testdata.one.SimpleStaticMethods;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.exc.UnrecognizedPropertyException;
import org.hamcrest.Matchers;
import org.testng.annotations.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Random;

import static io.restassured.RestAssured.given;

//@Test(groups = "E2ERemote")
public class E2ERemoteTests {
    //public static final String EndPointURL = "http://localhost:8080/";
    public static final String EndPointURL = "https://acc-simulators-ibs-dev.rd.campaign.adobe.com/";

    public void testMainHelloWorld() {
        given().when().get(EndPointURL + "test").then().assertThat()
                .body("overALLSystemState", Matchers.equalTo(IntegroAPI.SYSTEM_UP_MESSAGE));
    }

    public void testMainHelloWorld_negative() {
        given().when().get(EndPointURL + "hello").then().assertThat().statusCode(404);
    }

    public void testMainHelloWorldCall() {

        JavaCalls l_call = new JavaCalls();
        CallContent myContent = new CallContent();
        myContent.setClassName("com.adobe.campaign.tests.integro.tools.RandomManager");
        myContent.setMethodName("getCountries");
        myContent.setReturnType("java.lang.String");
        l_call.getCallContent().put("call1PL", myContent);

        given().body(l_call).post(EndPointURL + "call").then().assertThat().body("returnValues.call1PL",
                Matchers.containsInAnyOrder("AT", "AU", "CA", "CH", "DE", "US", "FR", "CN", "IN", "JP", "RU", "BR",
                        "ID", "GB", "MX"));
    }

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

    public void testCheckConnectivity() {

        Map<String, String> urlsMap = new HashMap<>();
        urlsMap.put("url1", "not really a url");
        urlsMap.put("url2", "localhost:" + 8080);

        given().body(urlsMap).post(EndPointURL + "service-check").then().assertThat().statusCode(200)
                .body("url1", Matchers.equalTo(false), "url2", Matchers.equalTo(true));

    }

    /**
     * Testing that we provide the correct error messages whenever the target method throws an error
     */
    public void testMainError_Case1InvocationError() {

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
    public void testMainEror_Case2AmbiguousMethodException() {

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
    public void testMainEror_passiingNull() throws JsonProcessingException {
        //ConfigValueHandlerIBS.ENVIRONMENT_VARS_SETTER_CLASS.activate("a.b.c.NonExistingClass");

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

    public void testIntegrity_pathsSet() {

        ConfigValueHandlerIBS.STATIC_INTEGRITY_PACKAGES.activate("com.adobe.campaign.tests.integro.");

        //Call 1
        JavaCalls l_myJavaCalls = new JavaCalls();

        CallContent l_cc = new CallContent();
        l_cc.setClassName("com.adobe.campaign.tests.integro.tools.RandomManager");
        l_cc.setMethodName("getRandomEmail");

        Properties l_envVars = new Properties();
        l_envVars.put("AC.UITEST.MAILING.PREFIX", "bada");
        l_envVars.put("AC.INTEGRO.MAILING.BASE", "boom.com");

        l_myJavaCalls.setEnvironmentVariables(l_envVars);

        l_myJavaCalls.getCallContent().put("call1PL", l_cc);

        given().body(l_myJavaCalls).post(EndPointURL + "call").then().assertThat().statusCode(200).
                body("returnValues.call1PL", Matchers.endsWith("@boom.com"))
                .body("returnValues.call1PL", Matchers.startsWith("bada"));

        JavaCalls l_myJavaCallsB = new JavaCalls();
        CallContent l_ccB = new CallContent();
        l_ccB.setClassName("com.adobe.campaign.tests.integro.tools.RandomManager");
        l_ccB.setMethodName("getRandomEmail");
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
    public void testIntegrity_case2_pathsNotSet() {

        //Call 1
        JavaCalls l_myJavaCalls = new JavaCalls();
        //l_myJavaCalls.getLocalClassLoader().setPackagePaths(new HashSet<>());

        CallContent l_cc = new CallContent();
        l_cc.setClassName("com.adobe.campaign.tests.integro.tools.RandomManager");
        l_cc.setMethodName("getRandomEmail");

        Properties l_authentication = new Properties();
        l_authentication.put("AC.UITEST.MAILING.PREFIX", "bada");
        l_authentication.put("AC.INTEGRO.MAILING.BASE", "boom.com");

        l_myJavaCalls.setEnvironmentVariables(l_authentication);

        l_myJavaCalls.getCallContent().put("call1PL", l_cc);

        given().body(l_myJavaCalls).post(EndPointURL + "call").then().assertThat().statusCode(200).
                body("returnValues.call1PL", Matchers.endsWith("@boom.com"))
                .body("returnValues.call1PL", Matchers.startsWith("bada"));

        //call 2 -- independant call. No access to the set environment variables
        JavaCalls l_myJavaCallsB = new JavaCalls();
        CallContent l_ccB = new CallContent();
        l_ccB.setClassName("com.adobe.campaign.tests.integro.tools.RandomManager");
        l_ccB.setMethodName("getRandomEmail");
        l_myJavaCallsB.getCallContent().put("call2PL", l_ccB);

        given().body(l_myJavaCallsB).post(EndPointURL + "call").then().assertThat().statusCode(200).
                body("returnValues.call2PL", Matchers.not(Matchers.endsWith("@boom.com")))
                .body("returnValues.call2PL", Matchers.not(Matchers.startsWith("bada")));

    }

    public void testEnvironmentVars() {

        //Call 1
        JavaCalls l_myJavaCalls = new JavaCalls();

        CallContent l_cc = new CallContent();
        l_cc.setClassName("com.adobe.campaign.tests.integro.core.SystemValueHandler");
        l_cc.setMethodName("fetchExecutionProperty");
        l_cc.setArgs(new Object[] { "ABC" });

        Properties l_authentication = new Properties();
        l_authentication.put("ABC", 123);
        l_myJavaCalls.setEnvironmentVariables(l_authentication);

        l_myJavaCalls.getCallContent().put("call1PL", l_cc);

        given().body(l_myJavaCalls).post(EndPointURL + "call").then().assertThat().statusCode(200).
                body("returnValues.call1PL", Matchers.equalTo("123"));
    }

    //@Test
    public void campaignTest() {
        //Call 1
        JavaCalls l_myJavaCalls = new JavaCalls();

        CallContent l_cc = new CallContent();
        l_cc.setClassName("utils.CampaignUtils");
        l_cc.setMethodName("setCurrentAuthenticationToLocalSession");
        l_cc.setArgs(new Object[] { "https://accintg-dev134.rd.campaign.adobe.com/nl/jsp/soaprouter.jsp",
                "___e6b37c15-dad1-4d14-8752-c253f91316b5",
                "@JeSsPAdsjmjXiPy1_tAS-a1_8Yu9LTm2Dq3saDWRCrYDzveLUDU5vAt3fV2WAyrR4FbQ3UjBRbdjDvv3nelC0byLEWCDxaH7g8A7Ttth0JTlHNS4f877FwlxituJzNTP",
                "9576",
                "8.6" });

        Random n = new Random();
        int y = n.nextInt(5000);

        CallContent l_cc2 = new CallContent();
        l_cc2.setClassName("testhelper.NmsRecipientHelper");
        l_cc2.setMethodName("createSimpleRecipient");
        l_cc2.setArgs(new Object[] { "https://accintg-dev134.rd.campaign.adobe.com/nl/jsp/soaprouter.jsp",
                "f" + y, "l" + y, "e" + y + "@adobe.com" });

        Properties l_authentication = new Properties();
        l_authentication.put("ABC", 123);
        l_authentication.put("AC.UITEST.MAILING.PORT", "143");
        l_authentication.put("AC.UITEST.MAILING.PREFIX", "testqa");
        l_authentication.put("AC.UITEST.MAILING.PWD", "changeme");
        l_authentication.put("AC.UITEST.MAILING.PROVIDER", "imap");
        l_authentication.put("AC.UITEST.MAILING.FOLDER", "INBOX");
        l_authentication.put("AC.TEST.IMS.USER.TOKEN.ENABLED", "false");
        l_authentication.put("AC.UITEST.LANGUAGE", "en_US");
        l_authentication.put("AC.UITEST.MAILING.HOST", "acc-simulators.email.corp.adobe.com");
        l_authentication.put("AC.UITEST.MAILING.ID", "testqa@acc-simulators.email.corp.adobe.com");
        l_authentication.put("AC.UITEST.SIMULATORS.PUSH.IOS", "http://acc-simulators.dev.corp.adobe.com:443/");
        l_authentication.put("AC.UITEST.SIMULATORS.PUSH.ANDROID",
                "https://push-simulator.corp.ethos11-stage-va7.ethos.adobe.net/");
        l_myJavaCalls.setEnvironmentVariables(l_authentication);

        l_myJavaCalls.getCallContent().put("connect", l_cc);

        l_myJavaCalls.getCallContent().put("createProfile", l_cc2);

        given().body(l_myJavaCalls).post(EndPointURL + "call").then().assertThat().statusCode(200).
        body("$", Matchers.hasKey("returnValues.createProfile"));
               // body("returnValues.createProfile", Matchers.equalTo("123"));
    }

}
