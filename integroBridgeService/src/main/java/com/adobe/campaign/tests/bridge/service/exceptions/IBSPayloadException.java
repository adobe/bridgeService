/*
 * Copyright 2022 Adobe
 * All Rights Reserved.
 *
 * NOTICE: Adobe permits you to use, modify, and distribute this file in
 * accordance with the terms of the Adobe license agreement accompanying
 * it.
 */
package com.adobe.campaign.tests.bridge.service.exceptions;

/**
 * This exception is thrown whenever there is an inconsistency in the payload
 */
public class IBSPayloadException extends RuntimeException {
    private static final long serialVersionUID = -6489221630558571708L;

    public IBSPayloadException(String message) {
        super(message);
    }
}
