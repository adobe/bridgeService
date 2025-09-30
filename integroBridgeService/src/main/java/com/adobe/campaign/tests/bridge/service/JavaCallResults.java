/*
 * Copyright 2022 Adobe
 * All Rights Reserved.
 *
 * NOTICE: Adobe permits you to use, modify, and distribute this file in
 * accordance with the terms of the Adobe license agreement accompanying
 * it.
 */
package com.adobe.campaign.tests.bridge.service;

import com.adobe.campaign.tests.bridge.service.exceptions.IBSRunTimeException;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

public class JavaCallResults implements Serializable {
    private static final long serialVersionUID = 2535562419078918507L;

    private Map<String, Long> callDurations;

    private Map<String, Object> returnValues = new HashMap<>();

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private HashMap<String, Boolean> assertionResults;


    JavaCallResults() {
        setReturnValues(new HashMap<>());
        setCallDurations(new HashMap<>());
        setAssertionResults(new HashMap<>());
    }

    public Map<String, Object> getReturnValues() {
        return returnValues;
    }

    public void setReturnValues(Map<String, Object> returnValues) {
        this.returnValues = returnValues;
    }

    public Map<String, Long> getCallDurations() {
        return callDurations;
    }

    public void setCallDurations(Map<String, Long> callDurations) {
        this.callDurations = callDurations;
    }

    /**
     * Stored the given result in the return Object
     *
     * @param in_key          a key to store the result with
     * @param callResult      a result object
     * @param in_callDuration The duration of the call
     */
    public void addResult(String in_key, Object callResult, long in_callDuration) {
        returnValues.put(in_key, callResult);
        callDurations.put(in_key, in_callDuration);
    }

    /**
     * Used for assertions where we assert the duration of the executions. If the given object is a key, we return the duration stored for that key. Otherwise we return the exact 'long' value of the given duration.
     *
     * @param in_keyOrValue An object that is either a key or an
     * @return A duration in milliseconds
     */
    public Long expandDurations(Object in_keyOrValue) {
        if (getCallDurations().containsKey(in_keyOrValue) ) {
            return callDurations.get(in_keyOrValue);
        } else  {
            try {
                return Long.parseLong(
                        String.valueOf(in_keyOrValue));
            } catch (NumberFormatException nfe) {
                throw new IBSRunTimeException("The given keyOrValue "+in_keyOrValue+" could not be mapped to a duration.");
            }
        }

    }

    public HashMap<String, Boolean> getAssertionResults() {
        return assertionResults;
    }

    public void setAssertionResults(HashMap<String, Boolean> assertionResults) {
        this.assertionResults = assertionResults;
    }
}
