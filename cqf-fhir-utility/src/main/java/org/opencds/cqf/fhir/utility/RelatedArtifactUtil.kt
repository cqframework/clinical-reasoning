package org.opencds.cqf.fhir.utility

import ca.uhn.fhir.context.FhirVersionEnum
import ca.uhn.fhir.rest.server.exceptions.UnprocessableEntityException
import org.hl7.fhir.exceptions.FHIRException

object RelatedArtifactUtil {
    @JvmStatic
    fun <T : Enum<*>> getRelatedArtifactType(code: String?, fhirVersion: FhirVersionEnum): T? {
        try {
            @Suppress("UNCHECKED_CAST")
            return when (fhirVersion) {
                FhirVersionEnum.DSTU3 ->
                    org.hl7.fhir.dstu3.model.RelatedArtifact.RelatedArtifactType.fromCode(code)

                FhirVersionEnum.R4 ->
                    org.hl7.fhir.r4.model.RelatedArtifact.RelatedArtifactType.fromCode(code)

                FhirVersionEnum.R5 ->
                    org.hl7.fhir.r5.model.RelatedArtifact.RelatedArtifactType.fromCode(code)

                else -> throw UnprocessableEntityException("Unsupported version: $fhirVersion")
            }
                as T?
        } catch (e: FHIRException) {
            throw UnprocessableEntityException("Invalid related artifact code")
        }
    }
}
