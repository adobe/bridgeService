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
    private static final long serialVersionUID = 2535562419078918507L;

    private Map<String, Long> callDurations;

    private Map<String, Object> returnValues = new HashMap<>();

    JavaCallResults() {
        setReturnValues(new HashMap<>());
        setCallDurations(new HashMap<>());
    }


    public Map<String, Object> getReturnValues() {
        return returnValues;
    }

    public Map<String, Long> getCallDurations() {
        return callDurations;
    }

    public void setCallDurations(Map<String, Long> callDurations) {
        this.callDurations = callDurations;
    }

    public void setReturnValues(Map<String, Object> returnValues) {
        this.returnValues = returnValues;
    }


    /**
     * Stored the given result in the return Object
     *
     * @param in_key     a key to store the result with
     * @param callResult a result object
     * @param in_callDuration The duration of the call
     */
    public void addResult(String in_key, Object callResult, long in_callDuration) {
        returnValues.put(in_key, callResult);
        callDurations.put(in_key, in_callDuration);
    }
    
}
