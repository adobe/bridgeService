/*
 * Copyright 2022 Adobe
 * All Rights Reserved.
 *
 * NOTICE: Adobe permits you to use, modify, and distribute this file in
 * accordance with the terms of the Adobe license agreement accompanying
 * it.
 */
package com.adobe.campaign.tests.bridge.service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * This class is meant to contain the error payload in care of exceptions
 */
public class ErrorObject {
    protected static final String STD_NOT_APPLICABLE = "Not Set";

    private String title;
    private int code;
    private String detail;
    private String bridgeServiceException;
    private String originalException;
    private String originalMessage;
    private String failureAtStep;
    private List<String> stackTrace;

    /**
     * The main constructor for ErrorObject
     *
     * @param in_exception The exception we throw
     * @param in_title The title of the error
     * @param in_errorCode The error code we return
     * @param in_includeStackTrace A flag to tell us if we should include the stack trace.
     */
    public ErrorObject(Exception in_exception, String in_title, int in_errorCode, boolean in_includeStackTrace) {
        this.setTitle(in_title);
        this.setCode(in_errorCode);
        this.setDetail(in_exception.getMessage());
        this.setBridgeServiceException(in_exception.getClass().getTypeName());
        Throwable originalExceptionClass = extractOriginalException(in_exception);
        this.setFailureAtStep(LogManagement.fetchCurrentStep());

        this.setStackTrace(new ArrayList<>());

        //Check if we have a different root cause
        this.setOriginalException((originalExceptionClass != null) ? originalExceptionClass.getClass()
                .getTypeName() : STD_NOT_APPLICABLE);
        this.setOriginalMessage(
                (originalExceptionClass != null) ? originalExceptionClass.getMessage() : STD_NOT_APPLICABLE);

        if (in_includeStackTrace) {
            Arrays.stream(Optional.ofNullable(originalExceptionClass).orElse(in_exception).getStackTrace())
                    .forEach(i -> this.stackTrace.add(i.toString()));
        }


    }

    public ErrorObject(Exception in_exception) {
        this(in_exception, STD_NOT_APPLICABLE, -1);
    }

    /**
     * The constructor for ErrorObject
     *
     * @param in_exception The exception we throw
     * @param in_title The title of the error
     * @param in_errorCode The error code we return
     */
    public ErrorObject(Exception in_exception, String in_title, int in_errorCode) {
        this(in_exception, in_title, in_errorCode, true);
    }

    /**
     * Extracts the exception originating the current one.
     * @param in_exception The exception we want to parse
     * @return An exception Object that was the origin of this one
     */
    public static Throwable extractOriginalException(Throwable in_exception) {
        return (in_exception.getCause()==null) ? null : extractOriginalException(in_exception.getCause(), in_exception);

    }

    /**
     * Extracts the last relevant exception. The following rules exist:
     * <ul>
     *     <li>Return previous exception if the current is null or the current is the same as the previous</li>
     *     <li>otherwise continue climbing the tree</li>
     * </ul>
     * @param in_currentException The exception we are looking at
     * @param previousException The exception that was before the current one
     * @return the current exception if it fulfills the rules
     */
    protected static Throwable extractOriginalException(Throwable in_currentException, Throwable previousException) {
        if (in_currentException == null) {
            return previousException;
        }
        if (in_currentException.getClass().getTypeName().equals(previousException.getClass().getTypeName())) {
            return previousException;
        } else {
            return extractOriginalException( in_currentException.getCause(), in_currentException);
        }
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = Optional.ofNullable(title).orElse("").trim();
    }

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public String getDetail() {
        return detail;
    }

    public void setDetail(String detail) {
        this.detail = Optional.ofNullable(detail).orElse("").trim();
    }

    public String getBridgeServiceException() {
        return bridgeServiceException;
    }

    public void setBridgeServiceException(String bridgeServiceException) {
        this.bridgeServiceException = bridgeServiceException;
    }

    public String getOriginalException() {
        return originalException;
    }

    public void setOriginalException(String originalException) {
        this.originalException = originalException;
    }

    public String getOriginalMessage() {
        return originalMessage;
    }

    public void setOriginalMessage(String originalMessage) {
        this.originalMessage = Optional.ofNullable(originalMessage).orElse("").trim();
    }

    public List<String> getStackTrace() {
        return stackTrace;
    }

    public void setStackTrace(List<String> stackTrace) {
        this.stackTrace = stackTrace;
    }

    public String getFailureAtStep() {
        return failureAtStep;
    }

    public void setFailureAtStep(String failureAtStep) {
        this.failureAtStep = failureAtStep;
    }
}
