package com.adobe.campaign.tests.service;

import com.adobe.campaign.tests.integro.tools.RandomManager;
import org.hamcrest.Matchers;
import org.testng.annotations.AfterGroups;
import org.testng.annotations.BeforeGroups;
import org.testng.annotations.Test;
import spark.Spark;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static io.restassured.RestAssured.given;

public class E2ETests {
    public static final String EndPointURL = "http://localhost:4567/";

    @BeforeGroups(groups = "E2E")
    public void startUpService() {
        IntegroAPI iapi = new IntegroAPI();
        iapi.startServices();
        Spark.awaitInitialization();
    }


    @Test(groups = "E2E")
    public void testMainHelloWorld() {
        given().when().get(EndPointURL + "test").then().assertThat().equals("All systems up");

    }

    @Test(groups = "E2E")
    public void testMainHelloWorld_negative() {
        given().when().get(EndPointURL + "hello").then().assertThat().statusCode(404);

    }

    @Test(groups = "E2E")
    public void testMainHelloWorldCall() throws IOException {

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
                .equals(IntegroAPI.ERROR_JSON_TRANSFORMATION);
    }

    @Test(groups = "E2E")
    public void testTestAccess() {
        JavaCallResults jcr = new JavaCallResults();

        given().body(jcr).post(EndPointURL + "call").then().statusCode(400).and().assertThat()
                .equals(IntegroAPI.ERROR_JSON_TRANSFORMATION);
    }

    @Test(groups = "E2E")
    public void testCheckConnectivity() throws IOException {


        Map<String, String> urlsMap = new HashMap<>();
        urlsMap.put("url1", "not really a url");
        urlsMap.put("url2", "acc-simulators.email.corp.adobe.com:143");


        given().body(urlsMap).post(EndPointURL + "service-check").then().assertThat().statusCode(200)
                .body("url1", Matchers.equalTo(false), "url2", Matchers.equalTo(true));

    }

    /**
     * Testing that we provide the correct error messages whenever the target method throws an error
     * @throws IOException
     */
    @Test(groups = "E2E")
    public void testMainEror_Case1InvocationError() throws IOException {

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

    @AfterGroups(groups = "E2E")
    public void tearDown() {
        Spark.stop();
    }
}
