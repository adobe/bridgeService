/*
 * Copyright 2022 Adobe
 * All Rights Reserved.
 *
 * NOTICE: Adobe permits you to use, modify, and distribute this file in
 * accordance with the terms of the Adobe license agreement accompanying
 * it.
 */
package com.adobe.campaign.tests.bridge.service.exceptions;

public class JavaObjectInaccessibleException extends RuntimeException {
    private static final long serialVersionUID = -6618553477788381745L;

    public JavaObjectInaccessibleException(String message, Throwable cause) {
        super(message, cause);
    }
}
