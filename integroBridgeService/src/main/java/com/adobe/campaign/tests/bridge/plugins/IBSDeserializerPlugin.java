/*
 * Copyright 2022 Adobe
 * All Rights Reserved.
 *
 * NOTICE: Adobe permits you to use, modify, and distribute this file in
 * accordance with the terms of the Adobe license agreement accompanying
 * it.
 */
package com.adobe.campaign.tests.bridge.plugins;

import java.util.Map;

public interface IBSDeserializerPlugin {
    /**
     * Lets us know if the given Object applies to this Plugin
     * @param in_object an object we want to deserialize
     * @return true if we can use this plugin for the given Object
     */
    public boolean appliesTo(Object in_object);

    /**
     * Applies the plugin to the given object
     * @param in_object an object we want to deserialize
     * @return A Map of data extracted from the object
     */
    public Map<String, Object> apply(Object in_object);
}

