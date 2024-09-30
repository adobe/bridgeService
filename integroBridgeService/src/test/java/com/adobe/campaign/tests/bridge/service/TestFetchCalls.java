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
import com.adobe.campaign.tests.bridge.testdata.one.*;
import com.adobe.campaign.tests.bridge.testdata.two.StaticMethodsIntegrity;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.hamcrest.Matchers;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import javax.mail.MessagingException;
import java.io.IOException;
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
        assertThat("We should now have a timeout", l_myJavaCalls.getTimeout(),
                Matchers.equalTo(Long.parseLong(ConfigValueHandlerIBS.DEFAULT_CALL_TIMEOUT.fetchValue())));

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

        l_cc.setArgs(new Object[] { MimeMessageMethods.createMessage("ab") });

        IntegroBridgeClassLoader iClassLoader = new IntegroBridgeClassLoader();
        Assert.assertThrows(NonExistentJavaObjectException.class, () -> l_cc.call(iClassLoader));
    }

    @Test
    public void testJSONCall_negativeNonExistingClass()
            throws MessagingException {

        CallContent l_cc = new CallContent();
        l_cc.setClassName("non.existant.class.NoWhereToBeFound");
        l_cc.setMethodName("getRandomString");

        l_cc.setArgs(new Object[] { MimeMessageMethods.createMessage("ab") });

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
        l_cc.setArgs(new Object[] { "HI" });

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

        assertThat("The retun value should be of a correct format",
                l_returnValue.getReturnValues().containsKey("call1"));
        assertThat("The retun value should be of a correct format",
                l_returnValue.getReturnValues().get("call1").toString(),
                Matchers.startsWith("A"));
        assertThat("The retun value should be of a correct format",
                l_returnValue.getReturnValues().get("call1").toString(),
                Matchers.endsWith(SimpleStaticMethods.SUCCESS_VAL));

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
     * Integrity Tests - Here all calls and env vars are in the package path. In this case each call has its own env
     * vars In issue https://git.corp.adobe.com/AdobeCampaignQE/integroBridgeService/issues/48 this is : Case 1 The
     * envvars of calls do not interfere with the others
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
                        .noneMatch(
                                x -> ConfigValueHandlerIBS.ENVIRONMENT_VARS_SETTER_CLASS.fetchValue().startsWith(x)));

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
                        .noneMatch(
                                x -> ConfigValueHandlerIBS.ENVIRONMENT_VARS_SETTER_CLASS.fetchValue().startsWith(x)));

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

        System.out.println(
                BridgeServiceFactory.transformJavaCallResultsToJSON(returnedValue, fetchedFromJSON.fetchSecrets()));

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
        jc.getCallContent().put("firstCall", l_cc);
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

    //Tests for validating that we can correctlly return the duration of a specific execution
    @Test
    public void testDurationReplacement() {
        JavaCallResults jcr = new JavaCallResults();
        jcr.getCallDurations().put("A", 100l);

        Long result = jcr.expandDurations(100l);
        assertThat("We should have replaced the value correctly", result, Matchers.equalTo(100l));

        Long result2 = jcr.expandDurations("A");
        assertThat("We should have replaced the value correctly", result2, Matchers.equalTo(100l));

        Long result3 = jcr.expandDurations(100);
        assertThat(result3, Matchers.equalTo(100l));

    }

    @Test
    public void testDurationReplacement_negative() {
        JavaCallResults jcr = new JavaCallResults();
        jcr.getCallDurations().put("A", 100l);

        Assert.assertThrows(IBSRunTimeException.class, () -> jcr.expandDurations(Boolean.FALSE));

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

    @Test
    public void test_issue35_callToClassWithNoModifiers() {
        JavaCalls jc = new JavaCalls();
        CallContent l_cc = new CallContent();
        l_cc.setClassName("com.adobe.campaign.tests.bridge.testdata.one.ClassWithNoModifiers");
        l_cc.setMethodName("hello");
        jc.getCallContent().put("one", l_cc);
        Exception expectedException = null;
        try {
            l_cc.call(jc.getLocalClassLoader());
        } catch (Exception e) {
            expectedException = e;
            assertThat("We should have a runtime exception", expectedException,
                    Matchers.instanceOf(RuntimeException.class));
            assertThat("The original exception should Illegalaccess", expectedException.getCause(), Matchers.instanceOf(
                    IllegalAccessException.class));
        }
        assertThat("We should have thrown an exception here", expectedException, Matchers.notNullValue());
        Assert.assertThrows(JavaObjectInaccessibleException.class, () -> jc.submitCalls());

    }

    @Test
    public void testExtractingMapTop() {
        JavaCalls jc = new JavaCalls();
        CallContent l_cc = new CallContent();
        l_cc.setClassName("com.adobe.campaign.tests.bridge.testdata.one.SimpleStaticMethods");
        l_cc.setMethodName("methodReturningMap");
        jc.getCallContent().put("one", l_cc);

        JavaCallResults result = jc.submitCalls();

        Object oneResultRaw = result.getReturnValues().get("one");
        assertThat("We should be able to access the data of the map", oneResultRaw, Matchers.instanceOf(Map.class));

        Map<String, Object> oneResultMap = (Map<String, Object>) oneResultRaw;

        assertThat("We should have the map keys", oneResultMap.keySet(),
                Matchers.containsInAnyOrder("object1", "object3"));
    }

    @Test
    public void testExtractingMapLvl_1() {
        Map mapOfString = SimpleStaticMethods.methodReturningMap();

        Map<String, Object> oneResultMap = (Map<String, Object>) MetaUtils.extractValuesFromObject(mapOfString);

        assertThat("We should have the map keys", oneResultMap.keySet(),
                Matchers.containsInAnyOrder("object1", "object3"));
    }

    @Test
    public void testExtractingMapLvl_2() {
        Map mapOfString = SimpleStaticMethods.methodReturningMap();

        Map<String, Object> oneResultMap = (Map<String, Object>) MetaUtils.extractValuesFromMap(mapOfString);

        assertThat("We should have the map keys", oneResultMap.keySet(),
                Matchers.containsInAnyOrder("object1", "object3"));
        assertThat("We should have the correct values", oneResultMap.get("object1"), Matchers.equalTo("value1"));
        assertThat("We should have the correct values", oneResultMap.get("object3"), Matchers.equalTo("value3"));
    }

    @Test
    public void testExtractingMapLvl_2_mapOfList() {
        Map mapOfString = SimpleStaticMethods.methodReturningMap();
        List<String> l_listValues = Arrays.asList("a", "b", "c");
        mapOfString.put("object2", l_listValues);

        Map<String, Object> oneResultMap = (Map<String, Object>) MetaUtils.extractValuesFromMap(mapOfString);

        assertThat("We should have the map keys", oneResultMap.keySet(),
                Matchers.containsInAnyOrder("object1", "object3", "object2"));
        assertThat("We should have the correct values", oneResultMap.get("object1"), Matchers.equalTo("value1"));
        assertThat("We should have the correct values", oneResultMap.get("object3"), Matchers.equalTo("value3"));
        assertThat("We should be able to access the data of the map", oneResultMap.get("object2"),
                Matchers.instanceOf(List.class));
        List<String> l_nestedList = (List<String>) oneResultMap.get("object2");
        assertThat("We should have the correct values", l_nestedList, Matchers.containsInAnyOrder("a", "b", "c"));

    }

    @Test
    public void testExtractingMapLvl_2_mapOfMap() {
        Map mapOfString = SimpleStaticMethods.methodReturningMap();
        Map<String, String> l_mapValues = new HashMap<>();
        l_mapValues.put("object5", "value5");
        l_mapValues.put("object6", "value6");
        mapOfString.put("object2", l_mapValues);

        Map<String, Object> oneResultMap = (Map<String, Object>) MetaUtils.extractValuesFromMap(mapOfString);

        assertThat("We should have the map keys", oneResultMap.keySet(),
                Matchers.containsInAnyOrder("object1", "object3", "object2"));
        assertThat("We should have the correct values", oneResultMap.get("object1"), Matchers.equalTo("value1"));
        assertThat("We should have the correct values", oneResultMap.get("object3"), Matchers.equalTo("value3"));
        assertThat("We should be able to access the data of the map", oneResultMap.get("object2"),
                Matchers.instanceOf(Map.class));
        Map<String, String> l_nestedMap = (Map<String, String>) oneResultMap.get("object2");
        assertThat("We should have the correct values", l_nestedMap.keySet(),
                Matchers.containsInAnyOrder("object5", "object6"));
        assertThat("We should have the correct values", l_nestedMap.get("object6"), Matchers.equalTo("value6"));

    }

    @Test
    public void testExtractingMapLvl_2_negative_mapOfNull() {
        Map mapOfString = SimpleStaticMethods.methodReturningMap();

        mapOfString.put("object2", null);

        Map<String, Object> oneResultMap = (Map<String, Object>) MetaUtils.extractValuesFromMap(mapOfString);

        assertThat("We should have the map keys", oneResultMap.keySet(),
                Matchers.containsInAnyOrder("object1", "object3", "object2"));
        assertThat("We should have the correct values", oneResultMap.get("object1"), Matchers.equalTo("value1"));
        assertThat("We should have the correct values", oneResultMap.get("object3"), Matchers.equalTo("value3"));
        assertThat("We should be able to access the data of the map", oneResultMap.get("object2"),
                Matchers.instanceOf(Map.class));
        Map<String, String> l_nestedMap = (Map<String, String>) oneResultMap.get("object2");
        assertThat("We should have an empty set of values", l_nestedMap.isEmpty());

    }

    @Test
    public void testExtractingMapIntegerLvl_2() {
        Map mapOfIntegerString = new HashMap();
        mapOfIntegerString.put(1, "value1");
        mapOfIntegerString.put(2, "value3");

        Map<String, Object> oneResultMap = (Map<String, Object>) MetaUtils.extractValuesFromMap(mapOfIntegerString);

        assertThat("We should have the map keys", oneResultMap.keySet(), Matchers.containsInAnyOrder(1, 2));
        assertThat("We should have the correct values", oneResultMap.get(1), Matchers.equalTo("value1"));
        assertThat("We should have the correct values", oneResultMap.get(2), Matchers.equalTo("value3"));
    }

    @Test
    public void testExtractingMapIntegerIntegerLvl_2() {
        Map mapOfIntegerString = new HashMap();
        mapOfIntegerString.put(1, 13);
        mapOfIntegerString.put(2, 17);

        Map<String, Object> oneResultMap = (Map<String, Object>) MetaUtils.extractValuesFromMap(mapOfIntegerString);

        assertThat("We should have the map keys", oneResultMap.keySet(), Matchers.containsInAnyOrder(1, 2));
        assertThat("We should have the correct values", oneResultMap.get(1), Matchers.equalTo(13));
        assertThat("We should have the correct values", oneResultMap.get(2), Matchers.equalTo(17));
    }

    @Test
    public void testExtractingJSONLvl_2() {
        Map mapOfString = SimpleStaticMethods.methodReturningMap();

        Map<String, Object> oneResultMap = (Map<String, Object>) MetaUtils.extractValuesFromMap(mapOfString);

        assertThat("We should have the map keys", oneResultMap.keySet(),
                Matchers.containsInAnyOrder("object1", "object3"));
        assertThat("We should have the correct values", oneResultMap.get("object1"), Matchers.equalTo("value1"));
        assertThat("We should have the correct values", oneResultMap.get("object3"), Matchers.equalTo("value3"));
    }

    //////////////////// Step Name in error
    @Test
    public void testErrorStepDetection() {
        //Method 1   throws exception
        JavaCalls l_myJavaCalls1 = new JavaCalls();
        CallContent l_cc1 = new CallContent();
        l_cc1.setClassName("com.adobe.campaign.tests.bridge.testdata.one.ClassWithNoModifiers");
        l_cc1.setMethodName("hello");
        l_myJavaCalls1.getCallContent().put("call1", l_cc1);

        //Method 2
        CallContent l_cc2 = new CallContent();
        l_cc2.setClassName(SimpleStaticMethods.class.getTypeName());
        l_cc2.setMethodName("methodAcceptingStringArgument");
        l_cc2.setArgs(new Object[] { "A" });
        l_myJavaCalls1.getCallContent().put("call2", l_cc2);

        //Error at step1
        try {
            l_myJavaCalls1.submitCalls();
            Assert.assertTrue(false, "We should not reach here");
        } catch (Exception e) {
            ErrorObject eo = new ErrorObject(e);
            assertThat("We should detect that the error is at the first call", eo.getFailureAtStep(),
                    Matchers.equalTo("call1"));

        }

        l_myJavaCalls1.getCallContent().put("call1", l_cc2);
        l_myJavaCalls1.getCallContent().put("call2", l_cc1);

        //Error at step2
        try {
            l_myJavaCalls1.submitCalls();
            Assert.assertTrue(false, "We should not reach here");
        } catch (Exception e) {
            ErrorObject eo = new ErrorObject(e);
            assertThat("We should detect that the error is at the second call", eo.getFailureAtStep(),
                    Matchers.equalTo("call2"));

        }
    }

    @Test
    public void testWithErrorAtEnvironmentValue() {
        //Method 1   throws exception
        JavaCalls l_myJavaCalls1 = new JavaCalls();
        CallContent l_cc1 = new CallContent();
        l_cc1.setClassName("com.adobe.campaign.tests.bridge.testdata.one.ClassWithNoModifiers");
        l_cc1.setMethodName("hello");
        l_myJavaCalls1.getCallContent().put("call1", l_cc1);

        //Method 2
        CallContent l_cc2 = new CallContent();
        l_cc2.setClassName(SimpleStaticMethods.class.getTypeName());
        l_cc2.setMethodName("methodAcceptingStringArgument");
        l_cc2.setArgs(new Object[] { "A" });
        l_myJavaCalls1.getCallContent().put("call2", l_cc2);

        ConfigValueHandlerIBS.ENVIRONMENT_VARS_SETTER_CLASS.activate(MyPropertiesHandler.class.getTypeName());
        ConfigValueHandlerIBS.ENVIRONMENT_VARS_SETTER_METHOD.activate("fillMeUpNonExisting");

        Properties l_envVars = new Properties();
        l_envVars.put("AC.UITEST.SMS.PORT", "234");

        l_myJavaCalls1.setEnvironmentVariables(l_envVars);

        //Error at step2
        try {
            l_myJavaCalls1.submitCalls();
            Assert.assertTrue(false, "We should not reach here");
        } catch (Exception e) {
            ErrorObject eo = new ErrorObject(e);
            assertThat("We should detect that the error is at the calling of the environment variables",
                    eo.getFailureAtStep(),
                    Matchers.equalTo(LogManagement.STD_STEPS.ENVVARS.value));

        }
    }

    //Managing headers
    @Test
    public void testUsingHeadersAsVariables() {
        Map<String, String> l_headerMap = Map.of("key1", "value1",
                ConfigValueHandlerIBS.SECRETS_FILTER_PREFIX.defaultValue + "key2", "value2");

        JavaCalls l_myJavaCalls = new JavaCalls();

        assertThat("We should not have any call results yet", l_myJavaCalls.getLocalClassLoader().getCallResultCache(),
                Matchers.anEmptyMap());

        l_myJavaCalls.addHeaders(l_headerMap);
        assertThat("We should not have any call results yet",
                l_myJavaCalls.getLocalClassLoader().getCallResultCache().size(),
                Matchers.equalTo(2));

        assertThat("We should not have any call results yet", l_myJavaCalls.getLocalClassLoader().getHeaderSet().size(),
                Matchers.equalTo(1));

        assertThat("We should not have any call results yet", l_myJavaCalls.getLocalClassLoader().getSecretSet().size(),
                Matchers.equalTo(1));

        assertThat("We should not have any call results yet", l_myJavaCalls.fetchSecrets(),
                Matchers.containsInAnyOrder("value2"));
    }

    @Test
    public void testUsingHeaders_filterWhichHeadersToInclude() {
        ConfigValueHandlerIBS.HEADERS_FILTER_PREFIX.activate("ibs-header-");
        ConfigValueHandlerIBS.SECRETS_FILTER_PREFIX.activate("ibs-sec-");
        Map<String, String> l_headersMap = Map.of("ibs-sec-key1", "value1", "ibs-header-key2", "value2", "key3",
                "value3");

        JavaCalls l_myJavaCalls = new JavaCalls();

        l_myJavaCalls.addHeaders(l_headersMap);
        assertThat("We should not have any call results yet",
                l_myJavaCalls.getLocalClassLoader().getCallResultCache().size(),
                Matchers.equalTo(2));

        assertThat("We should not have any call results yet", l_myJavaCalls.getLocalClassLoader().getHeaderSet().size(),
                Matchers.equalTo(1));

        assertThat("We should not have any call results yet", l_myJavaCalls.getLocalClassLoader().getSecretSet().size(),
                Matchers.equalTo(1));

        assertThat("We should not have any call results yet", l_myJavaCalls.fetchSecrets(),
                Matchers.containsInAnyOrder("value1"));
    }

    @Test
    public void testUsingHeaders_negativeHeader() {
        Map<String, String> l_secretsMap = Map.of("key1", "value1", "ibs-header-key2", "value2");

        JavaCalls l_myJavaCalls = new JavaCalls();
        CallContent l_cc = new CallContent();
        l_cc.setClassName(SimpleStaticMethods.class.getTypeName());
        l_cc.setMethodName("methodReturningString");

        l_myJavaCalls.getCallContent().put("ibs-header-key2", l_cc);

        Assert.assertThrows(IBSPayloadException.class, () -> l_myJavaCalls.addHeaders(l_secretsMap));
    }

    @Test
    public void testUsingHeaders_negativeSecret() {
        Map<String, String> l_secretsMap = Map.of("key1", "value1", "ibs-secret-key2", "value2");

        JavaCalls l_myJavaCalls = new JavaCalls();
        CallContent l_cc = new CallContent();
        l_cc.setClassName(SimpleStaticMethods.class.getTypeName());
        l_cc.setMethodName("methodReturningString");

        l_myJavaCalls.getCallContent().put("ibs-secret-key2", l_cc);

        boolean exceptionCheck = false;
        try {
            l_myJavaCalls.addHeaders(l_secretsMap);
        } catch (Exception e) {
            assertThat("The correct exception should have been thrown", e, Matchers.instanceOf(IBSPayloadException.class));
            assertThat("The correct exception message should have been thrown", e.getMessage(), Matchers.startsWith("We found a secret key"));
            exceptionCheck=true;
        }
        assertThat("We should have gone through the exception", exceptionCheck);
    }

    @Test
    public void testUsingHeaders_negativePassingSecrets()
            throws JsonProcessingException {

        Map<String, String> l_secretsMap = Map.of("key1", "value1", "ibs-secret-key2", "value2");

        JavaCalls l_myJavaCalls = new JavaCalls();
        CallContent l_cc = new CallContent();
        l_cc.setClassName(SimpleStaticMethods.class.getTypeName());
        l_cc.setMethodName("methodReturningString");

        l_myJavaCalls.getCallContent().put("call1", l_cc);

        l_myJavaCalls.addHeaders(l_secretsMap);

        String m1 = "value1";
        String m2 = "value2";
        List messages = Arrays.asList(m1, m2);
        //

        JavaCallResults jcr = new JavaCallResults();
        jcr.getReturnValues().put("value", MetaUtils.extractValuesFromObject(messages));

        Assert.assertThrows(IBSPayloadException.class,
                () -> BridgeServiceFactory.transformJavaCallResultsToJSON(jcr, l_myJavaCalls.fetchSecrets()));

        //Deactivate the Config Handler
        ConfigValueHandlerIBS.SECRETS_BLOCK_OUTPUT.activate("false");

        assertThat("We should successfully fetch the result string",
                BridgeServiceFactory.transformJavaCallResultsToJSON(jcr, l_myJavaCalls.fetchSecrets())
                        .contains("value2"));
    }

    //#111 Var args and list -array interoperability
    @Test
    public void testInListToArrayTransformation() throws NoSuchMethodException, ClassNotFoundException {
        JavaCalls l_myJavaCall = new JavaCalls();

        CallContent l_cc = new CallContent();
        l_cc.setClassName(SimpleStaticMethods.class.getTypeName());
        l_cc.setMethodName("methodAcceptingArrayArguments");
        var stringList = Arrays.asList("value1", "value2");
        l_cc.setArgs(new Object[]{ stringList });
        l_myJavaCall.getCallContent().put("call1", l_cc);

        Method l_myMethod = SimpleStaticMethods.class.getDeclaredMethod(l_cc.getMethodName(), String[].class);
        System.out.println(l_myMethod.getParameterTypes()[0]);
        assertThat("The first parameter is an array",l_myMethod.getParameterTypes()[0].isArray());
        assertThat("The first parameter is an array",l_myMethod.getParameterTypes()[0], Matchers.equalTo(String[].class));

        System.out.println(l_myMethod.getParameterTypes()[0].getComponentType().getTypeName());

        assertThat("We should now have an array of array", l_cc.getArgs()[0], Matchers.instanceOf(List.class));
        System.out.println("arg "+l_cc.getArgs()[0].getClass());

        Object[] newArgs = l_cc.castArgs(l_cc.getArgs(), l_myMethod);
        assertThat(newArgs, Matchers.notNullValue());
        assertThat("We should now have an array", newArgs[0].getClass().isArray());
        assertThat("We should now have an array of Strings", newArgs[0].getClass().getComponentType(), Matchers.equalTo(String.class));

        assertThat("We should get the correct return value", l_myJavaCall.call("call1"), Matchers.equalTo(2));
    }

    @Test
    public void testMetaIsListToArray() throws NoSuchMethodException {
        JavaCalls l_myJavaCall = new JavaCalls();

        CallContent l_cc = new CallContent();
        l_cc.setClassName(SimpleStaticMethods.class.getTypeName());
        l_cc.setMethodName("methodAcceptingArrayArguments");
        l_cc.setArgs(new Object[]{ new String[]{"value1", "value2"} });
        l_myJavaCall.getCallContent().put("call1", l_cc);

        assertThat("We should get the correct return value", l_myJavaCall.call("call1"), Matchers.equalTo(2));
    }


    @Test
    public void chainingComplexCalls() {
        JavaCalls l_myJavaCall = new JavaCalls();

        CallContent l_cc1 = new CallContent();
        l_cc1.setClassName("com.adobe.campaign.tests.bridge.testdata.one.MimeMessageMethods");
        l_cc1.setMethodName("fetchMessages");
        l_cc1.setArgs(new Object[]{"complexCalls", 4});

        l_myJavaCall.getCallContent().put("fetchMessages", l_cc1);

        CallContent l_cc2 = new CallContent();
        l_cc2.setClassName("com.adobe.campaign.tests.bridge.testdata.one.MimeMessageMethods");
        l_cc2.setMethodName("fetchMessageSubjects");
        l_cc2.setArgs(new Object[]{"fetchMessages"});

        l_myJavaCall.getCallContent().put("fetchSubjects", l_cc2);

        JavaCallResults jcr = l_myJavaCall.submitCalls();

        assertThat("we should have our results", jcr.getReturnValues().keySet(), Matchers.containsInAnyOrder("fetchMessages", "fetchSubjects"));
        assertThat("we should have our results", jcr.getReturnValues().get("fetchSubjects"), Matchers.instanceOf(List.class));

        List<String> fetchSubjects = (List<String>) jcr.getReturnValues().get("fetchSubjects");
        assertThat("we should have our results",
                fetchSubjects, Matchers.hasItem("a subject by me complexCalls_3"));
    }

    @Test
    public void chainingComplexCallsArray() {
        JavaCalls l_myJavaCall = new JavaCalls();

        CallContent l_cc1 = new CallContent();
        l_cc1.setClassName("com.adobe.campaign.tests.bridge.testdata.one.MimeMessageMethods");
        l_cc1.setMethodName("fetchMessagesArray");
        l_cc1.setArgs(new Object[]{"complexCallsArray", 4});

        l_myJavaCall.getCallContent().put("fetchMessages", l_cc1);

        CallContent l_cc2 = new CallContent();
        l_cc2.setClassName("com.adobe.campaign.tests.bridge.testdata.one.MimeMessageMethods");
        l_cc2.setMethodName("fetchMessageArraySubjects");
        l_cc2.setArgs(new Object[]{"fetchMessages"});

        l_myJavaCall.getCallContent().put("fetchSubjects", l_cc2);

        JavaCallResults jcr = l_myJavaCall.submitCalls();

        assertThat("we should have succeeded", jcr.getReturnValues().keySet(), Matchers.containsInAnyOrder("fetchMessages", "fetchSubjects"));
        assertThat("we should have a liste returned", jcr.getReturnValues().get("fetchSubjects"), Matchers.instanceOf(List.class));

        List<String> fetchSubjects = (List<String>) jcr.getReturnValues().get("fetchSubjects");
        assertThat("we should have our results",
                fetchSubjects, Matchers.hasItem("a subject by me complexCallsArray_3"));

    }

    @Test
    public void issue176_callingMethodAcceptingStringAndArray() {
        JavaCalls l_myJavaCall = new JavaCalls();

        CallContent l_cc1 = new CallContent();
        l_cc1.setClassName(SimpleStaticMethods.class.getTypeName());
        l_cc1.setMethodName("methodAcceptingStringAndArray");
        String[] l_array = new String[]{"value1", "value2"};
        l_cc1.setArgs(new Object[]{"ASD", l_array});

        l_myJavaCall.getCallContent().put("fetchResults", l_cc1);

        JavaCallResults jcr = l_myJavaCall.submitCalls();

        assertThat("we should have succeeded", jcr.getReturnValues().keySet(), Matchers.containsInAnyOrder("fetchResults"));


    }

}

