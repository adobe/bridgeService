/*
 * Copyright 2022 Adobe
 * All Rights Reserved.
 *
 * NOTICE: Adobe permits you to use, modify, and distribute this file in
 * accordance with the terms of the Adobe license agreement accompanying
 * it.
 */
package com.adobe.campaign.tests.bridge.service;

import org.hamcrest.Matcher;
import org.hamcrest.Matchers;

public class Assertion {

    public Object actualValue;
    public String matcher;
    public Object expectedValue;
    protected TYPES type;

    ;
    public Assertion() {
        this.type = TYPES.RESULT;
    }

    public boolean perform(IntegroBridgeClassLoader iClassLoader, JavaCallResults in_callResults) {

        //Fetch Matcher
        CallContent l_cc = new CallContent();
        l_cc.setClassName(Matchers.class.getTypeName());
        l_cc.setMethodName(matcher);
        l_cc.setArgs(new Object[] {
                this.type.equals(TYPES.RESULT) ? this.expectedValue : in_callResults.expandDurations(
                        this.expectedValue) });

        var l_matcher = (Matcher) l_cc.call(iClassLoader);

        if (this.type.equals(TYPES.RESULT)) {
            return l_matcher.matches(iClassLoader.getCallResultCache().getOrDefault(
                    actualValue, actualValue));
        } else {
            return l_matcher.matches(in_callResults.expandDurations(this.actualValue));
        }
    }

    protected enum TYPES {RESULT, DURATION}

}
