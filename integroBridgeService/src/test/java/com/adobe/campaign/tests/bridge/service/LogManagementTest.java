/*
 * Copyright 2022 Adobe
 * All Rights Reserved.
 *
 * NOTICE: Adobe permits you to use, modify, and distribute this file in
 * accordance with the terms of the Adobe license agreement accompanying
 * it.
 */
package com.adobe.campaign.tests.bridge.service;

import org.testng.Assert;
import org.testng.annotations.AfterGroups;
import org.testng.annotations.BeforeGroups;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import spark.Spark;

import java.io.IOException;

public class LogManagementTest {

    @Test
    public void testLogManagement() {
        LogManagement lm = new LogManagement();
        assert LogManagement.STD_CURRENT_STEP.equals("currentStep");
    }

    @BeforeMethod
    public void cleanCache() {
        ConfigValueHandlerIBS.resetAllValues();
    }



}
