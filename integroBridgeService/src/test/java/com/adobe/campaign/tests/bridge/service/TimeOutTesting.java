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
import com.adobe.campaign.tests.bridge.service.exceptions.IBSTimeOutException;
import com.adobe.campaign.tests.bridge.testdata.one.EnvironmentVariableHandler;
import com.adobe.campaign.tests.bridge.testdata.one.SimpleStaticMethods;
import org.hamcrest.Matchers;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.Properties;

import static org.hamcrest.MatcherAssert.assertThat;

public class TimeOutTesting {
    @BeforeMethod
    @AfterClass
    public void reset() {
        ConfigValueHandlerIBS.resetAllValues();
        MyPropertiesHandler.resetAll();
        EnvironmentVariableHandler.setIntegroCache(new Properties());
        ConfigValueHandlerIBS.ENVIRONMENT_VARS_SETTER_CLASS.activate(EnvironmentVariableHandler.class.getTypeName());

    }

    @Test
    public void testConfigAndTooling() {
        assertThat(ConfigValueHandlerIBS.DEFAULT_CALL_TIMEOUT.systemName, Matchers.equalTo("IBS.TIMEOUT.DEFAULT"));
        assertThat(ConfigValueHandlerIBS.DEFAULT_CALL_TIMEOUT.defaultValue, Matchers.equalTo("10000"));

        ConfigValueHandlerIBS.DEFAULT_CALL_TIMEOUT.activate("500");
        Long l_sleepDuration = Long.parseLong(ConfigValueHandlerIBS.DEFAULT_CALL_TIMEOUT.fetchValue());

        long l_start = System.currentTimeMillis();
        SimpleStaticMethods.methodWithTimeOut(l_sleepDuration);
        long l_end = System.currentTimeMillis();

        long l_actualDuration = l_end - l_start;
        assertThat("We should be within the sleep duration", l_actualDuration,
                Matchers.greaterThanOrEqualTo(l_sleepDuration));
    }

    @Test
    public void testPrepareData() {
        String l_expectedDuration = "500";
        ConfigValueHandlerIBS.DEFAULT_CALL_TIMEOUT.activate(l_expectedDuration);
        Long l_sleepDuration = Long.parseLong(ConfigValueHandlerIBS.DEFAULT_CALL_TIMEOUT.fetchValue());

        JavaCalls jc = new JavaCalls();
        CallContent cc1 = new CallContent();
        cc1.setClassName(SimpleStaticMethods.class.getTypeName());
        cc1.setMethodName("methodWithTimeOut");
        cc1.setArgs(new Object[] { 300l });
        jc.getCallContent().put("call1", cc1);

        long l_start = System.currentTimeMillis();
        JavaCallResults jcr = jc.submitCalls();
        long l_end = System.currentTimeMillis();
        long l_actualDuration = l_end - l_start;

         assertThat("The duration of the call should be less than " + l_expectedDuration, l_actualDuration,
                Matchers.lessThan(l_sleepDuration));

        assertThat("The stored duration should be less than the measured duration", jcr.getCallDurations().get("call1"),
                Matchers.lessThanOrEqualTo(l_actualDuration));

    }


    @Test
    public void testTimeOut() {
        String l_expectedDuration = "300";
        ConfigValueHandlerIBS.DEFAULT_CALL_TIMEOUT.activate(l_expectedDuration);
        Long l_sleepDuration = Long.parseLong(ConfigValueHandlerIBS.DEFAULT_CALL_TIMEOUT.fetchValue());

        JavaCalls jc = new JavaCalls();
        CallContent cc1 = new CallContent();
        cc1.setClassName(SimpleStaticMethods.class.getTypeName());
        cc1.setMethodName("methodWithTimeOut");
        cc1.setArgs(new Object[] { l_sleepDuration + 100 });
        jc.getCallContent().put("call1", cc1);

        long l_start = System.currentTimeMillis();
        boolean is_testPassed = false;
        try {
            jc.submitCalls();
        } catch (Exception exp) {
            assertThat("The thrown exception should be of the correct type", exp, Matchers.instanceOf(
                    IBSTimeOutException.class));
            assertThat(exp.getMessage(), Matchers.containsString("call1"));
            assertThat(exp.getMessage(), Matchers.containsString(l_expectedDuration));

            is_testPassed = true;
        }

        assertThat("We should have gone in the exception", is_testPassed);
    }

    @Test
    public void testTimeOutOverriddenInCall_Fail() {
        String l_expectedDuration = "500";
        ConfigValueHandlerIBS.DEFAULT_CALL_TIMEOUT.activate(l_expectedDuration);
        Long l_sleepDuration = Long.parseLong(ConfigValueHandlerIBS.DEFAULT_CALL_TIMEOUT.fetchValue());

        JavaCalls jc = new JavaCalls();
        jc.setTimeout(299l);
        CallContent cc1 = new CallContent();
        cc1.setClassName(SimpleStaticMethods.class.getTypeName());
        cc1.setMethodName("methodWithTimeOut");
        cc1.setArgs(new Object[] { l_sleepDuration - 100 });
        jc.getCallContent().put("call1", cc1);

        long l_start = System.currentTimeMillis();
        Assert.assertThrows(IBSTimeOutException.class, () -> jc.submitCalls());
        long l_end = System.currentTimeMillis();
        long l_actualDuration = l_end - l_start;

        assertThat("The duration of the call should be less than " + l_expectedDuration, l_actualDuration,
                Matchers.lessThan(l_sleepDuration));

        assertThat("The duration of the call should be less than " + l_expectedDuration, l_actualDuration,
                Matchers.greaterThanOrEqualTo(jc.getTimeout()));
    }

    @Test
    public void testTimeOutOverriddenInCall_Pass() {
        String l_expectedDuration = "300";
        ConfigValueHandlerIBS.DEFAULT_CALL_TIMEOUT.activate(l_expectedDuration);
        Long l_sleepDuration = Long.parseLong(ConfigValueHandlerIBS.DEFAULT_CALL_TIMEOUT.fetchValue());

        JavaCalls jc = new JavaCalls();
        jc.setTimeout(450l);
        CallContent cc1 = new CallContent();
        cc1.setClassName(SimpleStaticMethods.class.getTypeName());
        cc1.setMethodName("methodWithTimeOut");
        cc1.setArgs(new Object[] { l_sleepDuration + 100 });
        jc.getCallContent().put("call1", cc1);

        long l_start = System.currentTimeMillis();
        jc.submitCalls();
        long l_end = System.currentTimeMillis();
        long l_actualDuration = l_end - l_start;

        assertThat("The duration of the call should be less than " + l_expectedDuration, l_actualDuration,
                Matchers.greaterThan(l_sleepDuration));

        assertThat("The duration of the call should be less than " + l_expectedDuration, l_actualDuration,
                Matchers.lessThan(jc.getTimeout()));

    }

    @Test
    public void testTimeoutOverride() {
        String l_expectedDuration = "200";
        ConfigValueHandlerIBS.DEFAULT_CALL_TIMEOUT.activate(l_expectedDuration);
        Long l_sleepDuration = Long.parseLong(ConfigValueHandlerIBS.DEFAULT_CALL_TIMEOUT.fetchValue());

        JavaCalls jc = new JavaCalls();
        jc.setTimeout(0l);
        CallContent cc1 = new CallContent();
        cc1.setClassName(SimpleStaticMethods.class.getTypeName());
        cc1.setMethodName("methodWithTimeOut");
        cc1.setArgs(new Object[] { l_sleepDuration + 100 });
        jc.getCallContent().put("call1", cc1);

        long l_start = System.currentTimeMillis();
        jc.submitCalls();
        long l_end = System.currentTimeMillis();
        long l_actualDuration = l_end - l_start;

        assertThat("The duration of the call should be less than " + l_expectedDuration, l_actualDuration,
                Matchers.greaterThan(l_sleepDuration));

    }


}
