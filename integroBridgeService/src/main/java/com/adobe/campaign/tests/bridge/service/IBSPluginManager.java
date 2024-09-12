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
import com.adobe.campaign.tests.bridge.service.exceptions.IBSConfigurationException;
import org.reflections.Reflections;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.*;

public class IBSPluginManager {

    public static void loadPlugins() {
        ExtractionPlugins.loadPlugins();
    }

    static class ExtractionPlugins {

        static Set<IBSDeserializerPlugin> plugins = new LinkedHashSet<>();

        static Map<String, Object> apply(Object in_object) {
            Optional<IBSDeserializerPlugin> applicablePlugin = plugins.stream().filter(a -> a.appliesTo(in_object)).findFirst();

            if (!applicablePlugin.isPresent()) {
                return new HashMap<>();
            }
            return applicablePlugin.get().apply(in_object);
        }

        static void clearPlugins() {
            plugins = new LinkedHashSet<>();
        }

        /**
         * Checks if there is a plugin that applies to the given object
         *
         * @param in_object an arbitrary object
         * @return true if there is a plugin that applies to the given
         */
        static boolean appliesTo(Object in_object) {
            return plugins.stream().anyMatch(a -> a.appliesTo(in_object));
        }

        static void loadPlugins() {
            if (ConfigValueHandlerIBS.PLUGINS_PACKAGE.isSet()) {
                Reflections reflections = new Reflections(
                        ConfigValueHandlerIBS.PLUGINS_PACKAGE.fetchValue());
                Set<Class<? extends IBSDeserializerPlugin>> classes = reflections.getSubTypesOf(
                        IBSDeserializerPlugin.class);

                for (Class<? extends IBSDeserializerPlugin> implementingClass : classes) {
                    addPlugin(implementingClass);
                }
            }
        }

        static void addPlugin(Class<? extends IBSDeserializerPlugin> implementingClass) {
            try {
                Constructor<?> ctor = implementingClass.getConstructor();

                plugins.add((IBSDeserializerPlugin) ctor.newInstance());
            } catch (InstantiationException e) {
                throw new IBSConfigurationException("The given plugin " + implementingClass.getName()
                        + " cannot be instantiated.", e);
            } catch (IllegalAccessException e) {
                throw new IBSConfigurationException("We do not have access to the plugin " + implementingClass.getName()
                        + ". This is probably a scope issue. Please review this and rerun the IBS.", e);
            } catch (InvocationTargetException e) {
                throw new IBSConfigurationException(
                        "The constructor of the plugin " + implementingClass.getName() + " threw an exception.", e);
            } catch (NoSuchMethodException e) {
                throw new IBSConfigurationException(
                        "The plugin " + implementingClass.getName() + " does not have a default constructor.", e);
            }
        }
    }

}
