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
import com.adobe.campaign.tests.bridge.service.utils.ServiceTools;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import static spark.Spark.*;

public class IntegroAPI {
    private static final Logger log = LogManager.getLogger();

    protected static final String ERROR_JSON_TRANSFORMATION = "JSON Transformation issue : Problem processing request. The given json could not be mapped to a Java Call";
    protected static final String ERROR_CALLING_JAVA_METHOD = "Error during call of target Java Class and Method.";
    protected static final String ERROR_JAVA_OBJECT_NOT_FOUND = "Could not find the given class or method.";
    protected static final String ERROR_IBS_CONFIG = "The provided class and method for setting environment variables is not valid.";
    protected static final String ERROR_IBS_RUNTIME = "Problems with payload.";
    public static final String ERROR_CALL_TIMEOUT = "The call you made exceeds the set timeout limit.";
    public static final String ERROR_CONTENT_TYPE = "application/problem+json";
    protected static final String ERROR_AMBIGUOUS_METHOD = "No unique method could be identified that matches your request.";
    public static final String SYSTEM_UP_MESSAGE = "All systems up";
    public static final String ERROR_IBS_INTERNAL = "Internal IBS error. Please debug using the parameter debug=true. Please report this to the maintainer.";
    protected enum DeploymentMode {
            TEST, PRODUCTION
    }

    public static void startServices(int port) {

        if (!ServiceTools.isPortFree(port)) {
            throw new IBSConfigurationException("The port "+port+" is not currently free.");
        }

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
            res.type("application/json");
            Map<String, String> status = new HashMap<>();
            status.put("overALLSystemState",SYSTEM_UP_MESSAGE);
            status.put("deploymentMode", ConfigValueHandlerIBS.DEPLOYMENT_MODEL.fetchValue());

            status.put("bridgeServiceVersion", ConfigValueHandlerIBS.PRODUCT_VERSION.fetchValue());
            if (ConfigValueHandlerIBS.PRODUCT_USER_VERSION.isSet()) {
                status.put("hostVersion", ConfigValueHandlerIBS.PRODUCT_USER_VERSION.fetchValue());
            }

            return BridgeServiceFactory.transformMapTosResult(status);
        });

        post("/service-check", (req, res) -> {
            ServiceAccess l_serviceAccess = BridgeServiceFactory.createServiceAccess(req.body());
            return BridgeServiceFactory.transformServiceAccessResult(l_serviceAccess.checkAccessibilityOfExternalResources());
        });

        post("/call", (req, res) -> {
            JavaCalls fetchedFromJSON = BridgeServiceFactory.createJavaCalls(req.body());

            return BridgeServiceFactory.transformJavaCallResultsToJSON(fetchedFromJSON.submitCalls());
        });

        after((req, res) -> res.type("application/json"));

        exception(JsonProcessingException.class, (e, req, res) -> {
            int statusCode = 404;
            res.status(statusCode);
            res.type(ERROR_CONTENT_TYPE);
            res.body(BridgeServiceFactory.createExceptionPayLoad(
                    new ErrorObject(e, ERROR_JSON_TRANSFORMATION, statusCode)));
        });

        exception(AmbiguousMethodException.class, (e, req, res) -> {
            int statusCode = 404;
            res.status(statusCode);
            res.type(ERROR_CONTENT_TYPE);
            res.body(BridgeServiceFactory.createExceptionPayLoad(
                    new ErrorObject(e, ERROR_AMBIGUOUS_METHOD, statusCode)));
        });

        exception(IBSConfigurationException.class, (e, req, res) -> {
            int statusCode = 500;
            res.status(statusCode);
            res.type(ERROR_CONTENT_TYPE);

            res.body(BridgeServiceFactory.createExceptionPayLoad(new ErrorObject(e, ERROR_IBS_CONFIG, statusCode)));
        });

        exception(IBSRunTimeException.class, (e, req, res) -> {
            int statusCode = 500;
            res.status(statusCode);
            res.type(ERROR_CONTENT_TYPE);

            res.body(BridgeServiceFactory.createExceptionPayLoad(new ErrorObject(e, ERROR_IBS_RUNTIME, statusCode)));

        });

        exception(TargetJavaMethodCallException.class, (e, req, res) -> {
            int statusCode = 500;
            res.status(statusCode);
            res.type(ERROR_CONTENT_TYPE);

            res.body(BridgeServiceFactory.createExceptionPayLoad(
                    new ErrorObject(e, ERROR_CALLING_JAVA_METHOD, statusCode)));
        });

        exception(NonExistentJavaObjectException.class, (e, req, res) -> {
            int statusCode = 404;
            res.status(statusCode);
            res.type(ERROR_CONTENT_TYPE);

            res.body(BridgeServiceFactory.createExceptionPayLoad(
                    new ErrorObject(e, ERROR_JAVA_OBJECT_NOT_FOUND, statusCode)));
        });

        exception(IBSTimeOutException.class, (e, req, res) -> {
            int statusCode = 408;
            res.status(statusCode);
            res.type(ERROR_CONTENT_TYPE);

            res.body(BridgeServiceFactory.createExceptionPayLoad(new ErrorObject(e, ERROR_CALL_TIMEOUT, statusCode)));
        });
        //Internal exception
        exception(Exception.class, (e, req, res) -> {
            int statusCode = 500;
            res.status(statusCode);
            res.type(ERROR_IBS_INTERNAL);

            res.body(BridgeServiceFactory.createExceptionPayLoad(new ErrorObject(e, ERROR_IBS_INTERNAL, statusCode)));
        });

    }
}
