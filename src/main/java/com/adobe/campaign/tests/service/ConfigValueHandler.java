/*
 * MIT License
 *
 * © Copyright 2020 Adobe. All rights reserved.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package com.adobe.campaign.tests.service;

import java.util.Arrays;

public enum ConfigValueHandler {
    TEST_CHECK("SYSTEM_CHECK","", false),
    SSL_ACTIVE("IBS.SSL.ACTIVE","false", false),
    SSL_KEYSTORE_PATH( "IBS.SSL.KEYSTORE_PATH", null, false ),
    SSL_KEYSTORE_PASSWORD( "IBS.SSL.KEYSTORE_PWD", null, false ),
    SSL_TRUSTSTORE_PATH( "IBS.SSL.TRUSTSTORE_PATH", null, false ),
    SSL_TRUSTSTORE_PASSWORD( "IBS.SSL.STORESTORE_PWD", null, false )
    ;
    public final String defaultValue;
    public final String systemName;
    public final boolean requiredValue;

    ConfigValueHandler(String in_propertyName, String in_defaultValue, boolean in_requiredValue) {
        systemName =in_propertyName;
        defaultValue=in_defaultValue;
        requiredValue=in_requiredValue;
    }

    /**
     * Returns the value for our config element. If not in system, we return the default value.
     * @return The string value of the given property
     */
    public String fetchValue() {
        return System.getProperty(this.systemName, this.defaultValue);
    }

    /**
     * Sets the given value to our property
     * @param in_value
     */
    public void activate(String in_value) {
        System.setProperty(this.systemName, in_value);
    }

    /**
     * removed the given value from the system
     */
    public void reset() {
        System.clearProperty(this.systemName);
    }

    /**
     * Resets all of the values
     */
    public static void resetAllValues() {
        Arrays.stream(values()).forEach(ConfigValueHandler::reset);
    }

    /**
     * Checks if this config value is set
     * @return true if the value for our config item is in the system
     */
    public boolean isSet() {
        return System.getProperties().containsKey(this.systemName);
    }

    /**
     * Compares the value using equalsIgnoreCase
     * @param in_value
     * @return true if the given value is the same as the set one.
     */
    public boolean is(String in_value) {
        return this.fetchValue().equalsIgnoreCase(in_value);
    }
}
