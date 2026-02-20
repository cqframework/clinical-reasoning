package org.opencds.cqf.fhir.cr.visitor;

import java.net.HttpURLConnection;
import java.net.URL;
import org.opencds.cqf.fhir.utility.adapter.IAdapter;

/**
 * Default implementation of PackageDownloader that downloads from packages.fhir.org.
 */
class DefaultPackageDownloader implements PackageDownloader {
    private static final int CONNECT_TIMEOUT_MS = 10000; // 10 seconds
    private static final int READ_TIMEOUT_MS = 30000; // 30 seconds

    @Override
    public byte[] download(String packageUrl) {
        try {
            URL url = new URL(packageUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(CONNECT_TIMEOUT_MS);
            connection.setReadTimeout(READ_TIMEOUT_MS);
            connection.setRequestProperty("Accept", "application/tar+gzip");

            int responseCode = connection.getResponseCode();
            if (responseCode != HttpURLConnection.HTTP_OK) {
                IAdapter.logger.debug("Package download failed with HTTP {}: {}", responseCode, packageUrl);
                return null;
            }

            // Read package data
            try (var inputStream = connection.getInputStream()) {
                return inputStream.readAllBytes();
            }

        } catch (Exception e) {
            IAdapter.logger.debug("Error downloading package from {}: {}", packageUrl, e.getMessage());
            return null;
        }
    }
}
