/*
 * Copyright 2022 Adobe
 * All Rights Reserved.
 *
 * NOTICE: Adobe permits you to use, modify, and distribute this file in
 * accordance with the terms of the Adobe license agreement accompanying
 * it.
 */
package com.adobe.campaign.tests.bridge.testdata.two;

import com.adobe.campaign.tests.bridge.testdata.one.EnvironmentVariableHandler;

public class StaticMethodsIntegrity {

    //RandomManager.getRandomEmail
    public static String assembleBySystemValues() {
        return EnvironmentVariableHandler.getCacheProperty("PREFIX") + "+c@" + EnvironmentVariableHandler.getCacheProperty("SUFFIX");
    }
}
