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
    private static final long serialVersionUID = 9029559034607559596L;

    public TargetJavaMethodCallException(String message, Throwable cause) {
        super(message, cause);
    }
}
