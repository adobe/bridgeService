/*
 * Copyright 2022 Adobe
 * All Rights Reserved.
 *
 * NOTICE: Adobe permits you to use, modify, and distribute this file in
 * accordance with the terms of the Adobe license agreement accompanying
 * it.
 */
package com.adobe.campaign.tests.bridge.service;

import com.adobe.campaign.tests.bridge.service.exceptions.*;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import java.util.Map;
import java.util.Set;

public class BridgeServiceFactory {

    public static final String ERROR_CALL_TIMEOUT = "The call you made exceeds the set timeout limit.";
    public static final String ERROR_IBS_INTERNAL = "Internal IBS error. Please file a bug report with the project and provide this JSON in the report.";
    public static final String ERROR_PAYLOAD_INCONSISTENCY = "We detected an inconsistency in your payload.";
    static final String ERROR_JSON_TRANSFORMATION = "JSON Transformation issue : Problem processing request. The given json could not be mapped to a Java Call";
    static final String ERROR_CALLING_JAVA_METHOD = "Error during call of target Java Class and Method.";
    static final String ERROR_JAVA_OBJECT_NOT_FOUND = "Could not find the given class or method.";
    static final String ERROR_IBS_CONFIG = "The provided class and method for setting environment variables is not valid.";
    static final String ERROR_IBS_RUNTIME = "Problems with payload.";
    static final String ERROR_AMBIGUOUS_METHOD = "No unique method could be identified that matches your request.";
    static final String ERROR_JAVA_OBJECT_NOT_ACCESSIBLE = "The java object you want to call is inaccessible. This is very possibly a scope problem.";
    /**
     * Creates a Java Call Object given a JSON as a String
     * @param in_requestJSON A JSON Object as a String
     * @return A Java Call Object
     * @throws JsonProcessingException thrown when the JSON return object could not be created
     */
    public static JavaCalls createJavaCalls(String in_requestJSON) throws JsonProcessingException {
        LogManagement.logStep(LogManagement.STD_STEPS.ANALYZING_PAYLOAD);
        ObjectMapper mapper = new ObjectMapper();
        return mapper.readValue(in_requestJSON, JavaCalls.class);
    }

    /**
     * Transforms the results of the JavaCall to a JSON
     * @param in_callResults A JavaCallResults object to be transformed
     * @param in_secretValues A set of secrets that need to be checked for the output
     * @return The JSON representation of the Call result
     * @throws JsonProcessingException when failing to parse the JavaCallResults object
     * @throws IBSPayloadException when the output contains secrets that have been passed
     */
    public static String transformJavaCallResultsToJSON(JavaCallResults in_callResults, Set<String> in_secretValues)
            throws JsonProcessingException {
        LogManagement.logStep(LogManagement.STD_STEPS.GENERATING_RESPONSE);

        ObjectMapper mapper = new ObjectMapper();
        //Activate when we have No serializer found for class errors
        //mapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
        String lr_resultPayload = mapper.writeValueAsString(in_callResults);
        if (ConfigValueHandlerIBS.SECRETS_BLOCK_OUTPUT.is("true") && in_secretValues.stream().anyMatch(h -> lr_resultPayload.contains(h))) {
            throw new IBSPayloadException("Your return payload contains secrets. You may consider re-evaluating the headers you send. If they are not a secret, they can be put directly in the payload. Otherwise you can simply disable the "+ConfigValueHandlerIBS.SECRETS_BLOCK_OUTPUT.systemName+" option.");
        }
        return lr_resultPayload;
    }

    /**
     * Creates a ServiceAccess Object given a JSON as a String
     * @param in_requestJSON A JSON Object as a String
     * @return A Service Access Object
     * @throws JsonProcessingException thrown when the JSON return object could not be created
     */
    public static ServiceAccess createServiceAccess(String in_requestJSON) throws JsonProcessingException {
        LogManagement.logStep(LogManagement.STD_STEPS.ANALYZING_PAYLOAD);
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
        LogManagement.logStep(LogManagement.STD_STEPS.GENERATING_RESPONSE);
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

    /**
     * Maps a runtime exception to a serialized error payload using the standard IBS
     * exception-to-error-code mapping. Centralises the mapping so both the REST /call
     * endpoint and the MCP tools/call endpoint produce consistent error responses and
     * a new exception type only needs to be added in one place.
     *
     * @param e the exception to map
     * @return serialized JSON error payload
     */
    public static String createExceptionPayLoad(Exception e) {
        String title;
        int code;
        boolean includeStackTrace = true;

        if (e instanceof IBSTimeOutException) {
            title = ERROR_CALL_TIMEOUT;
            code = 408;
            includeStackTrace = false;
        } else if (e instanceof NonExistentJavaObjectException) {
            title = ERROR_JAVA_OBJECT_NOT_FOUND;
            code = 404;
            includeStackTrace = false;
        } else if (e instanceof AmbiguousMethodException) {
            title = ERROR_AMBIGUOUS_METHOD;
            code = 404;
            includeStackTrace = false;
        } else if (e instanceof IBSConfigurationException) {
            title = ERROR_IBS_CONFIG;
            code = 500;
        } else if (e instanceof IBSRunTimeException) {
            title = ERROR_IBS_RUNTIME;
            code = 500;
        } else if (e instanceof TargetJavaMethodCallException) {
            title = ERROR_CALLING_JAVA_METHOD;
            code = 500;
        } else if (e instanceof JavaObjectInaccessibleException) {
            title = ERROR_JAVA_OBJECT_NOT_ACCESSIBLE;
            code = 404;
            includeStackTrace = false;
        } else {
            title = ERROR_IBS_INTERNAL;
            code = 500;
        }

        return createExceptionPayLoad(new ErrorObject(e, title, code, includeStackTrace));
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
