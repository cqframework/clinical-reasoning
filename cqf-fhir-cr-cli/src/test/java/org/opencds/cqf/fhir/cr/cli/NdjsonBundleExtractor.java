package org.opencds.cqf.fhir.cr.cli;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.GZIPInputStream;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Resource;

public class NdjsonBundleExtractor {

    private static final FhirContext fhirContext = FhirContext.forR4();
    private static final IParser parser = fhirContext.newJsonParser().setPrettyPrint(true);

    public static void extractBundlesToDirs(Path ndjsonGzFile, Path outputRootDir) throws IOException {
        try (BufferedReader reader =
                new BufferedReader(new InputStreamReader(new GZIPInputStream(Files.newInputStream(ndjsonGzFile))))) {

            int bundleIndex = 0;
            String line;
            while ((line = reader.readLine()) != null) {
                Bundle bundle = (Bundle) parser.parseResource(line);

                if (bundle.getType() != Bundle.BundleType.TRANSACTION) {
                    System.err.println("⚠️ Skipping non-transaction bundle at line " + bundleIndex);
                    continue;
                }

                // Identify patient ID for directory name
                String patientIdDir = "bundle" + "%02d".formatted(bundleIndex);
                for (Bundle.BundleEntryComponent entry : bundle.getEntry()) {
                    Resource res = entry.getResource();
                    if (res != null && "Patient".equals(res.getResourceType().name())) {
                        String id = res.getIdElement().getIdPart();
                        if (id != null && !id.isEmpty()) {
                            patientIdDir = sanitizeFilename(id);
                        }
                        break;
                    }
                }

                Path patientDir = outputRootDir.resolve(patientIdDir);
                Files.createDirectories(patientDir);

                for (Bundle.BundleEntryComponent entry : bundle.getEntry()) {
                    Resource resource = entry.getResource();
                    if (resource != null) {
                        String resourceType = resource.getResourceType().name();
                        String resourceId = resource.getIdElement().getIdPart();

                        if (resourceId == null || resourceId.isEmpty()) {
                            System.err.printf("⚠️ Skipping resource with missing ID: %s%n", resourceType);
                            continue;
                        }

                        // Create subdir for resource type
                        Path typeDir = patientDir.resolve(resourceType);
                        Files.createDirectories(typeDir);

                        String filename = sanitizeFilename(resourceId) + ".json";
                        Path filePath = typeDir.resolve(filename);

                        try (BufferedWriter writer = Files.newBufferedWriter(filePath)) {
                            writer.write(parser.encodeResourceToString(resource));
                        }
                    }
                }

                bundleIndex++;
            }
        }

        System.out.println("✅ Bundles extracted and written to: " + outputRootDir);
    }

    private static String sanitizeFilename(String input) {
        return input.replaceAll("[^a-zA-Z0-9._-]", "_"); // replace unsafe characters
    }
}
