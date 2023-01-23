package com.adobe.campaign.tests.service;

import com.adobe.campaign.tests.integro.tools.NetworkTools;
import com.adobe.campaign.tests.service.utils.ServiceTools;
import org.hamcrest.Matchers;
import org.testng.annotations.Test;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.Arrays;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;

public class TestServiceUtilsTester {
    private static final List<String> urls = Arrays.asList("acc-simulators.email.corp.adobe.com:143", "acc-simulators.smpp.corp.adobe.com");

    @Test
    public void testTestAvailability() throws IOException {

        assertThat(urls.get(0)+" should be reachable",
                ServiceTools.isServiceAvailable(urls.get(0)));

        assertThat(urls.get(1)+" should be reachable",
                ServiceTools.isServiceAvailable(urls.get(1)));

        assertThat("\"not really a url\"  should be reachable",
                !ServiceTools.isServiceAvailable("http://nonExisting.url.really.not"));

        assertThat("\"not really a url\"  should be reachable",
                !ServiceTools.isServiceAvailable("not really a url"));
    }

    @Test
    public void testTestCall_negative() throws IOException {
        assertThat("EmptyString should NOT be reachable",
                !ServiceTools.isServiceAvailable(""));

        assertThat("Null should NOT be reachable",
                !ServiceTools.isServiceAvailable(null));
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


    @Test
    public void testISIPReachable() {

        assertThat("127.0.0.1 should be reachable",
                NetworkTools.isServerAvailable("127.0.0.1"));

        assertThat("Localhost should be reachable",
                NetworkTools.isServerAvailable("localhost"));

    }

    @Test
    public void testISIPUnreachable() {
        NetworkTools.setWAIT_BEFORE_INVALIDATE(500);
        assertThat("Inexisting ip adress should not be found",
                !NetworkTools.isServerAvailable("remoteHost"));

        assertThat("Inexisting ip adress should not be found",
                !NetworkTools.isServerAvailable("128.0.0.1"));
    }


    /**
     * Hello world to see if we can fetch the correct port
     *
     * @throws IOException
     */
    @Test
    public void getFreePort() throws IOException {

        int l_nextFreePort = NetworkTools.fetchNextFreePortNumber();

        assertThat(
                "The next free port should be our default port "
                        + l_nextFreePort,
                NetworkTools.isPortFree(l_nextFreePort));
    }

    /**
     * Negative test. Checking that an unreachables server's port doesn't cause
     * unwanted side effects
     *
     * @throws IOException
     */
    @Test
    public void getListeningPort_UnExistingServer() throws IOException {

        assertThat("The inexisting server shouldn't return true",
                !NetworkTools.isServerListening("RemoteUnexistingHost", 1233));

    }


}