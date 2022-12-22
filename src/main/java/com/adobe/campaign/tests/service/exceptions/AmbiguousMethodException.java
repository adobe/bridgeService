package com.adobe.campaign.tests.service.exceptions;

public class AmbiguousMethodException extends RuntimeException {

    public AmbiguousMethodException(String message) {
        super(message);
    }

    public AmbiguousMethodException(String message, Throwable cause) {
        super(message, cause);
    }
}
