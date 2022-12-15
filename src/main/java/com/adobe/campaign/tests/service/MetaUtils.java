package com.adobe.campaign.tests.service;

import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MetaUtils {
    public static final List<Class<?>> ManagedClasses = Arrays.asList(String.class, int.class, long.class, boolean.class,Integer.class, Long.class, Boolean.class, Object.class);

    /**
     * Extracts a possible field name given a method name
     * @param in_methodName
     * @return A possible field name
     */
    public static String extractFieldName(String in_methodName) {
        //Remove "get"
        String l_step1Transformation = in_methodName.startsWith("get") ? in_methodName.substring(3) : in_methodName;

        //Transform first character to lower
        StringBuilder sb = new StringBuilder();
        sb.append(Character.toLowerCase(l_step1Transformation.charAt(0)));
        sb.append(l_step1Transformation.substring(1));

        return sb.toString();
    }

    public static Map<String, Object> extractValuesFromObject(Object in_object) {
        Map<String, Object> lr_value = new HashMap<>();
        for (Method lt_m : in_object.getClass().getDeclaredMethods()) {

            if (lt_m.getReturnType() instanceof Serializable) {
                if (lt_m.getParameterCount()==0 && lt_m.canAccess(in_object) && ManagedClasses.contains(lt_m.getReturnType())) {
                    System.out.println(lt_m.getName()+":");
                    Object lt_returnValue = null;
                    try {
                        lt_returnValue = lt_m.invoke(in_object);
                        System.out.println("-> "+ ((lt_returnValue == null) ? "null" : lt_returnValue.toString()));
                        if (lt_returnValue != null) {
                            lr_value.put(extractFieldName(lt_m.getName()),lt_returnValue);
                        }
                    } catch (IllegalAccessException e) {
                        throw new RuntimeException(e);
                    } catch (InvocationTargetException e) {
                        throw new RuntimeException(e);
                    }

                }
            }

        }
        return lr_value;
    }
}
