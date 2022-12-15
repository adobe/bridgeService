package com.adobe.campaign.tests.service;

import com.adobe.campaign.tests.service.JavaCalls;
import com.fasterxml.jackson.databind.ObjectMapper;

import static spark.Spark.*;

public class IntegroAPI {

    public static void main(String[] args) {
       // IntegroAPI iapi = new IntegroAPI();
       // iapi.startServices();
        startServices();
    }

    protected static void startServices() {
        get("/hello", (req, res) -> "Hello World");
        post("/call", (req, res) -> {
            ObjectMapper mapper = new ObjectMapper();
            JavaCalls fetchedFromJSON = mapper.readValue(req.body(), JavaCalls.class);
            //return fetchedFromJSON.submitCalls().toString();
            //return req.body();
            return mapper.writeValueAsString(fetchedFromJSON.submitCalls());
        });

        /*
        after((req, res) -> {
            res.type("application/json");
        });

        exception(IllegalArgumentException.class, (e, req, res) -> {
            res.status(400);
        });

         */
    }
}
