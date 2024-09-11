/*
 * Copyright 2022 Adobe
 * All Rights Reserved.
 *
 * NOTICE: Adobe permits you to use, modify, and distribute this file in
 * accordance with the terms of the Adobe license agreement accompanying
 * it.
 */
package com.adobe.campaign.tests.bridge.service;

import com.adobe.campaign.tests.bridge.plugins.deserializer.MimeExtractionPluginDeserializer;
import com.adobe.campaign.tests.bridge.service.data.plugins.badctor.BadExtractionPlugin_abstract;
import com.adobe.campaign.tests.bridge.service.data.plugins.badctor.BadExtractionPlugin_badConstructor;
import com.adobe.campaign.tests.bridge.service.data.plugins.badctor.BadExtractionPlugin_ctorThrowsException;
import com.adobe.campaign.tests.bridge.service.exceptions.IBSConfigurationException;
import com.adobe.campaign.tests.bridge.testdata.one.MimeMessageMethods;
import org.hamcrest.Matchers;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import javax.mail.Message;
import javax.mail.MessagingException;
import java.lang.reflect.InvocationTargetException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;

public class PluginTests {
    @BeforeMethod
    @AfterClass
    public void setUp() {
        ConfigValueHandlerIBS.resetAllValues();
        IBSPluginManager.ExtractionPlugins.clearPlugins();
    }

    /////// Tests for the MultiPartMime plugin

    @Test
    public void testThePluginManager() throws MessagingException {
        var l_suffix = "def";

        Message l_message = MimeMessageMethods.createMultiPartAlternativeMessage(l_suffix);

        assertThat("The plugin should be absent", !IBSPluginManager.ExtractionPlugins.appliesTo(l_message));

        MimeExtractionPluginDeserializer mimePlugin = new MimeExtractionPluginDeserializer();

        //add plugin
        IBSPluginManager.ExtractionPlugins.plugins.add(mimePlugin);
        assertThat("The plugin should be added", IBSPluginManager.ExtractionPlugins.appliesTo(l_message));

        //remove plugin
        IBSPluginManager.ExtractionPlugins.clearPlugins();
        assertThat("The plugin should be absent", !IBSPluginManager.ExtractionPlugins.appliesTo(l_message));
    }

    @Test
    public void testThePluginManagerUsageLevel1() throws MessagingException {
        var l_suffix = "def";

        Message l_message = MimeMessageMethods.createMultiPartAlternativeMessage(l_suffix);

        MimeExtractionPluginDeserializer mimePlugin = new MimeExtractionPluginDeserializer();

        //add plugin
        IBSPluginManager.ExtractionPlugins.plugins.add(mimePlugin);
        assertThat("The plugin should be added", IBSPluginManager.ExtractionPlugins.appliesTo(l_message));

        Map<String, Object> l_result = (Map<String, Object>) IBSPluginManager.ExtractionPlugins.apply(l_message);

        assertThat(l_result.get("contentType"), Matchers.equalTo("text/plain"));
        assertThat(l_result.get("size"), Matchers.equalTo(-1));
        assertThat(l_result.get("subject"), Matchers.equalTo("This is the Subject Line " + l_suffix));
        assertThat(l_result.get("content"), Matchers.instanceOf(List.class));
        var contentList = (List<Map<String, Object>>) l_result.get("content");
        assertThat(contentList.size(), Matchers.equalTo(2));
        assertThat(contentList.get(0), Matchers.instanceOf(Map.class));
        assertThat(contentList.get(0).get("contentType"), Matchers.equalTo("text/plain"));
        assertThat(l_result.get("lineCount"), Matchers.equalTo(-1));

    }

    @Test
    public void testThePluginManagerUsage_negative() throws MessagingException {

        MimeExtractionPluginDeserializer mimePlugin = new MimeExtractionPluginDeserializer();

        String l_message = "This is a string";

        //add plugin
        IBSPluginManager.ExtractionPlugins.plugins.add(mimePlugin);
        assertThat("The MimePlugin Manager should not apply to a String", !IBSPluginManager.ExtractionPlugins.appliesTo(l_message));

        Map<String, Object> l_result = (Map<String, Object>) IBSPluginManager.ExtractionPlugins.apply(l_message);

        assertThat("The MimePlugin Manager should not apply to a String", l_result, Matchers.anEmptyMap());
    }

    @Test
    public void testUsageOfTheExtractionPlugIn() throws MessagingException {
        var l_suffix = "abc";
        Message l_message = MimeMessageMethods.createMultiPartAlternativeMessage(l_suffix);

        Map<String, Object> l_result = (Map<String, Object>) MetaUtils.extractValuesFromObject(l_message);
        assertThat("Since no plugin has been declared content should be generated by default", l_result.get("content"),
                Matchers.not(Matchers.instanceOf(List.class)));

        MimeExtractionPluginDeserializer mimePlugin = new MimeExtractionPluginDeserializer();
        IBSPluginManager.ExtractionPlugins.plugins.add(mimePlugin);

        l_result = (Map<String, Object>) MetaUtils.extractValuesFromObject(l_message);

        assertThat(l_result.get("contentType"), Matchers.equalTo("text/plain"));
        assertThat(l_result.get("size"), Matchers.equalTo(-1));
        assertThat(l_result.get("subject"), Matchers.equalTo("This is the Subject Line " + l_suffix));
        //assertThat(l_result.get("content"), Matchers.equalTo("a content by yours truly " + l_suffix));
        assertThat(l_result.get("content"), Matchers.instanceOf(List.class));
        var contentList = (List<Map<String, Object>>) l_result.get("content");
        assertThat(contentList.size(), Matchers.equalTo(2));
        assertThat(contentList.get(0), Matchers.instanceOf(Map.class));
        assertThat(contentList.get(0).get("contentType"), Matchers.equalTo("text/plain"));
        assertThat(l_result.get("lineCount"), Matchers.equalTo(-1));
    }

    @Test
    public void testUsageOfTheExtractionPlugInConfigBased() throws MessagingException {

        var l_suffix = "abc";
        Message l_message = MimeMessageMethods.createMultiPartAlternativeMessage(l_suffix);

        Map<String, Object> l_result = (Map<String, Object>) MetaUtils.extractValuesFromObject(l_message);
        assertThat("Since no plugin has been declared content should be generated by default", l_result.get("content"),
                Matchers.not(Matchers.instanceOf(List.class)));

        ConfigValueHandlerIBS.PLUGIN_DESERIALIZATION_PATH.activate(
                "com.adobe.campaign.tests.bridge.plugins.deserializer");
        IBSPluginManager.loadPlugins();

        //Activate the dates
        String pattern = "yyyy-MM-dd";
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat(pattern);
        String myDate = simpleDateFormat.format(new Date());
        ConfigValueHandlerIBS.DESERIALIZATION_DATE_FORMAT.activate(pattern);

        l_result = (Map<String, Object>) MetaUtils.extractValuesFromObject(l_message);

        assertThat(l_result.get("contentType"), Matchers.equalTo("text/plain"));
        assertThat(l_result.get("size"), Matchers.equalTo(-1));
        assertThat(l_result.get("subject"), Matchers.equalTo("This is the Subject Line " + l_suffix));
        //assertThat(l_result.get("content"), Matchers.equalTo("a content by yours truly " + l_suffix));
        assertThat(l_result.get("content"), Matchers.instanceOf(List.class));
        var contentList = (List<Map<String, Object>>) l_result.get("content");
        assertThat(contentList.size(), Matchers.equalTo(2));
        assertThat(contentList.get(0), Matchers.instanceOf(Map.class));
        assertThat(contentList.get(0).get("contentType"), Matchers.equalTo("text/plain"));
        assertThat(l_result.get("lineCount"), Matchers.equalTo(-1));
        assertThat(l_result.get("sentDate"), Matchers.equalTo(myDate));
    }

    //Negative tests
    @Test
    public void testExceptionsDuringPluginManager_badCtor() {
        try {
            IBSPluginManager.ExtractionPlugins.addPlugin(
                    BadExtractionPlugin_badConstructor.class);
            assertThat("An exception should have been thrown.", false);

        } catch (Exception e) {
            assertThat("We should encapsulate the exception into an IBS Config Exception", e,
                    Matchers.instanceOf(IBSConfigurationException.class));
            assertThat("The root cause should be that there is no default constructor", e.getCause(),
                    Matchers.instanceOf(NoSuchMethodException.class));
        }
    }

    @Test
    public void testExceptionsDuringPluginManager_invocationTarget() throws NoSuchMethodException {
        try {
            IBSPluginManager.ExtractionPlugins.addPlugin(
                    BadExtractionPlugin_ctorThrowsException.class);
            assertThat("An exception should have been thrown.", false);

        } catch (Exception e) {
            assertThat("We should encapsulate the exception into an IBS Config Exception", e,
                    Matchers.instanceOf(IBSConfigurationException.class));
            assertThat("The root cause should be an Invocation Target Exception", e.getCause(),
                    Matchers.instanceOf(InvocationTargetException.class));
        }
    }

    @Test
    public void testExceptionsDuringPluginManager_illegalAccess() throws NoSuchMethodException {

        ConfigValueHandlerIBS.PLUGIN_DESERIALIZATION_PATH.activate(
                "com.adobe.campaign.tests.bridge.service.data.plugins.badctor2");
        try {
            IBSPluginManager.loadPlugins();
            assertThat("An exception should have been thrown.", false);

        } catch (Exception e) {
            assertThat("We should encapsulate the exception into an IBS Config Exception", e,
                    Matchers.instanceOf(IBSConfigurationException.class));
            assertThat("The root cause should be an Illegal access exception", e.getCause(),
                    Matchers.instanceOf(IllegalAccessException.class));
        }
    }

    @Test
    public void testExceptionsDuringPluginManager_instantiationException() throws NoSuchMethodException {

        try {
            IBSPluginManager.ExtractionPlugins.addPlugin(
                    BadExtractionPlugin_abstract.class);
            assertThat("An exception should have been thrown.", false);

        } catch (Exception e) {
            assertThat("We should encapsulate the exception into an IBS Config Exception", e,
                    Matchers.instanceOf(IBSConfigurationException.class));
            assertThat("The root cause should be an Invocation Target Exception", e.getCause(),
                    Matchers.instanceOf(InstantiationException.class));
        }
    }

}
