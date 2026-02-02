package org.opencds.cqf.fhir.cr.visitor;

/**
 * Test implementation of PackageDownloader that uses MockPackageServer instead of hitting packages.fhir.org.
 */
public class TestPackageDownloader implements PackageDownloader {
    private final MockPackageServer mockServer;

    public TestPackageDownloader(MockPackageServer mockServer) {
        this.mockServer = mockServer;
    }

    @Override
    public byte[] download(String packageUrl) {
        // Parse the URL to extract package ID and version
        // Expected format: https://packages.fhir.org/{packageId}/{version}
        String[] parts = packageUrl.split("/");
        if (parts.length < 2) {
            return null;
        }

        String version = parts[parts.length - 1];
        String packageId = parts[parts.length - 2];

        // Handle "latest" version by mapping to a specific version in tests
        if ("latest".equals(version)) {
            // For now, return null for "latest" - tests should specify explicit versions
            return null;
        }

        return mockServer.getPackage(packageId, version);
    }
}
