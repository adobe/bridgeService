/*
 * Copyright 2022 Adobe
 * All Rights Reserved.
 *
 * NOTICE: Adobe permits you to use, modify, and distribute this file in
 * accordance with the terms of the Adobe license agreement accompanying
 * it.
 */
package com.adobe.campaign.tests.bridge.service;

import com.adobe.campaign.tests.bridge.service.exceptions.*;
import com.adobe.campaign.tests.bridge.service.utils.ServiceTools;
import com.adobe.campaign.tests.bridge.testdata.one.EnvironmentVariableHandler;
import com.adobe.campaign.tests.bridge.testdata.one.SimpleStaticMethods;
import com.adobe.campaign.tests.bridge.testdata.two.StaticMethodsIntegrity;
import com.fasterxml.jackson.databind.exc.UnrecognizedPropertyException;
import org.hamcrest.Matchers;
import org.testng.Assert;
import org.testng.annotations.*;
import spark.Spark;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import static io.restassured.RestAssured.given;

public class E2EPortCheck {
    private static int port1 = 8080;
    ServerSocket serverSocket1 = null;


    @BeforeMethod
    public void cleanCache() throws IOException {
        port1 = ServiceTools.fetchNextFreePortNumber();
        ConfigValueHandlerIBS.resetAllValues();
        //Block the socket
        serverSocket1 = new ServerSocket(port1);

    }

    @Test
    public void testMainHelloWorld() {
        Assert.assertThrows(IBSConfigurationException.class, () -> IntegroAPI.startServices(port1));
    }

    @AfterMethod(alwaysRun = true)
    public void tearDown() throws IOException {
        ConfigValueHandlerIBS.resetAllValues();

        //release socket
        serverSocket1.close();
    }
}
