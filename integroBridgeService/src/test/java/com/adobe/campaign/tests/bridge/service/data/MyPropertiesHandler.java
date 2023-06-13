/*
 * Copyright 2022 Adobe
 * All Rights Reserved.
 *
 * NOTICE: Adobe permits you to use, modify, and distribute this file in
 * accordance with the terms of the Adobe license agreement accompanying
 * it.
 */
package com.adobe.campaign.tests.bridge.service.data;

import java.util.Properties;

public class MyPropertiesHandler {
    public static Properties myProps;

    public void fillMeUp(Properties in_properties) {
        myProps=in_properties;
    }

    public Properties getMyProps() {
        return myProps;
    }

    public String getMyProp(String s) {
        return myProps.getProperty(s);
    }

    public static void resetAll() {
        myProps=new Properties();
    }

}
