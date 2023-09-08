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

import java.util.HashMap;
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
     * Extracted the information related to the exceptions caused by the application
     * @param in_exception The exception that was raised
     * @param in_ibsSystemMessage The Error message we provide to the users
     * @param in_errorCode The code of the exception
     * @return A string that represents the error payload
     */
    public static String createExceptionPayLoad(Exception in_exception, String in_ibsSystemMessage, int in_errorCode) {
        Map<String, Object> l_errorFields = new HashMap<>();
        l_errorFields.put("title", in_ibsSystemMessage);
        l_errorFields.put("bridgServiceException", in_exception.getClass().getTypeName());
        l_errorFields.put("originalException", (in_exception.getCause() != null) ? in_exception.getCause().getClass().getTypeName() : "N/A");

        l_errorFields.put("code", in_errorCode);
        l_errorFields.put("detail", in_exception.getMessage());

        return getErrorPayloadAdString(in_ibsSystemMessage, l_errorFields);
    }

    //Calls the testable getPayloadAdString
    private static String getErrorPayloadAdString(String in_ibsSystemMessage, Map<String, Object> l_errorFields) {
        return getErrorPayloadAdString(new ObjectMapper(), in_ibsSystemMessage, l_errorFields);
    }

    /**
     * Creates a JSON from the given map
     * @param o an Object mapper object
     * @param in_ibsSystemErrorMessage The System message
     * @param in_errorFields The map payload of error data
     * @return
     */
    protected static String getErrorPayloadAdString(ObjectMapper o, String in_ibsSystemErrorMessage, Map<String, Object> in_errorFields) {
        try {
            return o.writeValueAsString(in_errorFields);
        } catch (JsonProcessingException e) {
            return "Problem creating error payload. Original error is " + in_ibsSystemErrorMessage;
        }
    }
}
