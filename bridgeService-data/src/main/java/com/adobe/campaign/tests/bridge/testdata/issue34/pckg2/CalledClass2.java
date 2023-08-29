/**************************************************************************
 * ADOBE CONFIDENTIAL
 *
 * Copyright 2019 Adobe Systems Incorporated All Rights Reserved.
 * NOTICE: All information contained herein is, and remains the property of Adobe Systems
 * Incorporated and its suppliers, if any. The intellectual and technical concepts contained herein
 * are proprietary to Adobe Systems Incorporated and its suppliers and are protected by all
 * applicable intellectual property laws, including trade secret and copyright laws. Dissemination
 * of this information or reproduction of this material is strictly forbidden unless prior written
 * permission is obtained from Adobe Systems Incorporated.
 *
 * @author Mickaël Gobbo, Gaël Le Polles, David Mendez-Acuna
 **************************************************************************/

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
