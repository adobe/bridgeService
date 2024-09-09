/*
 * Copyright 2022 Adobe
 * All Rights Reserved.
 *
 * NOTICE: Adobe permits you to use, modify, and distribute this file in
 * accordance with the terms of the Adobe license agreement accompanying
 * it.
 */
package com.adobe.campaign.tests.bridge.testdata.one;

public class ForbidReflection extends SecurityManager{
    @Override
    public void checkPackageAccess(String pkg) {
        super.checkPackageAccess(pkg);
        // don't allow the use of the reflection package
        if(pkg.equals("java.lang.reflect")){
            throw new SecurityException("Reflection is not allowed!");
        }
    }

    public static String youShallNotPass() {
        return "This should not be possible!";
    }
}
