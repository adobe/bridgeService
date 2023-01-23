package com.adobe.campaign.tests.service;

import com.adobe.campaign.tests.service.exceptions.AmbiguousMethodException;
import com.adobe.campaign.tests.service.exceptions.NonExistantJavaObjectException;
import com.adobe.campaign.tests.service.exceptions.TargetJavaClassException;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import spark.Spark;

import java.io.File;

import static com.adobe.campaign.tests.service.ConfigValueHandler.*;
import static spark.Spark.*;

public class IntegroAPI {
    private static final Logger log = LogManager.getLogger();

    public static final String ERROR_JSON_TRANSFORMATION = "JSON Transformation issue : Problem processing request. The given json could not be mapped to a Java Call";

    protected static void
    startServices() {

        if (Boolean.parseBoolean(SSL_ACTIVE.fetchValue())) {
            File l_file = new File(SSL_KEYSTORE_PATH.fetchValue());
            log.info("Keystore file was found? {}", l_file.exists());
            secure(SSL_KEYSTORE_PATH.fetchValue(), SSL_KEYSTORE_PASSWORD.fetchValue(),
                    SSL_TRUSTSTORE_PATH.fetchValue(), SSL_TRUSTSTORE_PASSWORD.fetchValue());
        }

        get("/test", (req, res) -> {
            return "All systems up "+TEST_CHECK.fetchValue();
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
            res.body(ERROR_JSON_TRANSFORMATION);
            res.status(400);
            res.body(e.getMessage());
        });

        exception( AmbiguousMethodException.class, (e, req, res) -> {
            res.body(ERROR_JSON_TRANSFORMATION);
            res.status(400);
            res.body(e.getMessage());
        });

        exception( TargetJavaClassException.class, (e, req, res) -> {
            res.body(ERROR_JSON_TRANSFORMATION);
            res.status(400);
            res.body(e.getMessage());
        });

        exception( NonExistantJavaObjectException.class, (e, req, res) -> {
            res.body(ERROR_JSON_TRANSFORMATION);
            res.status(400);
            res.body(e.getMessage());
        });


    }
}
