/*
 * Copyright 2022 Adobe
 * All Rights Reserved.
 *
 * NOTICE: Adobe permits you to use, modify, and distribute this file in
 * accordance with the terms of the Adobe license agreement accompanying
 * it.
 */
package com.adobe.campaign.tests.bridge.service;

import java.util.Arrays;

public enum ConfigValueHandlerIBS {
    DEPLOYMENT_MODEL("IBS.DEPLOYMENT.MODEL", " - in test", false, "This property is used for flagging the deplyment model."),
    SSL_ACTIVE("IBS.SSL.ACTIVE", "false", false, "This property is used to flag if the system is in SSL mode."),
    SSL_KEYSTORE_PATH("IBS.SSL.KEYSTORE_PATH", null, false,
            "This property is used to flag the location of the key store."),
    SSL_KEYSTORE_PASSWORD("IBS.SSL.KEYSTORE_PWD", null, false,
            "This property is to be used for setting the password for the keystore"),
    SSL_TRUSTSTORE_PATH("IBS.SSL.TRUSTSTORE_PATH", null, false,
            "This property is used to flag the location of the trust store."),
    SSL_TRUSTSTORE_PASSWORD("IBS.SSL.STORESTORE_PWD", null, false,
            "This property is to be used for setting the password for the trust store"),
    DEFAULT_SERVICE_PORT("IBS.SERVICE_CHECK.DEFAULT.PORT", "80", false,
            "The default port to be used when doing a service check."),
    //Make required
    ENVIRONMENT_VARS_SETTER_CLASS("IBS.ENVVARS.SETTER.CLASS",
            "com.adobe.campaign.tests.integro.core.SystemValueHandler", false,
            "When set, we use the given class to store the static execution variables."),
    //Make required
    ENVIRONMENT_VARS_SETTER_METHOD("IBS.ENVVARS.SETTER.METHOD", "setIntegroCache", false,
            "When set, we use the given method to store the static execution variables."),
    STATIC_INTEGRITY_PACKAGES("IBS.CLASSLOADER.STATIC.INTEGRITY.PACKAGES", "", false,
            "This parameter is used for flagging the packages that are to to be used by the IBS class loader. When used, the static variables are not stored between java calls."),
    PRODUCT_VERSION("IBS.PRODUCT.VERSION","not found", false, "The version of the BridgeService, which is used to identify the version that is accessed."),
    PRODUCT_USER_VERSION("IBS.PRODUCT.USER.VERSION","not set", false, "The version of the BridgeService, which is used to identify the version that is accessed."),
    AUTOMATIC_INTEGRITY_PACKAGE_INJECTION(
            "IBS.CLASSLOADER.AUTOMATIC.INTEGRITY.INJECTION", "true", false, "When true, we include the called class package in the path. This allows for maintaining static variables in the call."),
    PRODUCT_DEPLOYMENT_URL("IBS.DEPLOYMENT.URL","https://acc-simulators-ibs.rd.campaign.adobe.com/", false, "The URL of the deployment of IBS."),
    DEFAULT_CALL_TIMEOUT("IBS.TIMEOUT.DEFAULT"
            ,"10000" , false, "This value sets a default timeout. If set to '0' we wait indefinitely.");

    public final String systemName;
    public final String defaultValue;
    public final boolean requiredValue;
    public final String description;

    ConfigValueHandlerIBS(String in_propertyName, String in_defaultValue, boolean in_requiredValue, String in_description) {
        systemName = in_propertyName;
        defaultValue = in_defaultValue;
        requiredValue = in_requiredValue;
        description = in_description;
    }

    /**
     * Resets all of the values
     */
    public static void resetAllValues() {
        Arrays.stream(values()).forEach(ConfigValueHandlerIBS::reset);
    }

    /**
     * Returns the value for our config element. If not in system, we return the default value.
     *
     * @return The string value of the given property
     */
    public String fetchValue() {
        return System.getProperty(this.systemName, this.defaultValue);
    }

    /**
     * Sets the given value to our property
     *
     * @param in_value the value to be used for setting the environment variable.
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
     * Checks if this config value is set
     *
     * @return true if the value for our config item is in the system
     */
    public boolean isSet() {
        return System.getProperties().containsKey(this.systemName);
    }

    /**
     * Compares the value using equalsIgnoreCase
     *
     * @param in_value The value to compare the environment variable
     * @return true if the given value is the same as the set one.
     */
    public boolean is(String in_value) {
        return this.fetchValue().equalsIgnoreCase(in_value);
    }

}
