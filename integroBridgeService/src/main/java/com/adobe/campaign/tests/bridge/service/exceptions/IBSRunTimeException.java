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
 * This exception is intended to be used in the case of exceptions that are related to the IBS, but not related to JSON transformations
 * <p>
 * author: gandomi
 */
public class IBSRunTimeException extends RuntimeException {
    public IBSRunTimeException(String message) {
        super(message);
    }
}
