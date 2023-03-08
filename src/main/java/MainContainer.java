import com.adobe.campaign.tests.service.ConfigValueHandler;
import com.adobe.campaign.tests.service.IntegroAPI;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.util.Properties;

public class MainContainer {
    private static final Logger log = LogManager.getLogger();
    public static final int PROD_PORT = 443;
    public static final int TEST_PORT = 8080;
    public static void main(String[] args) {

        /*
        Properties properties = new Properties();
        try {
            //properties.load(MainContainer.class.getClassLoader().getResourceAsStream("ibs.properties"));
            //ConfigValueHandler.PRODUCT_VERSION.activate(properties.getProperty(ConfigValueHandler.PRODUCT_VERSION.systemName));
            //ConfigValueHandler.PRODUCT_VERSION.activate(MainContainer.class.getPackage().getImplementationVersion());
        } catch (IOException e) {
            log.error("error");
            throw new RuntimeException(e);
        }
        */

        if (args.length == 0) {
            log.info("In Prod Mode - SSL");
            System.setProperty("https.protocols", "TLSv1.2");

            ConfigValueHandler.DEPLOYMENT_MODEL.activate(" - in production");
            IntegroAPI.startServices(PROD_PORT);
        } else if (args[0].equalsIgnoreCase("test")) {
            log.info("In Test Mode");

            IntegroAPI.startServices(TEST_PORT);
        } else {

            log.error("You need to pass the argument 'test' for this to work, or provide the key store values.");
        }
    }
}
