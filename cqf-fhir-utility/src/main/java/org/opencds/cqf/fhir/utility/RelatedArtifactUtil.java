package org.opencds.cqf.fhir.utility;

import ca.uhn.fhir.context.FhirVersionEnum;
import ca.uhn.fhir.rest.server.exceptions.UnprocessableEntityException;
import org.hl7.fhir.exceptions.FHIRException;

public class RelatedArtifactUtil {

    private RelatedArtifactUtil() {}

    @SuppressWarnings({"rawtypes", "unchecked"})
    public static <T extends Enum> T getRelatedArtifactType(String code, FhirVersionEnum fhirVersion) {
        try {
            switch (fhirVersion) {
                case DSTU3 -> {
                    return (T) org.hl7.fhir.dstu3.model.RelatedArtifact.RelatedArtifactType.fromCode(
                        code);
                }
                case R4 -> {
                    return (T) org.hl7.fhir.r4.model.RelatedArtifact.RelatedArtifactType.fromCode(code);
                }
                case R5 -> {
                    return (T) org.hl7.fhir.r5.model.RelatedArtifact.RelatedArtifactType.fromCode(code);
                }
                default -> {
                    throw new UnprocessableEntityException(
                        "Unsupported version: " + fhirVersion.toString());
                }
            }
        } catch (FHIRException e) {
            throw new UnprocessableEntityException("Invalid related artifact code");
        }
    }

}
