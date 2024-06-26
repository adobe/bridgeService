/*
 * Copyright 2022 Adobe
 * All Rights Reserved.
 *
 * NOTICE: Adobe permits you to use, modify, and distribute this file in
 * accordance with the terms of the Adobe license agreement accompanying
 * it.
 */
package com.adobe.campaign.tests.bridge.service;

import com.adobe.campaign.tests.bridge.service.data.MyPropertiesHandler;
import com.adobe.campaign.tests.bridge.service.exceptions.ClassLoaderConflictException;
import com.adobe.campaign.tests.bridge.service.exceptions.IBSConfigurationException;
import com.adobe.campaign.tests.bridge.testdata.one.EnvironmentVariableHandler;
import org.hamcrest.Matchers;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import static org.hamcrest.MatcherAssert.assertThat;

public class IBSClassLoaderTests {
    @BeforeMethod(alwaysRun = true)
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
                Matchers.not(Matchers.contains(EnvironmentVariableHandler.class.getPackage().getName())));

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

    @Test
    public void testLoadClass_negativeNonexistantClassManual1() throws ClassNotFoundException {
        String l_nonExistantNormalClass = "com.adobe.campaign.tests.bridge.testdata.issue34.pckg1.NonExistant";

        IntegroBridgeClassLoader ibscl = new IntegroBridgeClassLoader();
        Exception caughtException=null;
        try {
            ibscl.loadClass(l_nonExistantNormalClass);
        } catch (Exception e) {
            caughtException=e;

        }
        assertThat("We should have thrown an excption", caughtException, Matchers.notNullValue());

        assertThat("We should have thrown an excption", caughtException, Matchers.instanceOf(ClassNotFoundException.class));
    }

    @Test
    public void testLoadClass_negativeNonexistantClassManual2_usingJavaClass() throws ClassNotFoundException {
        String l_nonExistantNormalClass = String.class.getTypeName()+"NonExistant";

        IntegroBridgeClassLoader ibscl = new IntegroBridgeClassLoader();
        Exception caughtException=null;
        try {
            ibscl.loadClass(l_nonExistantNormalClass);
        } catch (Exception e) {
            caughtException=e;

        }
        assertThat("We should have thrown an excption", caughtException, Matchers.notNullValue());

        assertThat("We should have thrown an excption", caughtException, Matchers.instanceOf(ClassNotFoundException.class));
    }

    @Test
    public void testsearchClass_negativeNonexistantClassManual2_usingJavaClass() throws ClassNotFoundException {
        String l_nonExistantNormalClass = "com.adobe.campaign.tests.bridge.testdata.issue34.pckg1.CalledClass1NonExistant";

        IntegroBridgeClassLoader ibscl = new IntegroBridgeClassLoader();
        assertThat("This class should not andd cannot be loaded" ,Matchers.not(ibscl.isClassLoaded(l_nonExistantNormalClass)));
    }


    @Test(enabled = E2ETests.AUTOMATIC_FLAG)
    public void testIssue34AutomaticLoading()  {
        ConfigValueHandlerIBS.INTEGRITY_PACKAGE_INJECTION_MODE.activate("automatic");
        JavaCalls l_myJavaCalls = new JavaCalls();

        //Call 1
        CallContent l_cc1 = new CallContent();
        l_cc1.setClassName("com.adobe.campaign.tests.bridge.testdata.issue34.pckg1.CalledClass1");
        l_cc1.setMethodName("calledMethod");
        l_myJavaCalls.getCallContent().put("call1", l_cc1);

        //Call 2
        CallContent l_cc2 = new CallContent();
        l_cc2.setClassName("com.adobe.campaign.tests.bridge.testdata.issue34.pckg1.CalledClass2");
        l_cc2.setMethodName("calledMethod");
        l_myJavaCalls.getCallContent().put("call2", l_cc2);

        l_myJavaCalls.submitCalls();

        assertThat("The called class should be loaded.", l_myJavaCalls.getLocalClassLoader().isClassLoaded("com.adobe.campaign.tests.bridge.testdata.issue34.pckg1.CalledClass1"));
        assertThat("The MiddleMan class should be loaded.", l_myJavaCalls.getLocalClassLoader().isClassLoaded("com.adobe.campaign.tests.bridge.testdata.issue34.pckg1.MiddleMan"));
        assertThat("The MiddleManFactory class should be loaded.", l_myJavaCalls.getLocalClassLoader().isClassLoaded("com.adobe.campaign.tests.bridge.testdata.issue34.pckg2.MiddleManClassFactory"));

    }


    @Test
    public void testIssue34AutomaticLoading_negativeDefaultBehavior()  {
        ConfigValueHandlerIBS.INTEGRITY_PACKAGE_INJECTION_MODE.activate("nonExistantMode");
        JavaCalls l_myJavaCalls = new JavaCalls();

        //Call 1
        CallContent l_cc1 = new CallContent();
        l_cc1.setClassName("com.adobe.campaign.tests.bridge.testdata.issue34.pckg1.CalledClass1");
        l_cc1.setMethodName("calledMethod");
        l_myJavaCalls.getCallContent().put("call1", l_cc1);

        //Call 2
        CallContent l_cc2 = new CallContent();
        l_cc2.setClassName("com.adobe.campaign.tests.bridge.testdata.issue34.pckg1.CalledClass2");
        l_cc2.setMethodName("calledMethod");
        l_myJavaCalls.getCallContent().put("call2", l_cc2);

        Assert.assertThrows(IBSConfigurationException.class, () -> l_myJavaCalls.submitCalls());
        /* Related to issue #55
        assertThat("The called class should be loaded.", l_myJavaCalls.getLocalClassLoader().isClassLoaded("com.adobe.campaign.tests.bridge.testdata.issue34.pckg1.CalledClass1"));
        assertThat("The MiddleMan class should be loaded.", l_myJavaCalls.getLocalClassLoader().isClassLoaded("com.adobe.campaign.tests.bridge.testdata.issue34.pckg1.MiddleMan"));
        assertThat("The MiddleManFactory class should be loaded.", l_myJavaCalls.getLocalClassLoader().isClassLoaded("com.adobe.campaign.tests.bridge.testdata.issue34.pckg2.MiddleManClassFactory"));
        */
    }

    @Test
    public void testIssue34ManualLoading_case1Negative()  {
        ConfigValueHandlerIBS.INTEGRITY_PACKAGE_INJECTION_MODE.activate("semi-manual");
        JavaCalls l_myJavaCalls = new JavaCalls();

        //Call 1
        CallContent l_cc1 = new CallContent();
        l_cc1.setClassName("com.adobe.campaign.tests.bridge.testdata.issue34.pckg1.CalledClass1");
        l_cc1.setMethodName("calledMethod");
        l_myJavaCalls.getCallContent().put("call1", l_cc1);

        //Call 2
        CallContent l_cc2 = new CallContent();
        l_cc2.setClassName("com.adobe.campaign.tests.bridge.testdata.issue34.pckg1.CalledClass2");
        l_cc2.setMethodName("calledMethod");
        l_myJavaCalls.getCallContent().put("call2", l_cc2);
        Exception caughtException = null;
        try {
            l_myJavaCalls.submitCalls();
        } catch (Exception e) {
            caughtException=e;

        }
        assertThat("We should have thrown an exception", caughtException, Matchers.notNullValue());

        assertThat("We should have thrown an exception", caughtException, Matchers.instanceOf(IBSConfigurationException.class));
        assertThat("We should have thrown an exception", caughtException.getCause(), Matchers.instanceOf(ClassLoaderConflictException.class));
        assertThat("We should have thrown an exception", caughtException.getCause().getCause(), Matchers.instanceOf(LinkageError.class));

    }

    @Test
    public void testIssue34ManualLoading_case2()  {
        ConfigValueHandlerIBS.INTEGRITY_PACKAGE_INJECTION_MODE.activate("manual");
        ConfigValueHandlerIBS.STATIC_INTEGRITY_PACKAGES.activate("com.adobe.campaign.tests.bridge.testdata.issue34.pckg2.,com.adobe.campaign.tests.bridge.testdata.issue34.pckg1.");

        JavaCalls l_myJavaCalls = new JavaCalls();

        //Call 1
        CallContent l_cc1 = new CallContent();
        l_cc1.setClassName("com.adobe.campaign.tests.bridge.testdata.issue34.pckg1.CalledClass1");
        l_cc1.setMethodName("calledMethod");
        l_myJavaCalls.getCallContent().put("call1", l_cc1);

        //Call 2
        CallContent l_cc2 = new CallContent();
        l_cc2.setClassName("com.adobe.campaign.tests.bridge.testdata.issue34.pckg1.CalledClass2");
        l_cc2.setMethodName("calledMethod");
        l_myJavaCalls.getCallContent().put("call2", l_cc2);

        //Setting paths

        l_myJavaCalls.submitCalls();
        assertThat("The called class should be loaded.", l_myJavaCalls.getLocalClassLoader().isClassLoaded("com.adobe.campaign.tests.bridge.testdata.issue34.pckg1.CalledClass1"));
        assertThat("The MiddleMan class should be loaded.", l_myJavaCalls.getLocalClassLoader().isClassLoaded("com.adobe.campaign.tests.bridge.testdata.issue34.pckg1.MiddleMan"));
        assertThat("The MiddleManFactory class should be loaded.", l_myJavaCalls.getLocalClassLoader().isClassLoaded("com.adobe.campaign.tests.bridge.testdata.issue34.pckg2.MiddleManClassFactory"));

    }



}
