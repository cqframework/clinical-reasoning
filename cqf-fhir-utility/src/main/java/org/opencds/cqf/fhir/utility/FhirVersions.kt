package org.opencds.cqf.fhir.utility

import ca.uhn.fhir.context.FhirVersionEnum
import org.hl7.fhir.instance.model.api.IBase

object FhirVersions {
    /**
     * Returns a FhirVersionEnum for a given BaseType
     *
     * @param <BaseType> an IBase type
     * @param baseTypeClass the class of the resource to get the version for
     * @return the FhirVersionEnum corresponding to the baseTypeClass </BaseType>
     */
    @JvmStatic
    fun <BaseType : IBase?> forClass(baseTypeClass: Class<out BaseType?>): FhirVersionEnum {
        val packageName = baseTypeClass.`package`.name
        return when {
            packageName.contains("r5") -> FhirVersionEnum.R5
            packageName.contains("r4") -> FhirVersionEnum.R4
            packageName.contains("dstu3") -> FhirVersionEnum.DSTU3
            packageName.contains("dstu2016may") -> FhirVersionEnum.DSTU2_1
            packageName.contains("org.hl7.fhir.dstu2") -> FhirVersionEnum.DSTU2_HL7ORG
            packageName.contains("ca.uhn.fhir.model.dstu2") -> FhirVersionEnum.DSTU2
            else ->
                throw IllegalArgumentException(
                    "Unable to determine FHIR version for IBaseResource type: ${baseTypeClass.name}"
                )
        }
    }
}
