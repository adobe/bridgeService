/*
 * Copyright 2022 Adobe
 * All Rights Reserved.
 *
 * NOTICE: Adobe permits you to use, modify, and distribute this file in
 * accordance with the terms of the Adobe license agreement accompanying
 * it.
 */
import com.adobe.campaign.tests.bridge.service.ConfigValueHandlerIBS;
import com.adobe.campaign.tests.bridge.service.IntegroAPI;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class MainContainer {
    private static final Logger log = LogManager.getLogger();
    public static final int PROD_PORT = 443;
    public static final int TEST_PORT = 8080;

    public static void main(String[] args) {

        if (args.length == 0) {
            //TODO instead you args[0].equalsIgnoreCase("prod")
            log.info("In Prod Mode - SSL");
            System.setProperty("https.protocols", "TLSv1.2");

            ConfigValueHandlerIBS.DEPLOYMENT_MODEL.activate(" - in production");
            IntegroAPI.startServices(PROD_PORT);
        } else if (args[0].equalsIgnoreCase("test")) {
            log.info("In Test Mode");

            IntegroAPI.startServices(TEST_PORT);
        } else {

            log.error("You need to pass the argument 'test' for this to work, or provide the key store values.");
        }
    }
}
