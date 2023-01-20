package com.adobe.campaign.tests.service;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class MainContainer {
    private static final Logger log = LogManager.getLogger();
    public static void main(String[] args) {

        IntegroAPI iapi = new IntegroAPI();

        if (args.length == 0) {
            log.info("In Prod Mode - SSL");
            System.setProperty("https.protocols", "TLSv1.2");
            ConfigValueHandler.SSL_ACTIVE.activate("true");
            ConfigValueHandler.SSL_KEYSTORE_PATH.activate("/home/app/certificates/campaignkeystore.jks");
            ConfigValueHandler.SSL_KEYSTORE_PASSWORD.activate("#nlpass");
            ConfigValueHandler.TEST_CHECK.activate("in production");
            iapi.startServices();
        } else if (args[0].equalsIgnoreCase("test")) {
            log.info("In Test Mode");
            ConfigValueHandler.TEST_CHECK.activate("in test");
            iapi.startServices();
        } else {
            ConfigValueHandler.TEST_CHECK.activate("in test");
            log.error("You need to pass the argument 'test' for this to work, or provide the valie keystores.");
        }



    }
}
