/*
 * Copyright 2022 Adobe
 * All Rights Reserved.
 *
 * NOTICE: Adobe permits you to use, modify, and distribute this file in
 * accordance with the terms of the Adobe license agreement accompanying
 * it.
 */
package com.adobe.campaign.tests.bridge.testdata.one;

public class ClassWithMethodThrowingException {
    public String getFirst() {
        throw new RuntimeException("This is a test exception");
    }

    public String getSecond() {
        return "secondValue";
    }

    public String getThirdValue(String added) {
        return "third value is : " + added;
    }
}
