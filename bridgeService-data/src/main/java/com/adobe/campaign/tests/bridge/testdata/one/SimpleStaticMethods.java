/*
 * Copyright 2022 Adobe
 * All Rights Reserved.
 *
 * NOTICE: Adobe permits you to use, modify, and distribute this file in
 * accordance with the terms of the Adobe license agreement accompanying
 * it.
 */
package com.adobe.campaign.tests.bridge.testdata.one;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SimpleStaticMethods {

    public static final String SUCCESS_VAL = "_Success";

    public static String methodReturningString() {
        return SUCCESS_VAL;
    }

    public static List<String> methodReturningList() {
        return Arrays.asList("NA1", "NA2", "NA3", "NA4");
    }

    //This method returns a class with a method called get. This was causing issue #63
    public static ClassWithGet returnClassWithGet() {
        return  new ClassWithGet();
    }


    public static Map methodReturningMap() {
        Map mapOfString = new HashMap();
        mapOfString.put("object1", "value1");
        mapOfString.put("object3", "value3");

        return  mapOfString;
    }

    public static String methodAcceptingStringArgument(String in_stringArgument) {
        return in_stringArgument + SUCCESS_VAL;
    }

    public static int methodAcceptingIntArgument(int in_intArgument) {
        return in_intArgument *3;
    }

    public static String methodAcceptingTwoArguments(String in_stringArgument1, String in_stringArgument2) {
        return in_stringArgument1 + "+" + in_stringArgument2 + SUCCESS_VAL;
    }

    public static String methodThrowsException() {
        throw new IllegalArgumentException("Will always throw this");
    }

    public static String methodCallingMethodThrowingException() {
        return methodThrowsException();
    }

    public static String usesEnvironmentVariables() {
        return EnvironmentVariableHandler.getCacheProperty("ENVVAR1") + "_"
                + EnvironmentVariableHandler.getCacheProperty("ENVVAR2");
    }

    //For ambiguous exceptions
    //EmailClientTools.fetchEmail(String, String, boolean)
    public static String overLoadedMethod1Arg(String in_stringArgument) {
        return in_stringArgument + SUCCESS_VAL;
    }

    public static String overLoadedMethod1Arg(int in_intArgument) {
        return in_intArgument + SUCCESS_VAL;
    }

    //For impossible Objects exception
    public static String complexMethodAcceptor(Instantiable in_arg) {
        return SUCCESS_VAL;
    }

    //Exceptions
    //DateAndTimeTools.convertStringToDate
    public static void methodThrowingException(int in_value1, int in_value2) {
        if (in_value1==in_value2) {
            throw new IllegalArgumentException("We do not allow numbers that are equal.");
        }
    }

    //Timeouts
    public static void methodWithTimeOut(long in_sleepDuration) {
        try {
            Thread.sleep(in_sleepDuration);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    //Issue #34
    public static void methodThrowingLinkageError() {
        throw new LinkageError("This is for tests");
    }

    public static void methodCallingMethodThrowingExceptionAndPackingIt() {
        try {
            methodThrowsException();
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException("Got that one", e);
        }
    }
}
