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
                        String.format("Unsupported FHIR version: %s", resource.getStructureFhirVersionEnum()));
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

        String cqlContent = getCqlContent(resourcePath, cqlPath);
        cqlContentAttacher.accept(resource, cqlContent);
    }

    static String getCqlContent(Path rootPath, String relativePath) {
        var path = rootPath.resolve(relativePath).normalize();
        try {
            return new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new ResourceNotFoundException(String.format("Unable to read CQL content from path: %s", path));
        }
    }
}
