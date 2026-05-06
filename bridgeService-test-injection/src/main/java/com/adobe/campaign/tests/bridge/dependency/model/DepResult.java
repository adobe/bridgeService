/*
 * Copyright 2022 Adobe
 * All Rights Reserved.
 *
 * NOTICE: Adobe permits you to use, modify, and distribute this file in
 * accordance with the terms of the Adobe license agreement accompanying
 * it.
 */
package com.adobe.campaign.tests.bridge.dependency.model;

/**
 * A result type produced by the simulated dependency library.
 * Used by the injection model tests to trigger and verify classloader isolation behaviour.
 */
public class DepResult {

    private String value;

    public DepResult(String in_value) {
        this.value = in_value;
    }

    public String getValue() {
        return value;
    }
}
