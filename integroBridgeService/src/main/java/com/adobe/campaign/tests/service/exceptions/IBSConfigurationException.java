package com.adobe.campaign.tests.service.exceptions;

/**
 * This exception is intended to be used for issues related to configuration of the IBS.
 *
 * author: gandomi
 */
public class IBSConfigurationException extends RuntimeException {

    public IBSConfigurationException(String message, Exception nono) {
        super(message,nono);
    }
}
