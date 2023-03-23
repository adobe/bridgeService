package com.adobe.campaign.tests.service.exceptions;

public class NonExistentJavaObjectException extends RuntimeException {
    public NonExistentJavaObjectException(String message, Throwable cause) {
        super(message, cause);
    }

    public NonExistentJavaObjectException(String message) {
        super(message);
    }
}
