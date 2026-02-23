package org.opencds.cqf.fhir.cr.visitor;

/**
 * Interface for downloading packages from a package registry.
 * This allows for mocking in tests without hitting the real package registry.
 */
public interface PackageDownloader {
    /**
     * Downloads a package from the registry.
     *
     * @param packageUrl the full URL to download the package from
     * @return the package data as bytes, or null if download failed
     */
    byte[] download(String packageUrl);

    /**
     * Default implementation that downloads from the real package registry.
     */
    static PackageDownloader defaultDownloader() {
        return new DefaultPackageDownloader();
    }
}
