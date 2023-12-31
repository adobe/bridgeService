/*
 * Copyright 2022 Adobe
 * All Rights Reserved.
 *
 * NOTICE: Adobe permits you to use, modify, and distribute this file in
 * accordance with the terms of the Adobe license agreement accompanying
 * it.
 */
package com.adobe.campaign.tests.bridge.testdata.issue34.pckg1;

public class CalledClass1 {

    /**
     * This method is called directly
     */
    public static void calledMethod() {
    }

    /**
     * This method is never called directly or indirectly through another method
     * @param param
     * @return
     */
    public static MiddleMan irrelevantMethod(MiddleMan param) {
        MiddleMan responseData = null;

        return responseData;
    }

}
