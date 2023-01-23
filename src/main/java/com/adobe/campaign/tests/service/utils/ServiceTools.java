package com.adobe.campaign.tests.service.utils;

import com.adobe.campaign.tests.service.ConfigValueHandler;

public class ServiceTools {

    /**
     * Extracts the main path of a given DNS. We also accept DNS without protocol
     * @param in_url The full or partial URL. Examples: 'https://adobe.com:80', 'adobe.com:80' or 'https://adobe.com/support'
     * @return The DNS path in the above example adobe.com
     */
    public static String getIPPath(String in_url) {
        if (in_url==null || in_url.isEmpty()) {
            return null;
        }

        String l_pathWithoutProtocol = stripProtocol(in_url);

        String l_pathWithoutPort = l_pathWithoutProtocol.indexOf(':')>=0 ? l_pathWithoutProtocol.substring(0,l_pathWithoutProtocol.indexOf(':')) : l_pathWithoutProtocol;

        if (l_pathWithoutPort.isEmpty()) {
            return null;
        }

        return l_pathWithoutPort.indexOf('/')>=0?l_pathWithoutPort.substring(0, l_pathWithoutPort.indexOf('/')) : l_pathWithoutPort;
    }

    /**
     * removes protocol from url
     * @param in_url The full or partial URL. Examples: 'https://adobe.com:80', 'adobe.com:80' or 'https://adobe.com/support'
     * @return the DNS without protocol Examples: 'adobe.com:80', 'adobe.com:80' or 'adobe.com/support'
     */
    private static String stripProtocol(String in_url) {
        return in_url.contains(":/") ? in_url.substring(in_url.indexOf("://")+3) : in_url;
    }

    /**
     * Extracts the port of a given DNS. We also accept DNS without protocol
     *
     * @param in_url The full or partial URL. Examples: 'https://adobe.com:80', 'adobe.com:80' or
     *               'https://adobe.com/support'
     * @return The port provided in the DNS path without the protocol. We return null if the given value is null or
     * empty.
     */
    public static Integer getPort(String in_url) {
        if (in_url==null || in_url.isEmpty()) {
            return null;
        }
        String l_pathWithoutProtocol = stripProtocol(in_url);
        String l_pathWithoutDNS = l_pathWithoutProtocol.indexOf(':')>=0 ? l_pathWithoutProtocol.substring(l_pathWithoutProtocol.indexOf(':')+1) : ConfigValueHandler.DEFAULT_SERVICE_PORT.fetchValue();
        return Integer.parseInt(l_pathWithoutDNS.indexOf('/')>=0?l_pathWithoutDNS.substring(0, l_pathWithoutDNS.indexOf('/')) : l_pathWithoutDNS);
    }


}
