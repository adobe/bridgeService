package com.adobe.campaign.tests.service;

import com.adobe.campaign.tests.service.JavaCalls;
import com.adobe.campaign.tests.service.exceptions.AmbiguousMethodException;
import com.adobe.campaign.tests.service.exceptions.NonExistantJavaObjectException;
import com.adobe.campaign.tests.service.exceptions.TargetJavaClassException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import static spark.Spark.*;

public class IntegroAPI {

    public static final String ERROR_JSON_TRANSFORMATION = "JSON Transformation issue : Problem processing request. The given json could not be mapped to a Java Call";

    protected static void startServices() {
        get("/hello", (req, res) -> {
            return "Hello World";
        });

        post("/call", (req, res) -> {
            JavaCalls fetchedFromJSON = JavaCallsFactory.createJavaCalls(req.body());
            //return fetchedFromJSON.submitCalls().toString();
            //return req.body();

            return JavaCallsFactory.transformJavaCallResultsToJSON(fetchedFromJSON.submitCalls());
            //return fetchedFromJSON.submitCalls();
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
