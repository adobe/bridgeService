/*
 * Copyright 2022 Adobe
 * All Rights Reserved.
 *
 * NOTICE: Adobe permits you to use, modify, and distribute this file in
 * accordance with the terms of the Adobe license agreement accompanying
 * it.
 */
package com.adobe.campaign.tests.bridge.service;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.ThreadContext;

import java.util.Optional;

public class LogManagement {
    public static final String STD_CURRENT_STEP = "currentStep";
    private static final Logger log = LogManager.getLogger();

    /**
     * Logs the step in the context. it will later be used by the Error Object to generate step info in the error
     * reponse. This is for standard steps.
     *
     * @param in_state A constant static field
     */
    protected static void logStep(STD_STEPS in_state) {
        logStep(in_state.value);
    }

    /**
     * Logs the step in the context. it will later be used by the Error Object to generate step info in the error
     * reponse
     *
     * @param in_step A constant static field
     */
    protected static void logStep(String in_step) {
        log.info("About to perform step : {}", in_step);
        ThreadContext.put(STD_CURRENT_STEP, in_step);
    }

    /**
     * Returns the current step in which we are in. When not being executed in the context of JavaCalls, we return 'Not
     * in a Step'
     *
     * @return The name of the step being executed. 'Not in a Step' if we are not in the context of a step
     */
    public static String fetchCurrentStep() {
        return Optional.ofNullable(ThreadContext.get(STD_CURRENT_STEP)).orElse(STD_STEPS.NOT_IN_A_STEP.value);
    }

    protected enum STD_STEPS {
        ENVVARS("Setting Environment Variables"),
        SEND_RESULT("Returning result"),
        NOT_IN_A_STEP("Not in a Step"),
        ANALYZING_PAYLOAD("Analyzing Payload"),
        GENERATING_RESPONSE("Generating Response"),
        STORE_HEADERS("Storing Headers"),
        CLEANUP("Cleaning up temporary files");

        final String value;

        STD_STEPS(String in_value) {
            this.value = in_value;
        }
    }

    ;
}
