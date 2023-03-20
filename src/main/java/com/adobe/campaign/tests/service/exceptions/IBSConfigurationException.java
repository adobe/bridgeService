package com.adobe.campaign.tests.service.exceptions;

public class IBSConfigurationException extends RuntimeException {

    public IBSConfigurationException(String message, NonExistantJavaObjectException nono) {
        super(message,nono);
    }
}
