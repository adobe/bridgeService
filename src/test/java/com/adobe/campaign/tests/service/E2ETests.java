package com.adobe.campaign.tests.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.hamcrest.Matchers;
import org.testng.Assert;
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
        ConfigValueHandler.resetAllValues();
    }

    @Test(groups = "E2E")
    public void testMainHelloWorld() {
        ConfigValueHandler.PRODUCT_VERSION.activate("101");

        given().when().get(EndPointURL + "test").then().assertThat().body(Matchers.startsWith("All systems up"))
                .body(Matchers.endsWith("101"));

        ConfigValueHandler.PRODUCT_USER_VERSION.activate("F");

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
        myContent.setClassName("com.adobe.campaign.tests.integro.tools.RandomManager");
        myContent.setMethodName("getCountries");
        myContent.setReturnType("java.lang.String");
        l_call.getCallContent().put("call1PL", myContent);

        given().body(l_call).post(EndPointURL + "call").then().assertThat().body("returnValues.call1PL",
                Matchers.containsInAnyOrder("AT", "AU", "CA", "CH", "DE", "US", "FR", "CN", "IN", "JP", "RU", "BR",
                        "ID", "GB", "MX"));
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
    @Test(groups = "E2E")
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
    @Test(groups = "E2E")
    public void testMainEror_Case3IBSConfigurationException1() {
        ConfigValueHandler.ENVIRONMENT_VARS_SETTER_CLASS.activate("a.b.c.NonExistingClass");

        JavaCalls l_call = new JavaCalls();
        CallContent myContent = new CallContent();
        myContent.setClassName("com.adobe.campaign.tests.integro.tools.RandomManager");
        myContent.setMethodName("fetchRandomFileName");
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
    public void testMainEror_passiingNull() throws JsonProcessingException {
        ConfigValueHandler.ENVIRONMENT_VARS_SETTER_CLASS.activate("a.b.c.NonExistingClass");

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

    @Test(groups = "E2E")
    public void testIntegrity_pathsSet() {

        ConfigValueHandler.STATIC_INTEGRITY_PACKAGES.activate("com.adobe.campaign.tests.integro.");

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
    @Test(groups = "E2E")
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
                body("returnValues.call2PL", Matchers.endsWith("localhost.corp.adobe.com"))
                .body("returnValues.call2PL", Matchers.startsWith("testqa+"));

    }


    @Test(groups = "E2E")
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

    @AfterGroups(groups = "E2E", alwaysRun = true)
    public void tearDown() throws IOException {
        ConfigValueHandler.resetAllValues();
        Spark.stop();
        serverSocket1.close();
    }
}
