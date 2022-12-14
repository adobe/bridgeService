package com.adobe.campaign.tests.service;

import com.adobe.campaign.tests.integro.tools.RandomManager;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.hamcrest.Matchers;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;

public class TestFetchCalls {

    @Test
    public void testJSONCall()
            throws ClassNotFoundException, NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        JavaCalls l_myJavaCalls = new JavaCalls();

        CallContent l_cc = new CallContent();
        l_cc.setClassName(RandomManager.class.getTypeName());
        l_cc.setMethodName("fetchRandomCountry");

        l_cc.setArgs(new Object[]{"A"});

        l_myJavaCalls.getCallContent().put("fetchEmail", l_cc);

        assertThat("We should access our calls correctly", l_myJavaCalls.getCallContent().get("fetchEmail").getClassName(), Matchers.equalTo(RandomManager.class.getTypeName()));
        assertThat("We should access our calls correctly", l_myJavaCalls.getCallContent().get("fetchEmail").getMethodName(), Matchers.equalTo("fetchRandomCountry"));
        assertThat("We should access our calls correctly", l_myJavaCalls.getCallContent().get("fetchEmail").getArgs(), Matchers.arrayContainingInAnyOrder("A"));

        l_myJavaCalls.getCallContent().get("fetchEmail").setArgs(new Object[]{});

        Method l_definedMethod = l_myJavaCalls.getCallContent().get("fetchEmail").fetchMethod();
        assertThat("The method should not be null", l_definedMethod, Matchers.notNullValue());

        assertThat("We should have created the correct method",l_definedMethod.getName(), Matchers.equalTo(
                l_myJavaCalls.getCallContent().get("fetchEmail").getMethodName()));


        String returnedCountry = (String) l_myJavaCalls.getCallContent().get("fetchEmail").call();
        assertThat("We should get a good answer back from the call", Arrays.asList("AT", "AU",
                "CA", "CH", "DE", "US", "FR", "CN", "IN", "JP", "RU", "BR", "ID", "GB", "MX").stream().anyMatch(f -> f.equals(returnedCountry)));


    }

    @Test
    public void testJSONCallWithAruments()
            throws ClassNotFoundException, NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        JavaCalls l_myJavaCalls = new JavaCalls();

        CallContent l_cc = new CallContent();
        l_cc.setClassName(RandomManager.class.getTypeName());
        l_cc.setMethodName("getRandomNumber");

        l_cc.setArgs(new Object[]{13});

        l_myJavaCalls.getCallContent().put("fetchEmail", l_cc);



        assertThat("We should access our calls correctly", l_myJavaCalls.getCallContent().get("fetchEmail").getArgs(), Matchers.arrayContainingInAnyOrder(13));


        Method l_definedMethod = l_myJavaCalls.getCallContent().get("fetchEmail").fetchMethod();
        assertThat("The method should not be null", l_definedMethod, Matchers.notNullValue());

        assertThat("We should have created the correct method",l_definedMethod.getName(), Matchers.equalTo(
                l_myJavaCalls.getCallContent().get("fetchEmail").getMethodName()));


        Object returnedValue = l_myJavaCalls.getCallContent().get("fetchEmail").call();
        assertThat("We should get a good answer back from the call", (Integer) returnedValue, Matchers.lessThan(13));

       
    }

    @Test
    public void testJSONCallWithTwoAruments()
            throws ClassNotFoundException, NoSuchMethodException, InvocationTargetException, IllegalAccessException {

        CallContent l_cc = new CallContent();
        l_cc.setClassName(RandomManager.class.getTypeName());
        l_cc.setMethodName("getUniqueEmail");

        l_cc.setArgs(new Object[]{"A","B"});


        Method l_definedMethod = l_cc.fetchMethod();
        assertThat("The method should not be null", l_definedMethod, Matchers.notNullValue());

        assertThat("We should have created the correct method",l_definedMethod.getName(), Matchers.equalTo(l_cc.getMethodName()));

        assertThat("We should have created the correct method",l_definedMethod.getParameterCount(), Matchers.equalTo(2));

        Object returnedValue = l_cc.call();
        assertThat("We should get a good answer back from the call", returnedValue.toString(), Matchers.startsWith("A+"));
        assertThat("We should get a good answer back from the call", returnedValue.toString(), Matchers.endsWith("@B"));

    }


    @Test
    public void testJSONTransformation()
            throws JsonProcessingException, ClassNotFoundException, NoSuchMethodException, InvocationTargetException,
            IllegalAccessException {
        String l_jsonString = "{\"callContent\" :{\"call1\" :  {"
                + "    \"class\": \""+RandomManager.class.getTypeName()+"\",\n"
                + "    \"method\": \"getUniqueEmail\",\n"
                + "    \"returnType\": \"java.lang.String\",\n"
                + "    \"args\": [\n"
                + "        \"A\",\n"
                + "        \"B\"\n"
                + "    ]\n"
                + "}\n"
                + "}}";

        ObjectMapper mapper = new ObjectMapper();
        JavaCalls fetchedFromJSON = mapper.readValue(l_jsonString, JavaCalls.class);
        CallContent l_cc = fetchedFromJSON.getCallContent().get("call1");
        assertThat(l_cc.getMethodName(), Matchers.equalTo("getUniqueEmail"));

        Method l_definedMethod = l_cc.fetchMethod();
        assertThat("The method should not be null", l_definedMethod, Matchers.notNullValue());

        assertThat("We should have created the correct method",l_definedMethod.getName(), Matchers.equalTo(l_cc.getMethodName()));

        assertThat("We should have created the correct method",l_definedMethod.getParameterCount(), Matchers.equalTo(2));

        Object returnedValue = l_cc.call();
        assertThat("We should get a good answer back from the call", returnedValue.toString(), Matchers.startsWith("A+"));
        assertThat("We should get a good answer back from the call", returnedValue.toString(), Matchers.endsWith("@B"));
    }

    @Test
    public void testJSONTransformation2()
            throws JsonProcessingException, ClassNotFoundException, NoSuchMethodException, InvocationTargetException,
            IllegalAccessException {
        String l_jsonString = "{\"callContent\" :{\"call1\" :  {"
                + "    \"class\": \""+RandomManager.class.getTypeName()+"\",\n"
                + "    \"method\": \"getUniqueEmail\",\n"
                + "    \"returnType\": \"java.lang.String\",\n"
                + "    \"args\": [\n"
                + "        \"A\",\n"
                + "        \"B\"\n"
                + "    ]\n"
                + "}\n"
                + "}}";

        ObjectMapper mapper = new ObjectMapper();
        JavaCalls fetchedFromJSON = mapper.readValue(l_jsonString, JavaCalls.class);

        Assert.assertThrows(CallDefinitionNotFoundException.class, () -> fetchedFromJSON.call("nonExistant"));
        Object returnedValue = fetchedFromJSON.call("call1");
        assertThat("We should get a good answer back from the call", returnedValue.toString(), Matchers.startsWith("A+"));
        assertThat("We should get a good answer back from the call", returnedValue.toString(), Matchers.endsWith("@B"));

        Map<String, Object> l_returnValue = fetchedFromJSON.submitCalls().getReturnValues();
        assertThat("We should have an entry with the key call1", l_returnValue.containsKey("call1"));
        Object returnedValue2 = l_returnValue.get("call1");
        assertThat("We should get a good answer back from the call", returnedValue2.toString(), Matchers.startsWith("A+"));

        assertThat("We should get a good answer back from the call", returnedValue2.toString(), Matchers.endsWith("@B"));
    }

    @Test
    public void testJSONTransformation_deserialize()
            throws JsonProcessingException, ClassNotFoundException, NoSuchMethodException, InvocationTargetException,
            IllegalAccessException {
        String l_jsonString = "{\"callContent\" :{\"call1\" :  {"
                + "    \"class\": \""+RandomManager.class.getTypeName()+"\",\n"
                + "    \"method\": \"getUniqueEmail\",\n"
                + "    \"returnType\": \"java.lang.String\",\n"
                + "    \"args\": [\n"
                + "        \"A\",\n"
                + "        \"B\"\n"
                + "    ]\n"
                + "}\n"
                + "}}";

        ObjectMapper mapper = new ObjectMapper();
        JavaCalls fetchedFromJSON = mapper.readValue(l_jsonString, JavaCalls.class);

        JavaCallResults l_returnValue = fetchedFromJSON.submitCalls();

        System.out.println(mapper.writeValueAsString(l_returnValue));
    }


}
