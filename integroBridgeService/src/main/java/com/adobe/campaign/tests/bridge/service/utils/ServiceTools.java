/*
 * Copyright 2022 Adobe
 * All Rights Reserved.
 *
 * NOTICE: Adobe permits you to use, modify, and distribute this file in
 * accordance with the terms of the Adobe license agreement accompanying
 * it.
 */
package com.adobe.campaign.tests.bridge.service.utils;

import com.adobe.campaign.tests.bridge.service.ConfigValueHandlerIBS;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.net.*;

public class ServiceTools {

    /**
     * Extracts the main path of a given DNS. We also accept DNS without protocol
     * @param in_url The full or partial URL. Examples: 'https://adobe.com:80', 'adobe.com:80' or 'https://adobe.com/support'
     * @return The DNS path in the above example adobe.com
     */
    public static String getIPPath(String in_url) {
        if (in_url==null || in_url.isEmpty()) {
            return null;
        }

        String l_pathWithoutProtocol = stripProtocol(in_url);

        String l_pathWithoutPort = l_pathWithoutProtocol.indexOf(':')>=0 ? l_pathWithoutProtocol.substring(0,l_pathWithoutProtocol.indexOf(':')) : l_pathWithoutProtocol;

        if (l_pathWithoutPort.isEmpty()) {
            return null;
        }

        return l_pathWithoutPort.indexOf('/')>=0?l_pathWithoutPort.substring(0, l_pathWithoutPort.indexOf('/')) : l_pathWithoutPort;
    }

    /**
     * removes protocol from url
     * @param in_url The full or partial URL. Examples: 'https://adobe.com:80', 'adobe.com:80' or 'https://adobe.com/support'
     * @return the DNS without protocol Examples: 'adobe.com:80', 'adobe.com:80' or 'adobe.com/support'
     */
    private static String stripProtocol(String in_url) {
        return in_url.contains(":/") ? in_url.substring(in_url.indexOf("://")+3) : in_url;
    }

    /**
     * Extracts the port of a given DNS. We also accept DNS without protocol
     *
     * @param in_url The full or partial URL. Examples: 'https://adobe.com:80', 'adobe.com:80' or
     *               'https://adobe.com/support'
     * @return The port provided in the DNS path without the protocol. We return null if the given value is null or
     * empty.
     */
    public static Integer getPort(String in_url) {
        if (in_url==null || in_url.isEmpty()) {
            return null;
        }
        String l_pathWithoutProtocol = stripProtocol(in_url);
        String l_pathWithoutDNS = l_pathWithoutProtocol.indexOf(':')>=0 ? l_pathWithoutProtocol.substring(l_pathWithoutProtocol.indexOf(':')+1) : ConfigValueHandlerIBS.DEFAULT_SERVICE_PORT.fetchValue();
        return Integer.parseInt(l_pathWithoutDNS.indexOf('/')>=0?l_pathWithoutDNS.substring(0, l_pathWithoutDNS.indexOf('/')) : l_pathWithoutDNS);
    }

    private static int STD_WAIT_BEFORE_INVALIDATE = 5000;

    protected static Logger log = LogManager.getLogger();

    protected static int STD_PORT_SERIES_START = 50000;

    public static void setWAIT_BEFORE_INVALIDATE(int in_waitTimeMS) {
        STD_WAIT_BEFORE_INVALIDATE = in_waitTimeMS;
    }

    /**
     * This method lets us know if the given IP Address (or DNS) is reachable
     *
     * @param in_inetAddress
     *            An IP or DNS Address
     * @return true if the IP Can be accessed
     * @throws IOException if a network error occurs
     */
    protected static boolean isInetAddressReachable(InetAddress in_inetAddress)
            throws IOException {

        return in_inetAddress.isReachable(STD_WAIT_BEFORE_INVALIDATE);

    }

    /**
     * This method lets us know if the given IP Address (or DNS) is reachable. This method also treats badly defined
     * urls as unreachable.
     *
     * @param in_dnsAddress An IP or DNS Address
     * @return true if the IP Can be accessed
     */
    public static boolean isServiceAvailable(String in_dnsAddress) {
        String dns = ServiceTools.getIPPath(in_dnsAddress);
        Integer port = ServiceTools.getPort(in_dnsAddress);

        if (dns == null || port == null) {
            return false;
        }

        return isServerListening(dns, port);

    }

    /**
     * This method lets us know if the given IP Address (or DNS) is reachable
     *
     * @param in_ipAddress
     *            An IP or DNS Address
     * @return true if the IP Can be accessed
     */
    public static boolean isServerAvailable(String in_ipAddress) {

        try {
            return isInetAddressReachable(InetAddress.getByName(in_ipAddress));
        } catch (UnknownHostException e) {
            log.error("Caught UnknownHost Error : " + e);
        } catch (IOException e) {
            log.error("IOException " + e + " occurred.");
        }
        return false;
    }



    /**
     * This method lets you know if a port on a given server is accessible
     *
     * @param host The server you want to use
     * @param port The port number you want to use
     * @return true if port is used
     */
    public static boolean isServerListening(String host, int port) {
        if (!isServerAvailable(host))
            return false;
        else {
            try (Socket s = new Socket(host, port)){
                return true;
            } catch (Exception e) {
                log.error("Found error " + e);
                return false;
            }
        }
    }

    /**
     * This method given a host returns the next free port number on localhost.
     *
     * @return the number of the next free port
     * @throws IOException if a network error occurs
     */
    public static int fetchNextFreePortNumber() throws IOException {

        try (ServerSocket socket = new ServerSocket(0)) {
            return socket.getLocalPort();
        }
    }

    /**
     * This method lets us know if a given port is free on the local host
     *
     * @param in_port Port you want to use
     * @return true if port is not used
     */
    public static boolean isPortFree(int in_port) {

        try (
                ServerSocket ss = new ServerSocket(in_port);
                DatagramSocket ds = new DatagramSocket(in_port);
        ) {
            ss.setReuseAddress(true);
            ds.setReuseAddress(true);
            return true;
        } catch (IOException e) {
            log.error(e);
        }

        return false;
    }


}
