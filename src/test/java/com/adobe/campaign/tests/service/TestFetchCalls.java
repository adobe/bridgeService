package com.adobe.campaign.tests.service;

import com.adobe.campaign.tests.integro.core.SystemValueHandler;
import com.adobe.campaign.tests.integro.tools.RandomManager;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.hamcrest.Matchers;
import org.testng.Assert;
import org.testng.annotations.AfterGroups;
import org.testng.annotations.BeforeGroups;
import org.testng.annotations.Test;
import spark.Spark;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Map;
import java.util.Properties;

import static io.restassured.RestAssured.given;
import static org.hamcrest.MatcherAssert.assertThat;

public class TestFetchCalls {

    @Test
    public void testJSONCall()
            throws ClassNotFoundException, NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        JavaCalls l_myJavaCalls = new JavaCalls();

        CallContent l_cc = new CallContent();
        l_cc.setClassName(RandomManager.class.getTypeName());
        l_cc.setMethodName("fetchRandomCountry");

        l_cc.setArgs(new Object[] { "A" });

        l_myJavaCalls.getCallContent().put("fetchEmail", l_cc);

        assertThat("We should access our calls correctly",
                l_myJavaCalls.getCallContent().get("fetchEmail").getClassName(),
                Matchers.equalTo(RandomManager.class.getTypeName()));
        assertThat("We should access our calls correctly",
                l_myJavaCalls.getCallContent().get("fetchEmail").getMethodName(),
                Matchers.equalTo("fetchRandomCountry"));
        assertThat("We should access our calls correctly", l_myJavaCalls.getCallContent().get("fetchEmail").getArgs(),
                Matchers.arrayContainingInAnyOrder("A"));

        l_myJavaCalls.getCallContent().get("fetchEmail").setArgs(new Object[] {});

        Method l_definedMethod = l_myJavaCalls.getCallContent().get("fetchEmail").fetchMethod();
        assertThat("The method should not be null", l_definedMethod, Matchers.notNullValue());

        assertThat("We should have created the correct method", l_definedMethod.getName(),
                Matchers.equalTo(l_myJavaCalls.getCallContent().get("fetchEmail").getMethodName()));

        String returnedCountry = (String) l_myJavaCalls.getCallContent().get("fetchEmail").call();
        assertThat("We should get a good answer back from the call",
                Arrays.asList("AT", "AU", "CA", "CH", "DE", "US", "FR", "CN", "IN", "JP", "RU", "BR", "ID", "GB", "MX")
                        .stream().anyMatch(f -> f.equals(returnedCountry)));

    }

    @Test
    public void testJSONCallWithAruments()
            throws ClassNotFoundException, NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        JavaCalls l_myJavaCalls = new JavaCalls();

        CallContent l_cc = new CallContent();
        l_cc.setClassName(RandomManager.class.getTypeName());
        l_cc.setMethodName("getRandomNumber");

        l_cc.setArgs(new Object[] { 13 });

        l_myJavaCalls.getCallContent().put("fetchEmail", l_cc);

        assertThat("We should access our calls correctly", l_myJavaCalls.getCallContent().get("fetchEmail").getArgs(),
                Matchers.arrayContainingInAnyOrder(13));

        Method l_definedMethod = l_myJavaCalls.getCallContent().get("fetchEmail").fetchMethod();
        assertThat("The method should not be null", l_definedMethod, Matchers.notNullValue());

        assertThat("We should have created the correct method", l_definedMethod.getName(),
                Matchers.equalTo(l_myJavaCalls.getCallContent().get("fetchEmail").getMethodName()));

        Object returnedValue = l_myJavaCalls.getCallContent().get("fetchEmail").call();
        assertThat("We should get a good answer back from the call", (Integer) returnedValue, Matchers.lessThan(13));

    }

    @Test
    public void testJSONCallWithTwoAruments()
            throws ClassNotFoundException, NoSuchMethodException, InvocationTargetException, IllegalAccessException {

        CallContent l_cc = new CallContent();
        l_cc.setClassName(RandomManager.class.getTypeName());
        l_cc.setMethodName("getUniqueEmail");

        l_cc.setArgs(new Object[] { "A", "B" });

        Method l_definedMethod = l_cc.fetchMethod();
        assertThat("The method should not be null", l_definedMethod, Matchers.notNullValue());

        assertThat("We should have created the correct method", l_definedMethod.getName(),
                Matchers.equalTo(l_cc.getMethodName()));

        assertThat("We should have created the correct method", l_definedMethod.getParameterCount(),
                Matchers.equalTo(2));

        Object returnedValue = l_cc.call();
        assertThat("We should get a good answer back from the call", returnedValue.toString(),
                Matchers.startsWith("A+"));
        assertThat("We should get a good answer back from the call", returnedValue.toString(), Matchers.endsWith("@B"));

    }

    @Test
    public void testJSONTransformation()
            throws JsonProcessingException, ClassNotFoundException, NoSuchMethodException, InvocationTargetException,
            IllegalAccessException {
        String l_jsonString =
                "{\"callContent\" :{\"call1\" :  {" + "    \"class\": \"" + RandomManager.class.getTypeName() + "\",\n"
                        + "    \"method\": \"getUniqueEmail\",\n" + "    \"returnType\": \"java.lang.String\",\n"
                        + "    \"args\": [\n" + "        \"A\",\n" + "        \"B\"\n" + "    ]\n" + "}\n" + "}}";

        ObjectMapper mapper = new ObjectMapper();
        JavaCalls fetchedFromJSON = mapper.readValue(l_jsonString, JavaCalls.class);
        CallContent l_cc = fetchedFromJSON.getCallContent().get("call1");
        assertThat(l_cc.getMethodName(), Matchers.equalTo("getUniqueEmail"));

        Method l_definedMethod = l_cc.fetchMethod();
        assertThat("The method should not be null", l_definedMethod, Matchers.notNullValue());

        assertThat("We should have created the correct method", l_definedMethod.getName(),
                Matchers.equalTo(l_cc.getMethodName()));

        assertThat("We should have created the correct method", l_definedMethod.getParameterCount(),
                Matchers.equalTo(2));

        Object returnedValue = l_cc.call();
        assertThat("We should get a good answer back from the call", returnedValue.toString(),
                Matchers.startsWith("A+"));
        assertThat("We should get a good answer back from the call", returnedValue.toString(), Matchers.endsWith("@B"));
    }

    @Test
    public void testJSONTransformation2()
            throws JsonProcessingException, ClassNotFoundException, NoSuchMethodException, InvocationTargetException,
            IllegalAccessException {
        String l_jsonString =
                "{\"callContent\" :{\"call1\" :  {" + "    \"class\": \"" + RandomManager.class.getTypeName() + "\",\n"
                        + "    \"method\": \"getUniqueEmail\",\n" + "    \"returnType\": \"java.lang.String\",\n"
                        + "    \"args\": [\n" + "        \"A\",\n" + "        \"B\"\n" + "    ]\n" + "}\n" + "}}";

        ObjectMapper mapper = new ObjectMapper();
        JavaCalls fetchedFromJSON = mapper.readValue(l_jsonString, JavaCalls.class);

        Assert.assertThrows(CallDefinitionNotFoundException.class, () -> fetchedFromJSON.call("nonExistant"));
        Object returnedValue = fetchedFromJSON.call("call1");
        assertThat("We should get a good answer back from the call", returnedValue.toString(),
                Matchers.startsWith("A+"));
        assertThat("We should get a good answer back from the call", returnedValue.toString(), Matchers.endsWith("@B"));

        Map<String, Object> l_returnValue = fetchedFromJSON.submitCalls().getReturnValues();
        assertThat("We should have an entry with the key call1", l_returnValue.containsKey("call1"));
        Object returnedValue2 = l_returnValue.get("call1");
        assertThat("We should get a good answer back from the call", returnedValue2.toString(),
                Matchers.startsWith("A+"));

        assertThat("We should get a good answer back from the call", returnedValue2.toString(),
                Matchers.endsWith("@B"));
    }

    @Test
    public void testJSONTransformation_deserialize()
            throws JsonProcessingException, ClassNotFoundException, NoSuchMethodException, InvocationTargetException,
            IllegalAccessException {
        String l_jsonString =
                "{\"callContent\" :{\"call1\" :  {" + "    \"class\": \"" + RandomManager.class.getTypeName() + "\",\n"
                        + "    \"method\": \"getUniqueEmail\",\n" + "    \"returnType\": \"java.lang.String\",\n"
                        + "    \"args\": [\n" + "        \"A\",\n" + "        \"B\"\n" + "    ]\n" + "}\n" + "}}";

        ObjectMapper mapper = new ObjectMapper();
        JavaCalls fetchedFromJSON = mapper.readValue(l_jsonString, JavaCalls.class);

        JavaCallResults l_returnValue = fetchedFromJSON.submitCalls();

        assertThat("The retun value should be of a correct format",l_returnValue.returnValues.containsKey("call1"));
        assertThat("The retun value should be of a correct format",l_returnValue.returnValues.get("call1").toString(), Matchers.startsWith("A"));
        assertThat("The retun value should be of a correct format",l_returnValue.returnValues.get("call1").toString(), Matchers.endsWith("B"));

    }

    @Test(enabled = false)
    public void testJSONTransformation_deserializeEmail()
            throws JsonProcessingException, ClassNotFoundException, NoSuchMethodException, InvocationTargetException,
            IllegalAccessException, MessagingException, UnsupportedEncodingException {
        String l_jsonString =
                "{\"callContent\" :{\"call1\" :  {" + "    \"class\": \"" + MimeMessageFactory.class.getTypeName()
                        + "\",\n" + "    \"method\": \"getMessage\",\n" + "    \"returnType\": \"java.lang.String\",\n"
                        + "    \"args\": [" + "    ]\n" + "}\n" + "}}";

        ObjectMapper mapper = new ObjectMapper();
        JavaCalls fetchedFromJSON = mapper.readValue(l_jsonString, JavaCalls.class);

        JavaCallResults l_returnValue = fetchedFromJSON.submitCalls();

        //System.out.println(mapper.writeValueAsString(l_returnValue));
        MimeMessage l_message = MimeMessageFactory.getMessage("one");
        //l_message.writeTo();
    }

    @Test
    public void testValueGenerator() {
        assertThat("We should get the expected field", MetaUtils.extractFieldName("getSubject"),
                Matchers.equalTo("subject"));

        assertThat("We should get the expected field", MetaUtils.extractFieldName("getSubjectMatter"),
                Matchers.equalTo("subjectMatter"));

        assertThat("We should get the expected field", MetaUtils.extractFieldName("fetchValue"),
                Matchers.equalTo("fetchValue"));
    }

    @Test
    public void testDeserializer()
            throws MessagingException, UnsupportedEncodingException, InvocationTargetException, IllegalAccessException {
        String l_suffix = "one";
        Message l_message = MimeMessageFactory.getMessage(l_suffix);
        //List<Message> = ArrayList
        assertThat("This class should not be serializable", !(l_message instanceof Serializable));

        Map<String, Object> l_result = MetaUtils.extractValuesFromObject(l_message);

        assertThat(l_result.keySet(),
                Matchers.containsInAnyOrder("contentType", "size", "content", "subject", "lineCount"));

        assertThat(l_result.get("contentType"), Matchers.equalTo("text/plain"));
        assertThat(l_result.get("size"), Matchers.equalTo(-1));
        assertThat(l_result.get("subject"), Matchers.equalTo("a subject by me " + l_suffix));
        assertThat(l_result.get("content"), Matchers.equalTo("a content by yours truely " + l_suffix));
        assertThat(l_result.get("lineCount"), Matchers.equalTo(-1));

    }

    @Test
    public void testSeparationOfStaticFields()
            throws ClassNotFoundException, InvocationTargetException, NoSuchMethodException, IllegalAccessException,
            JsonProcessingException {

        JavaCalls l_myJavaCalls = new JavaCalls();


        CallContent l_cc = new CallContent();
        l_cc.setClassName("com.adobe.campaign.tests.integro.tools.RandomManager");
        l_cc.setMethodName("getRandomEmail");
        CallContent l_cc2 = new CallContent();
        l_cc2.setClassName("com.adobe.campaign.tests.integro.core.SystemValueHandler");
        l_cc2.setMethodName("setIntegroCache");
        Properties l_authentication = new Properties();
        l_authentication.put("AC.UITEST.MAILING.PREFIX","bada");
        l_authentication.put("AC.INTEGRO.MAILING.BASE","boom.com");
        l_cc2.setArgs(new Object[] { l_authentication });


        l_myJavaCalls.getCallContent().put("aaa", l_cc2);

        l_myJavaCalls.getCallContent().put("getRandomEmail", l_cc);


        JavaCallResults returnedValue = l_myJavaCalls.submitCalls();

        System.out.println(JavaCallsFactory.transformJavaCallResultsToJSON(returnedValue));

        assertThat("We should get a good answer back from the call", returnedValue.getReturnValues().get("getRandomEmail").toString(),
                Matchers.startsWith("bada+"));
        assertThat("We should get a good answer back from the call", returnedValue.getReturnValues().get("getRandomEmail").toString(), Matchers.endsWith("@boom.com"));


        JavaCalls l_myJavaCallsB = new JavaCalls();
        CallContent l_ccB = new CallContent();
        l_ccB.setClassName("com.adobe.campaign.tests.integro.tools.RandomManager");
        l_ccB.setMethodName("getRandomEmail");
        l_myJavaCallsB.getCallContent().put("getRandomEmailB", l_ccB);

        JavaCallResults returnedValueB = l_myJavaCallsB.submitCalls();
        assertThat("We should get a good answer back from the call", returnedValueB.getReturnValues().get("getRandomEmailB").toString(),
                Matchers.startsWith("+"));
        assertThat("We should get a good answer back from the call", returnedValueB.getReturnValues().get("getRandomEmailB").toString(), Matchers.endsWith("@"));




    }

    @BeforeGroups(groups = "E2E")
    public void startUpService() {
        IntegroAPI iapi = new IntegroAPI();
        iapi.startServices();
        Spark.awaitInitialization();
    }

    public static final String EndPointURL = "http://localhost:4567/";

    @Test(groups = "E2E")
    public void testMainHelloWorld() {
        given().when().get(EndPointURL + "hello").then().assertThat().equals("Hello Worlds");

    }

    @Test(groups = "E2E")
    public void testMainHelloWorldCall() throws JsonProcessingException {

        //  RestTools rt = new RestTools("http://localhost:4567/");
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

    @AfterGroups(groups = "E2E")
    public void tearDown() {
        Spark.stop();
    }

}
