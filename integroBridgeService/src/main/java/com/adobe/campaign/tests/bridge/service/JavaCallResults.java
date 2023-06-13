/*
 * Copyright 2022 Adobe
 * All Rights Reserved.
 *
 * NOTICE: Adobe permits you to use, modify, and distribute this file in
 * accordance with the terms of the Adobe license agreement accompanying
 * it.
 */
package com.adobe.campaign.tests.bridge.service;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

public class JavaCallResults  implements Serializable {
    Map<String, Object> returnValues;

    JavaCallResults() {
        returnValues = new HashMap<>();
    }

    public Map<String, Object> getReturnValues() {
        return returnValues;
    }


    /**
     * Stored the given result in the return Object
     * @param in_key a key to store the result with
     * @param callResult a result object
     */
    public void addResult(String in_key, Object callResult) {
        returnValues.put(in_key, callResult);
    }
    
}
