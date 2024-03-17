package com.adobe.campaign.tests.bridge.service.exceptions;

public class JavaObjectInaccessibleException extends RuntimeException {
    private static final long serialVersionUID = -6618553477788381745L;

    public JavaObjectInaccessibleException(String message, Throwable cause) {
        super(message, cause);
    }
}
