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
 * This exception is intended to be used for issues related to configuration of the IBS.
 * <p>
 * author: gandomi
 */
public class IBSConfigurationException extends RuntimeException {

    private static final long serialVersionUID = 4664435638875457216L;

    public IBSConfigurationException(String message, Throwable original) {
        super(message,original);
    }
}
