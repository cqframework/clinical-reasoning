package org.opencds.cqf.fhir.utility

import ca.uhn.fhir.context.FhirVersionEnum
import org.hl7.fhir.instance.model.api.IBaseBackboneElement
import org.hl7.fhir.instance.model.api.IBaseResource
import org.opencds.cqf.fhir.utility.adapter.IAdapterFactory
import org.opencds.cqf.fhir.utility.adapter.IParametersParameterComponentAdapter

object EndpointHelper {
    @JvmStatic
    fun getEndpoint(param: IParametersParameterComponentAdapter): IBaseResource? {
        return if (param.hasResource()) param.resource
        else newEndpointResource(param.fhirContext().version.version, param.primitiveValue)
    }

    @JvmStatic
    fun getEndpoint(fhirVersion: FhirVersionEnum, param: IBaseBackboneElement?): IBaseResource? {
        if (param == null) {
            return null
        }
        val adapter = IAdapterFactory.forFhirVersion(fhirVersion).createParametersParameter(param)
        return getEndpoint(adapter)
    }

    private fun newEndpointResource(
        fhirVersion: FhirVersionEnum,
        address: String?,
    ): IBaseResource? {
        return when (fhirVersion) {
            FhirVersionEnum.DSTU3 -> org.hl7.fhir.dstu3.model.Endpoint().setAddress(address)
            FhirVersionEnum.R4 -> org.hl7.fhir.r4.model.Endpoint().setAddress(address)
            FhirVersionEnum.R5 -> org.hl7.fhir.r5.model.Endpoint().setAddress(address)
            else -> null
        }
    }
}
