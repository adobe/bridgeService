/*
 * Copyright 2022 Adobe
 * All Rights Reserved.
 *
 * NOTICE: Adobe permits you to use, modify, and distribute this file in
 * accordance with the terms of the Adobe license agreement accompanying
 * it.
 */
package com.adobe.campaign.tests.bridge.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.hamcrest.Matchers;
import org.mockito.Mockito;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import static org.hamcrest.MatcherAssert.assertThat;

public class ErrorObjectTest {
    @BeforeMethod
    @AfterClass
    public void reset() {
        ConfigValueHandlerIBS.resetAllValues();
    }

    @Test
    public void testErrorObjectCreation() {
        String detail = "ABC";
        ClassNotFoundException cnfe = new ClassNotFoundException(detail);
        String message = "Highlevel message";
        int errorCode = 404;

        ErrorObject eo = new ErrorObject(cnfe, message, errorCode);

        assertThat("We should have the correct title", eo.getTitle(), Matchers.equalTo(message));
        assertThat("We should have the correct error code", eo.getCode(), Matchers.equalTo(errorCode));
        assertThat("We should have the correct error code", eo.getDetail(), Matchers.equalTo(detail));
        assertThat("We should have the correct top exception", eo.getBridgeServiceException(),
                Matchers.equalTo(ClassNotFoundException.class.getTypeName()));
        assertThat("We should have the correct top exception", eo.getOriginalException(), Matchers.equalTo("Not Set"));
    }

    @Test
    public void getOriginalException() {
        //case 1 simple object
        String detail = "ABC";
        ClassNotFoundException cnfe = new ClassNotFoundException(detail);

        assertThat("In a simple case where cause is null we should have null of N/A", cnfe.getCause(),
                Matchers.nullValue());
        assertThat("In a simple case where cause is null we should have null of N/A",
                ErrorObject.extractOriginalException(cnfe), Matchers.nullValue());

    }

    @Test
    public void simpleChainedException() {
        //Simple with Cause
        String originalDetail = "illegal detail2";
        IllegalArgumentException iae2 = new IllegalArgumentException(originalDetail);

        String detail2 = "DEF";
        ClassNotFoundException cnfe2 = new ClassNotFoundException(detail2, iae2);

        assertThat("In a simple case where cause is null we should have null of N/A", cnfe2.getCause(),
                Matchers.equalTo(iae2));
        assertThat("In a simple case where cause is null we should have null of N/A",
                ErrorObject.extractOriginalException(cnfe2),
                Matchers.equalTo(iae2));
    }

    @Test
    public void chainedException() {
        //Simple with Cause
        String originalDetail = "exc 1";
        IllegalArgumentException exc1 = new IllegalArgumentException(originalDetail);

        String middleException = "exc 2";
        ClassNotFoundException exc2 = new ClassNotFoundException(middleException, exc1);

        String bottomException = "exc 3";
        RuntimeException exc3 = new RuntimeException(bottomException, exc2);

        assertThat("In a simple case where cause is null we should have null of N/A",
                ErrorObject.extractOriginalException(exc3),
                Matchers.equalTo(exc1));
    }

    @Test
    public void chainedExceptionRepetition() {
        //Simple with Cause
        String originalDetail = "exc 1";
        ClassNotFoundException exc1 = new ClassNotFoundException(originalDetail);

        String middleException = "exc 2";
        ClassNotFoundException exc2 = new ClassNotFoundException(middleException, exc1);

        String bottomException = "exc 3";
        RuntimeException exc3 = new RuntimeException(bottomException, exc2);

        assertThat("In a simple case where cause is null we should have null of N/A",
                ErrorObject.extractOriginalException(exc3),
                Matchers.equalTo(exc2));

        ErrorObject l_errorObject = new ErrorObject(exc3,"some title", 343);
        assertThat("We should have extracted the correct ibs message", l_errorObject.getDetail(), Matchers.equalTo(bottomException));

        assertThat("We should have extracted the correct original message", l_errorObject.getOriginalMessage(), Matchers.equalTo(middleException));
    }

    @Test
    public void testFactory() throws JsonProcessingException {
        ObjectMapper omMock = Mockito.mock(ObjectMapper.class);
        Mockito.when(omMock.writeValueAsString(Mockito.any())).thenThrow(JsonProcessingException.class);
        assertThat("When a JSON processing exception is thrown we return a String",
                BridgeServiceFactory.getErrorPayloadAsString(omMock,
                        new ErrorObject(new ClassNotFoundException(), "A", 404)),
                Matchers.equalTo("Problem creating error payload. Original error is " + "A"));
    }
}
