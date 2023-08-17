/*
 * Copyright 2022 Adobe
 * All Rights Reserved.
 *
 * NOTICE: Adobe permits you to use, modify, and distribute this file in
 * accordance with the terms of the Adobe license agreement accompanying
 * it.
 */
package com.adobe.campaign.tests.bridge.service.exceptions;

public class NonExistentJavaObjectException extends RuntimeException {
    private static final long serialVersionUID = -6973899601813799743L;

    public NonExistentJavaObjectException(String message, Throwable cause) {
        super(message, cause);
    }

    public NonExistentJavaObjectException(String message) {
        super(message);
    }
}
