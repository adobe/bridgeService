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
 * This exception is thrown whenever a call takes a longer time than is expected
 */
public class IBSTimeOutException extends RuntimeException {

    public IBSTimeOutException(String message) {
        super(message);
    }
}
