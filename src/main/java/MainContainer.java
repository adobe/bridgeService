import com.adobe.campaign.tests.service.ConfigValueHandler;
import com.adobe.campaign.tests.service.IntegroAPI;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.util.Properties;

import static com.adobe.campaign.tests.service.ConfigValueHandler.PRODUCT_VERSION;

public class MainContainer {
    private static final Logger log = LogManager.getLogger();
    public static final int PROD_PORT = 443;
    public static final int TEST_PORT = 8080;
    public static void main(String[] args) {

        if (args.length == 0) {
            log.info("In Prod Mode - SSL");
            System.setProperty("https.protocols", "TLSv1.2");
            
            IntegroAPI.startServices(PROD_PORT);
        } else if (args[0].equalsIgnoreCase("test")) {
            log.info("In Test Mode");
            ConfigValueHandler.DEPLOYMENT_MODEL.activate("in test");
            IntegroAPI.startServices(TEST_PORT);
        } else {
            ConfigValueHandler.DEPLOYMENT_MODEL.activate("in test");
            log.error("You need to pass the argument 'test' for this to work, or provide the value keystores.");
        }
    }
}
