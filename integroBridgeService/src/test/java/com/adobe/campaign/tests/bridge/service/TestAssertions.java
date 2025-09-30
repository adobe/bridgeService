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
import com.adobe.campaign.tests.bridge.testdata.one.EnvironmentVariableHandler;
import com.adobe.campaign.tests.bridge.testdata.one.SimpleStaticMethods;
import org.hamcrest.Matchers;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import static org.hamcrest.MatcherAssert.assertThat;


public class TestAssertions {

    @BeforeMethod
    @AfterClass
    public void reset() {
        ConfigValueHandlerIBS.resetAllValues();
        MyPropertiesHandler.resetAll();
        ConfigValueHandlerIBS.ENVIRONMENT_VARS_SETTER_CLASS.activate(EnvironmentVariableHandler.class.getTypeName());
        ConfigValueHandlerIBS.INTEGRITY_PACKAGE_INJECTION_MODE.activate("manual");
    }

    @Test
    public void simpleAssertion() {
        JavaCalls l_myJavaCall = new JavaCalls();

        CallContent l_cc = new CallContent();
        l_cc.setClassName(SimpleStaticMethods.class.getTypeName());
        l_cc.setMethodName("methodReturningString");

        l_cc.setArgs(new Object[] { "_Success" });

        l_myJavaCall.getCallContent().put("fetchString", l_cc);

        Assertion l_assert = new Assertion();
        assertThat("By default the type should be RESULT", l_assert.getType(), Matchers.equalTo(Assertion.TYPES.RESULT));

        l_assert.setMatcher("equalTo");
        l_assert.setActualValue("A");
        l_assert.setExpectedValue("B");

        assertThat("We should be false", l_assert.perform(l_myJavaCall.getLocalClassLoader(), new JavaCallResults()), Matchers.equalTo(false));

        l_assert.setActualValue("B");

        assertThat("We should now be true", l_assert.perform(l_myJavaCall.getLocalClassLoader(), new JavaCallResults()));
    }

    @Test
    public void assertionInProduction_expandActual() {
        JavaCalls l_myJavaCall = new JavaCalls();

        CallContent l_cc = new CallContent();
        l_cc.setClassName(SimpleStaticMethods.class.getTypeName());
        l_cc.setMethodName("methodReturningString");

        l_myJavaCall.getCallContent().put("fetchString", l_cc);

        l_myJavaCall.submitCalls();

        Assertion l_assert = new Assertion();
        l_assert.setType(Assertion.TYPES.RESULT);
        l_assert.setMatcher("equalTo");
        l_assert.setActualValue("fetchString");
        l_assert.setExpectedValue("_Success");

        assertThat("We should correctly compare the values", l_assert.perform(l_myJavaCall.getLocalClassLoader(), new JavaCallResults()));

    }

    @Test
    public void assertionInProduction_expandAll() {
        JavaCalls l_myJavaCall = new JavaCalls();

        CallContent l_cc = new CallContent();
        l_cc.setClassName(SimpleStaticMethods.class.getTypeName());
        l_cc.setMethodName("methodReturningString");

        l_myJavaCall.getCallContent().put("fetchString1", l_cc);

        CallContent l_cc2 = new CallContent();
        l_cc2.setClassName(SimpleStaticMethods.class.getTypeName());
        l_cc2.setMethodName("methodReturningString");

        l_myJavaCall.getCallContent().put("fetchString2", l_cc2);

        l_myJavaCall.submitCalls();

        Assertion l_assert = new Assertion();
        l_assert.setType(Assertion.TYPES.RESULT);
        l_assert.setMatcher("equalTo");
        l_assert.setActualValue("fetchString1");
        l_assert.setExpectedValue("fetchString2");

        assertThat("We should correctly compare the values", l_assert.perform(l_myJavaCall.getLocalClassLoader(), new JavaCallResults()));

    }


    @Test
    public void assertionInProduction_calculateDuration() {
        JavaCalls l_myJavaCall = new JavaCalls();

        CallContent l_cc = new CallContent();
        l_cc.setClassName(SimpleStaticMethods.class.getTypeName());
        l_cc.setMethodName("methodWithTimeOut");
        l_cc.setArgs(new Object[]{100});

        l_myJavaCall.getCallContent().put("spendTime", l_cc);

        JavaCallResults jcr = l_myJavaCall.submitCalls();

        assertThat("Checking the duration", jcr.getCallDurations().get("spendTime"), Matchers.greaterThanOrEqualTo(100l));

        Assertion l_assert = new Assertion();
        l_assert.setType(Assertion.TYPES.DURATION);
        l_assert.setActualValue("spendTime");
        l_assert.setMatcher("greaterThanOrEqualTo");
        l_assert.setExpectedValue(100);

        assertThat("We should have a duration greater or Equal to 100ms", l_assert.perform(l_myJavaCall.getLocalClassLoader(), jcr));

    }


    @Test
    public void assertionInProductionE2E_calculateDuration() {
        JavaCalls l_myJavaCall = new JavaCalls();

        CallContent l_cc = new CallContent();
        l_cc.setClassName(SimpleStaticMethods.class.getTypeName());
        l_cc.setMethodName("methodWithTimeOut");
        l_cc.setArgs(new Object[]{100});

        l_myJavaCall.getCallContent().put("spendTime", l_cc);

        Assertion l_assert = new Assertion();
        l_assert.setType(Assertion.TYPES.DURATION);
        l_assert.setActualValue("spendTime");
        l_assert.setMatcher("greaterThanOrEqualTo");
        l_assert.setExpectedValue(100);
        l_myJavaCall.getAssertions().put("The duration should be greater that 100ms", l_assert);

        JavaCallResults jcr = l_myJavaCall.submitCalls();

        assertThat("Checking the duration", jcr.getCallDurations().get("spendTime"), Matchers.greaterThanOrEqualTo(100l));
        assertThat("We should have a duration greater or Equal to 100ms", jcr.getAssertionResults().containsKey("The duration should be greater that 100ms"));
    }


    @Test
    public void assertionInProductionE2E_expandAll() {
        JavaCalls l_myJavaCall = new JavaCalls();

        CallContent l_cc = new CallContent();
        l_cc.setClassName(SimpleStaticMethods.class.getTypeName());
        l_cc.setMethodName("methodReturningString");

        l_myJavaCall.getCallContent().put("fetchString1", l_cc);

        CallContent l_cc2 = new CallContent();
        l_cc2.setClassName(SimpleStaticMethods.class.getTypeName());
        l_cc2.setMethodName("methodReturningString");

        l_myJavaCall.getCallContent().put("fetchString2", l_cc2);

        Assertion l_assert = new Assertion();
        l_assert.setType(Assertion.TYPES.RESULT);
        l_assert.setMatcher("equalTo");
        l_assert.setActualValue("fetchString1");
        l_assert.setExpectedValue("fetchString2");

        l_myJavaCall.getAssertions().put("Both values should be the same", l_assert);

        JavaCallResults jcr = l_myJavaCall.submitCalls();

        assertThat("Our assertion should be correct", jcr.getAssertionResults().containsKey("Both values should be the same"));
        assertThat("Our assertion should be correct", jcr.getAssertionResults().get("Both values should be the same"));


    }
}
