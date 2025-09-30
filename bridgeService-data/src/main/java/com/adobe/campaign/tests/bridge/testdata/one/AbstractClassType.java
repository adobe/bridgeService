/*
 * Copyright 2022 Adobe
 * All Rights Reserved.
 *
 * NOTICE: Adobe permits you to use, modify, and distribute this file in
 * accordance with the terms of the Adobe license agreement accompanying
 * it.
 */
package com.adobe.campaign.tests.bridge.testdata.one;

public abstract class AbstractClassType {
    String valueString;

    public AbstractClassType(String in_valueString) {
        valueString = in_valueString;
    }



    public abstract String getValueString();

}
