package org.opencds.cqf.fhir.utility.client.terminology

import ca.uhn.fhir.context.FhirVersionEnum
import java.util.Optional
import org.hl7.fhir.instance.model.api.IBaseResource
import org.hl7.fhir.instance.model.api.IDomainResource
import org.opencds.cqf.fhir.utility.adapter.IEndpointAdapter
import org.opencds.cqf.fhir.utility.adapter.IParametersAdapter
import org.opencds.cqf.fhir.utility.adapter.IValueSetAdapter
import org.opencds.cqf.fhir.utility.client.TerminologyServerClientSettings

interface ITerminologyProviderRouter : ITerminologyProvider {
    fun expand(
        valueSet: IValueSetAdapter?,
        endpoints: MutableList<IEndpointAdapter?>?,
        parameters: IParametersAdapter?,
    ): IBaseResource?

    fun expand(
        endpoints: MutableList<IEndpointAdapter?>?,
        parameters: IParametersAdapter?,
        fhirVersion: FhirVersionEnum?,
    ): IBaseResource?

    fun expand(
        endpoints: MutableList<IEndpointAdapter?>?,
        parameters: IParametersAdapter?,
        url: String?,
        valueSetVersion: String?,
        fhirVersion: FhirVersionEnum?,
    ): IBaseResource?

    fun getValueSetResource(
        endpoints: MutableList<IEndpointAdapter?>?,
        url: String?,
    ): Optional<IDomainResource>

    fun getCodeSystemResource(
        endpoints: MutableList<IEndpointAdapter?>?,
        url: String?,
    ): Optional<IDomainResource>

    fun getLatestValueSetResource(
        endpoints: MutableList<IEndpointAdapter?>?,
        url: String?,
    ): Optional<IDomainResource>

    fun getTerminologyServerClientSettings(
        endpoints: IEndpointAdapter?
    ): TerminologyServerClientSettings?

    // Methods accepting ArtifactEndpointConfiguration for CRMI-compliant routing
    /**
     * Expands a ValueSet using CRMI artifact endpoint configuration routing. Endpoints are
     * prioritized based on artifactRoute matching the ValueSet's canonical URL.
     *
     * @param valueSet the ValueSet to expand
     * @param configurations list of artifact endpoint configurations
     * @param parameters expansion parameters
     * @return the expanded ValueSet, or null if expansion fails
     */
    fun expandWithConfigurations(
        valueSet: IValueSetAdapter?,
        configurations: MutableList<ArtifactEndpointConfiguration?>?,
        parameters: IParametersAdapter?,
    ): IBaseResource?

    /**
     * Gets a ValueSet resource using CRMI artifact endpoint configuration routing.
     *
     * @param configurations list of artifact endpoint configurations
     * @param url the canonical URL of the ValueSet
     * @return the ValueSet if found
     */
    fun getValueSetResourceWithConfigurations(
        configurations: MutableList<ArtifactEndpointConfiguration?>?,
        url: String?,
    ): Optional<IDomainResource>

    /**
     * Gets a CodeSystem resource using CRMI artifact endpoint configuration routing.
     *
     * @param configurations list of artifact endpoint configurations
     * @param url the canonical URL of the CodeSystem
     * @return the CodeSystem if found
     */
    fun getCodeSystemResourceWithConfigurations(
        configurations: MutableList<ArtifactEndpointConfiguration?>?,
        url: String?,
    ): Optional<IDomainResource>
}
