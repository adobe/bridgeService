/*
 * Copyright 2022 Adobe
 * All Rights Reserved.
 *
 * NOTICE: Adobe permits you to use, modify, and distribute this file in
 * accordance with the terms of the Adobe license agreement accompanying
 * it.
 */
package com.adobe.campaign.tests.bridge.service;

import com.adobe.campaign.tests.bridge.service.utils.ServiceTools;

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
     * Checks if the defined external resources are reachable
     *
     * @return a map of results where the key is the external service key, and a boolean telling us if we are able to
     * connect to the stored services
     */
    public Map<String, Boolean> checkAccessibilityOfExternalResources() {
        Map<String, Boolean> lr_availabilityMap = new HashMap<>();

        getExternalServices().keySet().forEach(k -> lr_availabilityMap.put(k, ServiceTools.isServiceAvailable(
                externalServices.get(k))));

        return lr_availabilityMap;
    }
}
