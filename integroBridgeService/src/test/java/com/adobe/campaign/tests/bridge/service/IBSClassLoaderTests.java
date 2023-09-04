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
import com.adobe.campaign.tests.bridge.service.exceptions.NonExistentJavaObjectException;
import com.adobe.campaign.tests.bridge.testdata.issue34.pckg1.CalledClass1;
import com.adobe.campaign.tests.bridge.testdata.issue34.pckg1.CalledClass2;
import com.adobe.campaign.tests.bridge.testdata.issue34.pckg1.MiddleMan;
import com.adobe.campaign.tests.bridge.testdata.issue34.pckg2.MiddleManClassFactory;
import com.adobe.campaign.tests.bridge.testdata.one.EnvironmentVariableHandler;
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
        String l_nonExistantNormalClass = CalledClass1.class.getTypeName()+"NonExistant";

        IntegroBridgeClassLoader ibscl = new IntegroBridgeClassLoader();
        Exception caughtException=null;
        try {
            ibscl.loadClass(l_nonExistantNormalClass);
        } catch (Exception e) {
            caughtException=e;

        }
        assertThat("We should have thrown an excption", caughtException, Matchers.notNullValue());

        assertThat("We should have thrown an excption", caughtException, Matchers.instanceOf(NonExistentJavaObjectException.class));
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

        assertThat("We should have thrown an excption", caughtException, Matchers.instanceOf(NonExistentJavaObjectException.class));
    }

    @Test
    public void testsearchClass_negativeNonexistantClassManual2_usingJavaClass() throws ClassNotFoundException {
        String l_nonExistantNormalClass = CalledClass1.class.getTypeName() + "NonExistant";

        IntegroBridgeClassLoader ibscl = new IntegroBridgeClassLoader();
        assertThat("This class should not andd cannot be loaded" ,Matchers.not(ibscl.isClassLoaded(l_nonExistantNormalClass)));
    }


    @Test
    public void testIssue34AutomaticLoading()  {
        ConfigValueHandlerIBS.INTEGRITY_PACKAGE_INJECTION_MODE.activate("automatic");
        JavaCalls l_myJavaCalls = new JavaCalls();

        //Call 1
        CallContent l_cc1 = new CallContent();
        l_cc1.setClassName(CalledClass1.class.getTypeName());
        l_cc1.setMethodName("calledMethod");
        l_myJavaCalls.getCallContent().put("call1", l_cc1);

        //Call 2
        CallContent l_cc2 = new CallContent();
        l_cc2.setClassName(CalledClass2.class.getTypeName());
        l_cc2.setMethodName("calledMethod");
        l_myJavaCalls.getCallContent().put("call2", l_cc2);

        l_myJavaCalls.submitCalls();

        assertThat("The called class should be loaded.", l_myJavaCalls.getLocalClassLoader().isClassLoaded(CalledClass1.class.getTypeName()));
        assertThat("The MiddleMan class should be loaded.", l_myJavaCalls.getLocalClassLoader().isClassLoaded(MiddleMan.class.getTypeName()));
        assertThat("The MiddleManFactory class should be loaded.", l_myJavaCalls.getLocalClassLoader().isClassLoaded(MiddleManClassFactory.class.getTypeName()));

    }

    @Test
    public void testIssue34ManualLoading_case1Negative()  {
        ConfigValueHandlerIBS.INTEGRITY_PACKAGE_INJECTION_MODE.activate("semi-manual");
        ConfigValueHandlerIBS.DEFAULT_CALL_TIMEOUT.activate("60000");
        JavaCalls l_myJavaCalls = new JavaCalls();

        //Call 1
        CallContent l_cc1 = new CallContent();
        l_cc1.setClassName(CalledClass1.class.getTypeName());
        l_cc1.setMethodName("calledMethod");
        l_myJavaCalls.getCallContent().put("call1", l_cc1);

        //Call 2
        CallContent l_cc2 = new CallContent();
        l_cc2.setClassName(CalledClass2.class.getTypeName());
        l_cc2.setMethodName("calledMethod");
        l_myJavaCalls.getCallContent().put("call2", l_cc2);
        Exception caughtException = null;
        try {
            l_myJavaCalls.submitCalls();
        } catch (Exception e) {
            caughtException=e;

        }
        assertThat("We should have thrown an excption", caughtException, Matchers.notNullValue());

        assertThat("We should have thrown an excption", caughtException, Matchers.instanceOf(IBSConfigurationException.class));
        assertThat("We should have thrown an excption", caughtException.getCause(), Matchers.instanceOf(ClassLoaderConflictException.class));
        assertThat("We should have thrown an excption", caughtException.getCause().getCause(), Matchers.instanceOf(LinkageError.class));

    }

    @Test
    public void testIssue34ManualLoading_case2()  {
        ConfigValueHandlerIBS.INTEGRITY_PACKAGE_INJECTION_MODE.activate("manual");
        ConfigValueHandlerIBS.DEFAULT_CALL_TIMEOUT.activate("60000");
        ConfigValueHandlerIBS.STATIC_INTEGRITY_PACKAGES.activate(MiddleManClassFactory.class.getPackageName()+","+MiddleMan.class.getPackageName());

        JavaCalls l_myJavaCalls = new JavaCalls();

        //Call 1
        CallContent l_cc1 = new CallContent();
        l_cc1.setClassName(CalledClass1.class.getTypeName());
        l_cc1.setMethodName("calledMethod");
        l_myJavaCalls.getCallContent().put("call1", l_cc1);

        //Call 2
        CallContent l_cc2 = new CallContent();
        l_cc2.setClassName(CalledClass2.class.getTypeName());
        l_cc2.setMethodName("calledMethod");
        l_myJavaCalls.getCallContent().put("call2", l_cc2);

        //Setting paths

        l_myJavaCalls.submitCalls();
        assertThat("The called class should be loaded.", l_myJavaCalls.getLocalClassLoader().isClassLoaded(CalledClass1.class.getTypeName()));
        assertThat("The MiddleMan class should be loaded.", l_myJavaCalls.getLocalClassLoader().isClassLoaded(MiddleMan.class.getTypeName()));
        assertThat("The MiddleManFactory class should be loaded.", l_myJavaCalls.getLocalClassLoader().isClassLoaded(MiddleManClassFactory.class.getTypeName()));

    }

}
