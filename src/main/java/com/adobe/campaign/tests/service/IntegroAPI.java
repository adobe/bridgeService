package com.adobe.campaign.tests.service;

import com.adobe.campaign.tests.service.exceptions.AmbiguousMethodException;
import com.adobe.campaign.tests.service.exceptions.NonExistantJavaObjectException;
import com.adobe.campaign.tests.service.exceptions.TargetJavaMethodCallException;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;

import static com.adobe.campaign.tests.service.ConfigValueHandler.*;
import static spark.Spark.*;

public class IntegroAPI {
    private static final Logger log = LogManager.getLogger();

    public static final String ERROR_JSON_TRANSFORMATION = "JSON Transformation issue : Problem processing request. The given json could not be mapped to a Java Call";
    private static final String ERROR_CALLING_JAVA_METHOD = "Error during call of target Java Class and Method.";
    private static final String ERROR_JAVA_OBJECT_NOT_FOUND = "Could not fid the given class or method.";

    public static void
    startServices(int port) {

        if (Boolean.parseBoolean(SSL_ACTIVE.fetchValue())) {
            File l_file = new File(SSL_KEYSTORE_PATH.fetchValue());
            log.info("Keystore file was found? {}", l_file.exists());
            secure(SSL_KEYSTORE_PATH.fetchValue(), SSL_KEYSTORE_PASSWORD.fetchValue(),
                    SSL_TRUSTSTORE_PATH.fetchValue(), SSL_TRUSTSTORE_PASSWORD.fetchValue());
        }
        else {
            port(port);
        }

        get("/test", (req, res) -> {
            res.type("text/plain");
            return "All systems up "+TEST_CHECK.fetchValue()+"\nversion : 0.0.7";
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

        exception( TargetJavaMethodCallException.class, (e, req, res) -> {
            StringBuilder response = new StringBuilder();
            response.append(ERROR_CALLING_JAVA_METHOD);
            response.append("\n");

            res.status(400);
            response.append(e.getMessage()).append("\n");
            res.body(response.toString());
        });

        exception( NonExistantJavaObjectException.class, (e, req, res) -> {
            StringBuilder response = new StringBuilder();
            response.append(ERROR_JAVA_OBJECT_NOT_FOUND);
            response.append("\n");
            response.append(e.getMessage());
            res.status(400);
            res.body(response.toString());
        });

    }
}
