package com.adobe.campaign.tests.service;

import com.adobe.campaign.tests.service.utils.ServiceTools;
import org.hamcrest.Matchers;
import org.testng.annotations.Test;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;

public class TestAccessTester {
    private static final List<String> urls = Arrays.asList("acc-simulators.email.corp.adobe.com:143", "acc-simulators.smpp.corp.adobe.com");

    @Test
    public void testTestCall() throws IOException {
        ServiceAccess ac = new ServiceAccess();
        assertThat("We should have a map of values", ac.getExternalServices(), Matchers.instanceOf(Map.class));
        assertThat("We should have a empty map of values", ac.getExternalServices().size(), Matchers.equalTo(0));

        assertThat(urls.get(0)+" should be reachable",
                ac.isServiceAvailable(urls.get(0)));

        assertThat(urls.get(1)+" should be reachable",
                ac.isServiceAvailable(urls.get(1)));

        assertThat("\"not really a url\"  should be reachable",
                !ac.isServiceAvailable("http://nonExisting.url.really.not"));

        assertThat("\"not really a url\"  should be reachable",
                !ac.isServiceAvailable("not really a url"));
    }

    @Test
    public void testTestCall_negative() throws IOException {
        ServiceAccess ac = new ServiceAccess();

        assertThat("EmptyString should NOT be reachable",
                !ac.isServiceAvailable(""));

        assertThat("Null should NOT be reachable",
                !ac.isServiceAvailable(null));
    }

    @Test
    public void testTestCalls() throws IOException {
        ServiceAccess ac = new ServiceAccess();
        Map<String, String> urlsMap = new HashMap<>();
        urlsMap.put("url1", urls.get(0));
        urlsMap.put("url2", urls.get(1));
        urlsMap.put("url3", "");

        ac.setExternalServices(urlsMap);

        Map<String, Boolean> result = ac.checkAccessibilityOfExternalResources();

        assertThat("We should have results for the urlsMap we passed", result.size()
                , Matchers.equalTo(urlsMap.size()));


        assertThat(urlsMap.get("url1")+" should be reachable",
                result.get("url1"));

        assertThat(urlsMap.get("url2")+" should be reachable",
                result.get("url2"));

        assertThat("Empty String should NOT  be reachable",
                !result.get("url3"));
    }


    @Test
    public void testTestCalls_negativeAndPositive() throws IOException {
        ServiceAccess ac = new ServiceAccess();
        Map<String, String> urlsMap = new HashMap<>();
        urlsMap.put("url1", "not really a url");
        urlsMap.put("url2", urls.get(1));

        ac.setExternalServices(urlsMap);

        Map<String, Boolean> result = ac.checkAccessibilityOfExternalResources();

        assertThat("We should have results for the urlsMap we passed", result.size()
                , Matchers.equalTo(urlsMap.size()));


        assertThat(urlsMap.get("url1")+" should Not be reachable",
                Matchers.not(result.get("url1")));

        assertThat(urlsMap.get("url2")+"  should be reachable",
                result.get("url2"));
    }

    @Test
    public void testServiceParsing() throws MalformedURLException {

        assertThat("We should get the correct path", ServiceTools.getIPPath("a.b.c:123"), Matchers.equalTo("a.b.c"));

        assertThat("We should get the correct path", ServiceTools.getIPPath("a.b.c:123/d/e/f"), Matchers.equalTo("a.b.c"));

        assertThat("We should get the correct path", ServiceTools.getIPPath("a.b.c"), Matchers.equalTo("a.b.c"));

        assertThat("We should get the correct path", ServiceTools.getIPPath("http://a.b.c:123/d/e/f"), Matchers.equalTo("a.b.c"));

        assertThat("We should get the correct path", ServiceTools.getIPPath("a.b.c/d/c/e"), Matchers.equalTo("a.b.c"));

        assertThat("We should get the correct path", ServiceTools.getIPPath(""), Matchers.nullValue());

        assertThat("We should get the correct path", ServiceTools.getIPPath(":123"), Matchers.nullValue());

    }

    @Test
    public void testServiceParsingPort() throws MalformedURLException {

        assertThat("We should get the correct path", ServiceTools.getPort("a.b.c:123"), Matchers.equalTo(123));

        assertThat("We should get the correct path", ServiceTools.getPort("a.b.c:123/d/e/f"), Matchers.equalTo(123));

        assertThat("We should get the correct path", ServiceTools.getPort("a.b.c"), Matchers.equalTo(Integer.parseInt(ConfigValueHandler.DEFAULT_SERVICE_PORT.fetchValue())));

        assertThat("We should get the correct path", ServiceTools.getPort("http://a.b.c:123/d/e/f"), Matchers.equalTo(123));

        assertThat("We should get the correct path", ServiceTools.getPort("a.b.c/d/c/e"), Matchers.equalTo(Integer.parseInt(ConfigValueHandler.DEFAULT_SERVICE_PORT.fetchValue())));

        assertThat("We should get the correct path", ServiceTools.getPort(""), Matchers.nullValue());

    }

}
