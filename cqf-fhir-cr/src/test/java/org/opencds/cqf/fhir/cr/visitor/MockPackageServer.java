package org.opencds.cqf.fhir.cr.visitor;

import ca.uhn.fhir.context.FhirContext;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.GZIPOutputStream;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.hl7.fhir.instance.model.api.IBaseResource;

/**
 * Mock package server for testing package fetching without hitting packages.fhir.org.
 *
 * This utility creates mock tar.gz packages from FHIR resources for testing purposes.
 */
public class MockPackageServer {
    private final FhirContext fhirContext;
    private final Map<String, byte[]> packages = new HashMap<>();

    public MockPackageServer(FhirContext fhirContext) {
        this.fhirContext = fhirContext;
    }

    /**
     * Registers a package with the mock server.
     *
     * @param packageId the package ID (e.g., "hl7.fhir.us.core")
     * @param version the package version (e.g., "6.1.0")
     * @param resources the resources to include in the package
     * @return this for chaining
     */
    public MockPackageServer registerPackage(String packageId, String version, IBaseResource... resources) {
        try {
            byte[] packageData = createPackage(resources);
            String key = packageId + "/" + version;
            packages.put(key, packageData);
            return this;
        } catch (IOException e) {
            throw new RuntimeException("Failed to create mock package", e);
        }
    }

    /**
     * Gets the package data for a given package ID and version.
     *
     * @param packageId the package ID
     * @param version the package version
     * @return the package data as a tar.gz byte array, or null if not found
     */
    public byte[] getPackage(String packageId, String version) {
        String key = packageId + "/" + version;
        return packages.get(key);
    }

    /**
     * Creates a tar.gz package from FHIR resources.
     */
    private byte[] createPackage(IBaseResource... resources) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        try (GZIPOutputStream gzos = new GZIPOutputStream(baos);
                TarArchiveOutputStream taos = new TarArchiveOutputStream(gzos)) {

            taos.setLongFileMode(TarArchiveOutputStream.LONGFILE_POSIX);

            // Add package.json
            addPackageJson(taos);

            // Add each resource as a JSON file in the package/
            int index = 0;
            for (IBaseResource resource : resources) {
                String json = fhirContext.newJsonParser().setPrettyPrint(false).encodeResourceToString(resource);
                String resourceType = resource.fhirType();
                String id = resource.getIdElement() != null
                                && resource.getIdElement().hasIdPart()
                        ? resource.getIdElement().getIdPart()
                        : "resource-" + index;

                String filename = "package/" + resourceType + "-" + id + ".json";
                addFileToTar(taos, filename, json.getBytes(StandardCharsets.UTF_8));
                index++;
            }

            taos.finish();
        }

        return baos.toByteArray();
    }

    /**
     * Adds a minimal package.json to the tar archive.
     */
    private void addPackageJson(TarArchiveOutputStream taos) throws IOException {
        String packageJson = "{\n  \"name\": \"test-package\",\n  \"version\": \"1.0.0\"\n}";
        addFileToTar(taos, "package/package.json", packageJson.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Adds a file to the tar archive.
     */
    private void addFileToTar(TarArchiveOutputStream taos, String filename, byte[] content) throws IOException {
        TarArchiveEntry entry = new TarArchiveEntry(filename);
        entry.setSize(content.length);
        taos.putArchiveEntry(entry);
        taos.write(content);
        taos.closeArchiveEntry();
    }
}
