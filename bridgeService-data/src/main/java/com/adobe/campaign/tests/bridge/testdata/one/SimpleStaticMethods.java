/*
 * Copyright 2022 Adobe
 * All Rights Reserved.
 *
 * NOTICE: Adobe permits you to use, modify, and distribute this file in
 * accordance with the terms of the Adobe license agreement accompanying
 * it.
 */
package com.adobe.campaign.tests.bridge.testdata.one;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SimpleStaticMethods {

    public static final String SUCCESS_VAL = "_Success";

    /**
     * Returns the success string constant used for testing.
     *
     * @return the string {@value #SUCCESS_VAL}
     */
    public static String methodReturningString() {
        return SUCCESS_VAL;
    }

    /**
     * Returns a fixed list of four test strings.
     *
     * @return {@code ["NA1", "NA2", "NA3", "NA4"]}
     */
    public static List<String> methodReturningList() {
        return Arrays.asList("NA1", "NA2", "NA3", "NA4");
    }

    /**
     * Returns a {@link ClassWithGet} instance.
     * Used to test scraping of objects that have a method named {@code get}.
     *
     * @return a new {@link ClassWithGet}
     */
    public static ClassWithGet returnClassWithGet() {
        return new ClassWithGet();
    }

    /**
     * Returns a fixed map containing two string entries.
     *
     * @return a map with keys {@code "object1"} and {@code "object3"} mapped to
     *         {@code "value1"} and {@code "value3"} respectively
     */
    public static Map methodReturningMap() {
        Map mapOfString = new HashMap();
        mapOfString.put("object1", "value1");
        mapOfString.put("object3", "value3");

        return mapOfString;
    }

    /**
     * Appends the success suffix to the given string.
     *
     * @param in_stringArgument the input string
     * @return {@code in_stringArgument + "_Success"}
     */
    public static String methodAcceptingStringArgument(String in_stringArgument) {
        return in_stringArgument + SUCCESS_VAL;
    }

    /**
     * Returns the given integer multiplied by three.
     *
     * @param in_intArgument the input integer
     * @return {@code in_intArgument * 3}
     */
    public static int methodAcceptingIntArgument(int in_intArgument) {
        return in_intArgument * 3;
    }

    /**
     * Concatenates two strings with a {@code +} separator and appends the success suffix.
     *
     * @param in_stringArgument1 the first string
     * @param in_stringArgument2 the second string
     * @return {@code in_stringArgument1 + "+" + in_stringArgument2 + "_Success"}
     */
    public static String methodAcceptingTwoArguments(String in_stringArgument1, String in_stringArgument2) {
        return in_stringArgument1 + "+" + in_stringArgument2 + SUCCESS_VAL;
    }

    /**
     * Returns the number of elements in the given list.
     *
     * @param in_ListArgument the input list
     * @return the size of {@code in_ListArgument}
     */
    public static int methodAcceptingListArguments(List<String> in_ListArgument) {
        return in_ListArgument.size();
    }

    /**
     * Returns the length of the given string array.
     *
     * @param in_arrayArgument the input array
     * @return the length of {@code in_arrayArgument}
     */
    public static int methodAcceptingArrayArguments(String[] in_arrayArgument) {
        return in_arrayArgument.length;
    }

    /**
     * Always throws an {@link IllegalArgumentException}.
     * Used to test error handling in the bridge layer.
     *
     * @return never returns normally
     * @throws IllegalArgumentException unconditionally
     */
    public static String methodThrowsException() {
        throw new IllegalArgumentException("Will always throw this");
    }

    /**
     * Delegates to {@link #methodThrowsException()} and propagates the exception.
     * Used to test nested exception handling.
     *
     * @return never returns normally
     * @throws IllegalArgumentException always
     */
    public static String methodCallingMethodThrowingException() {
        return methodThrowsException();
    }

    /**
     * Reads the environment variables {@code ENVVAR1} and {@code ENVVAR2} from the
     * IBS environment variable cache and returns them joined with an underscore.
     * Requires those variables to have been set via {@code environmentVariables} in the
     * call payload before this method is invoked.
     *
     * @return {@code ENVVAR1 + "_" + ENVVAR2}
     */
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

    /**
     * Throws an {@link IllegalArgumentException} if the two integer values are equal.
     * Used to test conditional exception handling.
     *
     * @param in_value1 first value
     * @param in_value2 second value
     * @throws IllegalArgumentException if {@code in_value1 == in_value2}
     */
    public static void methodThrowingException(int in_value1, int in_value2) {
        if (in_value1 == in_value2) {
            throw new IllegalArgumentException("We do not allow numbers that are equal.");
        }
    }

    /**
     * Sleeps for the given number of milliseconds.
     * Used to test timeout enforcement in the bridge layer.
     *
     * @param in_sleepDuration sleep duration in milliseconds
     */
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

    /**
     * Reads and returns the full text content of the given file using UTF-8 encoding.
     *
     * @param fileObject the file to read
     * @return the file contents as a string
     * @throws IOException if the file cannot be read
     */
    public static String methodAcceptingFile(File fileObject) throws IOException {
        return Files.readString(fileObject.toPath(), StandardCharsets.UTF_8);
    }

    //Issue #176
    public int methodAcceptingStringAndArray(String stringObject, String[] arrayObject) {
        return stringObject.length() + arrayObject.length;
    }

}
