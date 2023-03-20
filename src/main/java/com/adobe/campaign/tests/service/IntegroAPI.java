package com.adobe.campaign.tests.service;

import com.adobe.campaign.tests.service.exceptions.*;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;

import com.adobe.campaign.tests.service.ConfigValueHandler;
import static spark.Spark.*;

public class IntegroAPI {
    private static final Logger log = LogManager.getLogger();

    public static final String ERROR_JSON_TRANSFORMATION = "JSON Transformation issue : Problem processing request. The given json could not be mapped to a Java Call";
    private static final String ERROR_CALLING_JAVA_METHOD = "Error during call of target Java Class and Method.";
    private static final String ERROR_JAVA_OBJECT_NOT_FOUND = "Could not fid the given class or method.";
    private static final String ERROR_IBS_CONFIG = "The provided class and method for setting environment variables is not valid.";
    private static final String ERROR_IBS_RUNTIME = "Problems with payload. Check the passed environment variables";

    public static void
    startServices(int port) {

        if (Boolean.parseBoolean(ConfigValueHandler.SSL_ACTIVE.fetchValue())) {
            File l_file = new File(ConfigValueHandler.SSL_KEYSTORE_PATH.fetchValue());
            log.info("Keystore file was found? {}", l_file.exists());
            secure(ConfigValueHandler.SSL_KEYSTORE_PATH.fetchValue(), ConfigValueHandler.SSL_KEYSTORE_PASSWORD.fetchValue(),
                    ConfigValueHandler.SSL_TRUSTSTORE_PATH.fetchValue(), ConfigValueHandler.SSL_TRUSTSTORE_PASSWORD.fetchValue());
        }
        else {
            port(port);
        }

        get("/test", (req, res) -> {
            res.type("text/plain");

            StringBuilder sb = new StringBuilder("All systems up "+ ConfigValueHandler.DEPLOYMENT_MODEL.fetchValue());
            sb.append("\n");
            sb.append("Bridge Service Version : ");
            sb.append(ConfigValueHandler.PRODUCT_VERSION.fetchValue());
            if (ConfigValueHandler.PRODUCT_USER_VERSION.isSet()) {
                sb.append("\n");
                sb.append("Product user version : ");
                sb.append(ConfigValueHandler.PRODUCT_USER_VERSION.fetchValue());
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
