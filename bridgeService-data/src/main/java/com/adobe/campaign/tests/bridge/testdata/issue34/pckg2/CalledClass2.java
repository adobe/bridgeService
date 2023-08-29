/*
 * Copyright 2022 Adobe
 * All Rights Reserved.
 *
 * NOTICE: Adobe permits you to use, modify, and distribute this file in
 * accordance with the terms of the Adobe license agreement accompanying
 * it.
 */
package com.adobe.campaign.tests.bridge.testdata.issue34.pckg2;

import com.adobe.campaign.tests.bridge.testdata.issue34.pckg3.MiddleManClassFactory;
import com.adobe.campaign.tests.bridge.testdata.issue34.pckg1.MiddleMan;

public class CalledClass2 {

    private final static MiddleMan instantiatedStaticConstant = MiddleManClassFactory.getMarketingInstance();

    /**
     * This method is called directly
     * @return a string
     */
    public static String calledMethod() {

        return "Whatever";
    }


}
