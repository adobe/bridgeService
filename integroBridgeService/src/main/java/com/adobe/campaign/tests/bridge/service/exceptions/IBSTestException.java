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
 * This exception there only to reproduce internal errors in the bridgeService. This exception should NOT happen in
 * production.
 */
public class IBSTestException extends RuntimeException {
    public IBSTestException(String just_for_testing) {
        super(just_for_testing);
    }
}
