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
     * A getter for the Language Encodings
     * @return
     *

    public static LanguageEncodings getLanguageEncoding(){
    return languageEncoding;
    }
     */
    public static List<String> getCountries() {
        return countries;
    }



    public static Random getRandomGen() {
        return randomGen;
    }


    /**
     * This method returns a random country ISOA2 code.
     *
     * @return ISOA2 country code
     *
     * @author lepolles
     */
    public static String fetchRandomCountry() {
        int l_countryNr = countries.size();

        return countries.get(getRandomGen().nextInt(l_countryNr));
    }

}
