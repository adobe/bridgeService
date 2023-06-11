package com.adobe.campaign.tests.bridge.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.hamcrest.Matchers;

import java.util.Properties;

import static io.restassured.RestAssured.given;

//@Test(groups = "E2ERemote")
public class E2ERemoteTests {
    public static final String EndPointURL = "http://localhost:8080/";

    public void testMainHelloWorld() {
        given().when().get(EndPointURL + "test").then().assertThat().body(Matchers.startsWith("All systems up"));
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
                        "ID", "GB","MX"));
    }

    public void testErrors() {
        JavaCallResults jcr = new JavaCallResults();

        given().body(jcr).post(EndPointURL + "call").then().statusCode(400).and().assertThat()
                .body(Matchers.startsWith(IntegroAPI.ERROR_JSON_TRANSFORMATION));
    }

    public void testTestAccess() {
        JavaCallResults jcr = new JavaCallResults();

        given().body(jcr).post(EndPointURL + "call").then().statusCode(400).and().assertThat()
                .body(Matchers.startsWith(IntegroAPI.ERROR_JSON_TRANSFORMATION));
    }

    /**
     * Testing that we provide the correct error messages whenever the target method throws an error
     */
    public void testMainEror_Case1InvocationError() {

        JavaCalls l_call = new JavaCalls();
        CallContent myContent = new CallContent();
        myContent.setClassName("com.adobe.campaign.tests.integro.tools.RandomManager");
        myContent.setMethodName("getRandomNumber");
        myContent.setReturnType("java.lang.String");
        myContent.setArgs(
                new Object[] { 3, 3 });
        l_call.getCallContent().put("call1PL", myContent);

        given().body(l_call).post(EndPointURL + "call").then().assertThat().statusCode(400).body(
                Matchers.containsString("Minimum number must be strictly inferior than maximum number."));

    }

    /**
     * Testing that we provide the correct error messages whenever the target method throws an error
     */
    public void testMainEror_Case2AmbiguousMethodException() {

        JavaCalls l_call = new JavaCalls();
        CallContent myContent = new CallContent();
        myContent.setClassName("com.adobe.campaign.tests.integro.tools.RandomManager");
        myContent.setMethodName("fetchRandomFileName");
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
    public void testMainEror_Case4A_NonExistantJavaException() {

        JavaCalls l_call = new JavaCalls();
        CallContent myContent = new CallContent();
        myContent.setClassName("com.adobe.campaign.tests.integro.tools.NonExistingRandomManager");
        myContent.setMethodName("getRandomNumber");
        myContent.setReturnType("java.lang.String");
        l_call.getCallContent().put("call1PL", myContent);

        given().body(l_call).post(EndPointURL + "call").then().assertThat().statusCode(400).body(
                Matchers.containsString(IntegroAPI.ERROR_JAVA_OBJECT_NOT_FOUND));
    }

    /**
     * Testing that we provide the correct error messages whenever the target method throws an error
     */
    public void testMainEror_Case4B_NonExistantJavaException() {

        JavaCalls l_call = new JavaCalls();
        CallContent myContent = new CallContent();
        myContent.setClassName("com.adobe.campaign.tests.integro.tools.RandomManager");
        myContent.setMethodName("getRandomNumberNonExisting");
        myContent.setReturnType("java.lang.String");
        l_call.getCallContent().put("call1PL", myContent);

        given().body(l_call).post(EndPointURL + "call").then().assertThat().statusCode(400).body(
                Matchers.containsString(IntegroAPI.ERROR_JAVA_OBJECT_NOT_FOUND));
    }

    /**
     * Testing that we provide the correct error messages whenever the target method throws an error
     */
    public void testMainEror_passiingNull() throws JsonProcessingException {
        ConfigValueHandlerIBS.ENVIRONMENT_VARS_SETTER_CLASS.activate("a.b.c.NonExistingClass");

        String l_jsonString =
                "{\n"
                        + "    \"callContent\": {\n"
                        + "        \"call1\": {\n"
                        + "            \"class\": \"com.adobe.campaign.tests.integro.tools.RandomManager\",\n"
                        + "            \"method\": \"getRandomEmail\",\n"
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
        l_cc.setArgs(new Object[]{"ABC"});

        Properties l_authentication = new Properties();
        l_authentication.put("ABC", 123);
        l_myJavaCalls.setEnvironmentVariables(l_authentication);

        l_myJavaCalls.getCallContent().put("call1PL", l_cc);

        given().body(l_myJavaCalls).post(EndPointURL + "call").then().assertThat().statusCode(200).
                body("returnValues.call1PL", Matchers.equalTo("123"));
    }

}
