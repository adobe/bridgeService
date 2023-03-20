package com.adobe.campaign.tests.service.exceptions;

/**
 * This exception is intended to be used in the case of exceptions that are related to the IBS, but not related to JSON transformations
 *
 * author: gandomi
 */
public class IBSRunTimeException extends RuntimeException {
    public IBSRunTimeException(String message) {
        super(message);
    }
}
