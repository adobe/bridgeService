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
import com.sun.source.tree.AssertTree;
import org.hamcrest.Matchers;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.Arrays;

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
        assertThat("By default the type should be RESULT", l_assert.type, Matchers.equalTo(Assertion.TYPES.RESULT));

        l_assert.matcher = "equalTo";
        l_assert.actualValue = "A";
        l_assert.expected = "B";

        assertThat("We should be false", l_assert.perform(l_myJavaCall.getLocalClassLoader(), new JavaCallResults()), Matchers.equalTo(false));

        l_assert.actualValue="B";

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
        l_assert.type = Assertion.TYPES.RESULT;
        l_assert.matcher = "equalTo";
        l_assert.actualValue = "fetchString";
        l_assert.expected = "_Success";

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
        l_assert.type = Assertion.TYPES.RESULT;
        l_assert.matcher = "equalTo";
        l_assert.actualValue = "fetchString1";
        l_assert.expected = "fetchString2";

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
        l_assert.type = Assertion.TYPES.DURATION;
        l_assert.matcher = "greaterThanOrEqualTo";
        l_assert.actualValue = "spendTime";
        l_assert.expected = Arrays.asList("100");

        assertThat("We should have a duration greater or Equal to 100ms", l_assert.perform(l_myJavaCall.getLocalClassLoader(), jcr));

    }
}
