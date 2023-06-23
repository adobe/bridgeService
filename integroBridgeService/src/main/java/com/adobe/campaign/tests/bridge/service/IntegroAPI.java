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
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;

import static spark.Spark.*;

public class IntegroAPI {
    private static final Logger log = LogManager.getLogger();

    protected static final String ERROR_JSON_TRANSFORMATION = "JSON Transformation issue : Problem processing request. The given json could not be mapped to a Java Call";
    protected static final String ERROR_CALLING_JAVA_METHOD = "Error during call of target Java Class and Method.";
    protected static final String ERROR_JAVA_OBJECT_NOT_FOUND = "Could not find the given class or method.";
    protected static final String ERROR_IBS_CONFIG = "The provided class and method for setting environment variables is not valid.";
    protected static final String ERROR_IBS_RUNTIME = "Problems with payload. Check the passed environment variables.";
    public static final String ERROR_CALL_TIMEOUT = "The call you made exceeds the set timeout limit.";


    public static void
    startServices(int port) {

        if (Boolean.parseBoolean(ConfigValueHandlerIBS.SSL_ACTIVE.fetchValue())) {
            File l_file = new File(ConfigValueHandlerIBS.SSL_KEYSTORE_PATH.fetchValue());
            log.info("Keystore file was found? {}", l_file.exists());
            secure(ConfigValueHandlerIBS.SSL_KEYSTORE_PATH.fetchValue(), ConfigValueHandlerIBS.SSL_KEYSTORE_PASSWORD.fetchValue(),
                    ConfigValueHandlerIBS.SSL_TRUSTSTORE_PATH.fetchValue(), ConfigValueHandlerIBS.SSL_TRUSTSTORE_PASSWORD.fetchValue());
        }
        else {
            port(port);
        }

        get("/test", (req, res) -> {
            res.type("text/plain");

            StringBuilder sb = new StringBuilder("All systems up "+ ConfigValueHandlerIBS.DEPLOYMENT_MODEL.fetchValue());
            sb.append("\n");
            sb.append("Bridge Service Version : ");
            sb.append(ConfigValueHandlerIBS.PRODUCT_VERSION.fetchValue());
            if (ConfigValueHandlerIBS.PRODUCT_USER_VERSION.isSet()) {
                sb.append("\n");
                sb.append("Product user version : ");
                sb.append(ConfigValueHandlerIBS.PRODUCT_USER_VERSION.fetchValue());
            }
            return sb.toString();
        });

        post("/service-check", (req, res) -> {
            ServiceAccess l_serviceAccess = BridgeServiceFactory.createServiceAccess(req.body());
            return BridgeServiceFactory.transformServiceAccessResult(l_serviceAccess.checkAccessibilityOfExternalResources());
        });

        post("/call", (req, res) -> {
            JavaCalls fetchedFromJSON = BridgeServiceFactory.createJavaCalls(req.body());

            return BridgeServiceFactory.transformJavaCallResultsToJSON(fetchedFromJSON.submitCalls());
        });

        after((req, res) -> {
            res.type("application/json");
        });

        exception( JsonProcessingException.class, (e, req, res) -> {
            StringBuilder response = new StringBuilder();
            response.append(ERROR_JSON_TRANSFORMATION);
            response.append("\n");
            response.append(e.getMessage());
            res.status(400);
            res.body(response.toString());
        });

        exception( AmbiguousMethodException.class, (e, req, res) -> {
            StringBuilder response = new StringBuilder();
            response.append(ERROR_JSON_TRANSFORMATION);
            response.append("\n");
            response.append(e.getMessage());
            res.status(400);
            res.body(response.toString());
        });

        exception( IBSConfigurationException.class, (e, req, res) -> {
            StringBuilder response = new StringBuilder();
            response.append(ERROR_IBS_CONFIG);
            response.append("\n");
            response.append(e.getMessage());
            res.status(400);
            res.body(response.toString());
        });

        /* Not currently possible */
        exception( IBSRunTimeException.class, (e, req, res) -> {
            StringBuilder response = new StringBuilder();
            response.append(ERROR_IBS_RUNTIME);
            response.append("\n");
            response.append(e.getMessage());
            res.status(400);
            res.body(response.toString());
        });

        exception( TargetJavaMethodCallException.class, (e, req, res) -> {
            StringBuilder response = new StringBuilder();
            response.append(ERROR_CALLING_JAVA_METHOD);
            response.append("\n");

            res.status(400);
            response.append(e.getMessage()).append("\n");
            res.body(response.toString());
        });

        exception( NonExistentJavaObjectException.class, (e, req, res) -> {
            StringBuilder response = new StringBuilder();
            response.append(ERROR_JAVA_OBJECT_NOT_FOUND);
            response.append("\n");
            response.append(e.getMessage());
            res.status(400);
            res.body(response.toString());
        });

        exception( IBSTimeOutException.class, (e, req, res) -> {
            StringBuilder response = new StringBuilder();
            response.append(ERROR_CALL_TIMEOUT);
            response.append("\n");
            response.append(e.getMessage());
            res.status(408);
            res.body(response.toString());
        });

    }
}
