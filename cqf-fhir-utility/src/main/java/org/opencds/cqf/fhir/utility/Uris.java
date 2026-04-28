package org.opencds.cqf.fhir.utility;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

public class Uris {
    private Uris() {}

    public static boolean isUri(String uri) {
        if (uri == null) {
            return false;
        }

        return uri.startsWith("file:/") || uri.matches("\\w+?://.*");
    }

    public static boolean isFileUri(String uri) {
        if (uri == null) {
            return false;
        }

        return uri.startsWith("file") || !uri.matches("\\w+?://.*");
    }

    public static String ensureHttps(String urlString) throws MalformedURLException, URISyntaxException {
        URL url = URI.create(urlString).toURL();

        // Check if the protocol is already HTTPS
        if ("https".equalsIgnoreCase(url.getProtocol())) {
            return urlString;
        }

        // Construct a new URL with the HTTPS protocol
        URI httpsUrl = new URI(
                "https", // scheme
                null, // userinfo
                url.getHost(), // host
                url.getPort(), // port
                url.getFile(), // path
                null, // query
                null // fragment
                );
        return httpsUrl.toString();
    }
}
