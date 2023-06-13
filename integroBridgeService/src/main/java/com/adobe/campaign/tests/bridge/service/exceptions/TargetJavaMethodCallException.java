/*
 * Copyright 2022 Adobe
 * All Rights Reserved.
 *
 * NOTICE: Adobe permits you to use, modify, and distribute this file in
 * accordance with the terms of the Adobe license agreement accompanying
 * it.
 */
package com.adobe.campaign.tests.bridge.service.exceptions;

public class TargetJavaMethodCallException extends RuntimeException {
    public TargetJavaMethodCallException(String message, Throwable cause) {
        super(message, cause);
    }
}
