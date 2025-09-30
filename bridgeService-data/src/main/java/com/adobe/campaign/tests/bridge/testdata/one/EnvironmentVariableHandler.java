/*
 * Copyright 2022 Adobe
 * All Rights Reserved.
 *
 * NOTICE: Adobe permits you to use, modify, and distribute this file in
 * accordance with the terms of the Adobe license agreement accompanying
 * it.
 */
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
