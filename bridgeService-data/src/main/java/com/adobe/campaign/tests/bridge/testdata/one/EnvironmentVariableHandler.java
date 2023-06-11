package com.adobe.campaign.tests.bridge.testdata.one;

import java.util.Properties;

public class EnvironmentVariableHandler {

    static Properties cache = new Properties();

    public static void setIntegroCache(Properties in_properties) {
        cache = in_properties;
    }

    public static String getCacheProperty(String in_string) {
        return cache.getProperty(in_string);
    }
}
