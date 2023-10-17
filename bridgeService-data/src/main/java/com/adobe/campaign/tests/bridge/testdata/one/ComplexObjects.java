/*
 * Copyright 2022 Adobe
 * All Rights Reserved.
 *
 * NOTICE: Adobe permits you to use, modify, and distribute this file in
 * accordance with the terms of the Adobe license agreement accompanying
 * it.
 */
package com.adobe.campaign.tests.bridge.testdata.one;

//import io.restassured.path.json.JsonPath;
import org.json.simple.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class ComplexObjects {

    /*
    public static JsonPath returnJSONPath() {
        String jsonString = "{\r\n" +
                "  \"firstName\": \"Amod\",\r\n" +
                "  \"lastName\": \"Mahajan\"\r\n" +
                "}";

        return  JsonPath.from(jsonString);
    }
     */

    public static Map returnMap() {
        Map mapOfString = new HashMap();
        mapOfString.put("object1", "value1");
        mapOfString.put("object3", "value3");

        return  mapOfString;
    }

    public static JSONObject returnJSONSimple() {
        JSONObject jsonOfString = new JSONObject();
        jsonOfString.put("object1", "value1");
        jsonOfString.put("object3", "value3");

        return  jsonOfString;
    }
}
