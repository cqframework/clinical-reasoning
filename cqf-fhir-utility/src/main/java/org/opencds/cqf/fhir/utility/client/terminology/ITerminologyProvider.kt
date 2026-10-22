package org.opencds.cqf.fhir.utility.client.terminology

import ca.uhn.fhir.context.FhirVersionEnum
import ca.uhn.fhir.rest.client.api.IGenericClient
import java.util.Optional
import org.hl7.fhir.instance.model.api.IBaseParameters
import org.hl7.fhir.instance.model.api.IBaseResource
import org.hl7.fhir.instance.model.api.IDomainResource
import org.hl7.fhir.r4.model.TerminologyCapabilities
import org.opencds.cqf.fhir.utility.adapter.IEndpointAdapter
import org.opencds.cqf.fhir.utility.adapter.IParametersAdapter
import org.opencds.cqf.fhir.utility.adapter.IValueSetAdapter

interface ITerminologyProvider {
    fun expand(
        valueSet: IValueSetAdapter?,
        endpoint: IEndpointAdapter?,
        parameters: IParametersAdapter?,
    ): IBaseResource?

    fun expand(
        endpoint: IEndpointAdapter?,
        parameters: IParametersAdapter?,
        fhirVersion: FhirVersionEnum?,
    ): IBaseResource?

    fun expand(
        endpoint: IEndpointAdapter?,
        parameters: IParametersAdapter?,
        url: String?,
        valueSetVersion: String?,
        fhirVersion: FhirVersionEnum?,
    ): IBaseResource?

    fun expand(
        fhirClient: IGenericClient?,
        url: String?,
        parameters: IBaseParameters?,
    ): IBaseResource?

    val codeSystemClass: Class<out IBaseResource>?

    fun getCodeSystemResource(endpoint: IEndpointAdapter?, url: String?): Optional<IDomainResource>

    fun getLatestValueSetResource(
        endpoint: IEndpointAdapter?,
        url: String?,
    ): Optional<IDomainResource>

    fun getR4TerminologyCapabilities(endpoint: IEndpointAdapter?): TerminologyCapabilities?

    val valueSetClass: Class<out IBaseResource>?

    fun getValueSetResource(endpoint: IEndpointAdapter?, url: String?): Optional<IDomainResource>
}
