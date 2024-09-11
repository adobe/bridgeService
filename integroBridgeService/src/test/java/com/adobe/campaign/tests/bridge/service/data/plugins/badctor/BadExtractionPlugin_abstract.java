/*
 * Copyright 2022 Adobe
 * All Rights Reserved.
 *
 * NOTICE: Adobe permits you to use, modify, and distribute this file in
 * accordance with the terms of the Adobe license agreement accompanying
 * it.
 */
package com.adobe.campaign.tests.bridge.service.data.plugins.badctor;

import com.adobe.campaign.tests.bridge.plugins.IBSDeserializerPlugin;

import java.util.Map;

public abstract class BadExtractionPlugin_abstract implements IBSDeserializerPlugin {
    public BadExtractionPlugin_abstract() {
    }

    @Override
    public boolean appliesTo(Object in_object) {
        return false;
    }

    @Override
    public Map<String, Object> apply(Object in_object) {
        return null;
    }
}
