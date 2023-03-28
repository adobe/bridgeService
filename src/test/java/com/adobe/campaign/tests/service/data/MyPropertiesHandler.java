package com.adobe.campaign.tests.service.data;

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
