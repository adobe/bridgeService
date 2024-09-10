/*
 * Copyright 2022 Adobe
 * All Rights Reserved.
 *
 * NOTICE: Adobe permits you to use, modify, and distribute this file in
 * accordance with the terms of the Adobe license agreement accompanying
 * it.
 */
package com.adobe.campaign.tests.bridge.service;

import com.adobe.campaign.tests.bridge.plugins.IBSDeserializerPlugin;
import org.reflections.Reflections;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

public class IBSPluginManager {

    public static void loadPlugins() {
        ExtractionPlugins.loadPlugins();
    }

    static class ExtractionPlugins {

        static Set<IBSDeserializerPlugin> plugins = new LinkedHashSet<>();

        static Map<String, Object> apply(Object in_object) {
            return plugins.stream().filter(a -> a.appliesTo(in_object)).findFirst().get().apply(in_object);
        }

        static void clearPlugins() {
            plugins = new LinkedHashSet<>();
        }

        /**
         * Checks if there is a plugin that applies to the given object
         * @param in_object an arbitrary object
         * @return true if there is a plugin that applies to the given
         */
        static boolean appliesTo(Object in_object) {
            return plugins.stream().anyMatch(a -> a.appliesTo(in_object));
        }

        public static void loadPlugins() {
            if (ConfigValueHandlerIBS.PLUGIN_DESERIALIZATION_PATH.isSet()) {
                Reflections reflections = new Reflections(ConfigValueHandlerIBS.PLUGIN_DESERIALIZATION_PATH.fetchValue());
                Set<Class<? extends IBSDeserializerPlugin>> classes = reflections.getSubTypesOf(IBSDeserializerPlugin.class);

                for (Class<? extends IBSDeserializerPlugin> implementingClass : classes) {
                    try {
                        Constructor<?> ctor = implementingClass.getConstructor();

                        plugins.add( (IBSDeserializerPlugin) ctor.newInstance());
                    } catch (InstantiationException e) {
                        throw new RuntimeException(e);
                    } catch (IllegalAccessException e) {
                        throw new RuntimeException(e);
                    } catch (InvocationTargetException e) {
                        throw new RuntimeException(e);
                    } catch (NoSuchMethodException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        }
    }

}
