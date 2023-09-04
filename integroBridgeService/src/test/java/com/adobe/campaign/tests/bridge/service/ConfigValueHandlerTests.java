/*
 * Copyright 2022 Adobe
 * All Rights Reserved.
 *
 * NOTICE: Adobe permits you to use, modify, and distribute this file in
 * accordance with the terms of the Adobe license agreement accompanying
 * it.
 */
package com.adobe.campaign.tests.bridge.service;

import org.hamcrest.Matchers;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import static org.hamcrest.MatcherAssert.assertThat;

public class ConfigValueHandlerTests {
    @BeforeMethod
    @AfterClass
    public void reset() {
        ConfigValueHandlerIBS.resetAllValues();
    }

    @Test
    public void testIn() {
        ConfigValueHandlerIBS.INTEGRITY_PACKAGE_INJECTION_MODE.activate("manual");

        assertThat("We should correctly detect that our value is in the allowed values",
                ConfigValueHandlerIBS.INTEGRITY_PACKAGE_INJECTION_MODE.is("manual"));

        assertThat("We should correctly detect that our value is in the allowed values",
                ConfigValueHandlerIBS.INTEGRITY_PACKAGE_INJECTION_MODE.is("semi-manual", "manual"));

        assertThat("We should correctly detect that our value is in the allowed values",
                Matchers.not(ConfigValueHandlerIBS.INTEGRITY_PACKAGE_INJECTION_MODE.is("semi-manual", "automatic")));

        assertThat("We should correctly detect that our value is in the allowed values",
                ConfigValueHandlerIBS.INTEGRITY_PACKAGE_INJECTION_MODE.is("MaNual", "automatic"));

        assertThat("We should correctly detect that our value is in the allowed values",
                Matchers.not(ConfigValueHandlerIBS.INTEGRITY_PACKAGE_INJECTION_MODE.is("not really there")));
    }


}
