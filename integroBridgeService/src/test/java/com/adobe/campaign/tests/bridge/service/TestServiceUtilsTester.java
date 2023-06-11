package com.adobe.campaign.tests.bridge.service;

import com.adobe.campaign.tests.bridge.service.utils.ServiceTools;
import org.hamcrest.Matchers;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.Arrays;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;

public class TestServiceUtilsTester {
    
    private static final int port1 = 1111;
    private static final int port2 = port1+1;
    private static final List<String> urls = Arrays.asList("localhost:"+ port2, "localhost:"+ port1);
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
    public void testTestAvailability() {

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
    public void testTestCall_negative() {
        assertThat("EmptyString should NOT be reachable",
                !ServiceTools.isServiceAvailable(""));

        assertThat("Null should NOT be reachable",
                !ServiceTools.isServiceAvailable(null));
    }


    @Test
    public void testServiceParsing() {

        assertThat("We should get the correct path", ServiceTools.getIPPath("a.b.c:123"), Matchers.equalTo("a.b.c"));

        assertThat("We should get the correct path", ServiceTools.getIPPath("a.b.c:123/d/e/f"), Matchers.equalTo("a.b.c"));

        assertThat("We should get the correct path", ServiceTools.getIPPath("a.b.c"), Matchers.equalTo("a.b.c"));

        assertThat("We should get the correct path", ServiceTools.getIPPath("http://a.b.c:123/d/e/f"), Matchers.equalTo("a.b.c"));

        assertThat("We should get the correct path", ServiceTools.getIPPath("a.b.c/d/c/e"), Matchers.equalTo("a.b.c"));

        assertThat("We should get the correct path", ServiceTools.getIPPath(""), Matchers.nullValue());

        assertThat("We should get the correct path", ServiceTools.getIPPath(":123"), Matchers.nullValue());

    }

    @Test
    public void testServiceParsingPort() {

        assertThat("We should get the correct path", ServiceTools.getPort("a.b.c:123"), Matchers.equalTo(123));

        assertThat("We should get the correct path", ServiceTools.getPort("a.b.c:123/d/e/f"), Matchers.equalTo(123));

        assertThat("We should get the correct path", ServiceTools.getPort("a.b.c"), Matchers.equalTo(Integer.parseInt(
                ConfigValueHandlerIBS.DEFAULT_SERVICE_PORT.fetchValue())));

        assertThat("We should get the correct path", ServiceTools.getPort("http://a.b.c:123/d/e/f"), Matchers.equalTo(123));

        assertThat("We should get the correct path", ServiceTools.getPort("a.b.c/d/c/e"), Matchers.equalTo(Integer.parseInt(
                ConfigValueHandlerIBS.DEFAULT_SERVICE_PORT.fetchValue())));

        assertThat("We should get the correct path", ServiceTools.getPort(""), Matchers.nullValue());

    }


    @Test
    public void testISIPReachable() {

        assertThat("127.0.0.1 should be reachable",
                ServiceTools.isServerAvailable("127.0.0.1"));

        assertThat("Localhost should be reachable",
                ServiceTools.isServerAvailable("localhost"));

    }

    @Test
    public void testISIPUnreachable() {
        ServiceTools.setWAIT_BEFORE_INVALIDATE(500);
        assertThat("Inexisting ip adress should not be found",
                !ServiceTools.isServerAvailable("remoteHost"));

        assertThat("Inexisting ip adress should not be found",
                !ServiceTools.isServerAvailable("128.0.0.1"));
    }


    /**
     * Hello world to see if we can fetch the correct port
     */
    @Test
    public void getFreePort() throws IOException {

        int l_nextFreePort = ServiceTools.fetchNextFreePortNumber();

        assertThat(
                "The next free port should be our default port "
                        + l_nextFreePort,
                ServiceTools.isPortFree(l_nextFreePort));
    }

    /**
     * Negative test. Checking that an unreachables server's port doesn't cause
     * unwanted side effects
     *
     */
    @Test
    public void getListeningPort_UnExistingServer() {

        assertThat("The inexisting server shouldn't return true",
                !ServiceTools.isServerListening("RemoteUnexistingHost", 1233));

    }


}
