package org.opencds.cqf.fhir.utility.repository.ig;

import static java.util.Objects.requireNonNull;

import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.hl7.fhir.instance.model.api.IBaseResource;

class CqlContent {

    private CqlContent() {
        // intentionally empty
    }

    static void loadCqlContent(IBaseResource resource, Path resourcePath) {
        requireNonNull(resource, "resource can not be null");
        requireNonNull(resourcePath, "resourcePath can not be null");

        if (!"Library".equals(resource.fhirType())) {
            return;
        }

        String cqlPath = getCqlPath(resource);
        if (cqlPath == null) {
            return;
        }

        String cqlContent = getCqlContent(resourcePath.toString(), cqlPath);
        attachCqlContent(resource, cqlContent);
    }

    private static String getCqlPath(IBaseResource resource) {
        switch (resource.getStructureFhirVersionEnum()) {
            case DSTU3:
                return org.opencds.cqf.fhir.utility.dstu3.AttachmentUtil.getCqlLocation(resource);
            case R4:
                return org.opencds.cqf.fhir.utility.r4.AttachmentUtil.getCqlLocation(resource);
            case R5:
                return org.opencds.cqf.fhir.utility.r5.AttachmentUtil.getCqlLocation(resource);
            default:
                throw new IllegalArgumentException(
                        String.format("Unsupported FHIR version: %s", resource.getStructureFhirVersionEnum()));
        }
    }

    private static void attachCqlContent(IBaseResource resource, String cqlContent) {
        switch (resource.getStructureFhirVersionEnum()) {
            case DSTU3:
                org.opencds.cqf.fhir.utility.dstu3.AttachmentUtil.addData(resource, cqlContent);
                break;
            case R4:
                org.opencds.cqf.fhir.utility.r4.AttachmentUtil.addData(resource, cqlContent);
                break;
            case R5:
                org.opencds.cqf.fhir.utility.r5.AttachmentUtil.addData(resource, cqlContent);
                break;
            default:
                throw new IllegalArgumentException(
                        String.format("Unsupported FHIR version: %s", resource.getStructureFhirVersionEnum()));
        }
    }

    static String getCqlContent(String rootPath, String relativePath) {
        var path = Paths.get(rootPath).resolve(relativePath).normalize();
        try {
            return new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new ResourceNotFoundException(String.format("Unable to read CQL content from path: %s", path));
        }
    }
}
