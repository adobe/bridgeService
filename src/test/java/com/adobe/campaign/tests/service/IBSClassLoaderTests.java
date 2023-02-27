package com.adobe.campaign.tests.service;

import org.hamcrest.Matchers;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import static org.hamcrest.MatcherAssert.assertThat;

public class IBSClassLoaderTests {
    @BeforeMethod
    @AfterClass
    public void reset() {
        ConfigValueHandler.resetAllValues();
    }


    @Test
    public void testExtractPackages() {
        ConfigValueHandler.STATIC_INTEGRITY_PACKAGES.activate("a,b,c");

        IntegroBridgeClassLoader ibscl = new IntegroBridgeClassLoader();
        assertThat("We should have correctly extracted the package paths from STORE_CLASSES_FROM_PACKAGES", ibscl.getPackagePaths(),
                Matchers.is(new String[]{"a", "b", "c"}));

        ibscl.setPackagePaths("");

        assertThat("We should an empty array", ibscl.getPackagePaths(),
                Matchers.is(new String[]{}));
    }

    @Test
    public void testSearchPackages() {

        IntegroBridgeClassLoader ibscl = new IntegroBridgeClassLoader();

        assertThat("We should an empty array", ibscl.getPackagePaths(),
                Matchers.is(new String[]{}));

        ibscl.setPackagePaths("bau,cel,sab");

        assertThat("The given package should be found", ibscl.isClassAmongPackagePaths("baubak"));
        assertThat("The given package should be found", ibscl.isClassAmongPackagePaths("celeste"));
        assertThat("The given package should be found", ibscl.isClassAmongPackagePaths("sabine"));
        assertThat("The given package should be found", !ibscl.isClassAmongPackagePaths("crow"));
    }
}
