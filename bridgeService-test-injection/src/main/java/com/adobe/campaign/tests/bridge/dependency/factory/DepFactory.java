/*
 * Copyright 2022 Adobe
 * All Rights Reserved.
 *
 * NOTICE: Adobe permits you to use, modify, and distribute this file in
 * accordance with the terms of the Adobe license agreement accompanying
 * it.
 */
package com.adobe.campaign.tests.bridge.dependency.factory;

import com.adobe.campaign.tests.bridge.dependency.model.DepResult;
import com.adobe.campaign.tests.bridge.testdata.issue34.pckg1.MiddleMan;

/**
 * Simulated dependency library factory.
 * Lives in a separate package from DepResult to enable the split-package classloader conflict scenario:
 * when DepResult's package is in STATIC_INTEGRITY_PACKAGES but this factory's package is not,
 * parent classloader loads DepResult a second time (as this method's return type), causing a LinkageError.
 */
public class DepFactory {

    /**
     * Creates a DepResult instance.
     *
     * @param in_value the string value to embed in the result
     * @return a new DepResult
     */
    public static DepResult makeDepResult(String in_value) {
        return new DepResult(in_value);
    }

    /**
     * Creates a MiddleMan instance from the project (bridgeService-data) test data.
     * Used to demonstrate that a dependency library can return project types when
     * both packages are included in STATIC_INTEGRITY_PACKAGES.
     *
     * @return a new MiddleMan instance
     */
    public static MiddleMan makeMiddleMan() {
        return new MiddleMan();
    }
}
