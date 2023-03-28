package com.adobe.campaign.tests.service;

import com.adobe.campaign.tests.integro.core.SystemValueHandler;
import com.adobe.campaign.tests.service.data.MyPropertiesHandler;
import org.hamcrest.Matchers;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import static org.hamcrest.MatcherAssert.assertThat;

public class IBSClassLoaderTests {
    @BeforeMethod
    @AfterClass
    public void reset() {
        ConfigValueHandlerIBS.resetAllValues();
    }


    @Test
    public void testExtractPackages() {

        ConfigValueHandlerIBS.STATIC_INTEGRITY_PACKAGES.activate("a,b,c");

        IntegroBridgeClassLoader ibscl = new IntegroBridgeClassLoader();
        assertThat("We should have correctly extracted the package paths from STORE_CLASSES_FROM_PACKAGES", ibscl.getPackagePaths(),
                Matchers.contains("a", "b", "c"));

        ibscl.setPackagePaths("");

        assertThat("We should an empty array", ibscl.getPackagePaths(),
                Matchers.empty());

        ibscl.setPackagePaths("a");
        assertThat("We should have a single entry array", ibscl.getPackagePaths(),
                Matchers.contains("a"));

    }

    @Test
    public void testSearchPackages() {

        IntegroBridgeClassLoader ibscl = new IntegroBridgeClassLoader();

        assertThat("We should have an array containing only the systemvaluehandler", ibscl.getPackagePaths(),
                Matchers.not(Matchers.contains(SystemValueHandler.class.getPackage().getName())));

        ibscl.setPackagePaths("bau,cel,sab");

        assertThat("The given package should be found", ibscl.isClassAmongPackagePaths("baubak"));
        assertThat("The given package should be found", ibscl.isClassAmongPackagePaths("celeste"));
        assertThat("The given package should be found", ibscl.isClassAmongPackagePaths("sabine"));
        assertThat("The given package should be found", !ibscl.isClassAmongPackagePaths("crow"));
    }


    @Test
    public void testIncludeEnvironmentVarClassPath() {

        ConfigValueHandlerIBS.STATIC_INTEGRITY_PACKAGES.activate("a,b,c");
        ConfigValueHandlerIBS.ENVIRONMENT_VARS_SETTER_CLASS.activate(MyPropertiesHandler.class.getTypeName());

        IntegroBridgeClassLoader ibscl = new IntegroBridgeClassLoader();
        assertThat("We should have correctly extracted the package paths from STORE_CLASSES_FROM_PACKAGES", ibscl.getPackagePaths(),
                Matchers.containsInAnyOrder("a", "b", "c"));

        ibscl.setPackagePaths("");

        assertThat("We should an empty array", ibscl.getPackagePaths(),
                Matchers.empty());

        ibscl.setPackagePaths("a");
        assertThat("We should have a single entry array", ibscl.getPackagePaths(),
                Matchers.contains("a"));

    }
}
