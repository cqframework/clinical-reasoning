package org.opencds.cqf.fhir.utility.client.terminology;

import ca.uhn.fhir.context.FhirVersionEnum;
import java.util.List;
import java.util.Optional;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.instance.model.api.IDomainResource;
import org.opencds.cqf.fhir.utility.adapter.IEndpointAdapter;
import org.opencds.cqf.fhir.utility.adapter.IParametersAdapter;
import org.opencds.cqf.fhir.utility.adapter.IValueSetAdapter;
import org.opencds.cqf.fhir.utility.client.TerminologyServerClientSettings;

public interface ITerminologyProviderRouter extends ITerminologyProvider {

    IBaseResource expand(IValueSetAdapter valueSet, List<IEndpointAdapter> endpoints, IParametersAdapter parameters);

    IBaseResource expand(List<IEndpointAdapter> endpoints, IParametersAdapter parameters, FhirVersionEnum fhirVersion);

    IBaseResource expand(
            List<IEndpointAdapter> endpoints,
            IParametersAdapter parameters,
            String url,
            String valueSetVersion,
            FhirVersionEnum fhirVersion);

    Optional<IDomainResource> getValueSetResource(List<IEndpointAdapter> endpoints, String url);

    Optional<IDomainResource> getCodeSystemResource(List<IEndpointAdapter> endpoints, String url);

    Optional<IDomainResource> getLatestValueSetResource(List<IEndpointAdapter> endpoints, String url);

    TerminologyServerClientSettings getTerminologyServerClientSettings(IEndpointAdapter endpoints);

    // Methods accepting ArtifactEndpointConfiguration for CRMI-compliant routing

    /**
     * Expands a ValueSet using CRMI artifact endpoint configuration routing.
     * Endpoints are prioritized based on artifactRoute matching the ValueSet's canonical URL.
     *
     * @param valueSet the ValueSet to expand
     * @param configurations list of artifact endpoint configurations
     * @param parameters expansion parameters
     * @return the expanded ValueSet, or null if expansion fails
     */
    IBaseResource expandWithConfigurations(
            IValueSetAdapter valueSet,
            List<ArtifactEndpointConfiguration> configurations,
            IParametersAdapter parameters);

    /**
     * Gets a ValueSet resource using CRMI artifact endpoint configuration routing.
     *
     * @param configurations list of artifact endpoint configurations
     * @param url the canonical URL of the ValueSet
     * @return the ValueSet if found
     */
    Optional<IDomainResource> getValueSetResourceWithConfigurations(
            List<ArtifactEndpointConfiguration> configurations, String url);

    /**
     * Gets a CodeSystem resource using CRMI artifact endpoint configuration routing.
     *
     * @param configurations list of artifact endpoint configurations
     * @param url the canonical URL of the CodeSystem
     * @return the CodeSystem if found
     */
    Optional<IDomainResource> getCodeSystemResourceWithConfigurations(
            List<ArtifactEndpointConfiguration> configurations, String url);
}
