/*
 * Copyright 2022 Adobe
 * All Rights Reserved.
 *
 * NOTICE: Adobe permits you to use, modify, and distribute this file in
 * accordance with the terms of the Adobe license agreement accompanying
 * it.
 */
package com.adobe.campaign.tests.bridge.service;

import com.adobe.campaign.tests.bridge.service.data.MyPropertiesHandler;
import com.adobe.campaign.tests.bridge.service.exceptions.*;
import com.adobe.campaign.tests.bridge.testdata.one.EnvironmentVariableHandler;
import com.adobe.campaign.tests.bridge.testdata.one.Instantiable;
import com.adobe.campaign.tests.bridge.testdata.one.SimpleStaticMethods;
import com.adobe.campaign.tests.bridge.testdata.one.StaticType;
import com.adobe.campaign.tests.bridge.testdata.two.StaticMethodsIntegrity;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.hamcrest.Matchers;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;
import java.io.IOException;
import java.io.Serializable;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;

import static org.hamcrest.MatcherAssert.assertThat;

public class TestFetchCalls {
    @BeforeMethod
    @AfterClass
    public void reset() {
        ConfigValueHandlerIBS.resetAllValues();
        MyPropertiesHandler.resetAll();
        EnvironmentVariableHandler.setIntegroCache(new Properties());
        ConfigValueHandlerIBS.ENVIRONMENT_VARS_SETTER_CLASS.activate(EnvironmentVariableHandler.class.getTypeName());
        ConfigValueHandlerIBS.INTEGRITY_PACKAGE_INJECTION_MODE.activate("manual");

    }

    @Test
    public void testSimpleCall()
            throws ClassNotFoundException {
        JavaCalls l_myJavaCalls = new JavaCalls();

        CallContent l_cc = new CallContent();
        l_cc.setClassName(SimpleStaticMethods.class.getTypeName());
        l_cc.setMethodName("methodReturningString");

        l_cc.setArgs(new Object[] { "A" });

        l_myJavaCalls.getCallContent().put("fetchString", l_cc);

        assertThat("We should access our calls correctly",
                l_myJavaCalls.getCallContent().get("fetchString").getClassName(),
                Matchers.equalTo(SimpleStaticMethods.class.getTypeName()));
        assertThat("We should access our calls correctly",
                l_myJavaCalls.getCallContent().get("fetchString").getMethodName(),
                Matchers.equalTo("methodReturningString"));
        assertThat("We should access our calls correctly", l_myJavaCalls.getCallContent().get("fetchString").getArgs(),
                Matchers.arrayContainingInAnyOrder("A"));
        assertThat("We should now have a timeout", l_myJavaCalls.getTimeout(), Matchers.equalTo(Long.parseLong(ConfigValueHandlerIBS.DEFAULT_CALL_TIMEOUT.fetchValue())));

        l_myJavaCalls.getCallContent().get("fetchString").setArgs(new Object[] {});

        IntegroBridgeClassLoader iClassLoader = new IntegroBridgeClassLoader();
        Method l_definedMethod = l_myJavaCalls.getCallContent().get("fetchString").fetchMethod();
        assertThat("The method should not be null", l_definedMethod, Matchers.notNullValue());

        assertThat("We should have created the correct method", l_definedMethod.getName(),
                Matchers.equalTo(l_myJavaCalls.getCallContent().get("fetchString").getMethodName()));

        String returnedString = (String) l_myJavaCalls.getCallContent().get("fetchString").call(iClassLoader);
        assertThat("We should get a good answer back from the call",
                returnedString, Matchers.equalTo(SimpleStaticMethods.SUCCESS_VAL));
    }

    @Test
    public void testSimpleCallWithStringAruments()
            throws ClassNotFoundException {
        JavaCalls l_myJavaCalls = new JavaCalls();

        CallContent l_cc = new CallContent();
        l_cc.setClassName(SimpleStaticMethods.class.getTypeName());
        l_cc.setMethodName("methodAcceptingStringArgument");

        l_cc.setArgs(new Object[] { "13" });

        l_myJavaCalls.getCallContent().put("fetchString", l_cc);

        assertThat("We should access our calls correctly", l_myJavaCalls.getCallContent().get("fetchString").getArgs(),
                Matchers.arrayContainingInAnyOrder("13"));

        Method l_definedMethod = l_myJavaCalls.getCallContent().get("fetchString").fetchMethod();
        assertThat("The method should not be null", l_definedMethod, Matchers.notNullValue());

        assertThat("We should have created the correct method", l_definedMethod.getName(),
                Matchers.equalTo(l_myJavaCalls.getCallContent().get("fetchString").getMethodName()));

        IntegroBridgeClassLoader iClassLoader = new IntegroBridgeClassLoader();
        Object returnedValue = l_myJavaCalls.getCallContent().get("fetchString").call(iClassLoader);
        assertThat("We should get a good answer back from the call", (String) returnedValue,
                Matchers.endsWith(SimpleStaticMethods.SUCCESS_VAL));
    }

    @Test
    public void testSimpleCallWithIntAruments()
            throws ClassNotFoundException {
        JavaCalls l_myJavaCalls = new JavaCalls();

        CallContent l_cc = new CallContent();
        l_cc.setClassName(SimpleStaticMethods.class.getTypeName());
        l_cc.setMethodName("methodAcceptingIntArgument");

        l_cc.setArgs(new Object[] { 13 });

        l_myJavaCalls.getCallContent().put("fetchInt", l_cc);

        assertThat("We should access our calls correctly", l_myJavaCalls.getCallContent().get("fetchInt").getArgs(),
                Matchers.arrayContainingInAnyOrder(13));

        Method l_definedMethod = l_myJavaCalls.getCallContent().get("fetchInt").fetchMethod();
        assertThat("The method should not be null", l_definedMethod, Matchers.notNullValue());

        assertThat("We should have created the correct method", l_definedMethod.getName(),
                Matchers.equalTo(l_myJavaCalls.getCallContent().get("fetchInt").getMethodName()));

        IntegroBridgeClassLoader iClassLoader = new IntegroBridgeClassLoader();
        Object returnedValue = l_myJavaCalls.getCallContent().get("fetchInt").call(iClassLoader);
        assertThat("We should get a good answer back from the call", (Integer) returnedValue, Matchers.equalTo(39));
    }

    @Test
    public void testMakeMultipleCalls() {
        //Call 1
        JavaCalls l_myJavaCalls1 = new JavaCalls();
        CallContent l_cc1 = new CallContent();
        l_cc1.setClassName(SimpleStaticMethods.class.getTypeName());
        l_cc1.setMethodName("methodReturningString");
        l_cc1.setArgs(new Object[] {});
        l_myJavaCalls1.getCallContent().put("fetchCountry", l_cc1);

        CallContent l_cc2 = new CallContent();
        l_cc2.setClassName(SimpleStaticMethods.class.getTypeName());
        l_cc2.setMethodName("methodAcceptingStringArgument");
        l_cc2.setArgs(new Object[] { "A" });
        l_myJavaCalls1.getCallContent().put("fetchEmail", l_cc2);

        JavaCallResults jcr = l_myJavaCalls1.submitCalls();
        assertThat("We should get a good answer back from the call", jcr.getReturnValues().get("fetchEmail").toString(),
                Matchers.startsWith("A"));
        assertThat("We should get a good answer back from the call", jcr.getReturnValues().get("fetchEmail").toString(),
                Matchers.endsWith(SimpleStaticMethods.SUCCESS_VAL));

        //ClassLoader should maintain the context
        assertThat("The Classloader should maintain the cache of the results",
                l_myJavaCalls1.getLocalClassLoader().getCallResultCache().containsKey("fetchEmail"));
        assertThat("The Classloader should maintain the cache of the results",
                l_myJavaCalls1.getLocalClassLoader().getCallResultCache().get("fetchEmail"),
                Matchers.instanceOf(String.class));

    }

    @Test
    public void testJSONCallWithTwoAruments()
            throws ClassNotFoundException {

        CallContent l_cc = new CallContent();
        l_cc.setClassName(SimpleStaticMethods.class.getTypeName());
        l_cc.setMethodName("methodAcceptingTwoArguments");

        l_cc.setArgs(new Object[] { "A", "B" });

        Method l_definedMethod = l_cc.fetchMethod();
        assertThat("The method should not be null", l_definedMethod, Matchers.notNullValue());

        assertThat("We should have created the correct method", l_definedMethod.getName(),
                Matchers.equalTo(l_cc.getMethodName()));

        assertThat("We should have created the correct method", l_definedMethod.getParameterCount(),
                Matchers.equalTo(2));

        IntegroBridgeClassLoader iClassLoader = new IntegroBridgeClassLoader();
        Object returnedValue = l_cc.call(iClassLoader);
        assertThat("We should get a good answer back from the call", returnedValue.toString(),
                Matchers.startsWith("A+B"));
        assertThat("We should get a good answer back from the call", returnedValue.toString(),
                Matchers.endsWith(SimpleStaticMethods.SUCCESS_VAL));

    }

    @Test
    public void testJSONCall_negativeWithBadArguments()
            throws MessagingException {

        CallContent l_cc = new CallContent();
        l_cc.setClassName(SimpleStaticMethods.class.getTypeName());
        l_cc.setMethodName("methodReturningString");

        l_cc.setArgs(new Object[] { MimeMessageFactory.getMessage("ab") });

        IntegroBridgeClassLoader iClassLoader = new IntegroBridgeClassLoader();
        Assert.assertThrows(NonExistentJavaObjectException.class, () -> l_cc.call(iClassLoader));
    }

    @Test
    public void testJSONCall_negativeNonExistingClass()
            throws MessagingException {

        CallContent l_cc = new CallContent();
        l_cc.setClassName("non.existant.class.NoWhereToBeFound");
        l_cc.setMethodName("getRandomString");

        l_cc.setArgs(new Object[] { MimeMessageFactory.getMessage("ab") });

        IntegroBridgeClassLoader iClassLoader = new IntegroBridgeClassLoader();

        Assert.assertThrows(ClassNotFoundException.class, () -> l_cc.fetchMethod());

        Assert.assertThrows(NonExistentJavaObjectException.class, () -> l_cc.call(iClassLoader));

    }

    @Test
    public void testJSONCall_negativeNonExistingMethod() {

        CallContent l_cc = new CallContent();
        l_cc.setClassName(SimpleStaticMethods.class.getTypeName());
        l_cc.setMethodName("getNonExistingRandomString");

        IntegroBridgeClassLoader iClassLoader = new IntegroBridgeClassLoader();
        Assert.assertThrows(NonExistentJavaObjectException.class, () -> l_cc.call(iClassLoader));
    }

    @Test
    public void testCall_negativeIllegalArgumentException() {
        CallContent l_cc = new CallContent();
        l_cc.setClassName(SimpleStaticMethods.class.getTypeName());
        l_cc.setMethodName("complexMethodAcceptor");
        l_cc.setArgs(new Object[] {"HI"});

        IntegroBridgeClassLoader iClassLoader = new IntegroBridgeClassLoader();
        Assert.assertThrows(NonExistentJavaObjectException.class, () -> l_cc.call(iClassLoader));
    }

    @Test
    public void testJSONCallWithFailingTargetMethod() {

        CallContent l_cc = new CallContent();
        l_cc.setClassName(SimpleStaticMethods.class.getTypeName());
        l_cc.setMethodName("methodThrowingException");

        l_cc.setArgs(new Object[] { 5, 5 });

        IntegroBridgeClassLoader iClassLoader = new IntegroBridgeClassLoader();
        Assert.assertThrows(TargetJavaMethodCallException.class, () -> l_cc.call(iClassLoader));
    }

    @Test
    public void testMainHelloWorldCall_problem() throws IOException {

        JavaCalls l_call = new JavaCalls();
        CallContent myContent = new CallContent();
        myContent.setClassName("com.adobe.campaign.tests.bridge.testdata.one.SimpleStaticMethods");
        myContent.setMethodName("methodReturningString");
        myContent.setReturnType("java.lang.String");
        l_call.getCallContent().put("call1PL", myContent);

        ObjectMapper mapper = new ObjectMapper();

        String mappedJSON = mapper.writeValueAsString(l_call);
        System.out.println(mappedJSON);

        JavaCalls j2 = BridgeServiceFactory.createJavaCalls(mappedJSON);

        assertThat("Both calls should be equal", j2.getCallContent().get("call1PL"), Matchers.equalTo(myContent));

        assertThat("Both calls should be equal", j2, Matchers.equalTo(l_call));

        //Equal tests
        assertThat("Both calls should be equal", j2.getCallContent().get("call1PL"),
                Matchers.not(Matchers.equalTo(null)));

        assertThat("Both calls should be equal", j2, Matchers.equalTo(j2));

        assertThat("Both calls should be equal", myContent, Matchers.equalTo(myContent));

        assertThat("Both calls should be equal", j2, Matchers.not(Matchers.equalTo(null)));
    }

    @Test
    public void testJSONTransformation()
            throws IOException, ClassNotFoundException {
        String l_jsonString =
                "{\"callContent\" :{\"call1\" :  {" + "    \"class\": \"" + SimpleStaticMethods.class.getTypeName()
                        + "\",\n"
                        + "    \"method\": \"methodAcceptingTwoArguments\",\n"
                        + "    \"returnType\": \"java.lang.String\",\n"
                        + "    \"args\": [\n" + "        \"A\",\n" + "        \"B\"\n" + "    ]\n" + "}\n" + "}}";

        ObjectMapper mapper = new ObjectMapper();
        JavaCalls fetchedFromJSON = mapper.readValue(l_jsonString, JavaCalls.class);
        CallContent l_cc = fetchedFromJSON.getCallContent().get("call1");
        assertThat(l_cc.getMethodName(), Matchers.equalTo("methodAcceptingTwoArguments"));

        Method l_definedMethod = l_cc.fetchMethod();
        assertThat("The method should not be null", l_definedMethod, Matchers.notNullValue());

        assertThat("We should have created the correct method", l_definedMethod.getName(),
                Matchers.equalTo(l_cc.getMethodName()));

        assertThat("We should have created the correct method", l_definedMethod.getParameterCount(),
                Matchers.equalTo(2));

        IntegroBridgeClassLoader iClassLoader = new IntegroBridgeClassLoader();
        Object returnedValue = l_cc.call(iClassLoader);
        assertThat("We should get a good answer back from the call", returnedValue.toString(),
                Matchers.startsWith("A+B"));
        assertThat("We should get a good answer back from the call", returnedValue.toString(),
                Matchers.containsString("B"));
    }

    @Test
    public void testJSONTransformation2() throws JsonProcessingException {
        String l_jsonString =
                "{\"callContent\" :{\"call1\" :  {" + "    \"class\": \"" + SimpleStaticMethods.class.getTypeName()
                        + "\",\n"
                        + "    \"method\": \"methodAcceptingTwoArguments\",\n"
                        + "    \"returnType\": \"java.lang.String\",\n"
                        + "    \"args\": [\n" + "        \"A\",\n" + "        \"B\"\n" + "    ]\n" + "}\n" + "}}";

        ObjectMapper mapper = new ObjectMapper();
        JavaCalls fetchedFromJSON = mapper.readValue(l_jsonString, JavaCalls.class);

        Assert.assertThrows(CallDefinitionNotFoundException.class, () -> fetchedFromJSON.call("nonExistant"));
        Object returnedValue = fetchedFromJSON.call("call1");
        assertThat("We should get a good answer back from the call", returnedValue.toString(),
                Matchers.startsWith("A+"));
        assertThat("We should get a good answer back from the call", returnedValue.toString(),
                Matchers.endsWith(SimpleStaticMethods.SUCCESS_VAL));

        Map<String, Object> l_returnValue = fetchedFromJSON.submitCalls().getReturnValues();
        assertThat("We should have an entry with the key call1", l_returnValue.containsKey("call1"));
        Object returnedValue2 = l_returnValue.get("call1");
        assertThat("We should get a good answer back from the call", returnedValue2.toString(),
                Matchers.startsWith("A+B"));

        assertThat("We should get a good answer back from the call", returnedValue2.toString(),
                Matchers.endsWith(SimpleStaticMethods.SUCCESS_VAL));
    }

    @Test
    public void testJSONTransformation_deserialize() throws JsonProcessingException {
        String l_jsonString =
                "{\"callContent\" :{\"call1\" :  {" + "    \"class\": \"" + SimpleStaticMethods.class.getTypeName()
                        + "\",\n"
                        + "    \"method\": \"methodAcceptingTwoArguments\",\n"
                        + "    \"returnType\": \"java.lang.String\",\n"
                        + "    \"args\": [\n" + "        \"A\",\n" + "        \"B\"\n" + "    ]\n" + "}\n" + "}}";

        ObjectMapper mapper = new ObjectMapper();
        JavaCalls fetchedFromJSON = mapper.readValue(l_jsonString, JavaCalls.class);

        JavaCallResults l_returnValue = fetchedFromJSON.submitCalls();

        assertThat("The retun value should be of a correct format", l_returnValue.getReturnValues().containsKey("call1"));
        assertThat("The retun value should be of a correct format", l_returnValue.getReturnValues().get("call1").toString(),
                Matchers.startsWith("A"));
        assertThat("The retun value should be of a correct format", l_returnValue.getReturnValues().get("call1").toString(),
                Matchers.endsWith(SimpleStaticMethods.SUCCESS_VAL));

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
            throws MessagingException {
        String l_suffix = "one";
        Message l_message = MimeMessageFactory.getMessage(l_suffix);

        // ObjectMapper mapper = new ObjectMapper();
        // mapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
        //  System.out.println(mapper.writeValueAsString(l_message));

        //List<Message> = ArrayList
        assertThat("This class should not be serializable", !(l_message instanceof Serializable));

        Map<String, Object> l_result = (Map<String, Object>) MetaUtils.extractValuesFromObject(l_message);

        //assertThat("dddd", l_result instanceof Serializable);

        assertThat(l_result.keySet(),
                Matchers.containsInAnyOrder("contentType", "size", "content", "subject", "lineCount", "messageNumber",
                        "hashCode", "isExpunged"));

        assertThat(l_result.get("contentType"), Matchers.equalTo("text/plain"));
        assertThat(l_result.get("size"), Matchers.equalTo(-1));
        assertThat(l_result.get("subject"), Matchers.equalTo("a subject by me " + l_suffix));
        assertThat(l_result.get("content"), Matchers.equalTo("a content by yours truely " + l_suffix));
        assertThat(l_result.get("lineCount"), Matchers.equalTo(-1));

    }

    @Test
    public void testDeserializer_collection()
            throws MessagingException {
        String l_suffix = "two";
        List<Message> l_messages = Collections.singletonList(MimeMessageFactory.getMessage(l_suffix));

        List l_resultList = (List) MetaUtils.extractValuesFromList(l_messages);

        assertThat("We should have an array of one element", l_resultList.size(), Matchers.equalTo(1));

        Map<String, Object> l_result = (Map<String, Object>) l_resultList.get(0);

        assertThat(l_result.keySet(),
                Matchers.containsInAnyOrder("contentType", "size", "content", "subject", "lineCount", "messageNumber",
                        "hashCode", "isExpunged"));

        assertThat(l_result.get("contentType"), Matchers.equalTo("text/plain"));
        assertThat(l_result.get("size"), Matchers.equalTo(-1));
        assertThat(l_result.get("subject"), Matchers.equalTo("a subject by me " + l_suffix));
        assertThat(l_result.get("content"), Matchers.equalTo("a content by yours truely " + l_suffix));
        assertThat(l_result.get("lineCount"), Matchers.equalTo(-1));

    }

    @Test
    public void testDeserializerNull() {
        String l_returnObject = null;

        //List<Message> = ArrayList
        assertThat("This class should not be serializable", !(l_returnObject instanceof Serializable));

        Object extractedReturnObject = (Map<String, Object>) MetaUtils.extractValuesFromObject(l_returnObject);
        assertThat(extractedReturnObject, Matchers.instanceOf(Map.class));

        assertThat(((Map) extractedReturnObject).size(), Matchers.equalTo(0));
    }

    /**
     * Integrity Tests - Here all calls and env vars are in the package path. In this case each call has its own env
     * vars In issue https://git.corp.adobe.com/AdobeCampaignQE/integroBridgeService/issues/48 this is : Case 1 The
     * envvars of calls do not interfere with the others
     */
    @Test
    public void testIntegrityEnvVars_case1_automatedMode() {
        ConfigValueHandlerIBS.INTEGRITY_PACKAGE_INJECTION_MODE.activate("automatic");
        JavaCalls l_myJavaCalls = new JavaCalls();

        CallContent l_cc = new CallContent();
        l_cc.setClassName("com.adobe.campaign.tests.bridge.testdata.two.StaticMethodsIntegrity");
        l_cc.setMethodName("assembleBySystemValues");

        Properties l_envVars = new Properties();
        l_envVars.put("PREFIX", "bada");
        l_envVars.put("SUFFIX", "boom.com");

        l_myJavaCalls.getCallContent().put("getRandomEmail", l_cc);
        l_myJavaCalls.setEnvironmentVariables(l_envVars);

        JavaCallResults returnedValue = l_myJavaCalls.submitCalls();

        assertThat("We should get a good answer back from the call",
                returnedValue.getReturnValues().get("getRandomEmail").toString(),
                Matchers.startsWith("bada+"));
        assertThat("We should get a good answer back from the call",
                returnedValue.getReturnValues().get("getRandomEmail").toString(), Matchers.endsWith("@boom.com"));

        //Call 2
        JavaCalls l_myJavaCallsB = new JavaCalls();
        CallContent l_ccB = new CallContent();
        l_ccB.setClassName("com.adobe.campaign.tests.bridge.testdata.two.StaticMethodsIntegrity");
        l_ccB.setMethodName("assembleBySystemValues");
        l_myJavaCallsB.getCallContent().put("getRandomEmailB", l_ccB);

        Properties l_authenticationB = new Properties();
        l_authenticationB.put("PREFIX", "nana");
        l_authenticationB.put("SUFFIX", "noon.com");
        l_myJavaCallsB.setEnvironmentVariables(l_authenticationB);

        JavaCallResults returnedValueB = l_myJavaCallsB.submitCalls();

        assertThat("We should get a good answer back from the call",
                returnedValueB.getReturnValues().get("getRandomEmailB").toString(),
                Matchers.startsWith("nana+"));
        assertThat("We should get a good answer back from the call",
                returnedValueB.getReturnValues().get("getRandomEmailB").toString(),
                Matchers.endsWith("@noon.com"));
    }

    /**
     * Integrity Tests - Here all calls and env vars are in the package path. In this case each call has its own env
     * vars In issue https://git.corp.adobe.com/AdobeCampaignQE/integroBridgeService/issues/48 this is : Case 1 The
     * envvars of calls do not interfere with the others
     */
    @Test
    public void testIntegrityEnvVars_case1_allPathsSet_semiManualMode() {

        JavaCalls l_myJavaCalls = new JavaCalls();

        CallContent l_cc = new CallContent();
        l_cc.setClassName("com.adobe.campaign.tests.bridge.testdata.two.StaticMethodsIntegrity");
        l_cc.setMethodName("assembleBySystemValues");

        Properties l_envVars = new Properties();
        l_envVars.put("PREFIX", "bada");
        l_envVars.put("SUFFIX", "boom.com");

        l_myJavaCalls.getCallContent().put("getRandomEmail", l_cc);
        l_myJavaCalls.setEnvironmentVariables(l_envVars);

        JavaCallResults returnedValue = l_myJavaCalls.submitCalls();

        assertThat("We should get a good answer back from the call",
                returnedValue.getReturnValues().get("getRandomEmail").toString(),
                Matchers.startsWith("bada+"));
        assertThat("We should get a good answer back from the call",
                returnedValue.getReturnValues().get("getRandomEmail").toString(), Matchers.endsWith("@boom.com"));

        //Call 2
        JavaCalls l_myJavaCallsB = new JavaCalls();
        CallContent l_ccB = new CallContent();
        l_ccB.setClassName("com.adobe.campaign.tests.bridge.testdata.two.StaticMethodsIntegrity");
        l_ccB.setMethodName("assembleBySystemValues");
        l_myJavaCallsB.getCallContent().put("getRandomEmailB", l_ccB);

        Properties l_authenticationB = new Properties();
        l_authenticationB.put("PREFIX", "nana");
        l_authenticationB.put("SUFFIX", "noon.com");
        l_myJavaCallsB.setEnvironmentVariables(l_authenticationB);

        JavaCallResults returnedValueB = l_myJavaCallsB.submitCalls();

        assertThat("We should get a good answer back from the call",
                returnedValueB.getReturnValues().get("getRandomEmailB").toString(),
                Matchers.startsWith("nana+"));
        assertThat("We should get a good answer back from the call",
                returnedValueB.getReturnValues().get("getRandomEmailB").toString(),
                Matchers.endsWith("@noon.com"));
    }

    /**
     * Integrity Tests - Here all calls and env vars are in the package path. In this case each call has its own env vars
     * In issue https://git.corp.adobe.com/AdobeCampaignQE/integroBridgeService/issues/48 this is : Case 1 The envvars
     * of calls do not interfere with the others
     */
    @Test
    public void testIntegrityEnvVars_case1_allPathsSet_manualMode() {
        ConfigValueHandlerIBS.INTEGRITY_PACKAGE_INJECTION_MODE.activate("manual");

        ConfigValueHandlerIBS.STATIC_INTEGRITY_PACKAGES.activate(
                ConfigValueHandlerIBS.ENVIRONMENT_VARS_SETTER_CLASS.fetchValue()
                        + ",com.adobe.campaign.tests.bridge.testdata.two");
        JavaCalls l_myJavaCalls = new JavaCalls();

        CallContent l_cc = new CallContent();
        l_cc.setClassName(StaticMethodsIntegrity.class.getTypeName());
        l_cc.setMethodName("assembleBySystemValues");

        Properties l_envVars = new Properties();
        l_envVars.put("PREFIX", "bada");
        l_envVars.put("SUFFIX", "boom.com");

        l_myJavaCalls.getCallContent().put("getRandomEmail", l_cc);
        l_myJavaCalls.setEnvironmentVariables(l_envVars);

        JavaCallResults returnedValue = l_myJavaCalls.submitCalls();

        assertThat("We should get a good answer back from the call",
                returnedValue.getReturnValues().get("getRandomEmail").toString(),
                Matchers.startsWith("bada+"));
        assertThat("We should get a good answer back from the call",
                returnedValue.getReturnValues().get("getRandomEmail").toString(), Matchers.endsWith("@boom.com"));

        //Call 2
        JavaCalls l_myJavaCallsB = new JavaCalls();
        CallContent l_ccB = new CallContent();
        l_ccB.setClassName(StaticMethodsIntegrity.class.getTypeName());
        l_ccB.setMethodName("assembleBySystemValues");
        l_myJavaCallsB.getCallContent().put("getRandomEmailB", l_ccB);

        Properties l_authenticationB = new Properties();
        l_authenticationB.put("PREFIX", "nana");
        l_authenticationB.put("SUFFIX", "noon.com");
        l_myJavaCallsB.setEnvironmentVariables(l_authenticationB);

        JavaCallResults returnedValueB = l_myJavaCallsB.submitCalls();

        assertThat("We should get a good answer back from the call",
                returnedValueB.getReturnValues().get("getRandomEmailB").toString(),
                Matchers.startsWith("nana+"));
        assertThat("We should get a good answer back from the call",
                returnedValueB.getReturnValues().get("getRandomEmailB").toString(),
                Matchers.endsWith("@noon.com"));
    }

    /**
     * Integrity Tests - Here all calls and env vars are in the package path. In this case call2 doesn't have its own
     * env var In issue https://git.corp.adobe.com/AdobeCampaignQE/integroBridgeService/issues/48 this is : Case 1 The
     * envvars of calls do not interfere with the others
     */
    @Test
    public void testIntegrityEnvVars_case1B_allPathsSet_semiManualMode() {
        ConfigValueHandlerIBS.INTEGRITY_PACKAGE_INJECTION_MODE.activate("semi-manual");
        JavaCalls l_myJavaCalls = new JavaCalls();

        CallContent l_cc = new CallContent();
        l_cc.setClassName(StaticMethodsIntegrity.class.getTypeName());
        l_cc.setMethodName("assembleBySystemValues");

        Properties l_envVars = new Properties();
        l_envVars.put("PREFIX", "bada");
        l_envVars.put("SUFFIX", "boom.com");

        l_myJavaCalls.getCallContent().put("getRandomEmail", l_cc);
        l_myJavaCalls.setEnvironmentVariables(l_envVars);

        JavaCallResults returnedValue = l_myJavaCalls.submitCalls();

        assertThat("We should get a good answer back from the call",
                returnedValue.getReturnValues().get("getRandomEmail").toString(),
                Matchers.startsWith("bada+"));
        assertThat("We should get a good answer back from the call",
                returnedValue.getReturnValues().get("getRandomEmail").toString(), Matchers.endsWith("@boom.com"));

        //Call 2
        JavaCalls l_myJavaCallsB = new JavaCalls();
        CallContent l_ccB = new CallContent();
        l_ccB.setClassName(StaticMethodsIntegrity.class.getTypeName());
        l_ccB.setMethodName("assembleBySystemValues");
        l_myJavaCallsB.getCallContent().put("getRandomEmailB", l_ccB);

        JavaCallResults returnedValueB = l_myJavaCallsB.submitCalls();

        assertThat("We should get a good answer back from the call",
                returnedValueB.getReturnValues().get("getRandomEmailB").toString(),
                Matchers.not(Matchers.startsWith("bada+")));
        assertThat("We should get a good answer back from the call",
                returnedValueB.getReturnValues().get("getRandomEmailB").toString(),
                Matchers.not(Matchers.endsWith("@boom.com")));
    }

    /**
     * Integrity Tests - Here the envvars are not in the context, but our calls are. In this casee we use the same
     * method as is used In issue https://git.corp.adobe.com/AdobeCampaignQE/integroBridgeService/issues/48 this is :
     * Case 3 The envvars of calls do not interfere with the others
     */
    @Test
    public void testIntegrityEnvVars_case3_envNotInPathCallInPath_semiManualMode() {

        JavaCalls l_myJavaCalls = new JavaCalls();

        CallContent l_cc = new CallContent();
        l_cc.setClassName(StaticMethodsIntegrity.class.getTypeName());
        l_cc.setMethodName("assembleBySystemValues");

        Properties l_envVars = new Properties();
        l_envVars.put("PREFIX", "bada");
        l_envVars.put("SUFFIX", "boom.com");

        l_myJavaCalls.getCallContent().put("call1", l_cc);
        l_myJavaCalls.setEnvironmentVariables(l_envVars);

        //Remove the integrity paths
        l_myJavaCalls.getLocalClassLoader().setPackagePaths(new HashSet<>());
        //set call path to integrity path
        l_myJavaCalls.getLocalClassLoader().getPackagePaths().add("com.adobe.campaign.tests.integro.tools.");

        JavaCallResults returnedValue = l_myJavaCalls.submitCalls();

        assertThat("We should get a good answer back from the call",
                returnedValue.getReturnValues().get("call1").toString(),
                Matchers.startsWith("bada+"));
        assertThat("We should get a good answer back from the call",
                returnedValue.getReturnValues().get("call1").toString(),
                Matchers.endsWith("@boom.com"));

        //Call 2 - in this case our second call cannot access the values of the first
        JavaCalls l_myJavaCallsB = new JavaCalls();
        CallContent l_ccB = new CallContent();
        l_ccB.setClassName(EnvironmentVariableHandler.class.getTypeName());
        l_ccB.setMethodName("getCacheProperty");
        l_ccB.setArgs(new Object[] { "PREFIX" });
        l_myJavaCallsB.getCallContent().put("getProperty", l_ccB);

        JavaCallResults returnedValueB = l_myJavaCallsB.submitCalls();

        assertThat("We should get a good answer back from the call",
                returnedValueB.getReturnValues().get("getProperty").toString(),
                Matchers.not(Matchers.startsWith("nana+")));
    }

    /**
     * Integrity Tests - Here the envvars are not in the context, but our calls are. In issue
     * https://git.corp.adobe.com/AdobeCampaignQE/integroBridgeService/issues/48 this is : Case 3 The envvars of calls
     * do not interfere with the others
     */
    @Test
    public void testIntegrityEnvVars_case3B_allPathsSet_semiManualModeNEW() {
        ConfigValueHandlerIBS.STATIC_INTEGRITY_PACKAGES.activate("com.adobe.campaign.tests.bridgeservice.");

        JavaCalls l_myJavaCalls = new JavaCalls();

        CallContent l_cc = new CallContent();
        l_cc.setClassName(StaticMethodsIntegrity.class.getTypeName());
        l_cc.setMethodName("assembleBySystemValues");

        Properties l_envVars = new Properties();
        l_envVars.put("PREFIX", "bada");
        l_envVars.put("SUFFIX", "boom.com");

        l_myJavaCalls.getCallContent().put("getRandomEmail", l_cc);
        l_myJavaCalls.setEnvironmentVariables(l_envVars);

        //Remove the integrity paths
        l_myJavaCalls.getLocalClassLoader().setPackagePaths(new HashSet<>());
        //set call path to integrity path
        l_myJavaCalls.getLocalClassLoader().getPackagePaths().add("com.adobe.campaign.tests.bridgeservice.");

        JavaCallResults returnedValue = l_myJavaCalls.submitCalls();

        assertThat("We should get a good answer back from the call",
                returnedValue.getReturnValues().get("getRandomEmail").toString(),
                Matchers.startsWith("bada+"));
        assertThat("We should get a good answer back from the call",
                returnedValue.getReturnValues().get("getRandomEmail").toString(),
                Matchers.endsWith("@boom.com"));

        //Call 2
        JavaCalls l_myJavaCallsB = new JavaCalls();

        l_myJavaCallsB.getCallContent().put("getProperty", l_cc);

        Properties l_authenticationB = new Properties();
        l_authenticationB.put("PREFIX", "nana");
        l_authenticationB.put("SUFFIX", "noon.com");
        l_myJavaCallsB.setEnvironmentVariables(l_authenticationB);

        //Remove the integrity paths
        l_myJavaCallsB.getLocalClassLoader().setPackagePaths(new HashSet<>());
        //set call path to integrity path
        l_myJavaCallsB.getLocalClassLoader().getPackagePaths().add("com.adobe.campaign.tests.bridgeservice.");

        JavaCallResults returnedValueB = l_myJavaCallsB.submitCalls();

        assertThat("We should get a good answer back from the call",
                returnedValueB.getReturnValues().get("getProperty").toString(),
                Matchers.startsWith("nana+"));
        assertThat("We should get a good answer back from the call",
                returnedValueB.getReturnValues().get("getProperty").toString(),
                Matchers.endsWith("@noon.com"));
    }

    /**
     * Integrity Tests - Here the envvars are not in the context, but our calls are. In issue
     * https://git.corp.adobe.com/AdobeCampaignQE/integroBridgeService/issues/48 this is : Case 3 The envvars of calls
     * do not interfere with the others
     */
    @Test
    public void testIntegrityEnvVars_case3B_allPathsSet_manualMode() {
        ConfigValueHandlerIBS.INTEGRITY_PACKAGE_INJECTION_MODE.activate("manual");
        ConfigValueHandlerIBS.STATIC_INTEGRITY_PACKAGES.activate("com.adobe.campaign.tests.bridgeservice.");

        JavaCalls l_myJavaCalls = new JavaCalls();

        CallContent l_cc = new CallContent();
        l_cc.setClassName(StaticMethodsIntegrity.class.getTypeName());
        l_cc.setMethodName("assembleBySystemValues");

        Properties l_envVars = new Properties();
        l_envVars.put("PREFIX", "bada");
        l_envVars.put("SUFFIX", "boom.com");

        l_myJavaCalls.getCallContent().put("getRandomEmail", l_cc);
        l_myJavaCalls.setEnvironmentVariables(l_envVars);

        JavaCallResults returnedValue = l_myJavaCalls.submitCalls();

        assertThat("We should get a good answer back from the call",
                returnedValue.getReturnValues().get("getRandomEmail").toString(),
                Matchers.startsWith("bada+"));
        assertThat("We should get a good answer back from the call",
                returnedValue.getReturnValues().get("getRandomEmail").toString(),
                Matchers.endsWith("@boom.com"));

        //Call 2
        JavaCalls l_myJavaCallsB = new JavaCalls();

        l_myJavaCallsB.getCallContent().put("getProperty", l_cc);

        Properties l_authenticationB = new Properties();
        l_authenticationB.put("PREFIX", "nana");
        l_authenticationB.put("SUFFIX", "noon.com");
        l_myJavaCallsB.setEnvironmentVariables(l_authenticationB);

        JavaCallResults returnedValueB = l_myJavaCallsB.submitCalls();

        assertThat("We should get a good answer back from the call",
                returnedValueB.getReturnValues().get("getProperty").toString(),
                Matchers.startsWith("nana+"));
        assertThat("We should get a good answer back from the call",
                returnedValueB.getReturnValues().get("getProperty").toString(),
                Matchers.endsWith("@noon.com"));
    }

    /**
     * Here we do not set package paths In issue
     * https://git.corp.adobe.com/AdobeCampaignQE/integroBridgeService/issues/48 this is case 4 Both Env vars and
     * classes are not part of the integrity paths therefore the effects of the env vars is dispatched to all
     * consecutive calls including the class call itself
     */
    @Test
    public void testIntegrityEnvVars_case4_noPackagesInIntegrityPath_semiManualMode() {
        ConfigValueHandlerIBS.INTEGRITY_PACKAGE_INJECTION_MODE.activate("semi-manual");
        JavaCalls l_myJavaCalls = new JavaCalls();

        CallContent l_cc = new CallContent();
        l_cc.setClassName(StaticMethodsIntegrity.class.getTypeName());
        l_cc.setMethodName("assembleBySystemValues");

        Properties l_envVars = new Properties();

        l_envVars.put("PREFIX", "bada");
        l_envVars.put("SUFFIX", "boom.com");

        l_myJavaCalls.getCallContent().put("getRandomEmail", l_cc);
        l_myJavaCalls.setEnvironmentVariables(l_envVars);

        JavaCallResults returnedValue = l_myJavaCalls.submitCalls();

        assertThat("We should get a good answer back from the call",
                returnedValue.getReturnValues().get("getRandomEmail").toString(),
                Matchers.startsWith("bada+"));
        assertThat("We should get a good answer back from the call",
                returnedValue.getReturnValues().get("getRandomEmail").toString(), Matchers.endsWith("@boom.com"));

        //Call 2    -- In this case the Environment
        JavaCalls l_myJavaCallsB = new JavaCalls();
        CallContent l_ccB = new CallContent();
        l_ccB.setClassName(StaticMethodsIntegrity.class.getTypeName());
        l_ccB.setMethodName("assembleBySystemValues");
        l_myJavaCallsB.getCallContent().put("getRandomEmailB", l_ccB);

        JavaCallResults returnedValueB = l_myJavaCallsB.submitCalls();

        assertThat("We should get a good answer back from the call",
                returnedValueB.getReturnValues().get("getRandomEmailB").toString(),
                Matchers.startsWith("null+c@"));
        assertThat("We should get a good answer back from the call",
                returnedValueB.getReturnValues().get("getRandomEmailB").toString(),
                Matchers.endsWith("@null"));
    }

    /**
     * Here we do not set package paths In issue
     * https://git.corp.adobe.com/AdobeCampaignQE/integroBridgeService/issues/48 this is case 4 Both Env vars and
     * classes are not part of the integrity paths therefore the effects of the env vars is dispatched to all
     * consecutive calls including the class call itself
     */
    @Test
    public void testIntegrityEnvVars_case4_noPackagesInIntegrityPath_manualMode() {
        ConfigValueHandlerIBS.INTEGRITY_PACKAGE_INJECTION_MODE.activate("manual");

        JavaCalls l_myJavaCalls = new JavaCalls();

        CallContent l_cc = new CallContent();
        l_cc.setClassName(StaticMethodsIntegrity.class.getTypeName());
        l_cc.setMethodName("assembleBySystemValues");

        Properties l_envVars = new Properties();

        l_envVars.put("PREFIX", "bada");
        l_envVars.put("SUFFIX", "boom.com");

        l_myJavaCalls.getCallContent().put("getRandomEmail", l_cc);
        l_myJavaCalls.setEnvironmentVariables(l_envVars);

        //Remove the integrity paths
        l_myJavaCalls.getLocalClassLoader().setPackagePaths(new HashSet<>());

        JavaCallResults returnedValue = l_myJavaCalls.submitCalls();

        assertThat("We should get a good answer back from the call",
                returnedValue.getReturnValues().get("getRandomEmail").toString(),
                Matchers.startsWith("bada+"));
        assertThat("We should get a good answer back from the call",
                returnedValue.getReturnValues().get("getRandomEmail").toString(), Matchers.endsWith("@boom.com"));

        //Call 2    -- In this case the Environment
        JavaCalls l_myJavaCallsB = new JavaCalls();
        CallContent l_ccB = new CallContent();
        l_ccB.setClassName(StaticMethodsIntegrity.class.getTypeName());
        l_ccB.setMethodName("assembleBySystemValues");
        l_myJavaCallsB.getCallContent().put("getRandomEmailB", l_ccB);

        //Remove the integrity paths
        l_myJavaCallsB.getLocalClassLoader().setPackagePaths(new HashSet<>());

        JavaCallResults returnedValueB = l_myJavaCallsB.submitCalls();

        assertThat("We should get a good answer back from the call",
                returnedValueB.getReturnValues().get("getRandomEmailB").toString(),
                Matchers.startsWith("bada+"));
        assertThat("We should get a good answer back from the call",
                returnedValueB.getReturnValues().get("getRandomEmailB").toString(),
                Matchers.endsWith("@boom.com"));
    }

    /**
     * In this case the packages of the SystemValueHandler are added at the constructor time of Java calls. However, we
     * do not include the package path of the java call itself In issue
     * https://git.corp.adobe.com/AdobeCampaignQE/integroBridgeService/issues/48 this is case 2
     */
    @Test
    public void testIntegrityEnvVars_case2_withEnvVarPathsIncludedButNotCallPath_automatic() {
        ConfigValueHandlerIBS.INTEGRITY_PACKAGE_INJECTION_MODE.activate("automatic");
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

        assertThat("We should not have the env vars integrity path set",
                l_myJavaCalls.getLocalClassLoader().getPackagePaths().stream()
                        .noneMatch(
                                x -> ConfigValueHandlerIBS.ENVIRONMENT_VARS_SETTER_CLASS.fetchValue().startsWith(x)));

        assertThat("Our class package should not yet be in the integrity path",
                l_myJavaCalls.getLocalClassLoader().getPackagePaths().stream()
                        .noneMatch(x -> x.equals("com.adobe.campaign.tests.bridgeservice.testdata2")));

        JavaCallResults returnedValueA = l_myJavaCalls.submitCalls();

        assertThat("The integrity path should not have been set. ",
                l_myJavaCalls.getLocalClassLoader().getPackagePaths().stream()
                        .noneMatch(x -> x.equals("com.adobe.campaign.tests.bridge.testdata.two")));

        assertThat("We should not have the envvars integrity path set",
                l_myJavaCalls.getLocalClassLoader().getPackagePaths().stream()
                        .noneMatch(x -> ConfigValueHandlerIBS.ENVIRONMENT_VARS_SETTER_CLASS.fetchValue().startsWith(x)));

        assertThat("We should get a good answer back from the call",
                returnedValueA.getReturnValues().get("call1PL").toString(),
                Matchers.startsWith("bada+"));
        assertThat("We should get a good answer back from the call",
                returnedValueA.getReturnValues().get("call1PL").toString(),
                Matchers.endsWith("@boom.com"));

    }


    /**
     * In this case the packages of the SystemValueHandler are added at the constructor time of Java calls. However, we
     * do not include the package path of the java call itself In issue
     * https://git.corp.adobe.com/AdobeCampaignQE/integroBridgeService/issues/48 this is case 2
     */
    @Test
    public void testIntegrityEnvVars_case2_withEnvVarPathsIncludedButNotCallPath_semiManualMode() {
        ConfigValueHandlerIBS.INTEGRITY_PACKAGE_INJECTION_MODE.activate("semi-manual");
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

        assertThat("We should not have the env vars integrity path set",
                l_myJavaCalls.getLocalClassLoader().getPackagePaths().stream()
                        .noneMatch(
                                x -> ConfigValueHandlerIBS.ENVIRONMENT_VARS_SETTER_CLASS.fetchValue().startsWith(x)));

        assertThat("Our class package should not yet be in the integrity path",
                l_myJavaCalls.getLocalClassLoader().getPackagePaths().stream()
                        .noneMatch(x -> x.equals("com.adobe.campaign.tests.bridgeservice.testdata2")));

        JavaCallResults returnedValueA = l_myJavaCalls.submitCalls();

        assertThat("We should now have added the our class path to the integrity path",
                l_myJavaCalls.getLocalClassLoader().getPackagePaths().stream()
                        .anyMatch(x -> x.equals("com.adobe.campaign.tests.bridge.testdata.two")));

        assertThat("We should not have the envvars integrity path set",
                l_myJavaCalls.getLocalClassLoader().getPackagePaths().stream()
                        .anyMatch(x -> ConfigValueHandlerIBS.ENVIRONMENT_VARS_SETTER_CLASS.fetchValue().startsWith(x)));

        assertThat("We should get a good answer back from the call",
                returnedValueA.getReturnValues().get("call1PL").toString(),
                Matchers.startsWith("bada+"));
        assertThat("We should get a good answer back from the call",
                returnedValueA.getReturnValues().get("call1PL").toString(),
                Matchers.endsWith("@boom.com"));

    }

    /**
     * In this case the packages of the SystemValueHandler are added at the constructor time of Java calls. However, we
     * do not include the package path of the java call itself In issue
     * https://git.corp.adobe.com/AdobeCampaignQE/integroBridgeService/issues/48 this is case 2
     * <p>
     * In this case we test how the system will work by default if we do not have auto injection
     */
    @Test
    public void testIntegrityEnvVars_case2_withEnvVarPathsIncludedButNotCallPath_manualMode() {
        ConfigValueHandlerIBS.INTEGRITY_PACKAGE_INJECTION_MODE.activate("manual");
        ConfigValueHandlerIBS.STATIC_INTEGRITY_PACKAGES.activate(
                ConfigValueHandlerIBS.ENVIRONMENT_VARS_SETTER_CLASS.fetchValue());

        //Call 1
        JavaCalls l_myJavaCalls = new JavaCalls();

        CallContent l_cc = new CallContent();
        l_cc.setClassName(StaticMethodsIntegrity.class.getTypeName());
        l_cc.setMethodName("assembleBySystemValues");

        Properties l_envVars = new Properties();
        l_envVars.put("PREFIX", "bada");
        l_envVars.put("SUFFIX", "boom.com");

        l_myJavaCalls.setEnvironmentVariables(l_envVars);

        l_myJavaCalls.getLocalClassLoader().getPackagePaths()
                .add(ConfigValueHandlerIBS.ENVIRONMENT_VARS_SETTER_CLASS.fetchValue());

        l_myJavaCalls.getCallContent().put("call1PL", l_cc);
        assertThat("We should not have had the envvars integrity path set",
                l_myJavaCalls.getLocalClassLoader().getPackagePaths().stream()
                        .noneMatch(x -> l_cc.getClassName().startsWith(x)));

        JavaCallResults returnedValueA = l_myJavaCalls.submitCalls();

        assertThat("We should not have added the our class path to the integrity path",
                l_myJavaCalls.getLocalClassLoader().getPackagePaths().stream()
                        .noneMatch(x -> x.equals("com.adobe.campaign.tests.integro.tools")));

        assertThat("We should get a good answer back from the call",
                returnedValueA.getReturnValues().get("call1PL").toString(),
                Matchers.startsWith("null+"));
        assertThat("We should get a good answer back from the call",
                returnedValueA.getReturnValues().get("call1PL").toString(),
                Matchers.endsWith("@null"));

    }

    /**
     * In this case the packages of the SystemValueHandler are added at the constructor time of Java calls. However, we
     * do not include the package path of the java call itself In issue
     * https://git.corp.adobe.com/AdobeCampaignQE/integroBridgeService/issues/48 this is case 2
     * <p>
     * In this case we are using an unknown mode. If the mode is not within "manual","semi-manual" or "automatic" the
     * default mode is taken , I.e. automatic.
     */
    @Test
    public void testIntegrityEnvVars_case2_withEnvVarPathsIncludedButNotCallPath_negativeUnknownMode() {
        ConfigValueHandlerIBS.INTEGRITY_PACKAGE_INJECTION_MODE.activate("non-existant");
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

        assertThat("We should not have the env vars integrity path set",
                l_myJavaCalls.getLocalClassLoader().getPackagePaths().stream()
                        .noneMatch(
                                x -> ConfigValueHandlerIBS.ENVIRONMENT_VARS_SETTER_CLASS.fetchValue().startsWith(x)));

        assertThat("Our class package should not yet be in the integrity path",
                l_myJavaCalls.getLocalClassLoader().getPackagePaths().stream()
                        .noneMatch(x -> x.equals("com.adobe.campaign.tests.bridgeservice.testdata2")));

        JavaCallResults returnedValueA = l_myJavaCalls.submitCalls();

        assertThat("The integrity path should not have been set. ",
                l_myJavaCalls.getLocalClassLoader().getPackagePaths().stream()
                        .noneMatch(x -> x.equals("com.adobe.campaign.tests.bridge.testdata.two")));

        assertThat("We should not have the envvars integrity path set",
                l_myJavaCalls.getLocalClassLoader().getPackagePaths().stream()
                        .noneMatch(x -> ConfigValueHandlerIBS.ENVIRONMENT_VARS_SETTER_CLASS.fetchValue().startsWith(x)));

        assertThat("We should get a good answer back from the call",
                returnedValueA.getReturnValues().get("call1PL").toString(),
                Matchers.startsWith("bada+"));
        assertThat("We should get a good answer back from the call",
                returnedValueA.getReturnValues().get("call1PL").toString(),
                Matchers.endsWith("@boom.com"));

    }

    @Test
    public void testSeparationOfStaticFields_json() throws IOException {

        String l_jsonString =
                "{\n"
                        + "    \"callContent\": {\n"
                        + "        \"call1\": {\n"
                        + "            \"class\": \"com.adobe.campaign.tests.bridge.testdata.two.StaticMethodsIntegrity\",\n"
                        + "            \"method\": \"assembleBySystemValues\",\n"
                        + "            \"returnType\": \"java.lang.String\",\n"
                        + "            \"args\": []\n"
                        + "        }\n"
                        + "    },\n"
                        + "    \"environmentVariables\": {\n"
                        + "        \"PREFIX\": \"tyrone\",\n"
                        + "        \"SUFFIX\" : \"profane.com\"\n"
                        + "    }\n"
                        + "}";

        ObjectMapper mapper = new ObjectMapper();
        JavaCalls fetchedFromJSON = mapper.readValue(l_jsonString, JavaCalls.class);

        JavaCallResults returnedValue = fetchedFromJSON.submitCalls();

        System.out.println(BridgeServiceFactory.transformJavaCallResultsToJSON(returnedValue));

        assertThat("We should get a good answer back from the call",
                returnedValue.getReturnValues().get("call1").toString(),
                Matchers.startsWith("tyrone+"));

        assertThat("We should get a good answer back from the call",
                returnedValue.getReturnValues().get("call1").toString(), Matchers.endsWith("@profane.com"));

    }

    @Test
    public void testIssueWithAmbiguousCallException() {
        CallContent l_cc = new CallContent();
        l_cc.setClassName(SimpleStaticMethods.class.getTypeName());
        l_cc.setMethodName("overLoadedMethod1Arg");
        l_cc.setArgs(new Object[] { "testqa+krs3726@acc-simulators.email.corp.adobe.com" });
        IntegroBridgeClassLoader ibcl = new IntegroBridgeClassLoader();

        Assert.assertThrows(AmbiguousMethodException.class,
                () -> l_cc.fetchMethod(ibcl.loadClass(l_cc.getClassName())));

    }

    @Test(enabled = false)
    public void testIssueWithAmbiguousCall() throws ClassNotFoundException {
        CallContent l_cc = new CallContent();
        l_cc.setClassName(SimpleStaticMethods.class.getTypeName());
        l_cc.setMethodName("overLoadedMethod1Arg");
        l_cc.setArgs(new Object[] { "testqa+krs3726@acc-simulators.email.corp.adobe.com" });
        IntegroBridgeClassLoader ibcl = new IntegroBridgeClassLoader();

        List<Method> l_methods = l_cc.fetchMethodCandidates(ibcl.loadClass(l_cc.getClassName()));

        assertThat("We should only find one method", l_methods.size(), Matchers.equalTo(1));
    }

    @Test
    public void testIssueWithNonExistantMethodException_internalMethod() {
        ConfigValueHandlerIBS.STATIC_INTEGRITY_PACKAGES.activate("com.adobe.campaign.tests.bridge.testdata.one.");
        CallContent l_cc = new CallContent();
        l_cc.setClassName(SimpleStaticMethods.class.getTypeName());
        l_cc.setMethodName("methodAcceptingStringArgumentNonExistant");
        l_cc.setArgs(new Object[] { "testqa+krs3726@acc-simulators.email.corp.adobe.com" });
        IntegroBridgeClassLoader ibcl = new IntegroBridgeClassLoader();

        Assert.assertThrows(NonExistentJavaObjectException.class,
                () -> l_cc.fetchMethod(ibcl.loadClass(l_cc.getClassName())));
    }

    @Test
    public void testIssueWithNonExistantMethodException_internalClass() {
        ConfigValueHandlerIBS.INTEGRITY_PACKAGE_INJECTION_MODE.activate("semi-manual");
        ConfigValueHandlerIBS.STATIC_INTEGRITY_PACKAGES.activate("com.adobe.campaign.tests.bridge.testdata.one.");
        CallContent l_cc = new CallContent();
        l_cc.setClassName("com.adobe.campaign.tests.bridge.testdata.one.SimpleStaticMethodsNonExistant");
        l_cc.setMethodName("methodAcceptingStringArgumentNonExistant");
        l_cc.setArgs(new Object[] { "testqa+krs3726@acc-simulators.email.corp.adobe.com" });
        IntegroBridgeClassLoader ibcl = new IntegroBridgeClassLoader();

        Assert.assertThrows(ClassNotFoundException.class,
                () -> l_cc.fetchMethod(ibcl.loadClass(l_cc.getClassName())));
    }

    @Test
    public void testIssueWithNonExistantMethodException_externalMethod() {
        ConfigValueHandlerIBS.INTEGRITY_PACKAGE_INJECTION_MODE.activate("semi-manual");

        CallContent l_cc = new CallContent();
        l_cc.setClassName("com.adobe.campaign.tests.bridge.testdata.one.SimpleStaticMethods");
        l_cc.setMethodName("methodAcceptingStringArgumentNonExistant");
        l_cc.setArgs(new Object[] { "testqa+krs3726@acc-simulators.email.corp.adobe.com" });
        IntegroBridgeClassLoader ibcl = new IntegroBridgeClassLoader();

        Assert.assertThrows(NonExistentJavaObjectException.class,
                () -> l_cc.fetchMethod(ibcl.loadClass(l_cc.getClassName())));
    }

    @Test
    public void testIssueWithNonExistantClassException_externalClass() {

        CallContent l_cc = new CallContent();
        l_cc.setClassName("com.adobe.campaign.tests.bridge.testdata.one.SimpleStaticMethodsNonExistant");
        l_cc.setMethodName("methodAcceptingStringArgument");
        l_cc.setArgs(new Object[] { "testqa+krs3726@acc-simulators.email.corp.adobe.com" });
        IntegroBridgeClassLoader ibcl = new IntegroBridgeClassLoader();

        Assert.assertThrows(ClassNotFoundException.class,
                () -> l_cc.fetchMethod(ibcl.loadClass(l_cc.getClassName())));
    }

    @Test(enabled = false)
    public void testIssueWithAmbiguousCall_Apache() throws ClassNotFoundException {
        CallContent l_cc = new CallContent();
        l_cc.setClassName("com.adobe.campaign.tests.bridge.testdata.one.SimpleStaticMethods");
        l_cc.setMethodName("overLoadedMethod1Arg");
        System.out.println(boolean.class.getTypeName());
        l_cc.setArgs(new Object[] { "testqa+krs3726@acc-simulators.email.corp.adobe.com" });
        //l_cc.setArgTypes(new Object[] { "java.lang.String", "java.lang.String", "" });
        IntegroBridgeClassLoader ibcl = new IntegroBridgeClassLoader();
        List<Method> l_methods = l_cc.fetchMethodCandidates(ibcl.loadClass(l_cc.getClassName()));
        assertThat("We should only find one method", l_methods.size(), Matchers.equalTo(1));
    }


    @Test
    public void testIssue34LinkageError() throws ClassNotFoundException {
        JavaCalls jc = new JavaCalls();
        CallContent l_cc = new CallContent();
        l_cc.setClassName(SimpleStaticMethods.class.getTypeName());
        l_cc.setMethodName("methodThrowingLinkageError");
        jc.getCallContent().put("firstCall",l_cc);
        Assert.assertThrows(ClassLoaderConflictException.class, () -> l_cc.call(jc.getLocalClassLoader()));

        Assert.assertThrows(IBSConfigurationException.class, () -> jc.submitCalls());
    }

    /**
     * Related to issue #3: Where we want a clear message + the original error whenever there is an invocation target
     * exception
     */
    @Test
    public void testIssueWithBetterMessageOnInvocationTarget() {
        CallContent l_cc = new CallContent();
        l_cc.setClassName(SimpleStaticMethods.class.getTypeName());
        l_cc.setMethodName("methodThrowingException");
        l_cc.setArgs(new Object[] { 5, 5 });
        IntegroBridgeClassLoader ibcl = new IntegroBridgeClassLoader();
        try {
            l_cc.call(ibcl);
            assertThat("We should not get here", false);
        } catch (Exception e) {
            assertThat("The error should be of the type TargetJavaMethodCallException", e,
                    Matchers.instanceOf(TargetJavaMethodCallException.class));
            assertThat("We should have correct static messages ", e.getMessage(), Matchers.startsWith(
                    "We experienced an exception when calling the provided method com.adobe.campaign.tests.bridge.testdata.one.SimpleStaticMethods.methodThrowingException."));
            assertThat("The message should contain the target message as well", e.getMessage(), Matchers.endsWith(
                    "Provided error message : java.lang.IllegalArgumentException: We do not allow numbers that are equal."));
        }

    }

    /***** #2 Variable replacement ******/
    @Test(description = "Issue #2 : Allowing for passing values between calls")
    public void testValuePassing() {
        JavaCalls l_myJavaCalls1 = new JavaCalls();

        CallContent l_cc1A = new CallContent();
        l_cc1A.setClassName(SimpleStaticMethods.class.getTypeName());
        l_cc1A.setMethodName("methodAcceptingStringArgument");
        l_cc1A.setArgs(new Object[] { "XXX" });
        l_myJavaCalls1.getCallContent().put("fetchFirstName", l_cc1A);

        CallContent l_cc1B = new CallContent();
        l_cc1B.setClassName(SimpleStaticMethods.class.getTypeName());
        l_cc1B.setMethodName("methodAcceptingTwoArguments");
        l_cc1B.setArgs(new Object[] { "fetchFirstName", "YYY" });
        l_myJavaCalls1.getCallContent().put("fetchMail", l_cc1B);

        JavaCallResults x = l_myJavaCalls1.submitCalls();

        assertThat("We should have fetched the value from the first call",
                x.getReturnValues().get("fetchMail").toString(), Matchers.startsWith(
                        x.getReturnValues().get("fetchFirstName").toString()));
    }

    @Test
    public void testValueReplacement() {
        IntegroBridgeClassLoader icl = new IntegroBridgeClassLoader();
        icl.getCallResultCache().put("AAA", "XXXX");

        CallContent l_cc1B = new CallContent();
        l_cc1B.setClassName(SimpleStaticMethods.class.getTypeName());
        l_cc1B.setMethodName("methodAcceptingTwoArguments");
        l_cc1B.setArgs(new Object[] { "AAA", "B" });

        Object[] result = l_cc1B.expandArgs(icl);
        assertThat("We should have replaced the value correctly", result.length, Matchers.equalTo(2));
        assertThat("We should have replaced the value correctly", result[0].toString(), Matchers.equalTo("XXXX"));

    }

    @Test
    public void testVariableReplacementComplexObjects() {
        IntegroBridgeClassLoader icl = new IntegroBridgeClassLoader();
        Instantiable objectValue = new Instantiable("6");
        icl.getCallResultCache().put("AAA", objectValue);

        CallContent l_cc1B = new CallContent();
        l_cc1B.setClassName(StaticType.class.getTypeName());
        l_cc1B.setMethodName("fetchInstantiableStringValue");
        l_cc1B.setArgs(new Object[] { "AAA" });

        Object[] result = l_cc1B.expandArgs(icl);
        assertThat("We should have replaced the value correctly", result.length, Matchers.equalTo(1));
        assertThat("We should have replaced the value correctly", result[0],
                Matchers.instanceOf(Instantiable.class));
        assertThat("We should have replaced the value correctly", result[0],
                Matchers.equalTo(objectValue));
    }

    @Test
    public void testExtractable() {

        Class<?> l_myClass = MimeMessage.class;

        assertThat("MimeMessage is not serializable", Message.class instanceof Serializable);

        assertThat("MimeMessage is not serializable", !MetaUtils.isExtractable(l_myClass));

        Class<?> l_String = String.class;

        assertThat("String is serializable", MetaUtils.isExtractable(l_String));

        Class<?> l_int = int.class;

        assertThat("int is extractable", MetaUtils.isExtractable(l_int));

        Class<?> l_list = List.class;

        assertThat("list is extractable", MetaUtils.isExtractable(l_list));
    }

    @Test
    public void testExtractableMethod() throws NoSuchMethodException {
        Method l_simpleGetter = MimeMessage.class.getDeclaredMethod("getFileName");

        assertThat("getFileName is extractable", MetaUtils.isExtractable(l_simpleGetter));

        Method l_simpleSetter = MimeMessage.class.getDeclaredMethod("setFrom");

        assertThat("setSender is should not be extractable", !MetaUtils.isExtractable(l_simpleSetter));

        Method l_simpleIs = String.class.getDeclaredMethod("isEmpty");

        assertThat("isEmpty is extractable", MetaUtils.isExtractable(l_simpleIs));

        Method l_hashSet = MimeMessage.class.getDeclaredMethod("getSize");
        assertThat("HashCode is NOT extractable", MetaUtils.isExtractable(l_hashSet));

    }

    @Test
    public void testExtract() throws NoSuchMethodException {

        String l_simpleString = "testValue";

        assertThat("getFileName is extractable", MetaUtils.extractValuesFromObject(l_simpleString),
                Matchers.equalTo(l_simpleString));

        //List<String>
        Object l_simpleList = Collections.singletonList(l_simpleString);

        assertThat("sss", l_simpleList instanceof Collection);

        Method l_simpleSetter = MimeMessage.class.getDeclaredMethod("setFrom");

        assertThat("setSender is should not be extractable", !MetaUtils.isExtractable(l_simpleSetter));

        Method l_simpleIs = String.class.getDeclaredMethod("isEmpty");

        assertThat("isEmpty is extractable", MetaUtils.isExtractable(l_simpleIs));

        Method l_hashSet = MimeMessage.class.getDeclaredMethod("getSize");
        assertThat("HashCode is NOT extractable", MetaUtils.isExtractable(l_hashSet));
    }

    @Test
    public void prepareExtractMimeMessage()
            throws MessagingException, JsonProcessingException {
        Message m1 = MimeMessageFactory.getMessage("five");
        Message m2 = MimeMessageFactory.getMessage("six");
        List<Message> messages = Arrays.asList(m1, m2);
        Object x = messages;

        assertThat("The values should be extractable", MetaUtils.isExtractable(x.getClass()));
        JavaCallResults jcr = new JavaCallResults();
        jcr.getReturnValues().put("value", MetaUtils.extractValuesFromObject(x));

        String value = BridgeServiceFactory.transformJavaCallResultsToJSON(jcr);

        System.out.println(value);

    }

    @Test
    public void prepareExtractSimpleString()
            throws JsonProcessingException {
        String m1 = "five";
        String m2 = "six";
        List<String> messages = Arrays.asList(m1, m2);
        Object x = messages;

        assertThat("The values should be extractable", MetaUtils.isExtractable(x.getClass()));
        JavaCallResults jcr = new JavaCallResults();
        jcr.getReturnValues().put("value", MetaUtils.extractValuesFromObject(x));

        String value = BridgeServiceFactory.transformJavaCallResultsToJSON(jcr);

        System.out.println(value);

    }

    @Test
    public void prepareExtractSimpleInt()
            throws JsonProcessingException {
        int m1 = 5;
        int m2 = 6;
        List messages = Arrays.asList(m1, m2);
        Object x = messages;

        assertThat("The values should be extractable", MetaUtils.isExtractable(x.getClass()));
        JavaCallResults jcr = new JavaCallResults();
        jcr.getReturnValues().put("value", MetaUtils.extractValuesFromObject(x));

        String value = BridgeServiceFactory.transformJavaCallResultsToJSON(jcr);

        System.out.println(value);
    }

    @Test
    public void testEnvironmentVariablesBadConfigValuesForTargetA() {

        ConfigValueHandlerIBS.ENVIRONMENT_VARS_SETTER_CLASS.activate(MyPropertiesHandler.class.getTypeName());
        ConfigValueHandlerIBS.ENVIRONMENT_VARS_SETTER_METHOD.activate("fillMeUpNonExisting");

        JavaCalls jc = new JavaCalls();

        Properties l_envVars = new Properties();
        l_envVars.put("AC.UITEST.SMS.PORT", "234");

        jc.setEnvironmentVariables(l_envVars);

        Assert.assertThrows(IBSConfigurationException.class, () -> jc.submitCalls());
    }

    @Test
    public void testEnvironmentVariablesBadConfigValuesForTargetB() {

        ConfigValueHandlerIBS.ENVIRONMENT_VARS_SETTER_CLASS.activate("ClassThatDoesntExist");
        ConfigValueHandlerIBS.ENVIRONMENT_VARS_SETTER_METHOD.activate("fillMeUpNonExisting");

        JavaCalls jc = new JavaCalls();

        Properties l_envVars = new Properties();
        l_envVars.put("AC.UITEST.SMS.PORT", "234");

        jc.setEnvironmentVariables(l_envVars);

        Assert.assertThrows(IBSConfigurationException.class, () -> jc.submitCalls());
    }

    //Related to issue 44
    //Class is not loaded if it is not in the
    // If we use the same class loader we will be able to fetch the results
    @Test
    public void testSEnvironmentVariablesBadConfigValues()
            throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        ConfigValueHandlerIBS.INTEGRITY_PACKAGE_INJECTION_MODE.activate("semi-manual");
        ConfigValueHandlerIBS.ENVIRONMENT_VARS_SETTER_CLASS.activate(MyPropertiesHandler.class.getTypeName());
        ConfigValueHandlerIBS.ENVIRONMENT_VARS_SETTER_METHOD.activate("fillMeUp");

        JavaCalls jc = new JavaCalls();

        Properties props = new Properties();
        String myKey = "ABC";
        props.setProperty(myKey, "456");

        jc.setEnvironmentVariables(props);

        assertThat("We should not have the envvars integrity path set before execute",
                jc.getLocalClassLoader().getPackagePaths().stream()
                        .noneMatch(
                                x -> ConfigValueHandlerIBS.ENVIRONMENT_VARS_SETTER_CLASS.fetchValue().startsWith(x)));

        jc.submitCalls();

        assertThat("We should not have the envvars integrity path set before execute",
                jc.getLocalClassLoader().getPackagePaths().stream()
                        .anyMatch(x -> ConfigValueHandlerIBS.ENVIRONMENT_VARS_SETTER_CLASS.fetchValue().startsWith(x)));

        assertThat("System value handler should have the value",
                !MyPropertiesHandler.myProps.containsKey(myKey));

        //Use same class loader as before
        JavaCalls jc2 = new JavaCalls();
        jc2.setLocalClassLoader(jc.getLocalClassLoader());

        CallContent l_cc2 = new CallContent();
        l_cc2.setClassName(MyPropertiesHandler.class.getTypeName());
        l_cc2.setMethodName("getMyProp");
        l_cc2.setArgs(new Object[] { myKey });

        jc2.getCallContent().put("call2", l_cc2);
        JavaCallResults jcr = jc2.submitCalls();

        assertThat("We should have a result", jcr.getReturnValues().get("call2"), Matchers.notNullValue());
        assertThat("We should have a result", jcr.getReturnValues().get("call2").toString(), Matchers.equalTo("456"));

    }

    @Test
    public void testSEnvironmentVariablesBadConfigValuesNegative() {

        ConfigValueHandlerIBS.ENVIRONMENT_VARS_SETTER_CLASS.activate(MyPropertiesHandler.class.getTypeName());
        ConfigValueHandlerIBS.ENVIRONMENT_VARS_SETTER_METHOD.activate("fillMeUp");

        JavaCalls jc = new JavaCalls();

        Properties x = new Properties();
        String myKey = "ABC";
        x.put(myKey, 456);
        x.put("EFG", "567");

        jc.setEnvironmentVariables(x);

        Assert.assertThrows(IBSRunTimeException.class, () -> jc.submitCalls());
    }

    @Test
    public void testIsConstructorCall() {
        CallContent l_cc = new CallContent();
        l_cc.setClassName(Instantiable.class.getTypeName());
        l_cc.setMethodName("Instantiable");
        l_cc.setArgs(new Object[] { "hello" });

        assertThat("We should be in a constructor call", l_cc.isConstructorCall());

        CallContent l_cc2 = new CallContent();
        l_cc2.setClassName("Instantiable");
        l_cc2.setMethodName("Instantiable");
        l_cc2.setArgs(new Object[] { "myBToken" });

        assertThat("We should be in a constructor call", l_cc2.isConstructorCall());

    }

    @Test
    public void testIsConstructorCallNullMethod() {
        CallContent l_cc = new CallContent();
        l_cc.setClassName(Instantiable.class.getTypeName());
        l_cc.setArgs(new Object[] { "myBToken" });

        assertThat("We should be in a constructor call", l_cc.isConstructorCall());

        CallContent l_cc2 = new CallContent();
        l_cc2.setClassName("Instantiable");
        l_cc2.setArgs(new Object[] { "myBToken" });

        assertThat("We should be in a constructor call", l_cc2.isConstructorCall());

    }

    @Test
    public void testIsConstructorCallNegative() {
        CallContent l_cc = new CallContent();
        l_cc.setClassName(Instantiable.class.getTypeName());
        l_cc.setMethodName("getValueString");

        assertThat("We should not be in a constructor call", !l_cc.isConstructorCall());

        CallContent l_cc2 = new CallContent();
        l_cc2.setClassName("Instantiable");
        l_cc2.setMethodName("getValueString");

        assertThat("We should not be in a constructor call", !l_cc2.isConstructorCall());

    }

    @Test
    public void testFetchMethodCandidates() throws ClassNotFoundException {
        CallContent l_cc = new CallContent();
        l_cc.setClassName(Instantiable.class.getTypeName());
        l_cc.setArgs(new Object[] { "kj" });

        IntegroBridgeClassLoader ibcl = new IntegroBridgeClassLoader();
        List<Constructor> l_constructors = l_cc.fetchConstructorCandidates(ibcl.loadClass(l_cc.getClassName()));
        assertThat("We should only find one method", l_constructors.size(), Matchers.equalTo(1));

    }

    @Test
    public void testFetchMethodCandidates2() throws ClassNotFoundException {
        StringBuilder sb = new StringBuilder();
        sb.append("3");
        CallContent l_cc = new CallContent();
        l_cc.setClassName("java.lang.StringBuilder");

        IntegroBridgeClassLoader ibcl = new IntegroBridgeClassLoader();
        List<Constructor> l_constructors = l_cc.fetchConstructorCandidates(ibcl.loadClass(l_cc.getClassName()));
        assertThat("We should only find one method", l_constructors.size(), Matchers.equalTo(1));
    }

    @Test
    public void testCallConstructor_case1()
            throws ClassNotFoundException, JsonProcessingException {

        Instantiable reference = new Instantiable("3");
        JavaCalls jc = new JavaCalls();
        CallContent l_cc = new CallContent();
        l_cc.setClassName("com.adobe.campaign.tests.bridge.testdata.one.Instantiable");
        l_cc.setArgs(new Object[] { "3" });
        jc.getCallContent().put("call1", l_cc);
    }

    @Test
    public void testCallConstructor_case1_negative()
            throws ClassNotFoundException, JsonProcessingException {

        Instantiable reference = new Instantiable("3");
        JavaCalls jc = new JavaCalls();
        CallContent l_cc = new CallContent();
        l_cc.setClassName("com.adobe.campaign.tests.bridge.testdata.one.Instantiable");
        l_cc.setArgs(new Object[] {});
        jc.getCallContent().put("call1", l_cc);

        Assert.assertThrows(NonExistentJavaObjectException.class, () -> jc.submitCalls());
    }

    @Test
    public void testCallConstructor_case1_negative2()
            throws ClassNotFoundException, JsonProcessingException {

        // To be removed with issue #60 : added for coverage
        Instantiable reference = new Instantiable("3", "3");
        Instantiable reference2 = new Instantiable("3", 3);
        reference.setValueString("4");
        StaticType x = new StaticType();
        StaticType.fetchInstantiableStringValue(reference);

        JavaCalls jc = new JavaCalls();
        CallContent l_cc = new CallContent();
        l_cc.setClassName("com.adobe.campaign.tests.bridge.testdata.one.Instantiable");
        l_cc.setArgs(new Object[] { "A", "B" });
        jc.getCallContent().put("call1", l_cc);

        Assert.assertThrows(AmbiguousMethodException.class, () -> jc.submitCalls());
    }

    @Test
    public void testCallConstructor_case2_InstanceMethod() {

        Instantiable reference = new Instantiable("3");
        JavaCalls jc = new JavaCalls();
        CallContent l_cc = new CallContent();
        l_cc.setClassName(Instantiable.class.getTypeName());
        l_cc.setArgs(new Object[] { "3" });
        jc.getCallContent().put("call1", l_cc);

        CallContent l_cc2 = new CallContent();
        l_cc2.setClassName("call1");
        l_cc2.setMethodName("getValueString");
        jc.getCallContent().put("call2", l_cc2);

        JavaCallResults jcr = jc.submitCalls();

        assertThat("We should get a good answer back from the call", jcr.getReturnValues().get("call2"),
                Matchers.equalTo(reference.getValueString()));
    }

    @Test
    public void testCallConstructor_case3_InstanceMethod() {

        Instantiable reference = new Instantiable("3");
        JavaCalls jc = new JavaCalls();
        CallContent l_cc = new CallContent();
        l_cc.setClassName(Instantiable.class.getTypeName());
        l_cc.setArgs(new Object[] { "3" });
        jc.getCallContent().put("call1", l_cc);

        CallContent l_cc2 = new CallContent();
        l_cc2.setClassName("call1");
        l_cc2.setMethodName("getValueString");
        jc.getCallContent().put("call2", l_cc2);

        CallContent l_cc3 = new CallContent();
        l_cc3.setClassName("call1");
        l_cc3.setMethodName("setValueString");
        l_cc3.setArgs(new Object[] { "7" });
        jc.getCallContent().put("call3", l_cc3);

        CallContent l_cc4 = new CallContent();
        l_cc4.setClassName("call1");
        l_cc4.setMethodName("getValueString");
        jc.getCallContent().put("call4", l_cc4);

        JavaCallResults jcr = jc.submitCalls();

        assertThat("We should get a good answer back from the call", jcr.getReturnValues().get("call2"),
                Matchers.equalTo(reference.getValueString()));

        assertThat("We should get a good answer back from the call", jcr.getReturnValues().get("call4"),
                Matchers.equalTo("7"));
    }

    @Test
    public void testCallConstructorAbstract_case4_negative() {

        // To be removed with issue #60 : added for coverage
            JavaCalls jc = new JavaCalls();
        CallContent l_cc = new CallContent();
        l_cc.setClassName("com.adobe.campaign.tests.bridge.testdata.one.AbstractClassType");
        l_cc.setArgs(new Object[] { "A" });
        jc.getCallContent().put("call1", l_cc);

        Assert.assertThrows(NonExistentJavaObjectException.class, () -> jc.submitCalls());
    }

    @Test
    public void testHashCallContent() {
        CallContent l_cc = new CallContent();
        l_cc.setClassName(Instantiable.class.getTypeName());
        l_cc.setArgs(new Object[] { "3" });


        CallContent l_cc2 = new CallContent();
        l_cc2.setClassName(Instantiable.class.getTypeName());
        l_cc2.setArgs(new Object[] { "5" });

        assertThat("Hashes are different", l_cc.hashCode(), Matchers.not(Matchers.equalTo(l_cc2.hashCode())));
    }


}

