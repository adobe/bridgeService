package com.adobe.campaign.tests.bridge.testdata.two;

import com.adobe.campaign.tests.bridge.testdata.one.EnvironmentVariableHandler;

public class StaticMethodsIntegrity {

    //RandomManager.getRandomEmail
    public static String assembleBySystemValues() {
        return EnvironmentVariableHandler.getCacheProperty("PREFIX") + "+c@" + EnvironmentVariableHandler.getCacheProperty("SUFFIX");
    }
}
