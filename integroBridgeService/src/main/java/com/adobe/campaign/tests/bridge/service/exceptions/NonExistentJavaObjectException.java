package com.adobe.campaign.tests.bridge.service.exceptions;

public class NonExistentJavaObjectException extends RuntimeException {
    public NonExistentJavaObjectException(String message, Throwable cause) {
        super(message, cause);
    }

    public NonExistentJavaObjectException(String message) {
        super(message);
    }
}
