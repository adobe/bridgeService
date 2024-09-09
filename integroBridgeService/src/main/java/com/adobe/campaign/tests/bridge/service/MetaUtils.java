/*
 * Copyright 2022 Adobe
 * All Rights Reserved.
 *
 * NOTICE: Adobe permits you to use, modify, and distribute this file in
 * accordance with the terms of the Adobe license agreement accompanying
 * it.
 */
package com.adobe.campaign.tests.bridge.service;

import com.adobe.campaign.tests.bridge.service.exceptions.IBSTestException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.stream.Collectors;

public class MetaUtils {
    public static final List<Class<?>> ManagedClasses = Arrays.asList(String.class, int.class, long.class,
            boolean.class, Integer.class, Long.class, Boolean.class, Object.class);
    public static final int RECURSION_DEPTH_LIMIT = Integer.parseInt(
            ConfigValueHandlerIBS.DESERIALIZATION_DEPTH_LIMIT.fetchValue());
    private static final Logger log = LogManager.getLogger();

    /**
     * Extracts a possible field name given a method name
     *
     * @param in_methodName The name of the method we want to extract
     * @return A possible field name
     */
    public static String extractFieldName(String in_methodName) {
        //Remove "get"
        String l_step1Transformation = in_methodName.startsWith("get") ? in_methodName.substring(3) : in_methodName;

        if (l_step1Transformation.isEmpty()) {
            return null;
        }

        //Transform first character to lower
        return Character.toLowerCase(l_step1Transformation.charAt(0)) + l_step1Transformation.substring(1);
    }

    /**
     * Used for deserializing Collections of unserializable objects.
     *
     * @param in_object A collection of complex objects
     * @return A List of serialized Objects
     */
    public static List extractValuesFromList(Collection in_object) {
        return (List<Map<String, Object>>) in_object.stream().map(MetaUtils::extractValuesFromObject)
                .collect(Collectors.toList());
    }

    public static boolean isExtractable(Class in_class) {
        return ManagedClasses.contains(in_class) || in_class.isPrimitive() || Collection.class.isAssignableFrom(
                in_class);
    }

    /**
     * Objects of these types should be returned as is
     *
     * @param in_class a Class object
     * @return true if the class needs not be managed
     */
    public static boolean isBasicReturnType(Class in_class) {
        return ManagedClasses.contains(in_class) || in_class.isPrimitive();
    }

    /**
     * Lets us know if we can extract this method
     *
     * @param in_method a method object
     * @return true if this method can be invoked in the case of extracting results
     */
    public static boolean isExtractable(Method in_method) {
        List<Boolean> tests = new ArrayList<>();

        tests.add(in_method.getReturnType() instanceof Serializable);
        tests.add(in_method.getName().startsWith("get") || in_method.getName().startsWith("is") || in_method.getName()
                .startsWith("has"));
        tests.add(isExtractable(in_method.getReturnType()));
        tests.add(in_method.getParameterCount() == 0);
        tests.add(Modifier.isPublic(in_method.getDeclaringClass().getModifiers()));

        return tests.stream().noneMatch(r -> r.equals(Boolean.FALSE));
    }

    /**
     * Used for deserializing of unserializeable Objects. This method will extract all the values from the object that
     * it can. It will follow a preset depth which is by default 1.
     *
     * @param in_object A complex object
     * @return A Map of serialized Objects
     */
    public static Object extractValuesFromObject(Object in_object) {
        return extractValuesFromObject(in_object, 0);
    }

    /**
     * Used for deserializing of unserializeable Objects. This method will extract all the values from the object that
     * it can. It will follow a preset depth which is by default 1.
     *
     * @param in_object      A complex object
     * @param recursionLevel The depth of the recursion
     * @return A Map of serialized Objects
     */
    public static Object extractValuesFromObject(Object in_object, int recursionLevel) {
        if (recursionLevel > RECURSION_DEPTH_LIMIT) {
            return "... of type " + in_object.getClass().getName();
        }
        Map<String, Object> lr_value = new HashMap<>();
        if (in_object == null) {
            return lr_value;
        } else if (isBasicReturnType(in_object.getClass())) {
            return in_object;
        } else if (in_object instanceof Collection) {
            return extractValuesFromList((Collection) in_object);
        } else if (in_object instanceof Map) {
            return extractValuesFromMap((Map) in_object);
        }

        for (Method lt_m : Arrays.stream(in_object.getClass().getMethods()).filter(MetaUtils::isExtractable).collect(
                Collectors.toSet())) {

            if (lt_m.getParameterCount() == 0 && isExtractable(lt_m.getReturnType())) {

                try {
                    Object lt_returnValue = lt_m.invoke(in_object);

                    //TODO Add option with null values (extract null)
                    if (lt_returnValue != null) {
                        lr_value.put(Optional.ofNullable(extractFieldName(lt_m.getName())).orElse("this"),
                                (lt_returnValue instanceof Serializable) ? lt_returnValue : extractValuesFromObject(
                                        lt_returnValue, recursionLevel + 1));
                        log.debug("Extracting method value {}={}", lt_m.getName(), lt_returnValue);
                    }
                } catch (IllegalAccessException | InvocationTargetException e) {
                    log.debug("Failed to execute {}.{}", lt_m.getDeclaringClass(), lt_m.getName());
                }

            }

        }
        return lr_value;
    }

    /**
     * Used for deserializing Maps of unserializable objects.
     *
     * @param in_object A collection of complex objects
     * @return A Map of serialized Objects
     */
    public static Map extractValuesFromMap(Map in_object) {
        Map<Object, Object> lr_returnObject = new HashMap<>();
        in_object.forEach((k, v) -> lr_returnObject.put(k, extractValuesFromObject(v)));

        //Just for testing internal issues
        if (ConfigValueHandlerIBS.TEMP_INTERNAL_ERROR_MODE.fetchValue().equals("active")) {
            log.warn("We are now in the intrnal error mode. This mode is not to be used in production");
            ConfigValueHandlerIBS.TEMP_INTERNAL_ERROR_MODE.reset();

            throw new IBSTestException("Just for testing");
        }
        return lr_returnObject;
    }
}
