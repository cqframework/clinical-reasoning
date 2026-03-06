package org.opencds.cqf.fhir.utility.repository.ig;

import static java.util.Objects.requireNonNull;

import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.BiConsumer;
import java.util.function.Function;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.opencds.cqf.fhir.utility.Libraries;

public class CqlContent {

    private CqlContent() {
        // intentionally empty
    }

    public static void loadCqlContent(IBaseResource resource, Path resourcePath) {
        requireNonNull(resource, "resource can not be null");
        requireNonNull(resourcePath, "resourcePath can not be null");

        if (!"Library".equals(resource.fhirType())) {
            return;
        }

        Function<IBaseResource, String> cqlPathExtractor = null;
        BiConsumer<IBaseResource, String> cqlContentAttacher = null;
        switch (resource.getStructureFhirVersionEnum()) {
            case DSTU3:
                cqlPathExtractor = org.opencds.cqf.fhir.utility.dstu3.AttachmentUtil::getCqlLocation;
                cqlContentAttacher = org.opencds.cqf.fhir.utility.dstu3.AttachmentUtil::addData;
                break;
            case R4:
                cqlPathExtractor = org.opencds.cqf.fhir.utility.r4.AttachmentUtil::getCqlLocation;
                cqlContentAttacher = org.opencds.cqf.fhir.utility.r4.AttachmentUtil::addData;
                break;
            case R5:
                cqlPathExtractor = org.opencds.cqf.fhir.utility.r5.AttachmentUtil::getCqlLocation;
                cqlContentAttacher = org.opencds.cqf.fhir.utility.r5.AttachmentUtil::addData;
                break;
            default:
                throw new IllegalArgumentException(
                        "Unsupported FHIR version: %s".formatted(resource.getStructureFhirVersionEnum()));
        }

        readAndAttachCqlContent(resource, resourcePath, cqlPathExtractor, cqlContentAttacher);
    }

    private static void readAndAttachCqlContent(
            IBaseResource resource,
            Path resourcePath,
            Function<IBaseResource, String> cqlPathExtractor,
            BiConsumer<IBaseResource, String> cqlContentAttacher) {
        String cqlPath = cqlPathExtractor.apply(resource);
        if (cqlPath == null) {
            return;
        }

        String expectedVersion = Libraries.getVersion(resource);
        String expectedName = Libraries.getName(resource);
        String cqlContent = getCqlContent(resourcePath, cqlPath);
        // validateContentMatchesLibrary(cqlContent, expectedVersion, expectedName);
        cqlContentAttacher.accept(resource, cqlContent);
    }

    static void validateContentMatchesLibrary(String cqlContent, String expectedVersion, String expectedName) {
        String nameInContent = getNameFromContent(cqlContent);
        if (expectedName != null && nameInContent != null && !expectedName.equals(nameInContent)) {
            throw new ResourceNotFoundException(
                    "Expected CQL content with library name %s but found %s".formatted(expectedName, nameInContent));
        }

        String versionInContent = getVersionFromContent(cqlContent);
        if (expectedVersion != null && versionInContent != null && !expectedVersion.equals(versionInContent)) {
            throw new ResourceNotFoundException("Expected CQL content with library version %s but found %s"
                    .formatted(expectedVersion, versionInContent));
        }
    }

    static String getCqlContent(Path rootPath, String relativePath) {
        var path = rootPath.resolve(relativePath).normalize();
        try {
            return new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new ResourceNotFoundException("Unable to read CQL content from path: %s".formatted(path));
        }
    }

    static String getNameFromContent(String cqlContent) {
        // Extract library name from CQL content (e.g., "library ExampleLibrary version '1.0.0'")
        if (cqlContent == null || cqlContent.isBlank()) {
            return null;
        }
        var matcher = java.util.regex.Pattern.compile("library\\s+([\\w.]+)").matcher(cqlContent);
        return matcher.find() ? matcher.group(1) : null;
    }

    static String getVersionFromContent(String cqlContent) {
        // Extract version from CQL content (e.g., "library ExampleLibrary version '1.0.0'")
        if (cqlContent == null || cqlContent.isBlank()) {
            return null;
        }
        var matcher = java.util.regex.Pattern.compile("version\\s+'([^']+)'").matcher(cqlContent);
        return matcher.find() ? matcher.group(1) : null;
    }
}
