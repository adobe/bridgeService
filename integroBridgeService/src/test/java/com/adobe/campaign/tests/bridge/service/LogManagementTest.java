/*
 * Copyright 2022 Adobe
 * All Rights Reserved.
 *
 * NOTICE: Adobe permits you to use, modify, and distribute this file in
 * accordance with the terms of the Adobe license agreement accompanying
 * it.
 */
package com.adobe.campaign.tests.bridge.service;

import org.testng.annotations.Test;

public class LogManagementTest {

    @Test
    public void testLogManagement() {
        LogManagement lm = new LogManagement();
        assert LogManagement.STD_CURRENT_STEP.equals("currentStep");
    }

}