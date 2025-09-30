/*
 * Copyright 2022 Adobe
 * All Rights Reserved.
 *
 * NOTICE: Adobe permits you to use, modify, and distribute this file in
 * accordance with the terms of the Adobe license agreement accompanying
 * it.
 */
package com.adobe.campaign.tests.bridge.service.exceptions;

public class CallDefinitionNotFoundException extends RuntimeException {
    private static final long serialVersionUID = 3333272575099510405L;

    public CallDefinitionNotFoundException(String s) {
        super(s);
    }
}
