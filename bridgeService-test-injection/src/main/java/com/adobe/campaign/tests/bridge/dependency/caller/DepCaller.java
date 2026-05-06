/*
 * Copyright 2022 Adobe
 * All Rights Reserved.
 *
 * NOTICE: Adobe permits you to use, modify, and distribute this file in
 * accordance with the terms of the Adobe license agreement accompanying
 * it.
 */
package com.adobe.campaign.tests.bridge.dependency.caller;

import com.adobe.campaign.tests.bridge.dependency.factory.DepFactory;
import com.adobe.campaign.tests.bridge.dependency.model.DepResult;

/**
 * Simulates project code that calls a dependency library in a static initializer.
 * When this class's package and DepResult's package are in STATIC_INTEGRITY_PACKAGES
 * but DepFactory's package is not, both the IBS classloader and the parent classloader
 * end up loading DepResult, which causes a LinkageError.
 */
public class DepCaller {

    private final static DepResult instantiatedStaticConstant = DepFactory.makeDepResult("initial");

    /**
     * Returns a fixed string, serving as the method to invoke via the /call endpoint in tests.
     *
     * @return a confirmation string
     */
    public static String doSomething() {
        return instantiatedStaticConstant.getValue();
    }
}
