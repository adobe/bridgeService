package com.adobe.campaign.tests.service;

import com.adobe.campaign.tests.service.exceptions.AmbiguousMethodException;
import com.adobe.campaign.tests.service.exceptions.NonExistantJavaObjectException;
import com.adobe.campaign.tests.service.exceptions.TargetJavaClassException;
import com.fasterxml.jackson.core.JsonProcessingException;

import static spark.Spark.*;

public class IntegroAPI {

    public static final String ERROR_JSON_TRANSFORMATION = "JSON Transformation issue : Problem processing request. The given json could not be mapped to a Java Call";

    protected static void
    startServices() {
        get("/test", (req, res) -> {
            return "All systems up";
        });

        post("/service-check", (req, res) -> {
            System.out.println("in sc");
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
