/*
 * Copyright 2022 Adobe
 * All Rights Reserved.
 *
 * NOTICE: Adobe permits you to use, modify, and distribute this file in
 * accordance with the terms of the Adobe license agreement accompanying
 * it.
 */
package com.adobe.campaign.tests.bridge.testdata.one;


import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Arrays;
import java.util.List;
import java.util.Random;

public class ClassWithLogger {
    protected static Logger log = LogManager.getLogger();

    private static final List<String> countries = Arrays.asList("AT", "AU",
            "CA", "CH", "DE");

    private static Random randomGen = new Random();

    /**
     * Returns the fixed list of ISO 3166-1 alpha-2 country codes available for testing:
     * {@code AT, AU, CA, CH, DE}.
     *
     * @return immutable list of country codes
     */
    public static List<String> getCountries() {
        return countries;
    }

    /**
     * Returns the shared {@link Random} generator used by this class.
     *
     * @return the shared {@link Random} instance
     */
    public static Random getRandomGen() {
        return randomGen;
    }

    /**
     * Returns a randomly selected ISO 3166-1 alpha-2 country code from the available set
     * ({@code AT, AU, CA, CH, DE}).
     *
     * @return a random country code
     */
    public static String fetchRandomCountry() {
        int l_countryNr = countries.size();
        return countries.get(getRandomGen().nextInt(l_countryNr));
    }

}
