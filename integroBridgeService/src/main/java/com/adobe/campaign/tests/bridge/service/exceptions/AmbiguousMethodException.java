/*
 * Copyright 2022 Adobe
 * All Rights Reserved.
 *
 * NOTICE: Adobe permits you to use, modify, and distribute this file in
 * accordance with the terms of the Adobe license agreement accompanying
 * it.
 */
package com.adobe.campaign.tests.bridge.service.exceptions;

public class AmbiguousMethodException extends RuntimeException {

    private static final long serialVersionUID = 6790779647449526934L;

    public AmbiguousMethodException(String message) {
        super(message);
    }
}
