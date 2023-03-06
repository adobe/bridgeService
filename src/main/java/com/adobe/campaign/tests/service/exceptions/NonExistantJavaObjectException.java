package com.adobe.campaign.tests.service.exceptions;

public class NonExistantJavaObjectException extends RuntimeException {
    public NonExistantJavaObjectException(String message, Throwable cause) {
        super(message, cause);
    }

    public NonExistantJavaObjectException(String message) {
        super(message);
    }
}
