/*
 * Copyright 2022 Adobe
 * All Rights Reserved.
 *
 * NOTICE: Adobe permits you to use, modify, and distribute this file in
 * accordance with the terms of the Adobe license agreement accompanying
 * it.
 */
package com.adobe.campaign.tests.bridge.testdata.one;

public class StaticType {
    public static String fetchInstantiableStringValue(Instantiable in_instantiableObject) {
        return in_instantiableObject.getValueString();
    }
}
