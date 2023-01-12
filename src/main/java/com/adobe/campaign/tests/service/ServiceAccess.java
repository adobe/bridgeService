package com.adobe.campaign.tests.service;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
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

    /**
     * This method lets us know if the given IP Address (or DNS) is reachable. This method also treats badly defined
     * urls as unreachable.
     *
     * @param in_dnsAddress An IP or DNS Address
     * @return true if the IP Can be accessed
     */
    public boolean isServiceAvailable(String in_dnsAddress) {

        HttpURLConnection huc = null;
        try {
            URL url = new URL(in_dnsAddress);

            huc = (HttpURLConnection) url.openConnection();
            huc.setRequestMethod("HEAD");
            return huc.getResponseCode() == HttpURLConnection.HTTP_OK;
        } catch (IOException e) {

            return false;
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
