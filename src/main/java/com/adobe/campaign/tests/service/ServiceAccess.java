package com.adobe.campaign.tests.service;

import com.adobe.campaign.tests.service.utils.ServiceTools;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.net.*;
import java.util.HashMap;
import java.util.Map;

/**
 * Used for checking the state of the services being accessed by the IntegroBridgeService
 */
public class ServiceAccess {

    private Map<String, String> externalServices;

    public ServiceAccess() {
        setExternalServices(new HashMap<>());
    }

    public Map<String, String> getExternalServices() {
        return this.externalServices;
    }

    public void setExternalServices(Map<String, String> externalServices) {
        this.externalServices = externalServices;
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
    public boolean isServiceAvailable(String in_dnsAddress) {
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
     * Checks if the defined external resources are reachable
     *
     * @return a map of results where the key is the external service key, and a boolean telling us if we are able to
     * connect to the stored services
     */
    public Map<String, Boolean> checkAccessibilityOfExternalResources() {
        Map<String, Boolean> lr_availabilityMap = new HashMap<>();

        getExternalServices().keySet().stream().forEach(k -> lr_availabilityMap.put(k, isServiceAvailable(
                externalServices.get(k))));

        return lr_availabilityMap;
    }
}
