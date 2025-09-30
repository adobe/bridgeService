/*
 * Copyright 2022 Adobe
 * All Rights Reserved.
 *
 * NOTICE: Adobe permits you to use, modify, and distribute this file in
 * accordance with the terms of the Adobe license agreement accompanying
 * it.
 */
package com.adobe.campaign.tests.bridge.service;

import org.hamcrest.Matchers;

/**
 * This class allows users to define an assertion. An assertion could either be based on results or the execution
 * duration.
 */
public class Assertion {

    private Object actualValue;
    private String matcher;
    private Object expectedValue;
    private TYPES type;

    public Assertion() {
        this.type = TYPES.RESULT;
    }

    public boolean perform(IntegroBridgeClassLoader iClassLoader, JavaCallResults in_callResults) {

        //Fetch Matcher
        CallContent l_cc = new CallContent();
        l_cc.setClassName(Matchers.class.getTypeName());
        l_cc.setMethodName(matcher);
        l_cc.setArgs(new Object[] {
                fetchCalculatedValue(this.expectedValue, in_callResults) });

        iClassLoader.getCallResultCache().put("expected", l_cc.call(iClassLoader));

        CallContent l_cc2 = new CallContent();
        l_cc2.setClassName("expected");
        l_cc2.setMethodName("matches");
        l_cc2.setArgs(new Object[] { fetchCalculatedValue(this.actualValue, in_callResults) });

        return (boolean) l_cc2.call(iClassLoader);
    }

    /**
     * If the assertion is of a DURATION type we etch the duration of a previous execution. In this case, we check if
     * the value the identity of a previous execution or if it is a simple duration object. If it is the identity of a
     * previous execution, we return its duration. If the assertion is of a RESULT type, we returnthe actual value as it
     * is.
     *
     * @param in_value       A key or a value
     * @param in_callResults The history of previous results
     * @return The expansion of the key or its plain value
     */
    private Object fetchCalculatedValue(Object in_value, JavaCallResults in_callResults) {
        return this.type.equals(TYPES.RESULT) ? in_value : in_callResults.expandDurations(
                in_value);
    }

    public Object getActualValue() {
        return actualValue;
    }

    public void setActualValue(Object actualValue) {
        this.actualValue = actualValue;
    }

    public String getMatcher() {
        return matcher;
    }

    public void setMatcher(String matcher) {
        this.matcher = matcher;
    }

    public Object getExpectedValue() {
        return expectedValue;
    }

    public void setExpectedValue(Object expectedValue) {
        this.expectedValue = expectedValue;
    }

    public TYPES getType() {
        return type;
    }

    public void setType(TYPES type) {
        this.type = type;
    }

    protected enum TYPES {RESULT, DURATION}

}
