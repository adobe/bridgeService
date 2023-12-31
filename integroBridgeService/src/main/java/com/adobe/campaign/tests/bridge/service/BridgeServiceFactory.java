/*
 * Copyright 2022 Adobe
 * All Rights Reserved.
 *
 * NOTICE: Adobe permits you to use, modify, and distribute this file in
 * accordance with the terms of the Adobe license agreement accompanying
 * it.
 */
package com.adobe.campaign.tests.bridge.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Map;

public class BridgeServiceFactory {
    /**
     * Creates a Java Call Object given a JSON as a String
     * @param in_requestJSON A JSON Object as a String
     * @return A Java Call Object
     */
    public static JavaCalls createJavaCalls(String in_requestJSON) throws JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper();
        return mapper.readValue(in_requestJSON, JavaCalls.class);
    }

    /**
     * Transforms the results of the JavaCall to a JSON
     * @param in_callResults A JavaCallResults object to be transformed
     * @return The JSON representation of the Call result
     * @throws JsonProcessingException when failing to parse the JavaCallResults object
     */
    public static String transformJavaCallResultsToJSON(JavaCallResults in_callResults) throws JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper();
        return mapper.writeValueAsString(in_callResults);
    }

    /**
     * Creates a ServiceAccess Object given a JSON as a String
     * @param in_requestJSON A JSON Object as a String
     * @return A Service Access Object
     */
    public static ServiceAccess createServiceAccess(String in_requestJSON) throws JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper();
        Map<String, String> serviceMap = mapper.readValue(in_requestJSON, Map.class);
        ServiceAccess lr_sa = new ServiceAccess();
        lr_sa.setExternalServices(serviceMap);
        return lr_sa;
    }

    /**
     * Transforms the results of the ServiceCheck to a JSON
     * @param in_callResults The results of the service check
     * @return A string representation of the results
     * @throws JsonProcessingException thrown when the service call results could not be parsed
     */
    public static String transformServiceAccessResult(Map<String, Boolean> in_callResults)
            throws JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper();
        return mapper.writeValueAsString(in_callResults);
    }

    /**
     * Transforms the results of the TEST call to a JSON
     * @param in_testPayLoad The results of the service check
     * @return A string representation of the results
     * @throws JsonProcessingException thrown when the service call results could not be parsed
     */
    public static String transformMapTosResult(Map<String, String> in_testPayLoad)
            throws JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper();
        return mapper.writeValueAsString(in_testPayLoad);
    }

    /**
     * Extracted the information related to the exceptions caused by the application
     * @param in_errorObject The code of the exception
     * @return A string that represents the error payload
     */
    public static String createExceptionPayLoad(ErrorObject in_errorObject) {


        return getErrorPayloadAsString(in_errorObject);
    }

    //Calls the testable getPayloadAdString
    private static String getErrorPayloadAsString(ErrorObject in_errorObject) {
        return getErrorPayloadAsString(new ObjectMapper(), in_errorObject);
    }

    /**
     * Creates a JSON from the given error object
     * @param o an Object mapper object
     * @param in_errorObject The  payload of error data
     * @return the error as a payload
     */
    protected static String getErrorPayloadAsString(ObjectMapper o, ErrorObject in_errorObject) {
        try {
            return o.writeValueAsString(in_errorObject);
        } catch (JsonProcessingException e) {
            return "Problem creating error payload. Original error is " + in_errorObject.getTitle();
        }
    }
}
