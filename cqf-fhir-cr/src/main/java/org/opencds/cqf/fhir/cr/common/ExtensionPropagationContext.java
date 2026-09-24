package org.opencds.cqf.fhir.cr.common;

import static java.util.Objects.requireNonNull;

import ca.uhn.fhir.context.FhirVersionEnum;

/**
 * Describes an automatic extension copy during definition application.
 * Paths identify element definitions (for example, {@code RequestGroup.action}),
 * without instance indexes. The FHIR version describes the resources, not the operation name.
 */
public record ExtensionPropagationContext(
        String extensionUrl, FhirVersionEnum fhirVersion, String sourcePath, String targetPath) {
    public ExtensionPropagationContext {
        requireNonNull(extensionUrl, "extensionUrl can not be null");
        requireNonNull(fhirVersion, "fhirVersion can not be null");
        requireNonNull(sourcePath, "sourcePath can not be null");
        requireNonNull(targetPath, "targetPath can not be null");
    }
}
