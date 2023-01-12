package com.adobe.campaign.tests.service;

import org.hamcrest.Matchers;
import org.testng.annotations.Test;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;

public class TestAccessTester {
    private static final List<String> urls = Arrays.asList("https://dn.se", "https://github.com");

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
    public void testTestCalls() throws IOException {
        ServiceAccess ac = new ServiceAccess();
        Map<String, String> urlsMap = new HashMap<>();
        urlsMap.put("url1", urls.get(0));
        urlsMap.put("url2", urls.get(1));

        ac.setExternalServices(urlsMap);

        Map<String, Boolean> result = ac.checkAccessibilityOfExternalResources();

        assertThat("We should have results for the urlsMap we passed", result.size()
                , Matchers.equalTo(urlsMap.size()));


        assertThat(urlsMap.get("url1")+" should be reachable",
                result.get("url1"));

        assertThat(urlsMap.get("url2")+" should be reachable",
                result.get("url2"));
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
}
