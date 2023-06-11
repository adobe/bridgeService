package com.adobe.campaign.tests.bridge.service;

import com.adobe.campaign.tests.bridge.service.utils.ServiceTools;
import org.hamcrest.Matchers;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;

public class TestAccessTester {
    private static final int port1 = 1111;
    private static final int port2 = port1 + 1;
    private static final List<String> urls = Arrays.asList("localhost:" + port2, "localhost:" + port1);
    ServerSocket serverSocket1 = null;
    ServerSocket serverSocket2 = null;

    @BeforeClass(alwaysRun = true)
    public void prepare() throws IOException {
        serverSocket1 = new ServerSocket(port1);
        serverSocket2 = new ServerSocket(port2);
    }

    @AfterClass(alwaysRun = true)
    public void tearDown() throws IOException {
        serverSocket1.close();
        serverSocket2.close();
    }

    @Test
    public void testTestCall() {
        ServiceAccess ac = new ServiceAccess();
        assertThat("We should have a map of values", ac.getExternalServices(), Matchers.instanceOf(Map.class));
        assertThat("We should have a empty map of values", ac.getExternalServices().size(), Matchers.equalTo(0));

        assertThat(urls.get(0) + " should be reachable",
                ServiceTools.isServiceAvailable(urls.get(0)));

        assertThat(urls.get(1) + " should be reachable",
                ServiceTools.isServiceAvailable(urls.get(1)));

        assertThat("\"not really a url\"  should be reachable",
                !ServiceTools.isServiceAvailable("http://nonExisting.url.really.not"));

        assertThat("\"not really a url\"  should be reachable",
                !ServiceTools.isServiceAvailable("not really a url"));
    }

    @Test
    public void testTestCall_negative() {

        assertThat("EmptyString should NOT be reachable",
                !ServiceTools.isServiceAvailable(""));

        assertThat("Null should NOT be reachable",
                !ServiceTools.isServiceAvailable(null));
    }

    @Test
    public void testTestCalls() {

        ServiceAccess ac = new ServiceAccess();
        Map<String, String> urlsMap = new HashMap<>();
        urlsMap.put("url1", "localhost:" + port2);
        urlsMap.put("url2", "localhost:" + port1);
        urlsMap.put("url3", "");

        ac.setExternalServices(urlsMap);

        Map<String, Boolean> result = ac.checkAccessibilityOfExternalResources();

        assertThat("We should have results for the urlsMap we passed", result.size()
                , Matchers.equalTo(urlsMap.size()));

        assertThat(urlsMap.get("url1") + " should be reachable",
                result.get("url1"));

        assertThat(urlsMap.get("url2") + " should be reachable",
                result.get("url2"));

        assertThat("Empty String should NOT  be reachable",
                !result.get("url3"));
    }

    @Test
    public void testTestCalls_negativeAndPositive() {
        ServiceAccess ac = new ServiceAccess();
        Map<String, String> urlsMap = new HashMap<>();
        urlsMap.put("url1", "not really a url");
        urlsMap.put("url2", "localhost:" + port2);

        ac.setExternalServices(urlsMap);

        Map<String, Boolean> result = ac.checkAccessibilityOfExternalResources();

        assertThat("We should have results for the urlsMap we passed", result.size()
                , Matchers.equalTo(urlsMap.size()));

        assertThat(urlsMap.get("url1") + " should Not be reachable",
                Matchers.not(result.get("url1")));

        assertThat(urlsMap.get("url2") + "  should be reachable",
                result.get("url2"));
    }

    @Test
    public void testServiceParsing() {

        assertThat("We should get the correct path", ServiceTools.getIPPath("a.b.c:123"), Matchers.equalTo("a.b.c"));

        assertThat("We should get the correct path", ServiceTools.getIPPath("a.b.c:123/d/e/f"),
                Matchers.equalTo("a.b.c"));

        assertThat("We should get the correct path", ServiceTools.getIPPath("a.b.c"), Matchers.equalTo("a.b.c"));

        assertThat("We should get the correct path", ServiceTools.getIPPath("http://a.b.c:123/d/e/f"),
                Matchers.equalTo("a.b.c"));

        assertThat("We should get the correct path", ServiceTools.getIPPath("a.b.c/d/c/e"), Matchers.equalTo("a.b.c"));

        assertThat("We should get the correct path", ServiceTools.getIPPath(""), Matchers.nullValue());

        assertThat("We should get the correct path", ServiceTools.getIPPath(":123"), Matchers.nullValue());

    }

    @Test
    public void testServiceParsingPort() {

        assertThat("We should get the correct path", ServiceTools.getPort("a.b.c:123"), Matchers.equalTo(123));

        assertThat("We should get the correct path", ServiceTools.getPort("a.b.c:123/d/e/f"), Matchers.equalTo(123));

        assertThat("We should get the correct path", ServiceTools.getPort("a.b.c"),
                Matchers.equalTo(Integer.parseInt(ConfigValueHandlerIBS.DEFAULT_SERVICE_PORT.fetchValue())));

        assertThat("We should get the correct path", ServiceTools.getPort("http://a.b.c:123/d/e/f"),
                Matchers.equalTo(123));

        assertThat("We should get the correct path", ServiceTools.getPort("a.b.c/d/c/e"),
                Matchers.equalTo(Integer.parseInt(ConfigValueHandlerIBS.DEFAULT_SERVICE_PORT.fetchValue())));

        assertThat("We should get the correct path", ServiceTools.getPort(""), Matchers.nullValue());

    }

}
